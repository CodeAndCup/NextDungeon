package fr.perrier.dungeons.bungee.dashboard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.perrier.dungeons.bungee.NextDungeonBungee;
import fr.perrier.dungeons.common.model.dungeon.FloorData;
import fr.perrier.dungeons.common.model.dungeon.FloorMetadata;
import fr.perrier.dungeons.common.model.dungeon.Step;
import fr.perrier.dungeons.common.model.dungeon.config.Requirements;
import fr.perrier.dungeons.common.model.dungeon.config.Rules;
import fr.perrier.dungeons.common.model.dungeon.config.WorldConfig;
import fr.perrier.dungeons.common.utils.CuboidRegion;
import fr.perrier.dungeons.common.utils.Position;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RKeys;
import org.redisson.api.RMap;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.util.*;
import java.util.logging.Logger;

/**
 * Service de gestion des donjons via le dashboard (BungeeCord).
 *
 * Stockage des DungeonEntry :
 *   - Clé Redis : "{topic}:dd:{dungeonId}"  (RBucket<String>, StringCodec)
 *   - Valeur    : JSON brut produit par Gson
 *   → Aucun codec Redisson complexe, aucun problème de sérialisation de clé.
 */
public class DungeonManagementService {

    private final RedissonClient redissonClient;
    private final Gson gson = new GsonBuilder().create();
    private final Logger logger = NextDungeonBungee.getInstance().getLogger();

    private final String topic;
    private final String ddPrefix;       // "{topic}:dd:"
    private final String floorMap;       // "{topic}:floors"
    private final String floorMetaMap;   // "{topic}:floor_metadata"
    private final String syncChannel;    // "{topic}:sync"

    public DungeonManagementService(RedissonClient redissonClient) {
        this(redissonClient, "nextdungeon");
    }

    public DungeonManagementService(RedissonClient redissonClient, String topic) {
        this.redissonClient = redissonClient;
        this.topic          = topic;
        this.ddPrefix       = topic + ":dd:";
        this.floorMap       = topic + ":floors";
        this.floorMetaMap   = topic + ":floor_metadata";
        this.syncChannel    = topic + ":sync";
        logger.info("[DungeonMgmtService] topic='" + topic + "' ddPrefix='" + ddPrefix + "'");
    }

    // ── helpers RBucket ──────────────────────────────────────

    /** Clé Redis pour un donjon : "{topic}:dd:{id}" */
    private String ddKey(String dungeonId) { return ddPrefix + dungeonId; }

    /** Lit un DungeonEntry depuis Redis (StringCodec). */
    private DungeonEntry readEntry(String dungeonId) {
        RBucket<String> b = redissonClient.getBucket(ddKey(dungeonId), StringCodec.INSTANCE);
        String json = b.get();
        if (json == null) return null;
        try { return gson.fromJson(json, DungeonEntry.class); }
        catch (Exception e) { logger.warning("[dd] parse error for " + dungeonId + ": " + e.getMessage()); return null; }
    }

    /** Écrit un DungeonEntry dans Redis (StringCodec). */
    private void writeEntry(DungeonEntry entry) {
        redissonClient.getBucket(ddKey(entry.getId()), StringCodec.INSTANCE)
                      .set(gson.toJson(entry));
    }

    /** Supprime un DungeonEntry de Redis. */
    private void deleteEntry(String dungeonId) {
        redissonClient.getBucket(ddKey(dungeonId), StringCodec.INSTANCE).delete();
    }

    /** Liste tous les dungeonIds connus dans Redis (scan des clés "{topic}:dd:*"). */
    private List<String> listDashboardDungeonIds() {
        List<String> ids = new ArrayList<>();
        RKeys keys = redissonClient.getKeys();
        Iterable<String> it = keys.getKeysByPattern(ddPrefix + "*");
        for (String k : it) {
            String extracted = k.substring(ddPrefix.length());
            if (extracted.contains(":")) {
                logger.warning("[dd] suppression clé Redis corrompue: " + k);
                redissonClient.getBucket(k, StringCodec.INSTANCE).delete();
                continue;
            }
            ids.add(extracted);
        }
        return ids;
    }

