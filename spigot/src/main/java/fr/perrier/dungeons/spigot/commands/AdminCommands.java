package fr.perrier.dungeons.spigot.commands;

import fr.perrier.cupcodeapi.commands.annotations.Command;
import fr.perrier.cupcodeapi.commands.annotations.Param;
import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.configuration.ConfigLoader;
import fr.perrier.dungeons.spigot.instance.InstanceInfo;
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

    @Command(
            names = {"dungeon admin edit start", "dungeons admin edit start", "nextdungeon admin edit start", "nextdungeons admin edit start", "nd admin edit start"},
            permission = "nextdungeons.admin")
    public static void adminDungeonEditCommand(Player player, @Param(name = "Dungeon ID") String dungeonId, @Param(name = "Floor ID") String floorId) {
        Floor floor = Floor.getFloor(dungeonId + "_" + floorId);
        if (floor == null) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&cFloor not found."));
            return;
        }

        FloorInstance.generateNewInstanceAsync(floor.getId(),true,floorInstance -> floorInstance.sendToServer(player));
        player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&a✓ &fEdit mode started for floor &e" + floor.getId() + "&f."));
        player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&fPlease wait while the instance is being prepared..."));
    }

    @Command(
            names = {"dungeon admin edit stop", "dungeons admin edit stop", "nextdungeon admin edit stop", "nextdungeons admin edit stop", "nd admin edit stop"},
            permission = "nextdungeons.admin")
    public static void adminDungeonSaveCommand(Player player, @Param(name = "Confirm", baseValue = "none")String confirm) {
        if(!ServerUtil.isInEditMode()) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&cThis server is not in edit mode."));
            return;
        }

        InstanceInfo info = ServerUtil.getInstanceInfo();
        if(info == null || info.getFloorId() == null) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&cYou are not in a floor instance."));
            return;
        }

        Floor currentFloor = Floor.getFloor(info.getFloorId());

        if (currentFloor == null) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&cFloor not found."));
            return;
        }

        // Vérifier si des triggers existent dans la base de données
        Main.getInstance().getDatabaseManager().triggersExist(currentFloor.getId()).thenAccept(triggersExist -> {
            if(!triggersExist) {
                player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&eWarning: no saved triggers found for this floor in database."));
                player.sendMessage(ChatUtil.translate("&eIf you have trigger changes they will be lost on the next dungeon start / edit."));
                player.sendMessage(ChatUtil.translate("&eIf you want to discard without saving use &c/dungeon admin edit stop --confirm"));
            } else if(triggersExist && !confirm.equalsIgnoreCase("--confirm")) {
                player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&fAre you sure you want to end your edit mode?"));
                player.sendMessage(ChatUtil.translate("&fUse &b/dungeon admin edit stop --confirm &fif &ayes"));
            } else if (triggersExist && confirm.equalsIgnoreCase("--confirm")) {
                saveAndShutdown(player, currentFloor);
            }
        }).exceptionally(ex -> {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&cError checking triggers: " + ex.getMessage()));
            return null;
        });
    }

    /**
     * Sauvegarde le monde et arrête le serveur après l'édition
     */
    private static void saveAndShutdown(Player player, Floor currentFloor) {
        player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&a✓ &fEdit mode ended."));

        // Utiliser le provider d'instance pour sauvegarder le monde (adapté à CloudNet, ASP ou Vanilla)
        Main.getInstance().getInstanceProvider().saveEditWorldToTemplate(currentFloor).thenAccept(success -> {
            if (success) {
                player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&a✓ &fWorld changes saved to template."));
                player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&7Provider: &e" + Main.getInstance().getInstanceProvider().getType()));
                player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&7Triggers saved in database: &e" + Main.getInstance().getConfig().getString("DatabaseConfiguration.type")));
                player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&fThe server will now shutdown."));

                Bukkit.getScheduler().runTaskLater(Main.getInstance(), Bukkit::shutdown, 100L);
            } else {
                player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&cError while saving the world changes."));
                player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&cCheck the console for details."));
            }
        }).exceptionally(ex -> {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&cError while saving the world changes: " + ex.getMessage()));
            return null;
        });
    }

    @Command(
            names = {"dungeon admin webeditor start", "dungeons admin webeditor start", "nextdungeon admin webeditor start", "nextdungeons admin webeditor start", "nd admin webeditor start"},
            permission = "nextdungeons.admin")
    public static void adminDungeonWebEditorStartCommand(Player player) {
        if(Main.getInstance().getWebEditorManager().hasActiveEditor(player)) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&cYou already have an active web editor session."));
            return;
        }

        if(!ServerUtil.isInEditMode()) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&cThis server is not in edit mode."));
            return;
        }

        InstanceInfo info = ServerUtil.getInstanceInfo();
        if(info == null || info.getFloorId() == null) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&cYou are not in a floor instance."));
            return;
        }

        Dungeon currentDungeon = Dungeon.getDungeon(info.getFloorId().split("_")[0]);
        Floor currentFloor = Floor.getFloor(info.getFloorId());

        if (currentFloor == null) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&cFloor not found."));
            return;
        }

        Main.getInstance().getWebEditorManager().startWebEditor(player, currentDungeon.getName(), currentFloor.getId());

        // Le message de succès avec l'URL est maintenant géré dans DungeonWebEditorManager
    }

    @Command(
            names = {"dungeon admin webeditor stop", "dungeons admin webeditor stop", "nextdungeon admin webeditor stop", "nextdungeons admin webeditor stop", "nd admin webeditor stop"},
            permission = "nextdungeons.admin")
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

        FloorInstance.generateNewInstanceAsync(floor.getId(),false,floorInstance -> floorInstance.sendToServer(player));
        player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&a✓ &fTest instance started for floor &e" + floor.getId() + "&f."));
        player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&fPlease wait while the instance is being prepared..."));
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

    @Command(
            names = {"dungeon admin status", "dungeons admin status", "nextdungeon admin status", "nextdungeons admin status", "nd admin status"},
            permission = "nextdungeons.admin")
    public static void adminDungeonStatusParamCommand(Player player, @Param(name = "Instance ID", baseValue = "00000000-0000-0000-0000-000000000000") String instanceId) {
        player.sendMessage(ChatUtil.getBar());
        player.sendMessage(ChatUtil.translate("&6Dungeon Status"));

        boolean isDefaultInstance = instanceId.equals("00000000-0000-0000-0000-000000000000");
        if (isDefaultInstance && ServerUtil.isInstanceServer()) {
            InstanceInfo info = ServerUtil.getInstanceInfo();
            if (info != null) {
                player.sendMessage(ChatUtil.translate("&7Server Type: &aDungeon Instance"));
                player.sendMessage(ChatUtil.translate("&7Instance ID: &f" + info.getInstanceId()));
                player.sendMessage(ChatUtil.translate("&7Floor ID: &f" + info.getFloorId()));
                player.sendMessage(ChatUtil.translate("&7Created At: &f" + info.getCreatedAt()));

                FloorInstance instance = Main.getInstance().getRedisStorageService().getCurrentInstance().get();
                if (instance != null) {
                    player.sendMessage(ChatUtil.translate("&7Ready: &f" + instance.isReady()));
                }
            } else {
                player.sendMessage(ChatUtil.translate("&cInstance info introuvable."));
            }
        } else if (isDefaultInstance) {
            player.sendMessage(ChatUtil.translate("&7Server Type: &aLobby"));
        } else {
            FloorInstance instance = Main.getInstance().getRedisStorageService().getInstance(UUID.fromString(instanceId));
            if (instance == null) {
                player.sendMessage(ChatUtil.translate("&cInstance not found"));
            } else {
                InstanceInfo info = ServerUtil.getInstanceInfo(instance.getInstanceId());
                player.sendMessage(ChatUtil.translate("&7Server Type: &aDungeon Instance"));
                player.sendMessage(ChatUtil.translate("&7Instance ID: &f" + info.getInstanceId()));
                player.sendMessage(ChatUtil.translate("&7Floor ID: &f" + info.getFloorId()));
                player.sendMessage(ChatUtil.translate("&7Created At: &f" + info.getCreatedAt()));
                player.sendMessage(ChatUtil.translate("&7Ready: &f" + instance.isReady()));
            }
        }
        player.sendMessage(ChatUtil.getBar());
    }
}
