package fr.perrier.dungeons.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Getter
public class Dungeon {

    @Getter
    private static final HashMap<String, Dungeon> dungeons = new HashMap<>();

    private String id;
    private String name;
    @Setter
    private List<Floor> floors;

    public Dungeon(String id, String name) {
        this.id = id;
        this.name = name;
        this.floors = new ArrayList<>();
        dungeons.put(id, this);
    }


    public void addFloor(Floor floor) {
        this.floors.add(floor);
    }

    public void removeFloor(Floor floor) {
        this.floors.remove(floor);
    }

    public static Dungeon getDungeon(String id) {
        return dungeons.get(id);
    }

    public static void removeDungeon(String id) {
        dungeons.remove(id);
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
