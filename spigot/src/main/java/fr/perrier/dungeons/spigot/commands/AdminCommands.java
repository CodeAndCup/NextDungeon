package fr.perrier.dungeons.spigot.commands;

import fr.perrier.cupcodeapi.commands.annotations.Command;
import fr.perrier.cupcodeapi.commands.annotations.Param;
import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.instance.InstanceInfo;
import fr.perrier.dungeons.spigot.model.Dungeon;
import fr.perrier.dungeons.spigot.model.FloorInstance;
import fr.perrier.dungeons.spigot.model.Floor;
import fr.perrier.dungeons.spigot.utils.ServerUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Set;
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
        // Queue commands
        player.sendMessage(ChatUtil.translate("&#D10000/dungeon admin queue &8- &fQueue management"));
        // Other commands
        player.sendMessage(ChatUtil.translate("&#D10000/dungeon admin import &#D63333<world> <dungeon> <floor>"));
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
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Floor not found."));
            return;
        }

        FloorInstance.generateNewInstanceAsync(floor.getId(), Set.of(player.getUniqueId()), true, floorInstance -> floorInstance.sendToServer(player));
        player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#00FF00✓ &fEdit mode started for floor &e" + floor.getId() + "&f."));
        player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&fPlease wait while the instance is being prepared..."));
    }

    @Command(
            names = {"dungeon admin edit stop", "dungeons admin edit stop", "nextdungeon admin edit stop", "nextdungeons admin edit stop", "nd admin edit stop"},
            permission = "nextdungeons.admin")
    public static void adminDungeonSaveCommand(Player player, @Param(name = "Confirm", baseValue = "none")String confirm) {
        if(!ServerUtil.isInEditMode()) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000This server is not in edit mode."));
            return;
        }

        InstanceInfo info = ServerUtil.getInstanceInfo();
        if(info == null || info.getFloorId() == null) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000You are not in a floor instance."));
            return;
        }

        Floor currentFloor = Floor.getFloor(info.getFloorId());

        if (currentFloor == null) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Floor not found."));
            return;
        }

        // Vérifier si des triggers existent dans la base de données
        Main.getInstance().getDatabaseManager().triggersExist(currentFloor.getId()).thenAccept(triggersExist -> {
            if(!triggersExist) {
                player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&eWarning: no saved triggers found for this floor in database."));
                player.sendMessage(ChatUtil.translate("&eIf you have trigger changes they will be lost on the next dungeon start / edit."));
                player.sendMessage(ChatUtil.translate("&eIf you want to discard without saving use &#FF0000/dungeon admin edit stop --confirm"));
            } else{
                if(!confirm.equalsIgnoreCase("--confirm")) {
                    player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&fAre you sure you want to end your edit mode?"));
                    player.sendMessage(ChatUtil.translate("&fUse &b/dungeon admin edit stop --confirm &fif &#00FF00yes"));
                } else {
                    saveAndShutdown(player, currentFloor);
                }
            }
        }).exceptionally(ex -> {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Error checking triggers: " + ex.getMessage()));
            return null;
        });
    }

    /**
     * Save modifications and shutdown the server.
     *
     * @param player       Le joueur qui a initié la commande.
     * @param currentFloor Le floor actuellement édité.
     */
    private static void saveAndShutdown(Player player, Floor currentFloor) {
        player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#00FF00✓ &fEdit mode ended."));

        Main.getInstance().getInstanceProvider().saveEditWorldToTemplate(currentFloor).thenAccept(success -> {
            if (success) {
                player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#00FF00✓ &fWorld changes saved to template."));
                player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&7Provider: &e" + Main.getInstance().getInstanceProvider().getType()));
                player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&7Triggers saved in database: &e" + Main.getInstance().getConfig().getString("DatabaseConfiguration.type")));
                player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&fThe server will now shutdown."));

                Bukkit.getScheduler().runTaskLater(Main.getInstance(), Bukkit::shutdown, 100L);
            } else {
                player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Error while saving the world changes."));
                player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Check the console for details."));
            }
        }).exceptionally(ex -> {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Error while saving the world changes: " + ex.getMessage()));
            return null;
        });
    }

    @Command(
            names = {"dungeon admin webeditor start", "dungeons admin webeditor start", "nextdungeon admin webeditor start", "nextdungeons admin webeditor start", "nd admin webeditor start"},
            permission = "nextdungeons.admin")
    public static void adminDungeonWebEditorStartCommand(Player player) {
        InstanceInfo info = ServerUtil.getInstanceInfo();
        if(info == null || info.getFloorId() == null) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000You are not in a floor instance."));
            return;
        }

        if(!ServerUtil.isInEditMode()) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000This server is not in edit mode."));
            return;
        }

        if(Main.getInstance().getWebEditorManager().hasActiveEditor(player)) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000You already have an active web editor session."));
            return;
        }

        Dungeon currentDungeon = Dungeon.getDungeon(info.getFloorId().split("_")[0]);
        Floor currentFloor = Floor.getFloor(info.getFloorId());

        if (currentFloor == null) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Floor not found."));
            return;
        }

        Main.getInstance().getWebEditorManager().startWebEditor(player, currentDungeon.getName(), currentFloor.getId());
    }

    @Command(
            names = {"dungeon admin webeditor stop", "dungeons admin webeditor stop", "nextdungeon admin webeditor stop", "nextdungeons admin webeditor stop", "nd admin webeditor stop"},
            permission = "nextdungeons.admin")
    public static void adminDungeonWebEditorStopCommand(Player player) {
        Main.getInstance().getWebEditorManager().stopWebEditor(player);
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
                player.sendMessage(ChatUtil.translate("&7Server Type: &#00FF00Dungeon Instance"));
                player.sendMessage(ChatUtil.translate("&7Instance ID: &f" + info.getInstanceId()));
                player.sendMessage(ChatUtil.translate("&7Floor ID: &f" + info.getFloorId()));
                player.sendMessage(ChatUtil.translate("&7Created At: &f" + info.getCreatedAt()));

                FloorInstance instance = Main.getInstance().getDungeonService().getCurrentInstance();
                if (instance != null) {
                    player.sendMessage(ChatUtil.translate("&7Ready: &f" + instance.isReady()));
                }
            } else {
                player.sendMessage(ChatUtil.translate("&#FF0000Instance info introuvable."));
            }
        } else if (isDefaultInstance) {
            player.sendMessage(ChatUtil.translate("&7Server Type: &#00FF00Lobby"));
            player.sendMessage(ChatUtil.translate("&7Server Name: &f" + Main.getInstance().getServerNameService().getCachedServerName()));
        } else {
            FloorInstance instance = Main.getInstance().getDungeonService().getInstance(UUID.fromString(instanceId));
            if (instance == null) {
                player.sendMessage(ChatUtil.translate("&#FF0000Instance not found"));
            } else {
                InstanceInfo info = ServerUtil.getInstanceInfo(instance.getInstanceId());
                player.sendMessage(ChatUtil.translate("&7Server Type: &#00FF00Dungeon Instance"));
                player.sendMessage(ChatUtil.translate("&7Instance ID: &f" + info.getInstanceId()));
                player.sendMessage(ChatUtil.translate("&7Floor ID: &f" + info.getFloorId()));
                player.sendMessage(ChatUtil.translate("&7Created At: &f" + info.getCreatedAt()));
                player.sendMessage(ChatUtil.translate("&7Ready: &f" + instance.isReady()));
            }
        }
        player.sendMessage(ChatUtil.getBar());
    }

    @Command(names = {"dungeon admin queue", "dungeons admin queue", "nd admin queue"}, permission = "nextdungeons.admin")
    public static void adminQueueCommand(Player player) {
        player.sendMessage(ChatUtil.getBar());
        player.sendMessage(ChatUtil.translate("<gradient:#8B0000:bold>NextDungeon</gradient:#D10000> &8| &fQueue Management"));
        player.sendMessage("");
        player.sendMessage(ChatUtil.translate("&#D10000/dungeon admin queue status"));
        player.sendMessage(ChatUtil.translate("&#D10000/dungeon admin queue clear &#D63333<floor_id>"));
        player.sendMessage(ChatUtil.translate("&#D10000/dungeon admin queue list &#D63333<floor_id>"));
        player.sendMessage("");
        player.sendMessage(ChatUtil.getBar());
    }

    @Command(names = {"dungeon admin queue status", "dungeons admin queue status", "nd admin queue status"}, permission = "nextdungeons.admin")
    public static void adminQueueStatusCommand(Player player) {
        if (Main.getInstance().getDungeonQueueService() == null) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Queue service not available"));
            return;
        }

        player.sendMessage(ChatUtil.getBar());
        player.sendMessage(ChatUtil.translate("<gradient:#8B0000:bold>NextDungeon</gradient:#D10000> &8| &fQueue Status"));
        player.sendMessage("");

        for (String floorId : Main.getInstance().getDungeonQueueService().getActiveQueueFloors()) {
            Floor floor = Floor.getFloor(floorId);
            String floorName = floor != null ? floor.getName() : floorId;
            int queueSize = Main.getInstance().getDungeonQueueService().getQueueSize(floorId);
            int activeInstances = Main.getInstance().getDungeonQueueService().getActiveInstanceCount(floorId);
            int maxInstances = floor != null && floor.getRules() != null ? floor.getRules().getMaxInstance() : 0;

            player.sendMessage(ChatUtil.translate(String.format(
                "&#D10000%s &7(ID: %s)",
                floorName,
                floorId
            )));
            player.sendMessage(ChatUtil.translate(String.format(
                "  &7Queue Size: &e%d &7| Active Instances: &e%d/%s",
                queueSize,
                activeInstances,
                maxInstances > 0 ? String.valueOf(maxInstances) : "∞"
            )));
        }

        player.sendMessage("");
        player.sendMessage(ChatUtil.getBar());
    }

    @Command(names = {"dungeon admin queue clear", "dungeons admin queue clear", "nd admin queue clear"}, permission = "nextdungeons.admin")
    public static void adminQueueClearCommand(Player player, @Param(name = "Floor ID") String floorId) {
        if (Main.getInstance().getDungeonQueueService() == null) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Queue service not available"));
            return;
        }

        Main.getInstance().getDungeonQueueService().clearQueue(floorId);
        player.sendMessage(ChatUtil.translate(Main.getPrefix() + "Queue cleared for floor: " + floorId));
    }

    @Command(names = {"dungeon admin queue list", "dungeons admin queue list", "nd admin queue list"}, permission = "nextdungeons.admin")
    public static void adminQueueListCommand(Player player, @Param(name = "Floor ID") String floorId) {
        if (Main.getInstance().getDungeonQueueService() == null) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Queue service not available"));
            return;
        }

        Floor floor = Floor.getFloor(floorId);
        if (floor == null) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Floor not found: " + floorId));
            return;
        }

        player.sendMessage(ChatUtil.getBar());
        player.sendMessage(ChatUtil.translate("<gradient:#8B0000:bold>NextDungeon</gradient:#D10000> &8| &fQueue for " + floor.getName()));
        player.sendMessage("");

        var entries = Main.getInstance().getDungeonQueueService().getQueueEntries(floorId);
        if (entries.isEmpty()) {
            player.sendMessage(ChatUtil.translate("&7No players in queue"));
        } else {
            for (int i = 0; i < entries.size(); i++) {
                var entry = entries.get(i);
                player.sendMessage(ChatUtil.translate(String.format(
                    "&7%d. &f%s &7(Server: %s)",
                    i + 1,
                    entry.getPlayerName(),
                    entry.getServerName()
                )));
            }
        }

        player.sendMessage("");
        player.sendMessage(ChatUtil.getBar());
    }
}
