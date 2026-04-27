package fr.perrier.dungeons.module.labyrinth.lifecycle;

import fr.perrier.dungeons.common.model.dungeon.FloorType;
import fr.perrier.dungeons.module.labyrinth.manager.LabyrinthRunManager;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.event.FloorInstanceReadyEvent;
import fr.perrier.dungeons.spigot.model.Floor;
import fr.perrier.dungeons.spigot.model.FloorInstance;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Entry point hook (R4) — listens for the core
 * {@link FloorInstanceReadyEvent} and starts a labyrinth run for any
 * instance whose floor is of type
 * {@link FloorType#LABYRINTH}.
 *
 * <p>Classic floors are ignored ; the host engine keeps driving them.</p>
 */
public class LabyrinthInstanceReadyListener implements Listener {

    private final LabyrinthRunManager runManager;

    public LabyrinthInstanceReadyListener(LabyrinthRunManager runManager) {
        this.runManager = runManager;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInstanceReady(FloorInstanceReadyEvent event) {
        FloorInstance instance = event.getInstance();
        if (instance == null) return;
        String floorId = instance.getFloorId();
        if (floorId == null) return;

        Floor floor = Floor.getFloor(floorId);
        if (floor == null) return;
        if (floor.getFloorType() != FloorType.LABYRINTH) return;

        Main.getInstance().getLogger().info(
                "[MemoryLabyrinth] FloorInstance " + instance.getInstanceId()
                        + " ready on labyrinth floor " + floorId + " — starting run");

        runManager.startRun(instance, floorId, run -> {
            if (run == null) {
                Main.getInstance().getLogger().warning(
                        "[MemoryLabyrinth] Failed to start run for instance "
                                + instance.getInstanceId() + " (floor " + floorId + ")");
            }
        });
    }
}
