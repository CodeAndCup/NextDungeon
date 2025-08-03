package fr.perrier.dungeons.configuration;

import com.infernalsuite.asp.api.world.properties.SlimeProperties;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import fr.perrier.dungeons.utils.Position;
import lombok.Getter;
import java.util.UUID;

@Getter
public class WorldConfig {

    private final String folderName;
    private SlimePropertyMap properties;

    public WorldConfig(String folderName, String difficulty, Position spawn) {
        this.folderName = folderName;
        SlimePropertyMap properties = new SlimePropertyMap();
        properties.setValue(SlimeProperties.DIFFICULTY,difficulty);
        properties.setValue(SlimeProperties.SPAWN_X,(int)spawn.getX());
        properties.setValue(SlimeProperties.SPAWN_Y,(int)spawn.getY());
        properties.setValue(SlimeProperties.SPAWN_Z,(int)spawn.getZ());
        this.properties = properties;
    }
}
