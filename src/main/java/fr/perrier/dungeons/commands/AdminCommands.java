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

import java.util.UUID;

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
        floor.generateTemplate().thenRun(() -> {
            //TODO: Need to sync instances between servers with Redis Pub/Sub or something like ?
            FloorInstance floorInstance = new FloorInstance(floor.getId());
            floorInstance.sendToServer(player);
        });
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
        player.sendMessage(ChatUtil.getBar());
        player.sendMessage(ChatUtil.translate("&6Dungeon Status"));

        if (ServerUtil.isInstanceServer()) {
            ServerUtil.InstanceInfo info = ServerUtil.getInstanceInfo();
            player.sendMessage(ChatUtil.translate("&7Server Type: &aDungeon Instance"));
            player.sendMessage(ChatUtil.translate("&7Instance ID: &f" + info.instanceId()));
            player.sendMessage(ChatUtil.translate("&7Floor ID: &f" + info.floorId()));
            player.sendMessage(ChatUtil.translate("&7Created At: &f" + info.createdAt()));

            RedisStorageService storage = Main.getInstance().getRedisStorageService();
            FloorInstance instance = storage.getCurrentInstance().get();
            if (instance != null) {
                player.sendMessage(ChatUtil.translate("&7Ready: &f" + instance.isReady()));
            }
        } else {
            player.sendMessage(ChatUtil.translate("&7Server Type: &aLobby"));
        }

        player.sendMessage(ChatUtil.getBar());
    }

    @Command(names = "dungeon admin status")
    public static void adminDungeonStatusParamCommand(Player player, @Param(name = "Instance ID") String instanceId) {
        player.sendMessage(ChatUtil.getBar());
        player.sendMessage(ChatUtil.translate("&6Dungeon Status"));
        FloorInstance instance = Main.getInstance().getRedisStorageService().getInstance(UUID.fromString(instanceId));
        if(instance == null) {
            player.sendMessage(ChatUtil.translate("&cInstance not found"));
            return;
        }
        ServerUtil.InstanceInfo info = ServerUtil.getInstanceInfo(instance.getInstanceId());
        player.sendMessage(ChatUtil.translate("&7Server Type: &aDungeon Instance"));
        player.sendMessage(ChatUtil.translate("&7Instance ID: &f" + info.instanceId()));
        player.sendMessage(ChatUtil.translate("&7Floor ID: &f" + info.floorId()));
        player.sendMessage(ChatUtil.translate("&7Created At: &f" + info.createdAt()));
        player.sendMessage(ChatUtil.translate("&7Ready: &f" + instance.isReady()));
        player.sendMessage(ChatUtil.getBar());
    }
}
