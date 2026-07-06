package fr.perrier.dungeons.velocity.dashboard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.perrier.dungeons.velocity.NextDungeonVelocity;
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
import org.jspecify.annotations.NonNull;
import org.redisson.api.RBucket;
import org.redisson.api.RKeys;
import org.redisson.api.RMap;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.util.*;

import org.slf4j.Logger;

/**
 * Service de gestion des donjons via le dashboard (Velocity).
 * <p>
 * Stockage des DungeonEntry :
 * - Clé Redis : "{topic}:dd:{dungeonId}"  (RBucket<String>, StringCodec)
 * - Valeur    : JSON brut produit par Gson
 * → Aucun codec Redisson complexe, aucun problème de sérialisation de clé.
 */
public class DungeonManagementService {

    private final RedissonClient redissonClient;
    private final Gson gson = new GsonBuilder().create();
    private final Logger logger = NextDungeonVelocity.getInstance().getLogger();

    private final String ddPrefix;       // "{topic}:dd:"
    private final String floorMap;       // "{topic}:floors"
    private final String floorMetaMap;   // "{topic}:floor_metadata"
    private final String syncChannel;    // "{topic}:sync"

    public DungeonManagementService(RedissonClient redissonClient) {
        this(redissonClient, "nextdungeon");
    }

    public DungeonManagementService(RedissonClient redissonClient, String topic) {
        this.redissonClient = redissonClient;
        this.ddPrefix = topic + ":dd:";
        this.floorMap = topic + ":floors";
        this.floorMetaMap = topic + ":floor_metadata";
        this.syncChannel = topic + ":sync";
        logger.info("[DungeonMgmtService] topic='{}' ddPrefix='{}'", topic, ddPrefix);
    }

    // ── helpers RBucket ──────────────────────────────────────

    /**
     * Clé Redis pour un donjon : "{topic}:dd:{id}"
     */
    private String ddKey(String dungeonId) {
        return ddPrefix + dungeonId;
    }

    /**
     * Lit un DungeonEntry depuis Redis (StringCodec).
     */
    private DungeonEntry readEntry(String dungeonId) {
        RBucket<String> bucket = redissonClient.getBucket(ddKey(dungeonId), StringCodec.INSTANCE);
        String json = bucket.get();
        if (json == null) return null;
        try {
            return gson.fromJson(json, DungeonEntry.class);
        } catch (Exception e) {
            logger.warn("[dd] parse error for {}: {}", dungeonId, e.getMessage());
            return null;
        }
    }

    /**
     * Écrit un DungeonEntry dans Redis (StringCodec).
     */
    private void writeEntry(DungeonEntry entry) {
        redissonClient.getBucket(ddKey(entry.getId()), StringCodec.INSTANCE)
                .set(gson.toJson(entry));
    }

    /**
     * Supprime un DungeonEntry de Redis.
     */
    private void deleteEntry(String dungeonId) {
        redissonClient.getBucket(ddKey(dungeonId), StringCodec.INSTANCE).delete();
    }

