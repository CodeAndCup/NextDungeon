package fr.perrier.dungeons.commands;

import fr.perrier.cupcodeapi.commands.annotations.Command;
import fr.perrier.cupcodeapi.commands.annotations.Param;
import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.configuration.ConfigLoader;
import fr.perrier.dungeons.model.FloorInstance;
import fr.perrier.dungeons.model.Floor;
import fr.perrier.dungeons.storage.RedisStorageService;
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

        ServerUtil.sendToServer(player,floorInstance.getInstanceId());

        //TODO: Need to sync instances between servers with Redis Pub/Sub
    }

    @Command(names = "dungeon admin load")
    public static void adminDungeonLoadCommand(Player player, @Param(name = "Dungeon") String dungeonName) {
        ConfigLoader.loadDungeon(dungeonName);
        
        player.sendMessage(ChatUtil.getBar());
        player.sendMessage(ChatUtil.translate("&a Dungeon " + dungeonName + " loaded"));
        player.sendMessage(ChatUtil.getBar());
    }

    @Command(names = "dungeon admin goto")
    public static void adminDungeonGotoCommand(Player player, @Param(name = "Dungeon Server Name") String server) {
        ServerUtil.sendToServer(player,server);
    }

    @Command(names = "dungeon admin status")
    public static void adminDungeonStatusCommand(Player player) {
        RedisStorageService storage = Main.getInstance().getRedisStorageService();
        RedisStorageService.FloorInstanceState state = storage.getInstanceState();

        player.sendMessage(ChatUtil.getBar());
        player.sendMessage(ChatUtil.translate("&6Dungeon Status"));
        player.sendMessage(ChatUtil.translate("&7Instance State: &f" + state));

        FloorInstance instance = storage.getCurrentInstance().get();
        if (instance != null) {
            player.sendMessage(ChatUtil.translate("&7Instance ID: &f" + instance.getInstanceId()));
            player.sendMessage(ChatUtil.translate("&7Floor ID: &f" + instance.getFloorId()));
            player.sendMessage(ChatUtil.translate("&7Ready: &f" + instance.isReady()));
        }
        player.sendMessage(ChatUtil.getBar());
    }
}
