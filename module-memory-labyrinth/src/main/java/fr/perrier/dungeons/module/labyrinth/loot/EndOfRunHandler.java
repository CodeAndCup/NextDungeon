package fr.perrier.dungeons.module.labyrinth.loot;

import fr.perrier.dungeons.module.labyrinth.lifecycle.DoorController;
import fr.perrier.dungeons.module.labyrinth.manager.LabyrinthRunManager;
import fr.perrier.dungeons.module.labyrinth.manager.LabyrinthSaveManager;
import fr.perrier.dungeons.module.labyrinth.model.LabyrinthRun;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.model.FloorInstance;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * End-of-run orchestration. Three entry points :
 * <ol>
 *   <li>{@link #onFiniteCompletion(LabyrinthRun)} — finite floor's last
 *       room cleared and the player is about to enter "room N+1".</li>
 *   <li>{@link #onTotalWipe(LabyrinthRun)} — every initial player has
 *       died with no available revive.</li>
 *   <li>{@link #onVoluntaryExit(LabyrinthRun)} — placeholder for the
 *       core to call when a player runs an explicit leave command.</li>
 * </ol>
 *
 * <p>Each entry computes per-player loot, distributes vanilla items
 * directly into player inventories, and (P9) will fire the
 * {@code labyrinth.on_run_ended} Blockly trigger so admin workflows can
 * grant gold via Vault/MMOCore, play cinematics, etc.</p>
 */
public class EndOfRunHandler {

    private final LabyrinthRunManager runManager;
    private final LootCalculator lootCalculator;
    private final LabyrinthSaveManager saveManager;
    private final DoorController doorController;
    private final Logger logger;
    private fr.perrier.dungeons.module.labyrinth.event.LabyrinthTriggerBus triggerBus;

    @Getter
    private OnRunEndedCallback onRunEndedCallback;

    public EndOfRunHandler(LabyrinthRunManager runManager,
                           LootCalculator lootCalculator,
                           LabyrinthSaveManager saveManager,
                           DoorController doorController) {
        this.runManager = runManager;
        this.lootCalculator = lootCalculator;
        this.saveManager = saveManager;
        this.doorController = doorController;
        this.logger = Main.getInstance().getLogger();
    }

    /**
     * Wired in by P9 (Blockly hooks) — the module passes a callback
     * that fires the {@code labyrinth.on_run_ended} trigger via
     * {@link fr.perrier.dungeons.common.module.ModuleContext#fireTrigger}.
     */
    public void setOnRunEndedCallback(OnRunEndedCallback cb) {
        this.onRunEndedCallback = cb;
    }

    public void setTriggerBus(fr.perrier.dungeons.module.labyrinth.event.LabyrinthTriggerBus bus) {
        this.triggerBus = bus;
    }

    public void onFiniteCompletion(LabyrinthRun run) {
        end(run, true);
    }

    public void onTotalWipe(LabyrinthRun run) {
        end(run, false);
        if (run != null && run.isInfinite() && saveManager != null) {
            saveManager.deleteForRun(run);
            if (triggerBus != null) triggerBus.fireSaveInvalidated(run, null, "ALL_DEAD");
        }
    }

    public void onVoluntaryExit(LabyrinthRun run) {
        // Voluntary leave still ends the run — count as success only for
        // finite floors that are actually completed ; otherwise treat as
        // a failure-grade end. For now we mark as failure; finer
        // granularity (success / abandoned) can come in P11 polish.
        end(run, false);
        if (run != null && run.isInfinite() && saveManager != null) {
            // Voluntary infinite exit deletes the save (CDC §1.6).
            saveManager.deleteForRun(run);
            if (triggerBus != null) triggerBus.fireSaveInvalidated(run, null, "VOLUNTARY_EXIT");
        }
    }

    private void end(LabyrinthRun run, boolean success) {
        if (run == null) return;
        FloorInstance instance = Main.getInstance().getDungeonService().getInstance(run.getInstanceId());
        List<UUID> recipients = instance != null
                ? new ArrayList<>(instance.getPlayers())
                : new ArrayList<>(run.getInitialPlayerUuids());

        List<LootResult> results = lootCalculator.computeForPlayers(run, recipients, success);

        for (LootResult r : results) distribute(r);

        if (triggerBus != null) triggerBus.fireRunEnded(run, results, success);
        if (onRunEndedCallback != null) onRunEndedCallback.fire(run, results, success);

        if (doorController != null) doorController.closeDoors(run.getInstanceId());
        runManager.endRun(run.getInstanceId());
    }

    /**
     * Drop each rolled item into the player's inventory. Items use a
     * vanilla {@link Material} resolution in v1 ; unknown ids are
     * logged and skipped (Mythic/MMOCore items will land later via the
     * same reflection bridge as mob spawning).
     */
    private void distribute(LootResult result) {
        if (result == null || result.getPlayerId() == null) return;
        Player player = Bukkit.getPlayer(result.getPlayerId());
        if (player == null || !player.isOnline()) {
            logger.info("[MemoryLabyrinth] Player " + result.getPlayerId()
                    + " offline at end-of-run — skipping inventory drop (gold via Blockly trigger)");
            return;
        }
        List<String> dropped = new ArrayList<>();
        for (String itemId : result.getItemsRolled()) {
            ItemStack stack = resolveItem(itemId);
            if (stack == null) continue;
            player.getInventory().addItem(stack);
            dropped.add(itemId);
        }
        sendSummary(player, result, dropped);
    }

    private ItemStack resolveItem(String itemId) {
        if (itemId == null || itemId.isEmpty()) return null;
        try {
            Material material = Material.valueOf(itemId.toUpperCase());
            return new ItemStack(material, 1);
        } catch (IllegalArgumentException e) {
            logger.warning("[MemoryLabyrinth] Unknown itemId '" + itemId
                    + "' — Mythic/MMOCore item resolution will land later");
            return null;
        }
    }

    private void sendSummary(Player player, LootResult result, List<String> droppedItems) {
        String head = result.isSuccess()
                ? "§a§l✦ Memory Labyrinth — Run terminée"
                : "§c§l✦ Memory Labyrinth — Run échouée";
        player.sendMessage(head);
        player.sendMessage("§7Salles parcourues : §f" + result.getFinalRoomIndex()
                + " §8| §7Palier final : §f" + result.getTier());
        if (result.getGoldEarned() > 0) {
            player.sendMessage("§e🪙 Or estimé : §f" + result.getGoldEarned()
                    + " §7(distribué via le workflow `on_run_ended`)");
        }
        if (!droppedItems.isEmpty()) {
            player.sendMessage("§b🎁 Items reçus : §f" + String.join(", ", droppedItems));
        }
    }

    /**
     * Functional callback used by P9 to wire the Blockly trigger.
     */
    @FunctionalInterface
    public interface OnRunEndedCallback {
        void fire(LabyrinthRun run, List<LootResult> results, boolean success);
    }
}
