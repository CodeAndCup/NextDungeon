package fr.perrier.dungeons.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
public class Dungeon {

    private String id;
    private String name;
    @Setter
    private List<Floor> floors;

    public Dungeon(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public void addFloor(Floor floor) {
        this.floors.add(floor);
    }
}