    // =========================================================
    //  Dungeon CRUD
    // =========================================================

    public String getAllDungeonsJson() {
        try {
            RMap<String, FloorMetadata> metaMap = redissonClient.getMap(floorMetaMap);

            // 1. Donjons connus via le dashboard (RBucket StringCodec)
            Map<String, DungeonEntry> allDungeons = new LinkedHashMap<>();
            for (String id : listDashboardDungeonIds()) {
                DungeonEntry de = readEntry(id);
                if (de != null) {
                    System.out.println("[getAllDungeons] dashboard entry id='" + de.getId() + "'");
                    allDungeons.put(de.getId(), de);
                }
            }

            // 2. Donjons déduits depuis floor_metadata (floors Spigot)
            for (Map.Entry<String, FloorMetadata> e : metaMap.entrySet()) {
                String dungeonId = extractDungeonIdFromMeta(e.getValue());
                if (!allDungeons.containsKey(dungeonId)) {
                    allDungeons.put(dungeonId, new DungeonEntry(dungeonId, dungeonId, "", new ArrayList<>()));
                }
                DungeonEntry de = allDungeons.get(dungeonId);
                if (de.getFloorIds() == null) de.setFloorIds(new ArrayList<>());
                if (!de.getFloorIds().contains(e.getKey())) de.getFloorIds().add(e.getKey());
            }

            JsonArray arr = new JsonArray();
            for (DungeonEntry d : allDungeons.values()) {
                JsonObject o = new JsonObject();
                o.addProperty("id", d.getId());
                o.addProperty("name", d.getName() != null ? d.getName() : d.getId());
                o.addProperty("description", d.getDescription() != null ? d.getDescription() : "");
                o.addProperty("floorCount", d.getFloorIds() != null ? d.getFloorIds().size() : 0);
                arr.add(o);
            }
            JsonObject resp = new JsonObject();
            resp.addProperty("success", true);
            resp.add("dungeons", arr);
            resp.addProperty("total", arr.size());
            return gson.toJson(resp);
        } catch (Exception e) {
            return error("Failed to get dungeons: " + e.getMessage());
        }
    }

    public String getDungeonJson(String dungeonId) {
        try {
            DungeonEntry entry = readEntry(dungeonId);
            if (entry == null) entry = new DungeonEntry(dungeonId, dungeonId, "", new ArrayList<>());

            RMap<String, FloorMetadata> metaMap   = redissonClient.getMap(floorMetaMap);
            RMap<String, FloorData>     floorsMap = redissonClient.getMap(floorMap);

            List<String> floorIds = new ArrayList<>();
            for (Map.Entry<String, FloorMetadata> e : metaMap.entrySet()) {
                if (extractDungeonIdFromMeta(e.getValue()).equals(dungeonId)) floorIds.add(e.getKey());
            }
            if (entry.getFloorIds() != null) {
                for (String fid : entry.getFloorIds()) if (!floorIds.contains(fid)) floorIds.add(fid);
            }

            JsonObject dj = new JsonObject();
            dj.addProperty("id", entry.getId());
            dj.addProperty("name", entry.getName() != null ? entry.getName() : entry.getId());
            dj.addProperty("description", entry.getDescription() != null ? entry.getDescription() : "");
            JsonArray fa = new JsonArray();
            for (String fid : floorIds) { FloorData fd = floorsMap.get(fid); if (fd != null) fa.add(floorDataToJson(fd)); }
            dj.add("floors", fa);

            JsonObject resp = new JsonObject();
            resp.addProperty("success", true);
            resp.add("dungeon", dj);
            return gson.toJson(resp);
        } catch (Exception e) {
            return error("Failed to get dungeon: " + e.getMessage());
        }
    }

