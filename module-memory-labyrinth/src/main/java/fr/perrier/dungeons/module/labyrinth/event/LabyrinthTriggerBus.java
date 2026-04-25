package fr.perrier.dungeons.module.labyrinth.event;

import fr.perrier.dungeons.common.module.ModuleContext;
import fr.perrier.dungeons.module.labyrinth.loot.LootResult;
import fr.perrier.dungeons.module.labyrinth.model.DoorChoice;
import fr.perrier.dungeons.module.labyrinth.model.LabyrinthRun;
import fr.perrier.dungeons.module.labyrinth.model.RewardIcon;
import fr.perrier.dungeons.module.labyrinth.model.RoomTemplate;
import fr.perrier.dungeons.module.labyrinth.model.RoomType;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.model.FloorInstance;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Central dispatcher for the Memory Labyrinth Blockly hooks (CDC §5).
 *
 * <p>Holds the {@link ModuleContext} obtained at module enable time and
 * exposes typed {@code fireXxx} helpers for the lifecycle / boss /
 * save / end-of-run components, so call sites do not need to assemble
 * the {@code data} {@link Map} manually.</p>
 */
public class LabyrinthTriggerBus {

    public static final String TRIGGER_ROOM_ENTERED = "labyrinth_on_room_entered";
    public static final String TRIGGER_ROOM_CLEARED = "labyrinth_on_room_cleared";
    public static final String TRIGGER_DOORS_PROPOSED = "labyrinth_on_doors_proposed";
    public static final String TRIGGER_BOSS_KILLED = "labyrinth_on_boss_killed";
    public static final String TRIGGER_RUN_ENDED = "labyrinth_on_run_ended";
    public static final String TRIGGER_CHECKPOINT_SAVED = "labyrinth_on_checkpoint_saved";
    public static final String TRIGGER_SAVE_INVALIDATED = "labyrinth_on_save_invalidated";

    private final ModuleContext ctx;

    public LabyrinthTriggerBus(ModuleContext ctx) {
        this.ctx = ctx;
    }

    public void fireRoomEntered(LabyrinthRun run, FloorInstance instance, Player triggerPlayer) {
        if (run == null) return;
        Map<String, Object> data = baseData(run);
        data.put("roomIndex", run.getCurrentRoomIndex());
        data.put("roomType", typeName(run.getCurrentRoom()));
        data.put("rewardIcon", iconName(run.getCurrentRoomIcon()));
        data.put("playerUuid", triggerPlayer != null ? triggerPlayer.getUniqueId().toString() : null);
        ctx.fireTrigger(TRIGGER_ROOM_ENTERED, triggerPlayer, locationOf(triggerPlayer), data);
    }

    public void fireRoomCleared(LabyrinthRun run, FloorInstance instance, long clearTimeMs) {
        if (run == null) return;
        FloorInstance resolved = resolveInstance(run, instance);
        Player anchor = anyOnline(resolved);
        Map<String, Object> data = baseData(run);
        data.put("roomIndex", run.getCurrentRoomIndex());
        data.put("rewardIcon", iconName(run.getCurrentRoomIcon()));
        data.put("clearTimeMs", clearTimeMs);
        ctx.fireTrigger(TRIGGER_ROOM_CLEARED, anchor, locationOf(anchor), data);
    }

    public void fireDoorsProposed(LabyrinthRun run, FloorInstance instance) {
        if (run == null || run.getPendingChoice() == null) return;
        DoorChoice choice = run.getPendingChoice();
        FloorInstance resolved = resolveInstance(run, instance);
        Player anchor = anyOnline(resolved);
        Map<String, Object> data = baseData(run);
        data.put("iconLeft", iconName(choice.getIconLeft()));
        data.put("iconRight", iconName(choice.getIconRight()));
        data.put("nextIsBoss",
                choice.getLeft() != null && choice.getLeft().getType() == RoomType.BOSS);
        ctx.fireTrigger(TRIGGER_DOORS_PROPOSED, anchor, locationOf(anchor), data);
    }

    public void fireBossKilled(LabyrinthRun run, FloorInstance instance) {
        if (run == null) return;
        FloorInstance resolved = resolveInstance(run, instance);
        Player anchor = anyOnline(resolved);
        Map<String, Object> data = baseData(run);
        data.put("roomIndex", run.getCurrentRoomIndex());
        data.put("tier", tierOf(run));
        data.put("playersAlive", playersAliveCount(run, resolved));
        ctx.fireTrigger(TRIGGER_BOSS_KILLED, anchor, locationOf(anchor), data);
    }

