package fr.perrier.dungeons.configuration;

import fr.perrier.dungeons.utils.Position;
import lombok.Getter;
import java.util.UUID;

@Getter
public class WorldConfig {

    private final String folderName;
    private final String difficulty;
    private final Position spawn;

    public WorldConfig(String folderName, String difficulty, Position spawn) {
        this.folderName = folderName;
        this.difficulty = difficulty;
        this.spawn = spawn;
    }


    @Override
    public String toString() {
        return "WorldConfig{" +
                "folderName='" + folderName + '\'' +
                '}';
    }
}
