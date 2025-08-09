package fr.perrier.dungeons.storage.local;

import fr.perrier.dungeons.manager.FloorInstance;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LocalInstanceStorage {

    private FloorInstance currentFloorInstance;

    public boolean hasInstance() {
        return currentFloorInstance != null;
    }

    public void clear() {
        currentFloorInstance = null;
    }
}