    public String createOrUpdateDungeon(String requestBody, boolean isUpdate) {
        try {
            JsonObject body = gson.fromJson(requestBody, JsonObject.class);
            if (body == null) return error("Invalid request body");

            String id = body.has("id") && !body.get("id").isJsonNull() ? body.get("id").getAsString().trim() : "";
            if (isUpdate && id.isEmpty()) return error("Dungeon ID is required for update");
            if (id.isEmpty()) {
                String base = body.has("name") ? body.get("name").getAsString() : "dungeon";
                id = sanitize(base) + "_" + UUID.randomUUID().toString().substring(0, 8);
            }
            String name = body.has("name") ? body.get("name").getAsString().trim() : "";
            if (name.isEmpty()) return error("Dungeon name is required");
            String desc = body.has("description") ? body.get("description").getAsString() : "";

            DungeonEntry entry;
            if (isUpdate) {
                entry = readEntry(id);
                if (entry == null) entry = new DungeonEntry(id, name, desc, new ArrayList<>());
                else { entry.setName(name); entry.setDescription(desc); }
            } else {
                entry = new DungeonEntry(id, name, desc, new ArrayList<>());
            }

            writeEntry(entry);
            logger.info((isUpdate ? "Updated" : "Created") + " dungeon: " + id);
            JsonObject dungeonDataJson = new JsonObject();
            dungeonDataJson.addProperty("id", entry.getId());
            dungeonDataJson.addProperty("name", entry.getName() != null ? entry.getName() : id);
            dungeonDataJson.addProperty("description", entry.getDescription() != null ? entry.getDescription() : "");
            notifySyncChannel("DUNGEON_UPDATE", id, name, desc, dungeonDataJson.toString());

            JsonObject resp = new JsonObject();
            resp.addProperty("success", true);
            resp.addProperty("message", "Dungeon " + (isUpdate ? "updated" : "created"));
            resp.add("dungeon", entryToJson(entry));
            return gson.toJson(resp);
        } catch (Exception e) {
            return error("Failed to save dungeon: " + e.getMessage());
        }
    }

    public String deleteDungeon(String dungeonId) {
        try {
            System.out.println("[deleteDungeon] called with id='" + dungeonId + "'");
            RMap<String, FloorData>     floorsMap = redissonClient.getMap(floorMap);
            RMap<String, FloorMetadata> metaMap   = redissonClient.getMap(floorMetaMap);

            List<String> toRemove = new ArrayList<>();
            for (Map.Entry<String, FloorMetadata> e : metaMap.entrySet()) {
                if (extractDungeonIdFromMeta(e.getValue()).equals(dungeonId)) toRemove.add(e.getKey());
            }
            for (String fid : toRemove) {
                floorsMap.remove(fid);
                metaMap.remove(fid);
                notifySyncChannel("FLOOR_DELETE", fid);
            }
            deleteEntry(dungeonId);

            logger.info("Deleted dungeon: " + dungeonId + " (" + toRemove.size() + " floors)");
            notifySyncChannel("DUNGEON_DELETE", dungeonId);

            JsonObject resp = new JsonObject();
            resp.addProperty("success", true);
            resp.addProperty("message", "Dungeon deleted: " + dungeonId);
            return gson.toJson(resp);
        } catch (Exception e) {
            return error("Failed to delete dungeon: " + e.getMessage());
        }
    }

    // =========================================================
    //  Floor CRUD
    // =========================================================