    /**
     * Liste tous les dungeonIds connus dans Redis (scan des clés "{topic}:dd:*").
     */
    private List<String> listDashboardDungeonIds() {
        List<String> ids = new ArrayList<>();
        RKeys keys = redissonClient.getKeys();
        Iterable<String> keyIterator = keys.getKeysByPattern(ddPrefix + "*");
        for (String key : keyIterator) {
            String extracted = key.substring(ddPrefix.length());

            // Clé corrompue : un dungeonId valide ne contient jamais ':' (format Maven artifact)
            if (extracted.contains(":")) {
                logger.warn("[dd] suppression clé Redis corrompue: {}", key);
                redissonClient.getBucket(key, StringCodec.INSTANCE).delete();
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

            // 1. Dungeon know from dashboard (RBucket StringCodec)
            Map<String, DungeonEntry> allDungeons = new LinkedHashMap<>();
            for (String id : listDashboardDungeonIds()) {
                DungeonEntry de = readEntry(id);
                if (de != null) {
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

            JsonArray jsonArray = new JsonArray();
            for (DungeonEntry d : allDungeons.values()) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("id", d.getId());
                jsonObject.addProperty("name", d.getName() != null ? d.getName() : d.getId());
                jsonObject.addProperty("description", d.getDescription() != null ? d.getDescription() : "");
                jsonObject.addProperty("floorCount", d.getFloorIds() != null ? d.getFloorIds().size() : 0);
                jsonArray.add(jsonObject);
            }
            JsonObject resp = new JsonObject();
            resp.addProperty("success", true);
            resp.add("dungeons", jsonArray);
            resp.addProperty("total", jsonArray.size());
            return gson.toJson(resp);
        } catch (Exception e) {
            return error("Failed to get dungeons: " + e.getMessage());
        }
    }

    public String getDungeonJson(String dungeonId) {
        try {
            DungeonEntry dungeonEntry = readEntry(dungeonId);
            if (dungeonEntry == null)
                dungeonEntry = new DungeonEntry(dungeonId, dungeonId, "", new ArrayList<>());

            RMap<String, FloorMetadata> metaMap = redissonClient.getMap(floorMetaMap);
            RMap<String, FloorData> floorsMap = redissonClient.getMap(floorMap);

            List<String> floorIds = new ArrayList<>();
            for (Map.Entry<String, FloorMetadata> e : metaMap.entrySet()) {
                if (extractDungeonIdFromMeta(e.getValue()).equals(dungeonId)) floorIds.add(e.getKey());
            }
            if (dungeonEntry.getFloorIds() != null) {
                for (String fid : dungeonEntry.getFloorIds()) if (!floorIds.contains(fid)) floorIds.add(fid);
            }

            JsonObject dungeonObject = new JsonObject();
            dungeonObject.addProperty("id", dungeonEntry.getId());
            dungeonObject.addProperty("name", dungeonEntry.getName() != null ? dungeonEntry.getName() : dungeonEntry.getId());
            dungeonObject.addProperty("description", dungeonEntry.getDescription() != null ? dungeonEntry.getDescription() : "");
            dungeonObject.addProperty("dungeonType",
                    dungeonEntry.getDungeonType() != null ? dungeonEntry.getDungeonType() : "CLASSIC");
            if (dungeonEntry.getLabyrinthDungeonConfig() != null) {
                dungeonObject.add("labyrinthDungeonConfig",
                        gson.toJsonTree(dungeonEntry.getLabyrinthDungeonConfig()));
            }
            JsonArray fa = new JsonArray();
            for (String fid : floorIds) {
                FloorData fd = floorsMap.get(fid);
                if (fd != null) fa.add(floorDataToJson(fd));
            }
            dungeonObject.add("floors", fa);

            JsonObject resp = new JsonObject();
            resp.addProperty("success", true);
            resp.add("dungeon", dungeonObject);
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
            if (isUpdate && id.isEmpty())
                return error("Dungeon ID is required for update");
            if (id.isEmpty()) {
                String base = body.has("name") ? body.get("name").getAsString() : "dungeon";
                id = sanitize(base) + "_" + UUID.randomUUID().toString().substring(0, 8);
            }
            String name = body.has("name") ? body.get("name").getAsString().trim() : "";
            if (name.isEmpty())
                return error("Dungeon name is required");
            String desc = body.has("description") ? body.get("description").getAsString() : "";

            DungeonEntry entry;
            if (isUpdate) {
                entry = readEntry(id);
                if (entry == null)
                    entry = new DungeonEntry(id, name, desc, new ArrayList<>());
                else {
                    entry.setName(name);
                    entry.setDescription(desc);
                }
            } else {
                entry = new DungeonEntry(id, name, desc, new ArrayList<>());
            }

            // Labyrinth fields — optional, only set when the dashboard
            // sends them (CLASSIC dungeons leave them null).
            if (body.has("dungeonType") && !body.get("dungeonType").isJsonNull()) {
                entry.setDungeonType(body.get("dungeonType").getAsString());
            }
            if (body.has("labyrinthDungeonConfig") && !body.get("labyrinthDungeonConfig").isJsonNull()) {
                entry.setLabyrinthDungeonConfig(gson.fromJson(
                        body.get("labyrinthDungeonConfig"),
                        fr.perrier.dungeons.common.model.labyrinth.LabyrinthDungeonConfig.class));
            }

            writeEntry(entry);
            logger.info("{} dungeon: {}", isUpdate ? "Updated" : "Created", id);
            // Spigot persists this payload verbatim into the dungeons table; send the full
            // entry so dungeonType and labyrinthDungeonConfig survive the round trip.
            // Without this LabyrinthRunManager.parseDungeonConfig() returns null and the run aborts.
            JsonObject dungeonDataJson = entryToJson(entry);
            notifySyncChannel("DUNGEON_UPDATE", id, name, desc, dungeonDataJson.toString());

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Dungeon " + (isUpdate ? "updated" : "created"));
            response.add("dungeon", entryToJson(entry));
            return gson.toJson(response);
        } catch (Exception e) {
            return error("Failed to save dungeon: " + e.getMessage());
        }
    }

    public String deleteDungeon(String dungeonId) {
        try {
            RMap<String, FloorData> floorsMap = redissonClient.getMap(floorMap);
            RMap<String, FloorMetadata> metaMap = redissonClient.getMap(floorMetaMap);

            List<String> toRemove = new ArrayList<>();
            for (Map.Entry<String, FloorMetadata> e : metaMap.entrySet()) {
                if (extractDungeonIdFromMeta(e.getValue()).equals(dungeonId))
                    toRemove.add(e.getKey());
            }
            for (String fid : toRemove) {
                floorsMap.remove(fid);
                metaMap.remove(fid);
                notifySyncChannel("FLOOR_DELETE", fid);
            }
            deleteEntry(dungeonId);

            logger.info("Deleted dungeon: {} ({} floors)", dungeonId, toRemove.size());
            notifySyncChannel("DUNGEON_DELETE", dungeonId);

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Dungeon deleted: " + dungeonId);
            return gson.toJson(response);
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
            if (body == null)
                return error("Invalid request body");
            String rawId = body.has("id") ? body.get("id").getAsString().trim() : "";
            if (rawId.isEmpty())
                return error("Floor ID is required");

            String fullFloorId = dungeonId + "_" + rawId;
            RMap<String, FloorData> floorsMap = redissonClient.getMap(floorMap);
            RMap<String, FloorMetadata> metaMap = redissonClient.getMap(floorMetaMap);
            if (floorsMap.containsKey(fullFloorId))
                return error("Floor already exists: " + fullFloorId);

            FloorData fd = buildFloorData(fullFloorId, dungeonId, body);
            ValidationResult validationResult = validateFloor(fd);
            if (!validationResult.isValid())
                return error("Validation failed: " + String.join(", ", validationResult.getErrors()));

            // Fresh floor: version starts at 1. Checksum locks in the payload before it leaves the dashboard.
            fd.setUpdatedBy("dashboard-velocity");
            fd.setUpdatedAt(System.currentTimeMillis());
            fd.setChecksum(fd.calculateChecksum());

            floorsMap.fastPut(fullFloorId, fd);
            metaMap.fastPut(fullFloorId, FloorMetadata.from(fd));

            DungeonEntry dungeonEntry = readEntry(dungeonId);
            if (dungeonEntry != null) {
                if (dungeonEntry.getFloorIds() == null)
                    dungeonEntry.setFloorIds(new ArrayList<>());
                if (!dungeonEntry.getFloorIds().contains(fullFloorId))
                    dungeonEntry.getFloorIds().add(fullFloorId);
                writeEntry(dungeonEntry);
            }

            logger.info("Added floor {} to dungeon {}", fullFloorId, dungeonId);
            String floorDataJson = gson.toJson(fd);
            notifySyncChannel("FLOOR_UPDATE", fullFloorId, fd.getName(), dungeonId, floorDataJson);

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Floor created: " + fullFloorId);
            response.add("floor", floorDataToJson(fd));
            return gson.toJson(response);
        } catch (Exception e) {
            return error("Failed to add floor: " + e.getMessage());
        }
    }

    public String updateFloor(String floorId, String requestBody) {
        try {
            RMap<String, FloorData> floorsMap = redissonClient.getMap(floorMap);
            RMap<String, FloorMetadata> metaMap = redissonClient.getMap(floorMetaMap);
            FloorData existing = floorsMap.get(floorId);
            if (existing == null)
                return error("Floor not found: " + floorId);

            JsonObject body = gson.fromJson(requestBody, JsonObject.class);
            if (body == null)
                return error("Invalid request body");

            String dungeonId = existing.getDungeonId() != null ? existing.getDungeonId() : extractDungeonId(floorId);
            FloorData fd = buildFloorData(floorId, dungeonId, body);
            ValidationResult vr = validateFloor(fd);
            if (!vr.isValid())
                return error("Validation failed: " + String.join(", ", vr.getErrors()));

            // Carry the previous version forward so the listener on the spigot side can compare monotonically.
            fd.setVersion(existing.getVersion());
            fd.incrementVersion("dashboard-velocity");
            fd.setChecksum(fd.calculateChecksum());

            floorsMap.fastPut(floorId, fd);
            metaMap.fastPut(floorId, FloorMetadata.from(fd));
            logger.info("Updated floor: {} v{}", floorId, fd.getVersion());
            String dungeonIdForFloor = fd.getDungeonId() != null ? fd.getDungeonId() : extractDungeonId(floorId);
            String floorDataJson = gson.toJson(fd);
            notifySyncChannel("FLOOR_UPDATE", floorId, fd.getName(), dungeonIdForFloor, floorDataJson);

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Floor updated: " + floorId);
            response.add("floor", floorDataToJson(fd));
            return gson.toJson(response);
        } catch (Exception e) {
            return error("Failed to update floor: " + e.getMessage());
        }
    }

    public String deleteFloor(String dungeonId, String floorId) {
        try {
            RMap<String, FloorData> floorsMap = redissonClient.getMap(floorMap);
            RMap<String, FloorMetadata> metaMap = redissonClient.getMap(floorMetaMap);

            if (!floorsMap.containsKey(floorId))
                return error("Floor not found: " + floorId);

            floorsMap.remove(floorId);
            metaMap.remove(floorId);

            DungeonEntry dungeonEntry = readEntry(dungeonId);
            if (dungeonEntry != null && dungeonEntry.getFloorIds() != null) {
                dungeonEntry.getFloorIds().remove(floorId);
                writeEntry(dungeonEntry);
            }

            logger.info("Deleted floor: {}", floorId);
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
        FloorData floorData = new FloorData(fullFloorId, name, desc);
        floorData.setDungeonId(dungeonId);

        if (body.has("world") && !body.get("world").isJsonNull()) {
            JsonObject worldObject = body.getAsJsonObject("world");
            String folderName = dungeonId + "_" + fullFloorId;
            String difficulty = worldObject.has("difficulty") ? worldObject.get("difficulty").getAsString() : "NORMAL";
            Position spawn = new Position(0, 64, 0);
            if (worldObject.has("spawn") && !worldObject.get("spawn").isJsonNull()) {
                JsonObject spawnObject = worldObject.getAsJsonObject("spawn");
                spawn = new Position(spawnObject.has("x") ? spawnObject.get("x").getAsDouble() : 0, spawnObject.has("y") ? spawnObject.get("y").getAsDouble() : 64, spawnObject.has("z") ? spawnObject.get("z").getAsDouble() : 0,
                        spawnObject.has("yaw") ? spawnObject.get("yaw").getAsFloat() : 0f, spawnObject.has("pitch") ? spawnObject.get("pitch").getAsFloat() : 0f);
            }
            floorData.setWorldConfig(new WorldConfig(folderName, difficulty.toUpperCase(), spawn));
        }
        if (body.has("requirements") && !body.get("requirements").isJsonNull()) {
            JsonObject requirementsObject = body.getAsJsonObject("requirements");
            Requirements req = new Requirements();
            req.setMinLevel(requirementsObject.has("minLevel") ? requirementsObject.get("minLevel").getAsInt() : 0);
            req.setRetryCooldown(requirementsObject.has("retryCooldown") ? requirementsObject.get("retryCooldown").getAsLong() : 0L);
            if (requirementsObject.has("requiredFloorsId") && requirementsObject.get("requiredFloorsId").isJsonArray()) {
                List<String> floorIdList = new ArrayList<>();
                requirementsObject.getAsJsonArray("requiredFloorsId").forEach(e -> floorIdList.add(e.getAsString()));
                req.setRequiredFloorsId(floorIdList);
            }
            if (requirementsObject.has("removeCompletion") && requirementsObject.get("removeCompletion").isJsonArray()) {
                List<String> removeCompletionList = new ArrayList<>();
                requirementsObject.getAsJsonArray("removeCompletion").forEach(e -> removeCompletionList.add(e.getAsString()));
                req.setRemoveCompletion(removeCompletionList);
            }
            if (requirementsObject.has("requiredItems") && requirementsObject.get("requiredItems").isJsonArray()) {
                List<String> requiredItemsList = new ArrayList<>();
                requirementsObject.getAsJsonArray("requiredItems").forEach(e -> requiredItemsList.add(e.getAsString()));
                req.setRequiredItems(requiredItemsList);
            }
            if (requirementsObject.has("nonConsumedItems") && requirementsObject.get("nonConsumedItems").isJsonArray()) {
                List<String> nonConsumedItemsList = new ArrayList<>();
                requirementsObject.getAsJsonArray("nonConsumedItems").forEach(e -> nonConsumedItemsList.add(e.getAsString()));
                req.setNonConsumedItems(nonConsumedItemsList);
            }
            if (requirementsObject.has("forbiddenItems") && requirementsObject.get("forbiddenItems").isJsonArray()) {
                List<String> forbiddenItemsList = new ArrayList<>();
                requirementsObject.getAsJsonArray("forbiddenItems").forEach(e -> forbiddenItemsList.add(e.getAsString()));
                req.setForbiddenItems(forbiddenItemsList);
            }
            if (requirementsObject.has("party") && !requirementsObject.get("party").isJsonNull()) {
                JsonObject partyObject = requirementsObject.getAsJsonObject("party");
                Requirements.PartyRequirements party = new Requirements.PartyRequirements();
                party.setMinSize(partyObject.has("minSize") ? partyObject.get("minSize").getAsInt() : 1);
                party.setMaxSize(partyObject.has("maxSize") ? partyObject.get("maxSize").getAsInt() : 10);
                req.setPartyRequirements(party);
            }
            floorData.setRequirements(req);
        }
        if (body.has("rules") && !body.get("rules").isJsonNull()) {
            JsonObject rulesObject = body.getAsJsonObject("rules");
            Rules rules = new Rules();
            rules.setMaxLives(rulesObject.has("maxLives") ? rulesObject.get("maxLives").getAsInt() : 3);
            rules.setDeathBanDuration(rulesObject.has("deathBanDuration") ? rulesObject.get("deathBanDuration").getAsString() : "15m");
            rules.setGamemode(rulesObject.has("gamemode") ? rulesObject.get("gamemode").getAsString() : "ADVENTURE");
            rules.setAllowFlight(rulesObject.has("allowFlight") && rulesObject.get("allowFlight").getAsBoolean());
            rules.setMaxInstance(rulesObject.has("maxInstance") ? rulesObject.get("maxInstance").getAsInt() : 10);
            floorData.setRules(rules);
        }
        if (body.has("steps") && body.get("steps").isJsonArray()) {
            List<Step> steps = new ArrayList<>();
            body.getAsJsonArray("steps").forEach(jsonElement -> {
                JsonObject stepObject = jsonElement.getAsJsonObject();
                String stepId = stepObject.has("id") ? stepObject.get("id").getAsString() : "step" + steps.size();
                String stepName = stepObject.has("name") ? stepObject.get("name").getAsString() : stepId;
                steps.add(new Step(stepId, stepName, new CuboidRegion(readPosition(stepObject.has("pos1") ? stepObject.getAsJsonObject("pos1") : null), readPosition(stepObject.has("pos2") ? stepObject.getAsJsonObject("pos2") : null))));
            });
            floorData.setSteps(steps);
        }
        // Labyrinth fields — discriminator + per-difficulty inline config.
        // Authoritative source is the parent dungeon's type, not the body: a floor inside
        // a LABYRINTH dungeon is always LABYRINTH (and vice-versa). This prevents the UI
        // from accidentally downgrading a labyrinth floor to CLASSIC and matches the
        // runtime invariant LabyrinthInstanceReadyListener relies on.
        DungeonEntry parentDungeon = readEntry(dungeonId);
        boolean parentIsLabyrinth = parentDungeon != null
                && "LABYRINTH".equalsIgnoreCase(parentDungeon.getDungeonType());
        if (parentIsLabyrinth) {
            floorData.setFloorType(fr.perrier.dungeons.common.model.dungeon.FloorType.LABYRINTH);
            if (body.has("labyrinthFloorConfig") && !body.get("labyrinthFloorConfig").isJsonNull()) {
                floorData.setLabyrinthFloorConfig(gson.fromJson(
                        body.get("labyrinthFloorConfig"),
                        fr.perrier.dungeons.common.model.labyrinth.LabyrinthFloorConfig.class));
            }
        } else {
            floorData.setFloorType(fr.perrier.dungeons.common.model.dungeon.FloorType.CLASSIC);
            floorData.setLabyrinthFloorConfig(null);
        }
        floorData.setTriggers(null);
        return floorData;
    }

    private Position readPosition(JsonObject posObject) {
        if (posObject == null) return new Position(0, 0, 0);
        return new Position(
                posObject.has("x") ? posObject.get("x").getAsDouble() : 0,
                posObject.has("y") ? posObject.get("y").getAsDouble() : 0,
                posObject.has("z") ? posObject.get("z").getAsDouble() : 0
        );
    }

    private ValidationResult validateFloor(FloorData floorData) {
        List<String> errors = new ArrayList<>();
        if (floorData.getId() == null || floorData.getId().isBlank()) errors.add("Floor ID is required");
        if (floorData.getName() == null || floorData.getName().isBlank()) errors.add("Floor name is required");
        if (floorData.getWorldConfig() == null) errors.add("World config is required");
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
            msg.addProperty("sender", "dashboard-velocity");
            if (name != null) msg.addProperty("name", name);
            if (description != null) msg.addProperty("description", description);
            if (data != null) msg.addProperty("data", data);
            t.publish(msg.toString());
        } catch (Exception e) {
            logger.warn("Failed to publish sync: {}", e.getMessage());
        }
    }

    private JsonObject entryToJson(DungeonEntry dungeonEntry) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", dungeonEntry.getId());
        jsonObject.addProperty("name", dungeonEntry.getName() != null ? dungeonEntry.getName() : dungeonEntry.getId());
        jsonObject.addProperty("description", dungeonEntry.getDescription() != null ? dungeonEntry.getDescription() : "");
        JsonArray fids = new JsonArray();
        if (dungeonEntry.getFloorIds() != null) {
            dungeonEntry.getFloorIds().forEach(fids::add);
        }
        jsonObject.add("floorIds", fids);
        jsonObject.addProperty("dungeonType",
                dungeonEntry.getDungeonType() != null ? dungeonEntry.getDungeonType() : "CLASSIC");
        if (dungeonEntry.getLabyrinthDungeonConfig() != null) {
            jsonObject.add("labyrinthDungeonConfig",
                    gson.toJsonTree(dungeonEntry.getLabyrinthDungeonConfig()));
        }
        return jsonObject;
    }

    public JsonObject floorDataToJson(FloorData floorData) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", floorData.getId());
        obj.addProperty("name", floorData.getName());
        obj.addProperty("description", floorData.getDescription() != null ? floorData.getDescription() : "");
        if (floorData.getWorldConfig() != null) {
            JsonObject worldJsonObject = getWorldJsonObject(floorData);
            obj.add("world", worldJsonObject);
        }
        if (floorData.getRequirements() != null) {
            Requirements requirements = floorData.getRequirements();
            JsonObject requirementsObject = new JsonObject();
            requirementsObject.addProperty("minLevel", requirements.getMinLevel());
            requirementsObject.addProperty("retryCooldown", requirements.getRetryCooldown());

            JsonArray requirementsFloorsIdArray = new JsonArray();
            if (requirements.getRequiredFloorsId() != null) {
                requirements.getRequiredFloorsId().forEach(requirementsFloorsIdArray::add);
            }
            requirementsObject.add("requiredFloorsId", requirementsFloorsIdArray);

            JsonArray removeCompletionArray = new JsonArray();
            if (requirements.getRemoveCompletion() != null) {
                requirements.getRemoveCompletion().forEach(removeCompletionArray::add);
            }
            requirementsObject.add("removeCompletion", removeCompletionArray);

            JsonArray requirementsItemsArray = new JsonArray();
            if (requirements.getRequiredItems() != null) {
                requirements.getRequiredItems().forEach(requirementsItemsArray::add);
            }
            requirementsObject.add("requiredItems", requirementsItemsArray);

            JsonArray nonConsumedItemsArray = new JsonArray();
            if (requirements.getNonConsumedItems() != null) {
                requirements.getNonConsumedItems().forEach(nonConsumedItemsArray::add);
            }
            requirementsObject.add("nonConsumedItems", nonConsumedItemsArray);

            JsonArray forbiddenItemsArray = new JsonArray();
            if (requirements.getForbiddenItems() != null) {
                requirements.getForbiddenItems().forEach(forbiddenItemsArray::add);
            }
            requirementsObject.add("forbiddenItems", forbiddenItemsArray);

            if (requirements.getPartyRequirements() != null) {
                JsonObject p = new JsonObject();
                p.addProperty("minSize", requirements.getPartyRequirements().getMinSize());
                p.addProperty("maxSize", requirements.getPartyRequirements().getMaxSize());
                requirementsObject.add("party", p);
            }
            obj.add("requirements", requirementsObject);
        }
        if (floorData.getRules() != null) {
            Rules rules = floorData.getRules();
            JsonObject rulesObject = new JsonObject();
            rulesObject.addProperty("maxLives", rules.getMaxLives());
            rulesObject.addProperty("deathBanDuration", rules.getDeathBanDuration() != null ? rules.getDeathBanDuration() : "15m");
            rulesObject.addProperty("gamemode", rules.getGamemode() != null ? rules.getGamemode() : "ADVENTURE");
            rulesObject.addProperty("allowFlight", rules.isAllowFlight());
            rulesObject.addProperty("maxInstance", rules.getMaxInstance());
            obj.add("rules", rulesObject);
        }
        if (floorData.getSteps() != null) {
            JsonArray stepsArray = new JsonArray();
            for (Step step : floorData.getSteps()) {
                JsonObject stepObject = new JsonObject();
                stepObject.addProperty("id", step.getId());
                stepObject.addProperty("name", step.getName());
                if (step.getRegion() != null) {
                    stepObject.add("pos1", posToJson(step.getRegion().getPosition1()));
                    stepObject.add("pos2", posToJson(step.getRegion().getPosition2()));
                }
                stepsArray.add(stepObject);
            }
            obj.add("steps", stepsArray);
        }
        obj.addProperty("floorType",
                floorData.getFloorType() != null ? floorData.getFloorType().name() : "CLASSIC");
        if (floorData.getLabyrinthFloorConfig() != null) {
            obj.add("labyrinthFloorConfig", gson.toJsonTree(floorData.getLabyrinthFloorConfig()));
        }
        return obj;
    }

