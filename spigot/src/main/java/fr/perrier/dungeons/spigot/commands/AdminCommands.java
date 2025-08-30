package fr.perrier.dungeons.spigot.commands;

import fr.perrier.cupcodeapi.commands.annotations.Command;
import fr.perrier.cupcodeapi.commands.annotations.Param;
import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.configuration.ConfigLoader;
import fr.perrier.dungeons.spigot.model.Dungeon;
import fr.perrier.dungeons.spigot.model.FloorInstance;
import fr.perrier.dungeons.spigot.model.Floor;
import fr.perrier.dungeons.spigot.storage.RedisStorageService;
import fr.perrier.dungeons.spigot.utils.ServerUtil;
import jodd.io.FileUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Objects;
import java.util.UUID;

public class AdminCommands {

    @Command(names = {"dungeon admin help", "dungeons admin help", "nextdungeon admin help", "nextdungeons admin help", "nd admin help"}, permission = "nextdungeons.admin")
    public static void adminDungeonCommand(Player player) {
        player.sendMessage(ChatUtil.getBar());
        player.sendMessage(ChatUtil.translate("<gradient:#8B0000:bold>NextDungeon</gradient:#D10000> &8| &fAdmin Commands"));
        player.sendMessage("");
        player.sendMessage(ChatUtil.translate("&#D10000/dungeon admin help"));
        // Edit commands
        player.sendMessage(ChatUtil.translate("&#D10000/dungeon admin edit start &#D63333<dungeon> <floor>"));
        player.sendMessage(ChatUtil.translate("&#D10000/dungeon admin edit stop &#D63333[--confirm]"));
        player.sendMessage(ChatUtil.translate("&#D10000/dungeon admin webeditor start"));
        player.sendMessage(ChatUtil.translate("&#D10000/dungeon admin webeditor stop"));
        // Test commands
        player.sendMessage(ChatUtil.translate("&#D10000/dungeon admin test &#D63333<dungeon> <floor>"));
        // Other commands
        player.sendMessage(ChatUtil.translate("&#D10000/dungeon admin import &#D63333<world> <dungeon> <floor>"));
        player.sendMessage(ChatUtil.translate("&#D10000/dungeon admin load &#D63333<config>"));
        // Status commands
        player.sendMessage(ChatUtil.translate("&#D10000/dungeon admin status &#D63333<dungeon> [floor]"));
        player.sendMessage(ChatUtil.translate("&#D10000/dungeon admin goto &#D63333<server>"));
        player.sendMessage(ChatUtil.getBar());
    }

    @Command(names = "dungeon admin edit start", permission = "nextdungeons.admin")
    public static void adminDungeonEditCommand(Player player, @Param(name = "Dungeon ID") String dungeonId, @Param(name = "Floor ID") String floorId) {
        Floor floor = Floor.getFloor(dungeonId + "_" + floorId);
        if (floor == null) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&cFloor not found."));
            return;
        }

