package fr.perrier.dungeons.module.labyrinth.lifecycle;

import fr.perrier.dungeons.module.labyrinth.manager.LabyrinthRunManager;
import fr.perrier.dungeons.module.labyrinth.manager.LabyrinthSaveManager;
import fr.perrier.dungeons.module.labyrinth.model.LabyrinthRun;
import fr.perrier.dungeons.module.labyrinth.ui.RevivePromptComponent;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.listener.dungeons.InstancePlayerDeathListener;
import fr.perrier.dungeons.spigot.model.FloorInstance;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Resolves a boss kill (CDC §6.3) :
 * <ol>
 *   <li>Increment the run's difficulty tier.</li>
 *   <li>If the group has dead players, broadcast the
 *       {@link RevivePromptComponent} and arm the {@code reviveAvailable}
 *       one-shot flag on the run.</li>
 *   <li>(P6) Persist the Infinite checkpoint — stubbed here, wired by
 *       the save manager.</li>
 * </ol>
 *
 * <p>The actual revive command is intercepted by
 * {@link LabyrinthReviveListener} which calls back into
 * {@link #processReviveClick(Player, String)}.</p>
 */
public class BossEncounterHandler {

    public static final String REVIVE_COMMAND = "labyrinth_revive";

    private final LabyrinthRunManager runManager;
    private final Logger logger;
    private LabyrinthSaveManager saveManager;
    private fr.perrier.dungeons.module.labyrinth.event.LabyrinthTriggerBus triggerBus;

    public BossEncounterHandler(LabyrinthRunManager runManager) {
        this.runManager = runManager;
        this.logger = Main.getInstance().getLogger();
    }

    public void setSaveManager(LabyrinthSaveManager saveManager) {
        this.saveManager = saveManager;
    }

    public void setTriggerBus(fr.perrier.dungeons.module.labyrinth.event.LabyrinthTriggerBus bus) {
        this.triggerBus = bus;
    }

    /**
     * Called by the room lifecycle when the current room is BOSS and every
     * mob is dead.
     */
    public void handleBossKill(LabyrinthRun run) {
        if (run == null) return;

        // Tier scaling for the next 10-room block (CDC §6.3).
        if (run.getCurrentModifier() == null) {
            run.setCurrentModifier(new fr.perrier.dungeons.module.labyrinth.model.DifficultyModifier());
        }
        run.getCurrentModifier().incrementTier();

        // Revive prompt — only when a group has lost members.
        FloorInstance instance = Main.getInstance().getDungeonService().getInstance(run.getInstanceId());
        if (instance == null) {
            logger.warning("[MemoryLabyrinth] Boss kill resolved without a FloorInstance — skipping revive prompt");
        } else if (!run.getDeadPlayers().isEmpty()) {
            run.setReviveAvailable(true);
            RevivePromptComponent.send(instance, run.getDeadPlayers());
        }

        // Fire the boss-killed Blockly trigger before checkpoint save so
        // admin workflows can react before the (async) save completes.
        if (triggerBus != null) triggerBus.fireBossKilled(run, instance);

        // Persist the Infinite checkpoint (CDC §6.3).
        if (run.isInfinite() && saveManager != null) {
            saveManager.upsert(run).thenRun(() -> {
                if (triggerBus != null) triggerBus.fireCheckpointSaved(run, null);
            }).exceptionally(ex -> {
                logger.warning("[MemoryLabyrinth] Failed to upsert Infinite save: " + ex.getMessage());
                return null;
            });
        }
    }

    /**
     * Reset the revive flag when the run advances to the next room. Any
     * unconsumed revive expires (CDC §6.3).
     */
    public void onRoomTraversed(LabyrinthRun run) {
        if (run == null) return;
        run.setReviveAvailable(false);
    }

    /**
     * Process a click on the revive prompt. Validates :
     * <ul>
     *   <li>the sender is alive in the same instance,</li>
     *   <li>the target name maps to a UUID in {@link LabyrinthRun#getDeadPlayers()},</li>
     *   <li>the one-shot {@code reviveAvailable} flag is still on.</li>
     * </ul>
     */
    public void processReviveClick(Player sender, String targetName) {
        if (sender == null || targetName == null) return;

        // Locate the run via the instance the sender is in.
        UUID senderId = sender.getUniqueId();
        LabyrinthRun run = findRunOf(senderId);
        if (run == null) {
            sender.sendMessage("§cTu n'es pas dans une instance de Memory Labyrinth.");
            return;
        }
        if (!run.isReviveAvailable()) {
            sender.sendMessage("§cLe revive n'est plus disponible.");
            return;
        }
        if (run.getDeadPlayers().contains(senderId)) {
            sender.sendMessage("§cTu ne peux pas revive en étant mort toi-même.");
            return;
        }

        UUID targetId = uuidByName(run, targetName);
        if (targetId == null || !run.getDeadPlayers().contains(targetId)) {
            sender.sendMessage("§c" + targetName + " n'est pas dans la liste des morts.");
            return;
        }
        Player target = Bukkit.getPlayer(targetId);
        if (target == null || !target.isOnline()) {
            sender.sendMessage("§c" + targetName + " n'est plus en ligne — revive impossible.");
            return;
        }

        // Consume the one-shot, remove from deadPlayers, delegate to core.
        run.setReviveAvailable(false);
        run.getDeadPlayers().remove(targetId);
        InstancePlayerDeathListener.revivePlayer(target);

        FloorInstance instance = Main.getInstance().getDungeonService().getInstance(run.getInstanceId());
        if (instance != null) {
            RevivePromptComponent.sendCancellation(instance,
                    sender.getName() + " a ressuscité " + target.getName());
        }
    }

    private LabyrinthRun findRunOf(UUID playerId) {
        return runManager.findRunByPlayer(playerId);
    }

    private UUID uuidByName(LabyrinthRun run, String name) {
        for (UUID id : run.getDeadPlayers()) {
            String n = Bukkit.getOfflinePlayer(id).getName();
            if (n != null && n.equalsIgnoreCase(name)) return id;
        }
        return null;
    }
}
