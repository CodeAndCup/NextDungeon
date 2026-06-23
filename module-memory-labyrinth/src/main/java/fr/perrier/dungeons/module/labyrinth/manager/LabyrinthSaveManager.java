package fr.perrier.dungeons.module.labyrinth.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.perrier.dungeons.module.labyrinth.model.DifficultyModifier;
import fr.perrier.dungeons.module.labyrinth.model.LabyrinthRun;
import fr.perrier.dungeons.module.labyrinth.model.LabyrinthSave;
import fr.perrier.dungeons.common.model.labyrinth.RewardIcon;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.database.DatabaseManager;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * High-level wrapper around the {@link DatabaseManager} labyrinth save
 * API. Owns the {@link LabyrinthSave} ↔ {@link LabyrinthRun} conversions
 * and the Gson serialization to and from the {@code payload_json} column.
 */
public class LabyrinthSaveManager {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    /** Max resumable saves kept per (party, floor) — oldest pruned beyond this. */
    private static final int MAX_SAVES_PER_PARTY = 3;

    private final Logger logger;

    public LabyrinthSaveManager() {
        this.logger = Main.getInstance().getLogger();
    }

    /**
     * Look up a resumable save for a freshly created run. Matches the save
     * by {@code (partyHash, floorId)} where {@code partyHash} is computed
     * from the run's frozen initial composition.
     *
     * @return future resolving to the save (with verified checksum) or
     *         {@code null} when none / corrupt.
     */
    public CompletableFuture<LabyrinthSave> findSaveForRun(LabyrinthRun run) {
        if (run == null) return CompletableFuture.completedFuture(null);
        DatabaseManager db = Main.getInstance().getDatabaseManager();
        if (db == null) return CompletableFuture.completedFuture(null);
        String partyHash = LabyrinthSave.computePartyHash(run.getInitialPlayerUuids());
        return db.findLabyrinthSaveByPartyHash(partyHash, run.getFloorId())
                .thenApply(this::deserializeAndVerify);
    }

    /**
     * List every resumable save for the run's party (most recent first), with
     * verified checksums. Drives the resume-run selection list.
     */
    public CompletableFuture<List<LabyrinthSave>> findSavesForRun(LabyrinthRun run) {
        if (run == null) return CompletableFuture.completedFuture(List.of());
        DatabaseManager db = Main.getInstance().getDatabaseManager();
        if (db == null) return CompletableFuture.completedFuture(List.of());
        String partyHash = LabyrinthSave.computePartyHash(run.getInitialPlayerUuids());
        return db.findLabyrinthSavesByPartyHash(partyHash, run.getFloorId()).thenApply(payloads -> {
            List<LabyrinthSave> saves = new ArrayList<>();
            for (String json : payloads) {
                LabyrinthSave s = deserializeAndVerify(json);
                if (s != null) saves.add(s);
            }
            return saves;
        });
    }

    /**
     * Apply a verified {@link LabyrinthSave} onto a fresh run. Restores
     * tier, seed, accumulated icon counts and sets {@code currentRoomIndex}
     * to {@code lastBossClearedRoom} so the next pick proposes
     * {@code lastBossClearedRoom + 1} (combat).
     */
    public void applyResume(LabyrinthRun run, LabyrinthSave save) {
        if (run == null || save == null) return;
        if (run.getCurrentModifier() == null) {
            run.setCurrentModifier(new DifficultyModifier(save.getDifficultyTier()));
        } else {
            run.getCurrentModifier().setTier(Math.max(1, save.getDifficultyTier()));
        }
        run.setSeed(save.getSeed());
        run.getIconCounts().clear();
        if (save.getIconCounts() != null) {
            run.getIconCounts().putAll(save.getIconCounts());
        }
        run.setCurrentRoomIndex(save.getLastBossClearedRoom());
        run.setInfiniteSaveId(save.getId());
    }