    /**
     * Fired once per player at end-of-run (per-player loot rolls). The
     * {@code data} map carries that player's roll — admins reading the
     * trigger get a focused per-player view.
     */
    public void fireRunEnded(LabyrinthRun run, List<LootResult> results, boolean success) {
        if (run == null || results == null) return;
        for (LootResult r : results) {
            UUID pid = r.getPlayerId();
            Player player = pid != null ? Bukkit.getPlayer(pid) : null;
            Map<String, Object> data = baseData(run);
            data.put("success", success);
            data.put("goldEarned", r.getGoldEarned());
            data.put("itemsRolled", String.join(",", r.getItemsRolled()));
            data.put("itemsRolledCount", r.getItemsRolled().size());
            data.put("tier", r.getTier());
            data.put("finalRoomIndex", r.getFinalRoomIndex());
            data.put("iconCounts", iconCountsCsv(r.getIconCounts()));
            data.put("playerUuid", pid != null ? pid.toString() : null);
            ctx.fireTrigger(TRIGGER_RUN_ENDED, player, locationOf(player), data);
        }
    }

    public void fireCheckpointSaved(LabyrinthRun run, FloorInstance instance) {
        if (run == null) return;
        FloorInstance resolved = resolveInstance(run, instance);
        Player anchor = anyOnline(resolved);
        Map<String, Object> data = baseData(run);
        data.put("roomIndex", run.getCurrentRoomIndex());
        data.put("tier", tierOf(run));
        ctx.fireTrigger(TRIGGER_CHECKPOINT_SAVED, anchor, locationOf(anchor), data);
    }

    public void fireSaveInvalidated(LabyrinthRun run, FloorInstance instance, String reason) {
        if (run == null) return;
        FloorInstance resolved = resolveInstance(run, instance);
        Player anchor = anyOnline(resolved);
        Map<String, Object> data = baseData(run);
        data.put("reason", reason != null ? reason : "UNKNOWN");
        ctx.fireTrigger(TRIGGER_SAVE_INVALIDATED, anchor, locationOf(anchor), data);
    }

    private FloorInstance resolveInstance(LabyrinthRun run, FloorInstance hint) {
        if (hint != null) return hint;
        if (run == null || run.getInstanceId() == null) return null;
        return Main.getInstance().getDungeonService().getInstance(run.getInstanceId());
    }

    private Map<String, Object> baseData(LabyrinthRun run) {
        Map<String, Object> data = new HashMap<>();
        data.put("instanceId", run.getInstanceId() != null ? run.getInstanceId().toString() : null);
        data.put("floorId", run.getFloorId());
        return data;
    }

    private static String typeName(RoomTemplate room) {
        return room == null || room.getType() == null ? null : room.getType().name();
    }

    private static String iconName(RewardIcon icon) {
        return icon == null ? RewardIcon.NONE.name() : icon.name();
    }

    private static int tierOf(LabyrinthRun run) {
        return run.getCurrentModifier() != null ? run.getCurrentModifier().getTier() : 1;
    }

    private static int playersAliveCount(LabyrinthRun run, FloorInstance instance) {
        if (instance == null) return 0;
        return Math.max(0, instance.getPlayers().size() - run.getDeadPlayers().size());
    }

    private static Player anyOnline(FloorInstance instance) {
        if (instance == null) return null;
        for (UUID id : instance.getPlayers()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) return p;
        }
        return null;
    }

    private static Location locationOf(Player p) {
        return p == null ? null : p.getLocation();
    }

    private static String iconCountsCsv(Map<RewardIcon, Integer> counts) {
        if (counts == null || counts.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<RewardIcon, Integer> e : counts.entrySet()) {
            if (e.getValue() == null || e.getValue() == 0) continue;
            if (!first) sb.append(',');
            sb.append(e.getKey().name()).append('=').append(e.getValue());
            first = false;
        }
        return sb.toString();
    }

    /** Helper for callers that need to forward a generic data map. */
    public static Map<String, Object> readonly(Map<String, Object> data) {
        return data == null ? Map.of() : Collections.unmodifiableMap(data);
    }
}