    public String addFloor(String dungeonId, String requestBody) {
        try {
            JsonObject body = gson.fromJson(requestBody, JsonObject.class);
            if (body == null) return error("Invalid request body");
            String rawId = body.has("id") ? body.get("id").getAsString().trim() : "";
            if (rawId.isEmpty()) return error("Floor ID is required");

            String fullFloorId = dungeonId + "_" + rawId;
            RMap<String, FloorData>     floorsMap = redissonClient.getMap(floorMap);
            RMap<String, FloorMetadata> metaMap   = redissonClient.getMap(floorMetaMap);
            if (floorsMap.containsKey(fullFloorId)) return error("Floor already exists: " + fullFloorId);

            FloorData fd = buildFloorData(fullFloorId, dungeonId, body);
            ValidationResult vr = validateFloor(fd);
            if (!vr.isValid()) return error("Validation failed: " + String.join(", ", vr.getErrors()));

            fd.setUpdatedBy("dashboard-bungee");
            fd.setUpdatedAt(System.currentTimeMillis());
            fd.setChecksum(fd.calculateChecksum());

            floorsMap.fastPut(fullFloorId, fd);
            metaMap.fastPut(fullFloorId, FloorMetadata.from(fd));

            DungeonEntry de = readEntry(dungeonId);
            if (de != null) {
                if (de.getFloorIds() == null) de.setFloorIds(new ArrayList<>());
                if (!de.getFloorIds().contains(fullFloorId)) de.getFloorIds().add(fullFloorId);
                writeEntry(de);
            }

            logger.info("Added floor " + fullFloorId + " to dungeon " + dungeonId);
            String floorDataJson = gson.toJson(fd);
            notifySyncChannel("FLOOR_UPDATE", fullFloorId, fd.getName(), dungeonId, floorDataJson);

            JsonObject resp = new JsonObject();
            resp.addProperty("success", true);
            resp.addProperty("message", "Floor created: " + fullFloorId);
            resp.add("floor", floorDataToJson(fd));
            return gson.toJson(resp);
        } catch (Exception e) {
            return error("Failed to add floor: " + e.getMessage());
        }
    }

    public String updateFloor(String floorId, String requestBody) {
        try {
            RMap<String, FloorData>     floorsMap = redissonClient.getMap(floorMap);
            RMap<String, FloorMetadata> metaMap   = redissonClient.getMap(floorMetaMap);
            FloorData existing = floorsMap.get(floorId);
            if (existing == null) return error("Floor not found: " + floorId);

            JsonObject body = gson.fromJson(requestBody, JsonObject.class);
            if (body == null) return error("Invalid request body");

            String dungeonId = existing.getDungeonId() != null ? existing.getDungeonId() : extractDungeonId(floorId);
            FloorData fd = buildFloorData(floorId, dungeonId, body);
            ValidationResult vr = validateFloor(fd);
            if (!vr.isValid()) return error("Validation failed: " + String.join(", ", vr.getErrors()));

            fd.setVersion(existing.getVersion());
            fd.incrementVersion("dashboard-bungee");
            fd.setChecksum(fd.calculateChecksum());

            floorsMap.fastPut(floorId, fd);
            metaMap.fastPut(floorId, FloorMetadata.from(fd));
            logger.info("Updated floor: " + floorId + " v" + fd.getVersion());
            String dungeonId_upd = fd.getDungeonId() != null ? fd.getDungeonId() : extractDungeonId(floorId);
            String floorDataJson_upd = gson.toJson(fd);
            notifySyncChannel("FLOOR_UPDATE", floorId, fd.getName(), dungeonId_upd, floorDataJson_upd);

            JsonObject resp = new JsonObject();
            resp.addProperty("success", true);
            resp.addProperty("message", "Floor updated: " + floorId);
            resp.add("floor", floorDataToJson(fd));
            return gson.toJson(resp);
        } catch (Exception e) {
            return error("Failed to update floor: " + e.getMessage());
        }
    }

