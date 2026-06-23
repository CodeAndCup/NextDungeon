package fr.perrier.dungeons.module.labyrinth.model;

import fr.perrier.dungeons.common.model.labyrinth.LabyrinthRoom;
import fr.perrier.dungeons.common.model.labyrinth.RewardIcon;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * In-memory state of an active Memory Labyrinth run, scoped to a
 * {@code FloorInstance}. One {@code LabyrinthRun} per active instance.
 *
 * <p>This object lives only on the server hosting the instance. The
 * cross-server / persistent counterpart is {@link LabyrinthSave} (Infinite
 * only).</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class LabyrinthRun {

    private UUID instanceId;
    private String floorId;

    /**
     * Optional tag the picker uses to filter rooms by difficulty
     * (typically the floor's tagFilter from {@code LabyrinthFloorConfig}).
     * Null/empty → no filter, picks from the whole dungeon pool.
     */
    private String tagFilter;

    /**
     * Cached max-rooms cap of the floor. {@link Integer#MAX_VALUE} for
     * the infinite floor. Read by {@code advanceToChosen} to detect
     * finite completion without re-reading the floor config.
     */
    private int maxRooms = Integer.MAX_VALUE;

    /** 0 = lobby, 1..N = combat / boss rooms. */
    private int currentRoomIndex;

    private LabyrinthRoom currentRoom;

    /**
     * Per-entry UUID of the current room. Two visits to the same template
     * get distinct UUIDs so {@link #aliveMobsByRoom} can key mobs reliably
     * across revisits.
     */
    private UUID currentRoomUuid;

    /** Icon of the room the players are currently inside. */
    private RewardIcon currentRoomIcon = RewardIcon.NONE;

    private final List<LabyrinthRoom> routeHistory = new ArrayList<>();

    /** Non-null while the player is between a cleared room and the next. */
    private DoorChoice pendingChoice;

    /** Mobs still alive per visited room (room UUID → count). */
    private final Map<UUID, Integer> aliveMobsByRoom = new HashMap<>();

    /** Players currently dead (resolved at each boss kill). */
    private final Set<UUID> deadPlayers = new HashSet<>();

    private DifficultyModifier currentModifier = new DifficultyModifier();

    private long seed;

    /** Cumulative icon counts since run start (or since save creation in Infinite). */
    private final Map<RewardIcon, Integer> iconCounts = new EnumMap<>(RewardIcon.class);

    /**
     * Frozen at run start (CDC Q2 = A). The Infinite save is keyed by
     * sha256(sorted(initialPlayerUuids)) ; if any initial player is missing
     * at resume, the save stays inaccessible.
     */
    private final Set<UUID> initialPlayerUuids = new HashSet<>();

    /** Wall-clock timestamp of run start (used for run duration / clearTimeMs). */
    private long startedAtMs;

    /** Wall-clock timestamp of last room entry (used for clearTimeMs). */
    private long currentRoomEnteredAtMs;

    /**
     * True when a one-shot revive is available after the latest boss kill
     * and has not yet been consumed. Cleared when consumed or when the
     * run advances to the next room (CDC §6.3 — « 1 revive par boss »).
     */
    private boolean reviveAvailable;

    /**
     * True while the Infinite lobby is awaiting the leader's reprendre/
     * nouvelle decision. The door controller respects this flag and
     * ignores anchor traversals until it flips back to false.
     */
    private boolean lobbyDecisionPending;

    /**
     * Identifier of the persisted Infinite save bound to this run, when
     * resumed. Null for a fresh run or after invalidation.
     */
    private String infiniteSaveId;

    /**
     * The resumable saves found at start (most recent first), held until the
     * leader makes the resume/new decision. Lets the prompt be (re)sent when
     * the leader actually connects — the save lookup resolves before players
     * join, so the prompt would otherwise be built while the leader is still
     * offline and silently dropped. In-memory only.
     */
    private java.util.List<LabyrinthSave> pendingResumeSaves = new java.util.ArrayList<>();

    // ===== v2 procedural paste (R6) =====
    //
    // Rooms live as static geometry in a template world. The runtime COPIES
    // each room into the instance world at a running offset so multiple
    // visits + multiple instances never collide. These fields drive that
    // pipeline.

    /**
     * Bukkit world name where rooms are pasted at runtime. Defaults to
     * the dungeon's worldId — same world holds both templates and the
     * runtime copies, separated by a large X offset.
     */
    private String instanceWorldId;

    /**
     * Initial paste anchor (X) in {@link #instanceWorldId}. Picked far
     * from typical builds (5000 by default) so the runtime copies do not
     * overlap with the admin's template region.
     */
    private int baseAnchorX = 5000;
    private int baseAnchorY = 100;
    private int baseAnchorZ = 0;

    /**
     * Cursor that walks along +X as rooms are entered. Stride per room
     * is computed from the source region's max horizontal dimension plus
     * a buffer, so big rooms do not bleed into the next.
     */
    private int nextRoomOffsetX = 0;

    /**
     * Anchor of the current room's copy (= base + offset at entry). Used
     * by the player TP override and by the mob/door coord translators
     * to map template-coords → instance-coords.
     */
    private int currentRoomAnchorX;
    private int currentRoomAnchorY;
    private int currentRoomAnchorZ;

    public int incrementIcon(RewardIcon icon) {
        if (icon == null || icon == RewardIcon.NONE) return 0;
        int next = iconCounts.getOrDefault(icon, 0) + 1;
        iconCounts.put(icon, next);
        return next;
    }

    public int getIconCount(RewardIcon icon) {
        if (icon == null) return 0;
        return iconCounts.getOrDefault(icon, 0);
    }

    public boolean isCurrentRoomBoss() {
        return currentRoomIndex > 0 && currentRoomIndex % 10 == 0;
    }

    public boolean isCurrentRoomLobby() {
        return currentRoomIndex == 0;
    }

    /**
     * Infinite mode is defined by the floor's room cap, not its name : a floor
     * with {@code maxRooms <= 0} has no cap and is stored here as
     * {@link Integer#MAX_VALUE} (set at run start from the floor config).
     */
    public boolean isInfinite() {
        return maxRooms == Integer.MAX_VALUE || maxRooms <= 0;
    }
}
