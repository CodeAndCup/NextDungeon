package fr.perrier.dungeons.storage.global;

import fr.perrier.dungeons.model.Floor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

public class FloorRegistry {

    private static final HashMap<String, Floor> floors = new HashMap<>();

    public static Floor getFloor(String id) {
        return floors.get(id);
    }

    public static void registerFloor(Floor floor) {
        floors.put(floor.getId(), floor);
    }

    public static void unregisterFloor(Floor floor) {
        floors.remove(floor.getId());
    }

    public Collection<Floor> getAllFloors() {
        return Collections.unmodifiableCollection(floors.values());
    }
}