    /**
     * Persist the run as an Infinite checkpoint at boss kill time. Writes
     * a new save row on first call and updates it on subsequent boss
     * kills (the DB layer is upsert-on-id).
     */
    public CompletableFuture<Void> upsert(LabyrinthRun run) {
        if (run == null || !run.isInfinite()) return CompletableFuture.completedFuture(null);
        DatabaseManager db = Main.getInstance().getDatabaseManager();
        if (db == null) return CompletableFuture.completedFuture(null);

        LabyrinthSave save = new LabyrinthSave();
        save.setId(run.getInfiniteSaveId() != null ? run.getInfiniteSaveId() : UUID.randomUUID().toString());
        save.setFloorId(run.getFloorId());
        save.setPartyHash(LabyrinthSave.computePartyHash(run.getInitialPlayerUuids()));
        save.setPlayerUuids(new ArrayList<>(run.getInitialPlayerUuids()));
        save.setLastBossClearedRoom(run.getCurrentRoomIndex());
        save.setDifficultyTier(run.getCurrentModifier() != null ? run.getCurrentModifier().getTier() : 1);
        save.setSeed(run.getSeed());
        save.setIconCounts(new EnumMap<>(run.getIconCounts()));
        long now = System.currentTimeMillis();
        if (run.getInfiniteSaveId() == null) save.setCreatedAt(now);
        save.setUpdatedAt(now);
        save.recomputeChecksum();

        run.setInfiniteSaveId(save.getId());

        String payload = GSON.toJson(save);
        String partyHash = save.getPartyHash();
        String floorId = save.getFloorId();
        return db.saveLabyrinthSave(save.getId(), floorId, partyHash, payload)
                .thenCompose(v -> pruneToCap(db, partyHash, floorId));
    }

    /**
     * Keep at most {@link #MAX_SAVES_PER_PARTY} saves per (party, floor),
     * deleting the oldest beyond the cap. Called after each checkpoint write.
     */
    private CompletableFuture<Void> pruneToCap(DatabaseManager db, String partyHash, String floorId) {
        return db.findLabyrinthSavesByPartyHash(partyHash, floorId).thenCompose(payloads -> {
            if (payloads == null || payloads.size() <= MAX_SAVES_PER_PARTY) {
                return CompletableFuture.completedFuture(null);
            }
            // Payloads are ordered most-recent-first — delete everything past the cap.
            CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
            for (int i = MAX_SAVES_PER_PARTY; i < payloads.size(); i++) {
                String id = parseId(payloads.get(i));
                if (id == null) continue;
                chain = chain.thenCompose(x -> db.deleteLabyrinthSave(id));
            }
            return chain;
        });
    }

    /** Best-effort id extraction (no checksum verification — used for pruning). */
    private String parseId(String json) {
        try {
            LabyrinthSave s = GSON.fromJson(json, LabyrinthSave.class);
            return s != null ? s.getId() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Drop the run's save (group wipe / leader picked « Nouvelle partie »
     * over an existing save).
     */
    public CompletableFuture<Void> deleteForRun(LabyrinthRun run) {
        if (run == null) return CompletableFuture.completedFuture(null);
        DatabaseManager db = Main.getInstance().getDatabaseManager();
        if (db == null || run.getInfiniteSaveId() == null) {
            return CompletableFuture.completedFuture(null);
        }
        String id = run.getInfiniteSaveId();
        run.setInfiniteSaveId(null);
        return db.deleteLabyrinthSave(id);
    }

    /**
     * Variant used when we know a save exists for a partyHash but no
     * {@code infiniteSaveId} was bound yet on the run (e.g. the leader
     * picked « Nouvelle partie » before resume was applied).
     */
    public CompletableFuture<Void> deleteByPartyHash(String partyHash, String floorId) {
        DatabaseManager db = Main.getInstance().getDatabaseManager();
        if (db == null || partyHash == null) return CompletableFuture.completedFuture(null);
        return db.findLabyrinthSaveByPartyHash(partyHash, floorId).thenCompose(json -> {
            if (json == null) return CompletableFuture.completedFuture(null);
            try {
                LabyrinthSave save = GSON.fromJson(json, LabyrinthSave.class);
                if (save == null || save.getId() == null) return CompletableFuture.completedFuture(null);
                return db.deleteLabyrinthSave(save.getId());
            } catch (Exception e) {
                logger.warning("[MemoryLabyrinth] Cannot parse save for delete-by-party: " + e.getMessage());
                return CompletableFuture.completedFuture(null);
            }
        });
    }

    private LabyrinthSave deserializeAndVerify(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            LabyrinthSave save = GSON.fromJson(json, LabyrinthSave.class);
            if (save == null) return null;
            if (!save.verifyChecksum()) {
                logger.severe("[MemoryLabyrinth] Save checksum mismatch — refusing resume for "
                        + save.getId());
                return null;
            }
            return save;
        } catch (Exception e) {
            logger.warning("[MemoryLabyrinth] Cannot parse save payload: " + e.getMessage());
            return null;
        }
    }
}
