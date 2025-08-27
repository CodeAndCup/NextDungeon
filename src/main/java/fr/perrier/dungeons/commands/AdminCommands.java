package fr.perrier.dungeons.commands;

import fr.perrier.cupcodeapi.commands.annotations.Command;
import fr.perrier.cupcodeapi.commands.annotations.Param;
import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.configuration.ConfigLoader;
import fr.perrier.dungeons.model.Dungeon;
import fr.perrier.dungeons.model.FloorInstance;
import fr.perrier.dungeons.model.Floor;
import fr.perrier.dungeons.storage.RedisStorageService;
import fr.perrier.dungeons.utils.ServerUtil;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.UUID;

public class AdminCommands {

    @Command(names = "dungeon admin help")
    public static void adminDungeonCommand(Player player) {
        player.sendMessage(ChatUtil.getBar());
        player.sendMessage(ChatUtil.translate("/dungeon admin help"));
        player.sendMessage(ChatUtil.translate("/dungeon admin edit <dungeonName> <floorName>"));
        player.sendMessage(ChatUtil.translate("/dungeon admin webeditor start"));
        player.sendMessage(ChatUtil.translate("/dungeon admin webeditor stop"));

        player.sendMessage(ChatUtil.translate("/dungeon admin test <dungeonName> <floorName>"));
        player.sendMessage(ChatUtil.translate("/dungeon admin import <world> <dungeonName> <floorName>"));
        player.sendMessage(ChatUtil.translate("/dungeon admin load <dungeonNameConfig>"));
        player.sendMessage(ChatUtil.translate("/dungeon admin status <dungeonName> [floorName]"));
        player.sendMessage(ChatUtil.getBar());
    }

    @Command(names = "dungeon admin edit")
    public static void adminDungeonEditCommand(Player player, @Param(name = "Dungeon ID") String dungeonId, @Param(name = "Floor ID") String floorId) {
        Floor floor = Floor.getFloor(dungeonId + "_" + floorId);
        if (floor == null) {
            player.sendMessage(ChatUtil.translate("&cFloor not found."));
            return;
        }

        FloorInstance floorInstance = new FloorInstance(floor.getId(),true);

        ServerUtil.sendToServer(player,floorInstance.getInstanceId());
    }

    @Command(names = "dungeon admin webeditor start")
    public static void adminDungeonWebEditorStartCommand(Player player) {
        if(Main.getInstance().getWebEditorManager().hasActiveEditor(player)) {
            player.sendMessage(ChatUtil.translate("&cYou already have an active web editor session."));
            return;
        }

        if(!ServerUtil.isInEditMode()) {
            player.sendMessage(ChatUtil.translate("&cThis server is not in edit mode."));
            return;
        }

        ServerUtil.InstanceInfo info = ServerUtil.getInstanceInfo();
        if(info == null || info.floorId() == null) {
            player.sendMessage(ChatUtil.translate("&cYou are not in a floor instance."));
            return;
        }

        Dungeon currentDungeon = Dungeon.getDungeon(info.floorId().split("_")[0]);
        Floor currentFloor = Floor.getFloor(info.floorId());

        if (currentFloor == null) {
            player.sendMessage(ChatUtil.translate("&cFloor not found."));
            return;
        }

        boolean success = Main.getInstance().getWebEditorManager().startWebEditor(player, currentDungeon.getName(), currentFloor.getId());

        if (success) {
            player.sendMessage("");
            player.sendMessage(ChatUtil.getBar());
            player.sendMessage(ChatUtil.translate("&6🏰 &lÉDITEUR WEB DÉMARRÉ"));
            player.sendMessage(ChatUtil.translate("&7Donjon: &e" + currentDungeon.getName()));
            player.sendMessage(ChatUtil.translate("&7Floor: &e" + currentFloor.getId() + " &8(" + currentFloor.getName() + ")"));
            player.sendMessage(ChatUtil.translate("&7URL: &b&nhttp://localhost:8080"));
            player.sendMessage(ChatUtil.translate("&7Arrêt: &c/dungeon admin webeditor stop"));
            player.sendMessage(ChatUtil.getBar());
        }
    }

    @Command(names = "dungeon admin webeditor stop")
    public static void adminDungeonWebEditorStopCommand(Player player) {
        boolean success = Main.getInstance().getWebEditorManager().stopWebEditor(player);
        if (success) {
            player.sendMessage(ChatUtil.translate("&a✓ Web editor stopped."));
        }
    }

    @Command(names = "dungeon admin test")
    public static void adminDungeonPlayCommand(Player player, @Param(name = "Dungeon ID") String dungeonId, @Param(name = "Floor ID") String floorId) {
        Floor floor = Floor.getFloor(dungeonId + "_" + floorId);
        if (floor == null) {
            player.sendMessage(ChatUtil.translate("&cFloor not found."));
            return;
        }

        FloorInstance floorInstance = new FloorInstance(floor.getId());

        ServerUtil.sendToServer(player,floorInstance.getInstanceId());
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
            player.sendMessage(ChatUtil.translate("&7Instance ID: &f" + Objects.requireNonNull(info).instanceId()));
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