    public String deleteFloor(String dungeonId, String floorId) {
        try {
            RMap<String, FloorData>     floorsMap = redissonClient.getMap(floorMap);
            RMap<String, FloorMetadata> metaMap   = redissonClient.getMap(floorMetaMap);
            if (!floorsMap.containsKey(floorId)) return error("Floor not found: " + floorId);
            floorsMap.remove(floorId);
            metaMap.remove(floorId);

            DungeonEntry de = readEntry(dungeonId);
            if (de != null && de.getFloorIds() != null) {
                de.getFloorIds().remove(floorId);
                writeEntry(de);
            }
            logger.info("Deleted floor: " + floorId);
            notifySyncChannel("FLOOR_DELETE", floorId);

            JsonObject resp = new JsonObject();
            resp.addProperty("success", true);
            resp.addProperty("message", "Floor deleted: " + floorId);
            return gson.toJson(resp);
        } catch (Exception e) {
            return error("Failed to delete floor: " + e.getMessage());
        }
    }

    // =========================================================
    //  Helpers
    // =========================================================

    private String extractDungeonId(String floorId) {
        int idx = floorId.lastIndexOf("_");
        return idx > 0 ? floorId.substring(0, idx) : floorId;
    }

    private String extractDungeonIdFromMeta(FloorMetadata meta) {
        if (meta.getDungeonId() != null && !meta.getDungeonId().isEmpty()) return meta.getDungeonId();
        return extractDungeonId(meta.getId());
    }

    private FloorData buildFloorData(String fullFloorId, String dungeonId, JsonObject body) {
        String name = body.has("name") ? body.get("name").getAsString() : fullFloorId;
        String desc = body.has("description") ? body.get("description").getAsString() : "";
        FloorData fd = new FloorData(fullFloorId, name, desc);
        fd.setDungeonId(dungeonId);

        if (body.has("world") && !body.get("world").isJsonNull()) {
            JsonObject w = body.getAsJsonObject("world");
            String folderName = dungeonId + "_" + fullFloorId;
            String difficulty = w.has("difficulty") ? w.get("difficulty").getAsString() : "NORMAL";
            Position spawn = new Position(0, 64, 0);
            if (w.has("spawn") && !w.get("spawn").isJsonNull()) {
                JsonObject sp = w.getAsJsonObject("spawn");
                spawn = new Position(sp.has("x")?sp.get("x").getAsDouble():0, sp.has("y")?sp.get("y").getAsDouble():64, sp.has("z")?sp.get("z").getAsDouble():0);
            }
            fd.setWorldConfig(new WorldConfig(folderName, difficulty.toUpperCase(), spawn));
        }
        if (body.has("requirements") && !body.get("requirements").isJsonNull()) {
            JsonObject r = body.getAsJsonObject("requirements");
            Requirements req = new Requirements();
            req.setMinLevel(r.has("minLevel") ? r.get("minLevel").getAsInt() : 0);
            req.setRetryCooldown(r.has("retryCooldown") ? r.get("retryCooldown").getAsLong() : 0L);
            if (r.has("requiredFloorsId") && r.get("requiredFloorsId").isJsonArray()) { List<String> l=new ArrayList<>(); r.getAsJsonArray("requiredFloorsId").forEach(e->l.add(e.getAsString())); req.setRequiredFloorsId(l); }
            if (r.has("removeCompletion") && r.get("removeCompletion").isJsonArray()) { List<String> l=new ArrayList<>(); r.getAsJsonArray("removeCompletion").forEach(e->l.add(e.getAsString())); req.setRemoveCompletion(l); }
            if (r.has("requiredItems")    && r.get("requiredItems").isJsonArray())    { List<String> l=new ArrayList<>(); r.getAsJsonArray("requiredItems").forEach(e->l.add(e.getAsString())); req.setRequiredItems(l); }
            if (r.has("forbiddenItems")   && r.get("forbiddenItems").isJsonArray())   { List<String> l=new ArrayList<>(); r.getAsJsonArray("forbiddenItems").forEach(e->l.add(e.getAsString())); req.setForbiddenItems(l); }
            if (r.has("party") && !r.get("party").isJsonNull()) {
                JsonObject p = r.getAsJsonObject("party");
                Requirements.PartyRequirements party = new Requirements.PartyRequirements();
                party.setMinSize(p.has("minSize")?p.get("minSize").getAsInt():1);
                party.setMaxSize(p.has("maxSize")?p.get("maxSize").getAsInt():10);
                req.setPartyRequirements(party);
            }
            fd.setRequirements(req);
        }
        if (body.has("rules") && !body.get("rules").isJsonNull()) {
            JsonObject r = body.getAsJsonObject("rules");
            Rules rules = new Rules();
            rules.setMaxLives(r.has("maxLives")?r.get("maxLives").getAsInt():3);
            rules.setDeathBanDuration(r.has("deathBanDuration")?r.get("deathBanDuration").getAsString():"15m");
            rules.setGamemode(r.has("gamemode")?r.get("gamemode").getAsString():"ADVENTURE");
            rules.setAllowFlight(r.has("allowFlight")&&r.get("allowFlight").getAsBoolean());
            rules.setMaxInstance(r.has("maxInstance")?r.get("maxInstance").getAsInt():10);
            fd.setRules(rules);
        }
        if (body.has("steps") && body.get("steps").isJsonArray()) {
            List<Step> steps = new ArrayList<>();
            body.getAsJsonArray("steps").forEach(se -> {
                JsonObject s = se.getAsJsonObject();
                String sid  = s.has("id")   ? s.get("id").getAsString()   : "step" + steps.size();
                String snam = s.has("name") ? s.get("name").getAsString() : sid;
                steps.add(new Step(sid, snam, new CuboidRegion(readPosition(s.has("pos1")?s.getAsJsonObject("pos1"):null), readPosition(s.has("pos2")?s.getAsJsonObject("pos2"):null))));
            });
            fd.setSteps(steps);
        }
        fd.setTriggers(null);
        return fd;
    }

