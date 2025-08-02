package fr.perrier.dungeons.commands;

import fr.perrier.cupcodeapi.commands.annotations.Command;
import fr.perrier.cupcodeapi.commands.annotations.Param;
import fr.perrier.dungeons.configuration.DungeonLoader;
import fr.perrier.dungeons.configuration.WorldConfig;
import fr.perrier.dungeons.model.Dungeon;
import fr.perrier.dungeons.model.Floor;
import fr.perrier.dungeons.utils.Position;
import org.bukkit.entity.Player;

public class AdminCommands {

    @Command(names = "dungeon admin")
    public static void adminDungeonCommand(Player player) {
        Floor floor = new Floor("floor1","Floor 1");
        WorldConfig worldConfig = new WorldConfig("floor1");
        worldConfig.setProperties("normal",new Position(0,100,0));
        floor.setWorldConfig(worldConfig);
        floor.play(player);
    }

    @Command(names = "dungeon admin play")
    public static void adminDungeonPlayCommand(Player player, @Param(name = "Dungeon") String dungeonName, @Param(name = "Floor") String floorName) {
        Dungeon dungeon = DungeonLoader.loadDungeon(dungeonName);
    }
}
