package fr.perrier.dungeons.module.labyrinth.ui;

import fr.perrier.dungeons.module.labyrinth.manager.LabyrinthRunManager;
import fr.perrier.dungeons.module.labyrinth.model.LabyrinthRun;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.model.FloorInstance;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

/**
 * Periodic task that pushes a "🌀 Salle X · Palier Y" action-bar line
 * to every player of every active labyrinth run. Cheap UX feedback so
 * players can see their progression and active difficulty tier without
 * opening a menu.
 *
 * <p>Ticks every second (20 ticks). Action-bar messages auto-fade after
 * about 3 seconds in vanilla, so a 1-second cadence keeps it stable
 * without flicker.</p>
 */
public class TierIndicatorTask {

    public static final long PERIOD_TICKS = 20L;

    private final LabyrinthRunManager runManager;
    private BukkitTask task;

    public TierIndicatorTask(LabyrinthRunManager runManager) {
        this.runManager = runManager;
    }

    public void start() {
        if (task != null) return;
        task = Bukkit.getScheduler().runTaskTimer(Main.getInstance(),
                this::tick, PERIOD_TICKS, PERIOD_TICKS);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        if (runManager.getActiveRunCount() == 0) return;
        for (LabyrinthRun run : runManager.getRunsView()) {
            int tier = run.getCurrentModifier() != null ? run.getCurrentModifier().getTier() : 1;
            int idx = run.getCurrentRoomIndex();
            String label = "§b🌀 §fSalle §e" + idx + " §8| §fPalier §e" + tier;
            TextComponent comp = new TextComponent(label);
            FloorInstance instance = Main.getInstance().getDungeonService().getInstance(run.getInstanceId());
            if (instance == null) continue;
            for (UUID id : instance.getPlayers()) {
                Player p = Bukkit.getPlayer(id);
                if (p != null && p.isOnline()) {
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR, comp);
                }
            }
        }
    }
}