    private Position readPosition(JsonObject p) {
        if (p == null) return new Position(0, 0, 0);
        return new Position(p.has("x")?p.get("x").getAsDouble():0, p.has("y")?p.get("y").getAsDouble():0, p.has("z")?p.get("z").getAsDouble():0);
    }

    private ValidationResult validateFloor(FloorData fd) {
        List<String> errors = new ArrayList<>();
        if (fd.getId() == null || fd.getId().isBlank()) errors.add("Floor ID is required");
        if (fd.getName() == null || fd.getName().isBlank()) errors.add("Floor name is required");
        if (fd.getWorldConfig() == null) errors.add("World config is required");
        return new ValidationResult(errors.isEmpty(), errors);
    }

    private void notifySyncChannel(String type, String id) {
        notifySyncChannel(type, id, null, null, null);
    }

    private void notifySyncChannel(String type, String id, String name, String description, String data) {
        try {
            RTopic t = redissonClient.getTopic(syncChannel);
            JsonObject msg = new JsonObject();
            msg.addProperty("type", type);
            msg.addProperty("id", id);
            msg.addProperty("sender", "dashboard-bungee");
            if (name != null) msg.addProperty("name", name);
            if (description != null) msg.addProperty("description", description);
            if (data != null) msg.addProperty("data", data);
            t.publish(msg.toString());
        } catch (Exception e) {
            logger.warning("Failed to publish sync: " + e.getMessage());
        }
    }

    private JsonObject entryToJson(DungeonEntry d) {
        JsonObject o = new JsonObject();
        o.addProperty("id", d.getId());
        o.addProperty("name", d.getName() != null ? d.getName() : d.getId());
        o.addProperty("description", d.getDescription() != null ? d.getDescription() : "");
        JsonArray fids = new JsonArray();
        if (d.getFloorIds() != null) d.getFloorIds().forEach(fids::add);
        o.add("floorIds", fids);
        return o;
    }

