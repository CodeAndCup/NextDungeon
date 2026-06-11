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
        // Finite WIN — boss kill at maxRooms. Player earned the loot.
        end(run, true, true);
    }

    public void onTotalWipe(LabyrinthRun run) {
        // Loss — no loot, ever (CDC owner refinement: only winners get rewarded).
        end(run, false, false);
        if (run != null && run.isInfinite() && saveManager != null) {
            saveManager.deleteForRun(run);
            if (triggerBus != null) triggerBus.fireSaveInvalidated(run, null, "ALL_DEAD");
        }
    }

    public void onVoluntaryExit(LabyrinthRun run) {
        // Voluntary exit only rewards on Infinite (the "extraction" win-state
        // for that mode — there is no boss-kill end). Finite voluntary exit
        // is treated as abandoning : no loot.
        boolean isExtraction = run != null && run.isInfinite();
        end(run, isExtraction, isExtraction);
        if (run != null && run.isInfinite() && saveManager != null) {
            // Voluntary infinite exit deletes the save (CDC §1.6).
            saveManager.deleteForRun(run);
            if (triggerBus != null) triggerBus.fireSaveInvalidated(run, null, "VOLUNTARY_EXIT");
        }
    }

    private void end(LabyrinthRun run, boolean success, boolean distributeLoot) {
        if (run == null) return;
        FloorInstance instance = Main.getInstance().getDungeonService().getInstance(run.getInstanceId());
        List<UUID> recipients = instance != null
                ? new ArrayList<>(instance.getPlayers())
                : new ArrayList<>(run.getInitialPlayerUuids());

        List<LootResult> results;
        if (distributeLoot) {
            results = lootCalculator.computeForPlayers(run, recipients, success);
            for (LootResult r : results) distribute(r);
        } else {
            // No loot — wipe / abandoned finite. Still build empty per-player
            // results so the on_run_ended trigger gets a consistent shape
            // (success=false, no items, no gold) and admin workflows can
            // react (e.g. show "you lost" cinematic).
            results = new ArrayList<>();
            for (UUID id : recipients) {
                LootResult empty = new LootResult();
                empty.setPlayerId(id);
                empty.setSuccess(success);
                empty.setFloorId(run.getFloorId());
                empty.setFinalRoomIndex(run.getCurrentRoomIndex());
                empty.setTier(run.getCurrentModifier() != null ? run.getCurrentModifier().getTier() : 1);
                empty.setIconCounts(new java.util.EnumMap<>(run.getIconCounts()));
                empty.setGoldEarned(0L);
                empty.setItemsRolled(new ArrayList<>());
                results.add(empty);
                Player p = Bukkit.getPlayer(id);
                if (p != null && p.isOnline()) {
                    p.sendMessage("§c§l✦ Memory Labyrinth — Run échouée");
                    p.sendMessage("§7Aucun loot — les récompenses ne sont distribuées qu'à la victoire.");
                }
            }
        }

        if (triggerBus != null) triggerBus.fireRunEnded(run, results, success);
        if (onRunEndedCallback != null) onRunEndedCallback.fire(run, results, success);

        // Close the external blessing session for the party — completed on a
        // win, abandoned otherwise. Fired while players are still in the
        // instance (before endRun releases it).
        fr.perrier.dungeons.module.labyrinth.blessing.BlessingBridge.endSession(recipients, success);

        if (doorController != null) doorController.closeDoors(run.getInstanceId());
        runManager.endRun(run.getInstanceId());

        // Hand back to the core instance lifecycle so players are returned
        // to their origin lobby and the CloudNet floor service shuts down
        // after the 30s broadcast — same flow as classic dungeons via
        // EndDungeonAction.execute → FloorInstance.complete.
        if (instance != null) {
            instance.complete(success);
        } else {
            logger.warning("[MemoryLabyrinth] End of run for " + run.getInstanceId()
                    + " — no FloorInstance found, service will NOT shut down. Players stuck.");
        }
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
