package fr.perrier.dungeons.commands;

import fr.perrier.cupcodeapi.commands.annotations.Command;
import fr.perrier.cupcodeapi.commands.annotations.Param;
import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.dungeons.configuration.ConfigLoader;
import fr.perrier.dungeons.configuration.WorldConfig;
import fr.perrier.dungeons.manager.FloorInstance;
import fr.perrier.dungeons.model.Floor;
import fr.perrier.dungeons.utils.Position;
import fr.perrier.dungeons.utils.ServerUtil;
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
    public static void adminDungeonCreateCommand(Player player, @Param(name = "Dungeon ID") String dungeonId, @Param(name = "Floor ID") String floorId) {

        //TODO: Register new floors in db or something like ?
        Floor floor = new Floor(dungeonId + "_" + floorId,floorId);
        floor.generateTemplate();

        FloorInstance floorInstance = new FloorInstance(floor.getId());

    }

    @Command(names = "dungeon admin test")
    public static void adminDungeonPlayCommand(Player player, @Param(name = "Dungeon ID") String dungeonId, @Param(name = "Floor ID") String floorId) {

        Floor floor = Floor.getFloor(dungeonId + "_" + floorId);

        FloorInstance floorInstance = new FloorInstance(floor.getId());
        ServerUtil.sendToServer(player,floorInstance.getInstanceName());

        //TODO: Need to sync instances between servers with Redis Pub/Sub
    }

    @Command(names = "dungeon admin load")
    public static void adminDungeonLoadCommand(Player player, @Param(name = "Dungeon") String dungeonName) {
        ConfigLoader.loadDungeon(dungeonName);
        
        player.sendMessage(ChatUtil.getBar());
        player.sendMessage(ChatUtil.translate("&a Dungeon " + dungeonName + " loaded"));
        player.sendMessage(ChatUtil.getBar());
    }
}
