package fr.perrier.dungeons.manager;

import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.utils.ASWMUtil;
import fr.perrier.dungeons.utils.DungeonConfigurationReader;

public class DungeonInstance {

    private SlimeWorld dungeonWorld;

    private void loadDungeonByFile(String dungeonFile) {
        DungeonConfigurationReader.DungeonConfiguration configuration = new DungeonConfigurationReader().readFrom(dungeonFile);

        SlimePropertyMap properties = new SlimePropertyMap();
        properties.setValue(SlimeProperties.DIFFICULTY,configuration.getDifficulty());
        properties.setValue(SlimeProperties.SPAWN_X,configuration.getSpawnX());
        properties.setValue(SlimeProperties.SPAWN_Y,configuration.getSpawnY());
        properties.setValue(SlimeProperties.SPAWN_Z,configuration.getSpawnZ());

         Main.getInstance().getAswmAPI().asyncLoadWorld(ASWMUtil.getLoaderType(),configuration.getWorldName(),true,properties).thenAccept(world -> {
             this.dungeonWorld = world.get();
         });
    }
}
