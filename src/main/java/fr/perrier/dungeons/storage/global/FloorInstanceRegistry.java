package fr.perrier.dungeons.storage.global;

import fr.perrier.dungeons.manager.FloorInstance;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

public class FloorInstanceRegistry {

    private static final HashMap<UUID, FloorInstance> floorInstances = new HashMap<>();

    public static FloorInstance getInstance(UUID id) {
        return floorInstances.get(id);
    }

    public static void registerInstance(FloorInstance instance) {
        floorInstances.put(instance.getInstanceId(), instance);
    }

    public static void unregisterInstance(UUID id) {
        floorInstances.remove(id);
    }

    public static Collection<FloorInstance> getAllInstances() {
        return Collections.unmodifiableCollection(floorInstances.values());
    }

    public static boolean hasInstance(UUID id) {
        return floorInstances.containsKey(id);
    }

    public static void clear() {
        floorInstances.clear();
    }
}
