package fr.perrier.dungeons.configuration;

import com.infernalsuite.asp.api.world.properties.SlimeProperties;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import fr.perrier.dungeons.utils.Position;
import lombok.Getter;
import java.util.UUID;

@Getter
public class WorldConfig {

    private String folderName;
    private UUID id;
    private SlimePropertyMap properties;

    public WorldConfig(String folderName) {
        this.folderName = folderName;
    }

    public void setProperties(String difficulty, Position spawn) {
        SlimePropertyMap properties = new SlimePropertyMap();
        properties.setValue(SlimeProperties.DIFFICULTY,difficulty);
        properties.setValue(SlimeProperties.SPAWN_X,spawn.getX());
        properties.setValue(SlimeProperties.SPAWN_Y,spawn.getY());
        properties.setValue(SlimeProperties.SPAWN_Z,spawn.getZ());
        this.properties = properties;
    }
}
