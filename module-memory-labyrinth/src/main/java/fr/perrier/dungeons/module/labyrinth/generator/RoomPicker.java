package fr.perrier.dungeons.module.labyrinth.generator;

import fr.perrier.dungeons.module.labyrinth.manager.RoomTemplateRegistry;
import fr.perrier.dungeons.module.labyrinth.model.DoorChoice;
import fr.perrier.dungeons.module.labyrinth.model.LabyrinthRun;
import fr.perrier.dungeons.common.model.labyrinth.RewardIcon;
import fr.perrier.dungeons.common.model.labyrinth.LabyrinthRoom;
import fr.perrier.dungeons.common.model.labyrinth.RoomType;
import fr.perrier.dungeons.spigot.Main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Procedural picker that builds the next {@link DoorChoice} at the end of
 * a cleared room.
 *
 * <p>Picker rules (CDC §6.2) :</p>
 * <ul>
 *   <li>If {@code (currentRoomIndex + 1) % 10 == 0} → next is BOSS, single
 *       door, both sides of the {@link DoorChoice} point to the same room.</li>
 *   <li>Otherwise → 2 distinct COMBAT rooms.</li>
 * </ul>
 *
 * <p>Filtering : candidates must carry the {@code floorId} tag (e.g.
 * {@code "easy"}, {@code "infinite"}). If too few candidates exist, the
 * picker logs a warning and falls back to the same room on both sides
 * (CDC §8 — assumed acceptable).</p>
 *
 * <p>Recent-history avoidance : the last {@link #RECENT_AVOID_WINDOW}
 * room ids are excluded if possible. If the pool is too small to honour
 * this constraint, the avoidance is dropped silently.</p>
 */
public class RoomPicker {

    /** Number of trailing route entries to avoid re-using when proposing. */
    public static final int RECENT_AVOID_WINDOW = 3;

    private final RoomTemplateRegistry registry;
    private final IconRoller iconRoller;
    private final Logger logger;

    public RoomPicker(RoomTemplateRegistry registry, IconRoller iconRoller) {
        this.registry = registry;
        this.iconRoller = iconRoller;
        this.logger = Main.getInstance().getLogger();
    }

    /**
     * Pick the lobby room for a run start. {@code instanceId} keys the
     * per-instance pool loaded by {@link RoomTemplateRegistry#load}.
     */
    public LabyrinthRoom pickLobby(java.util.UUID instanceId, String floorId, String tagFilter) {
        List<LabyrinthRoom> pool = filterByTag(registry.getByType(instanceId, RoomType.LOBBY), tagFilter);
        if (pool.isEmpty()) {
            logger.warning("[MemoryLabyrinth] No LOBBY room available for floor=" + floorId
                    + " (instance=" + instanceId + ")");
            return null;
        }
        return pool.get(rngFor((floorId == null ? 0 : floorId.hashCode()) ^ 1L).nextInt(pool.size()));
    }

    /**
     * Build the next {@link DoorChoice} based on the current run state.
     *
     * @return a populated DoorChoice, or {@code null} if the picker cannot
     *         find any candidate (logged as a warning).
     */
    public DoorChoice pickNext(LabyrinthRun run) {
        if (run == null) return null;
        int nextIndex = run.getCurrentRoomIndex() + 1;
        boolean nextIsBoss = nextIndex > 0 && nextIndex % 10 == 0;

        if (nextIsBoss) {
            return pickBossDoor(run);
        }
        return pickCombatPair(run);
    }

    private DoorChoice pickBossDoor(LabyrinthRun run) {
        List<LabyrinthRoom> pool = filterByTag(
                registry.getByType(run.getInstanceId(), RoomType.BOSS), run.getTagFilter());
        if (pool.isEmpty()) {
            logger.warning("[MemoryLabyrinth] No BOSS room for floor=" + run.getFloorId());
            return null;
        }
        Random rng = rngFor(run.getSeed() ^ run.getCurrentRoomIndex());
        LabyrinthRoom boss = pool.get(rng.nextInt(pool.size()));
        RewardIcon icon = iconRoller.rollFor(boss);
        return new DoorChoice(boss, icon, boss, icon, true);
    }

    private DoorChoice pickCombatPair(LabyrinthRun run) {
        List<LabyrinthRoom> base = filterByTag(
                registry.getByType(run.getInstanceId(), RoomType.COMBAT), run.getTagFilter());
        if (base.isEmpty()) {
            logger.warning("[MemoryLabyrinth] No COMBAT room for floor=" + run.getFloorId());
            return null;
        }

        List<LabyrinthRoom> candidates = avoidRecent(base, run);
        if (candidates.isEmpty()) {
            // Fall back to full base — pool too small to honour avoidance window.
            candidates = base;
        }

        Random rng = rngFor(run.getSeed() ^ run.getCurrentRoomIndex());
        LabyrinthRoom left = candidates.get(rng.nextInt(candidates.size()));

        LabyrinthRoom right;
        if (candidates.size() == 1) {
            // Only one option — same room on both sides (CDC §8 dégénéré).
            logger.warning("[MemoryLabyrinth] Combat pool reduced to 1 for floor=" + run.getFloorId()
                    + ", same room on both doors");
            right = left;
        } else {
            List<LabyrinthRoom> remaining = new ArrayList<>(candidates);
            remaining.remove(left);
            right = remaining.get(rng.nextInt(remaining.size()));
        }

        RewardIcon iconLeft = iconRoller.rollFor(left);
        RewardIcon iconRight = iconRoller.rollFor(right);
        return new DoorChoice(left, iconLeft, right, iconRight, false);
    }

    private List<LabyrinthRoom> filterByTag(List<LabyrinthRoom> pool, String tagFilter) {
        if (pool == null || pool.isEmpty()) return Collections.emptyList();
        if (tagFilter == null || tagFilter.isEmpty()) return pool;
        List<LabyrinthRoom> out = new ArrayList<>();
        for (LabyrinthRoom r : pool) {
            List<String> tags = r.getTags();
            if (tags != null && tags.contains(tagFilter)) out.add(r);
        }
        // If no room carries the requested tag, fall back to the full pool
        // so admins can keep minimal setups working without per-floor tagging.
        return out.isEmpty() ? pool : out;
    }

    private List<LabyrinthRoom> avoidRecent(List<LabyrinthRoom> pool, LabyrinthRun run) {
        List<LabyrinthRoom> route = run.getRouteHistory();
        if (route == null || route.isEmpty()) return pool;
        int from = Math.max(0, route.size() - RECENT_AVOID_WINDOW);
        List<String> recentIds = new ArrayList<>();
        for (int i = from; i < route.size(); i++) {
            LabyrinthRoom r = route.get(i);
            if (r != null) recentIds.add(r.getId());
        }
        if (recentIds.isEmpty()) return pool;
        List<LabyrinthRoom> out = new ArrayList<>();
        for (LabyrinthRoom r : pool) {
            if (!recentIds.contains(r.getId())) out.add(r);
        }
        return out;
    }

    private Random rngFor(long salt) {
        return new Random(salt);
    }
}
