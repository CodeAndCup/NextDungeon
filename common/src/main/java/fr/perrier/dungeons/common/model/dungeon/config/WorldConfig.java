package fr.perrier.dungeons.common.model.dungeon.config;

import fr.perrier.dungeons.common.utils.Position;
import lombok.Getter;

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