        FloorInstance floorInstance = new FloorInstance(floor.getId(),true);
        player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&a✓ &fEdit mode started for floor &e" + floor.getId() + "&f."));
        player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&fPlease wait while the instance is being prepared..."));
        floorInstance.sendToServer(player);
    }

    @Command(names = "dungeon admin edit stop", permission = "nextdungeons.admin")
    public static void adminDungeonSaveCommand(Player player, @Param(name = "Confirm", baseValue = "none")String confirm) {
        if(!ServerUtil.isInEditMode()) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&cThis server is not in edit mode."));
            return;
        }

        ServerUtil.InstanceInfo info = ServerUtil.getInstanceInfo();
        if(info == null || info.floorId() == null) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&cYou are not in a floor instance."));
            return;
        }

        Floor currentFloor = Floor.getFloor(info.floorId());

        if (currentFloor == null) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&cFloor not found."));
            return;
        }

        // Hey this path is really ugly but it works for now
        File savedFile = new File(Main.getInstance().getDataFolder() + "/../../../../../local/templates/" + currentFloor.getId() + "/default/plugins/NextDungeon/dungeons/", currentFloor.getId() + ".dungeon");

        if(!savedFile.exists()) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&eWarning no saved dungeon file found for this floor."));
            player.sendMessage(ChatUtil.translate("&eIf you have trigger changes they will be lost on the next dungeon start / edit."));
            player.sendMessage(ChatUtil.translate("&eIf you want to discard without saving use &c/dungeon admin edit stop --confirm"));
        } else if(savedFile.exists() && !confirm.equalsIgnoreCase("--confirm")) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&fAre you sure you want to end your edit mode?"));
            player.sendMessage(ChatUtil.translate("&fUse &b/dungeon admin edit stop --confirm &fif &ayes"));
        } else if (savedFile.exists() && confirm.equalsIgnoreCase("--confirm")) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&a✓ &fEdit mode ended."));
            try {
                Objects.requireNonNull(Bukkit.getWorld("world")).save();

                // Copy the world files to the template (strict minimum)
                FileUtil.copyDir(new File(Main.getInstance().getDataFolder() + "/../../world/data/"), new File(Main.getInstance().getDataFolder() + "/../../../../../local/templates/" + currentFloor.getId() + "/default/world/data"));
                FileUtil.copyDir(new File(Main.getInstance().getDataFolder() + "/../../world/entities/"), new File(Main.getInstance().getDataFolder() + "/../../../../../local/templates/" + currentFloor.getId() + "/default/world/entities"));
                FileUtil.copyDir(new File(Main.getInstance().getDataFolder() + "/../../world/region/"), new File(Main.getInstance().getDataFolder() + "/../../../../../local/templates/" + currentFloor.getId() + "/default/world/region"));
                //FileUtil.copyDir(new File(Main.getInstance().getDataFolder() + "/../../world/poi/"), new File(Main.getInstance().getDataFolder() + "/../../../../../local/templates/" + currentFloor.getId() + "/default/world/poi"));
                FileUtil.copyFile(new File(Main.getInstance().getDataFolder() + "/../../world/uid.dat"), new File(Main.getInstance().getDataFolder() + "/../../../../../local/templates/" + currentFloor.getId() + "/default/world/uid.dat"));
                FileUtil.copyFile(new File(Main.getInstance().getDataFolder() + "/../../world/level.dat"), new File(Main.getInstance().getDataFolder() + "/../../../../../local/templates/" + currentFloor.getId() + "/default/world/level.dat"));

                player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&a✓ &fWorld changes saved."));
                player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&fThe server will now shutdown."));
                Bukkit.getScheduler().runTaskLater(Main.getInstance(), Bukkit::shutdown, 100L);
            }catch (Exception e) {
                player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&cError while saving the world changes: " + e.getMessage()));
            }
        }
    }

    @Command(names = "dungeon admin webeditor start", permission = "nextdungeons.admin")
    public static void adminDungeonWebEditorStartCommand(Player player) {
        if(Main.getInstance().getWebEditorManager().hasActiveEditor(player)) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&cYou already have an active web editor session."));
            return;
        }

        if(!ServerUtil.isInEditMode()) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&cThis server is not in edit mode."));
            return;
        }

        ServerUtil.InstanceInfo info = ServerUtil.getInstanceInfo();
        if(info == null || info.floorId() == null) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&cYou are not in a floor instance."));
            return;
        }

        Dungeon currentDungeon = Dungeon.getDungeon(info.floorId().split("_")[0]);
        Floor currentFloor = Floor.getFloor(info.floorId());

        if (currentFloor == null) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&cFloor not found."));
            return;
        }

        boolean success = Main.getInstance().getWebEditorManager().startWebEditor(player, currentDungeon.getName(), currentFloor.getId());

        // Le message de succès avec l'URL est maintenant géré dans DungeonWebEditorManager
    }

    @Command(names = "dungeon admin webeditor stop", permission = "nextdungeons.admin")
    public static void adminDungeonWebEditorStopCommand(Player player) {
        boolean success = Main.getInstance().getWebEditorManager().stopWebEditor(player);
        if (success) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&a✓ Web editor stopped."));
        }
    }

    @Command(names = "dungeon admin test")
    public static void adminDungeonPlayCommand(Player player, @Param(name = "Dungeon ID") String dungeonId, @Param(name = "Floor ID") String floorId) {
        Floor floor = Floor.getFloor(dungeonId + "_" + floorId);
        if (floor == null) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&cFloor not found."));
            return;
        }

        FloorInstance floorInstance = new FloorInstance(floor.getId());
        player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&a✓ &fTest instance started for floor &e" + floor.getId() + "&f."));
        player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&fPlease wait while the instance is being prepared..."));
        floorInstance.sendToServer(player);
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
