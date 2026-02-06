package fr.perrier.dungeons.spigot.model;

import fr.perrier.dungeons.common.model.dungeon.FloorData;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.database.DatabaseTriggersManager;
import fr.perrier.dungeons.spigot.utils.ServerUtil;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

@Getter
@Setter
public class Floor extends FloorData {

    public Floor(String id, String name) {
        super(id, name);
        super.setTriggers(DatabaseTriggersManager.loadTriggers(id));
    }

    public Floor(String id, String name, String description) {
        super(id, name, description);
        super.setTriggers(DatabaseTriggersManager.loadTriggers(id));
    }

    public Floor(FloorData floorData) {
        super(floorData.getId(), floorData.getName(), floorData.getDescription(),
                floorData.getWorldConfig(), floorData.getRequirements(),
                floorData.getRules(), floorData.getSteps(), floorData.getTriggers());
    }

    /**
     * Retrieves a floor from Redis by its unique ID.
     *
     * @param id the unique ID of the floor to retrieve
     * @return the floor with the given ID, or null if not found
     */
    public static @Nullable Floor getFloor(String id) {
        return Main.getInstance().getDungeonService().getFloor(id);
    }

    /**
     * Synchronizes this floor to Redis, updating the local reference and
     * notifying other servers of the update.
     */
    public void updateMap() {
        Main.getInstance().getDungeonService().syncFloor(this.toFloorData());
    }

    /**
     * Asynchronously generates a floor template for this floor if it doesn't already exist.
     *
     * @return a future that completes with true if the template was successfully generated, or false if it already existed
     */
    public CompletableFuture<Boolean> generateTemplate() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            if(!ServerUtil.isFloorTemplateExists(this)) {
                ServerUtil.createFloorTemplate(this).thenAccept(future::complete);
            } else {
                future.complete(true);
            }
        });
        return future;
    }

    /**
     * Converts this Floor object to a FloorData object.
     *
     * @return a FloorData representation of this Floor
     */
    public FloorData toFloorData() {
        return new FloorData(
                getId(),
                getName(),
                getDescription(),
                getWorldConfig(),
                getRequirements(),
                getRules(),
                getSteps(),
                getTriggers()
        );
    }


    /**
     * A string representation of the floor, including its ID, name, world configuration,
     * requirements, rules, and steps.
     *
     * @return a string representation of the floor
     */
    @Override
    public String toString() {
        return "Floor{" +
                "id='" + getId() + '\'' +
                ", name='" + getName() + '\'' +
                ", worldConfig=" + getWorldConfig() +
                ", requirements=" + getRequirements() +
                ", rules=" + getRules() +
                ", steps=" + getSteps() +
                '}';
    }
}
