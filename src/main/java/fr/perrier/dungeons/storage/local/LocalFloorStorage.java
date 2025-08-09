package fr.perrier.dungeons.storage.local;

import fr.perrier.dungeons.model.Floor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LocalFloorStorage {

    private Floor currentFloor;

    public boolean hasFloor() {
        return currentFloor != null;
    }

    public void clear() {
        currentFloor = null;
    }
}
