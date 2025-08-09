package fr.perrier.dungeons.storage.local;

import fr.perrier.dungeons.Main;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class LocalStorage {

    private boolean lobby;
    private boolean ready;
    private UUID instanceId;
    private String floorId;
    // private static FloorConfig floorConfig;

    public LocalStorage() {
        this.lobby = Main.getInstance().getConfig().getBoolean("ServerConfiguration.isLobby");
        this.ready = false;
        this.instanceId = null;
        this.floorId = null;
    }
}