    public JsonObject floorDataToJson(FloorData fd) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", fd.getId());
        obj.addProperty("name", fd.getName());
        obj.addProperty("description", fd.getDescription() != null ? fd.getDescription() : "");
        if (fd.getWorldConfig() != null) {
            JsonObject w = new JsonObject();
            w.addProperty("folderName", fd.getWorldConfig().getFolderName());
            w.addProperty("difficulty", fd.getWorldConfig().getDifficulty());
            if (fd.getWorldConfig().getSpawn() != null) {
                JsonObject sp = new JsonObject();
                sp.addProperty("x", fd.getWorldConfig().getSpawn().getX());
                sp.addProperty("y", fd.getWorldConfig().getSpawn().getY());
                sp.addProperty("z", fd.getWorldConfig().getSpawn().getZ());
                w.add("spawn", sp);
            }
            obj.add("world", w);
        }
        if (fd.getRequirements() != null) {
            Requirements r = fd.getRequirements();
            JsonObject rj = new JsonObject();
            rj.addProperty("minLevel", r.getMinLevel());
            rj.addProperty("retryCooldown", r.getRetryCooldown());
            JsonArray rfi=new JsonArray(); if(r.getRequiredFloorsId()!=null) r.getRequiredFloorsId().forEach(rfi::add); rj.add("requiredFloorsId",rfi);
            JsonArray rmc=new JsonArray(); if(r.getRemoveCompletion()!=null) r.getRemoveCompletion().forEach(rmc::add); rj.add("removeCompletion",rmc);
            JsonArray ri=new JsonArray();  if(r.getRequiredItems()!=null) r.getRequiredItems().forEach(ri::add); rj.add("requiredItems",ri);
            JsonArray fi=new JsonArray();  if(r.getForbiddenItems()!=null) r.getForbiddenItems().forEach(fi::add); rj.add("forbiddenItems",fi);
            if (r.getPartyRequirements()!=null) { JsonObject p=new JsonObject(); p.addProperty("minSize",r.getPartyRequirements().getMinSize()); p.addProperty("maxSize",r.getPartyRequirements().getMaxSize()); rj.add("party",p); }
            obj.add("requirements", rj);
        }
        if (fd.getRules() != null) {
            Rules r = fd.getRules();
            JsonObject rj = new JsonObject();
            rj.addProperty("maxLives", r.getMaxLives());
            rj.addProperty("deathBanDuration", r.getDeathBanDuration()!=null?r.getDeathBanDuration():"15m");
            rj.addProperty("gamemode", r.getGamemode()!=null?r.getGamemode():"ADVENTURE");
            rj.addProperty("allowFlight", r.isAllowFlight());
            rj.addProperty("maxInstance", r.getMaxInstance());
            obj.add("rules", rj);
        }
        if (fd.getSteps() != null) {
            JsonArray sa = new JsonArray();
            for (Step s : fd.getSteps()) {
                JsonObject so = new JsonObject();
                so.addProperty("id", s.getId());
                so.addProperty("name", s.getName());
                if (s.getRegion()!=null) { so.add("pos1",posToJson(s.getRegion().getPosition1())); so.add("pos2",posToJson(s.getRegion().getPosition2())); }
                sa.add(so);
            }
            obj.add("steps", sa);
        }
        return obj;
    }

    private JsonObject posToJson(Position p) {
        JsonObject o = new JsonObject();
        if (p!=null) { o.addProperty("x",p.getX()); o.addProperty("y",p.getY()); o.addProperty("z",p.getZ()); }
        return o;
    }

    private String sanitize(String s) { return s.toLowerCase().replaceAll("[^a-z0-9_]","_"); }

    private String error(String msg) {
        JsonObject e = new JsonObject();
        e.addProperty("success", false);
        e.addProperty("error", msg);
        return gson.toJson(e);
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class DungeonEntry {
        private String id;
        private String name;
        private String description;
        private List<String> floorIds;
    }

    @Data @AllArgsConstructor
    public static class ValidationResult {
        private boolean valid;
        private List<String> errors;
    }
}

