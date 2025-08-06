package fr.perrier.dungeons.configuration;

import fr.perrier.dungeons.utils.Position;
import lombok.Getter;
import java.util.UUID;

@Getter
public class WorldConfig {

    private final String folderName;

    public WorldConfig(String folderName, String difficulty, Position spawn) {
        this.folderName = folderName;
    }


    @Override
    public String toString() {
        return "WorldConfig{" +
                "folderName='" + folderName + '\'' +
                '}';
    }
}
