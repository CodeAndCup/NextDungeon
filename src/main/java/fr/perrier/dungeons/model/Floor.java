package fr.perrier.dungeons.model;

import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.configuration.Requirements;
import fr.perrier.dungeons.configuration.Rules;
import fr.perrier.dungeons.configuration.WorldConfig;
import fr.perrier.dungeons.manager.DungeonFileManager;
import fr.perrier.dungeons.workflow.trigger.Trigger;
import fr.perrier.dungeons.utils.ServerUtil;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Getter
@Setter
public class Floor {

    private String id;

    private String name;
    private String description;

    private WorldConfig worldConfig;
    private Requirements requirements;
    private Rules rules;
    private List<Step> steps;
    private List<Trigger> triggers;

    public Floor(String id, String name) {
        this.id = id;
        this.name = name;
        this.description = "&cNo description";
        this.triggers = DungeonFileManager.loadTriggers(id);
        updateMap();
    }

    public Floor(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.triggers = DungeonFileManager.loadTriggers(id);
        updateMap();
    }

    /**
     * Retrieves a floor from Redis by its unique ID.
     *
     * @param id the unique ID of the floor to retrieve
     * @return the floor with the given ID, or null if not found
     */
    public static @Nullable Floor getFloor(String id) {
        return Main.getInstance().getRedisStorageService().getFloor(id);
    }

    /**
     * Synchronizes this floor to Redis, updating the local reference and
     * notifying other servers of the update.
     */
    public void updateMap() {
        Main.getInstance().getRedisStorageService().syncFloor(this);
    }

    /**
     * Asynchronously generates a floor template for this floor if it doesn't already exist.
     *
     * @return a future that completes with true if the template was successfully generated, or false if it already existed
     */
    public CompletableFuture<Boolean> generateTemplate() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            System.out.println("Is floor template exists : " + ServerUtil.isFloorTemplateExists(this));
            if(!ServerUtil.isFloorTemplateExists(this)) {
                ServerUtil.createFloorTemplate(this).thenAccept(future::complete);
            } else {
                System.out.println("Floor template already exists");
                future.complete(true);
            }
        });
        return future;
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
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", worldConfig=" + worldConfig +
                ", requirements=" + requirements +
                ", rules=" + rules +
                ", steps=" + steps +
                '}';
    }
}
