package fr.perrier.dungeons.commands;

import fr.perrier.cupcodeapi.commands.annotations.Command;
import fr.perrier.cupcodeapi.commands.annotations.Param;
import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.dungeons.configuration.ConfigLoader;
import fr.perrier.dungeons.configuration.WorldConfig;
import fr.perrier.dungeons.manager.FloorInstance;
import fr.perrier.dungeons.model.Floor;
import fr.perrier.dungeons.utils.Position;
import org.bukkit.entity.Player;

public class AdminCommands {

    @Command(names = "dungeon admin help")
    public static void adminDungeonCommand(Player player) {
        player.sendMessage(ChatUtil.getBar());
        player.sendMessage(ChatUtil.translate("/dungeon admin help"));
        player.sendMessage(ChatUtil.translate("/dungeon admin create <dungeonName> <floorName>"));
        player.sendMessage(ChatUtil.translate("/dungeon admin edit <dungeonName> <floorName>"));
        player.sendMessage(ChatUtil.translate("/dungeon admin test <dungeonName> <floorName>"));
        player.sendMessage(ChatUtil.translate("/dungeon admin import <world> <dungeonName> <floorName>"));
        player.sendMessage(ChatUtil.translate("/dungeon admin load <dungeonNameConfig>"));
        player.sendMessage(ChatUtil.translate("/dungeon admin status <dungeonName> [floorName]"));
        player.sendMessage(ChatUtil.getBar());
    }

    @Command(names = "dungeon admin create")
    public static void adminDungeonCreateCommand(Player player, @Param(name = "Dungeon") String dungeonName, @Param(name = "Floor") String floorName) {

    }

    @Command(names = "dungeon admin test")
    public static void adminDungeonPlayCommand(Player player, @Param(name = "Dungeon") String dungeonName, @Param(name = "Floor") String floorName) {
        Floor floor = new Floor("floor1","Floor 1");
        WorldConfig worldConfig = new WorldConfig("floor1","NORMAL",new Position(0,100,0));
        floor.setWorldConfig(worldConfig);

        FloorInstance floorInstance = new FloorInstance(floor.getId());
        player.teleport(floorInstance.getWorld().getSpawnLocation());
    }

    @Command(names = "dungeon admin load")
    public static void adminDungeonLoadCommand(Player player, @Param(name = "Dungeon") String dungeonName) {
        ConfigLoader.loadDungeon(dungeonName);
    }
}