    private static @NonNull JsonObject getWorldJsonObject(FloorData fd) {
        JsonObject worldObject = new JsonObject();
        worldObject.addProperty("folderName", fd.getWorldConfig().getFolderName());
        worldObject.addProperty("difficulty", fd.getWorldConfig().getDifficulty());

        if (fd.getWorldConfig().getSpawn() != null) {
            JsonObject spawnObject = new JsonObject();
            spawnObject.addProperty("x", fd.getWorldConfig().getSpawn().getX());
            spawnObject.addProperty("y", fd.getWorldConfig().getSpawn().getY());
            spawnObject.addProperty("z", fd.getWorldConfig().getSpawn().getZ());
            spawnObject.addProperty("yaw", fd.getWorldConfig().getSpawn().getYaw());
            spawnObject.addProperty("pitch", fd.getWorldConfig().getSpawn().getPitch());
            worldObject.add("spawn", spawnObject);
        }
        return worldObject;
    }

    private JsonObject posToJson(Position p) {
        JsonObject jsonObject = new JsonObject();
        if (p != null) {
            jsonObject.addProperty("x", p.getX());
            jsonObject.addProperty("y", p.getY());
            jsonObject.addProperty("z", p.getZ());
        }
        return jsonObject;
    }

    private String sanitize(String string) {
        return string.toLowerCase().replaceAll("[^a-z0-9_]", "_");
    }

    private String error(String msg) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("success", false);
        jsonObject.addProperty("error", msg);
        return gson.toJson(jsonObject);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DungeonEntry {
        private String id;
        private String name;
        private String description;
        private List<String> floorIds;

        /**
         * Discriminator for the dungeon kind. Defaults to {@code CLASSIC}
         * (string, not enum, so the dashboard JSON stays
         * forward-compatible across modules).
         */
        private String dungeonType;

        /**
         * Inline {@link fr.perrier.dungeons.common.model.labyrinth.LabyrinthDungeonConfig}
         * payload when {@code dungeonType == "LABYRINTH"}. Stored as the
         * concrete POJO so Gson roundtrips it transparently.
         */
        private fr.perrier.dungeons.common.model.labyrinth.LabyrinthDungeonConfig labyrinthDungeonConfig;

        /** Legacy four-arg constructor — defaults to a CLASSIC dungeon. */
        public DungeonEntry(String id, String name, String description, List<String> floorIds) {
            this(id, name, description, floorIds, "CLASSIC", null);
        }
    }

    @Data
    @AllArgsConstructor
    public static class ValidationResult {
        private boolean valid;
        private List<String> errors;
    }
}








