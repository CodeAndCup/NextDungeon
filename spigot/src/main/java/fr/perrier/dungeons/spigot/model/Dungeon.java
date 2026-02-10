package fr.perrier.dungeons.spigot.model;

import fr.perrier.dungeons.spigot.Main;
import lombok.Getter;

import java.util.*;

@Getter
public class Dungeon {

    private final String id;
    private String name;
    private List<Floor> floors;

    public Dungeon(String id, String name) {
        this.id = id;
        this.name = name;
        this.floors = new ArrayList<>();
    }

    /**
     * Adds a floor to this dungeon and synchronizes the update to Redis.
     *
     * @param floor the floor to add
     */
    public void addFloor(Floor floor) {
        this.floors.add(floor);
        Main.getInstance().getDungeonService().syncDungeon(this);
    }

    /**
     * Removes a floor from this dungeon and synchronizes the update to Redis.
     *
     * @param floor the floor to remove
     */
    public void removeFloor(Floor floor) {
        this.floors.remove(floor);
        Main.getInstance().getDungeonService().syncDungeon(this);
    }

    /**
     * Sets the list of floors for this dungeon and synchronizes the update to Redis.
     *
     * @param floors the new list of floors
     */
    public void setFloors(List<Floor> floors) {
        this.floors = floors;
        Main.getInstance().getDungeonService().syncDungeon(this);
    }


    /**
     * Renames this dungeon and synchronizes the update to Redis.
     *
     * @param newName the new name for the dungeon
     */
    public void rename(String newName) {
        this.name = newName;
        Main.getInstance().getDungeonService().syncDungeon(this);
    }

    /**
     * Deletes this dungeon from Redis storage.
     */
    public void delete() {
        Main.getInstance().getDungeonService().removeDungeon(this.id);
    }

    /**
     * Retrieves a dungeon from Redis by its unique ID.
     *
     * @param id the unique ID of the dungeon to retrieve
     * @return the dungeon with the given ID, or null if not found
     */
    public static Dungeon getDungeon(String id) {
        return Main.getInstance().getDungeonService().getDungeon(id);
    }

    /**
     * Retrieves all dungeons from Redis storage.
     *
     * @return an unmodifiable collection of all dungeons
     */
    public static Collection<Dungeon> getDungeons() {
        return Collections.unmodifiableCollection(Main.getInstance().getDungeonService().getDungeonsMap().values());
    }

    /**
     * Removes a dungeon from Redis storage by its unique ID.
     *
     * @param id the unique ID of the dungeon to remove
     */
    public static void removeDungeon(String id) {
        Main.getInstance().getDungeonService().removeDungeon(id);
    }

    @Override
    public String toString() {
        return "Dungeon{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", floors=" + floors +
                '}';
    }
}
