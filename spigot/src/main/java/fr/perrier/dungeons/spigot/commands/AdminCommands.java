package fr.perrier.dungeons.spigot.commands;

import fr.perrier.cupcodeapi.commands.annotations.Command;
import fr.perrier.cupcodeapi.commands.annotations.Param;
import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.dungeons.common.model.dungeon.FloorData;
import fr.perrier.dungeons.common.module.NextDungeonModule;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.instance.InstanceInfo;
import fr.perrier.dungeons.spigot.model.Dungeon;
import fr.perrier.dungeons.spigot.model.FloorInstance;
import fr.perrier.dungeons.spigot.model.Floor;
import fr.perrier.dungeons.spigot.module.ModuleLoader;
import fr.perrier.dungeons.spigot.parties.impl.DungeonPartyImpl;
import fr.perrier.dungeons.spigot.utils.ServerUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class AdminCommands {

    private AdminCommands() {}

    @Command(names = {"dungeon admin help", "dungeons admin help", "nextdungeon admin help", "nextdungeons admin help", "nd admin help"}, permission = "nextdungeon.admin")
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
        player.sendMessage(ChatUtil.translate("&#D10000/dungeon admin run &#D63333<floor>"));
        // Queue commands
        player.sendMessage(ChatUtil.translate("&#D10000/dungeon admin queue &8- &fQueue management"));
        // Status commands
        player.sendMessage(ChatUtil.translate("&#D10000/dungeon admin status &#D63333<dungeon> [floor]"));
        // Module commands
        player.sendMessage(ChatUtil.translate("&#D10000/dungeon admin module list"));
        player.sendMessage(ChatUtil.translate("&#D10000/dungeon admin module load &#D63333<file.jar>"));
        player.sendMessage(ChatUtil.translate("&#D10000/dungeon admin module unload &#D63333<moduleId>"));
        player.sendMessage(ChatUtil.translate("&#D10000/dungeon admin module reload &#D63333<moduleId>"));
        player.sendMessage(ChatUtil.getBar());
    }

    // ======================= Edit Commands =======================

    @Command(
            names = {"dungeon admin edit start", "dungeons admin edit start", "nextdungeon admin edit start", "nextdungeons admin edit start", "nd admin edit start"},
            permission = "nextdungeon.admin")
    public static void adminDungeonEditCommand(Player player, @Param(name = "Floor ID", tabCompleteFlags = {"floors"}) FloorData floorData) {
        if(floorData == null) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000FloorData not found."));
            return;
        }

        Floor floor = Floor.getFloor(floorData.getId());
        if (floor == null) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Floor not found."));
            return;
        }

        // Prevent starting edit instance if player already in an instance
        if (Main.getInstance().getDungeonService().isPlayerInAnyInstance(player.getUniqueId())) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000You are already in an instance or it is being prepared."));
            return;
        }
        FloorInstance.generateNewInstanceAsync(floor.getId(), Set.of(player.getUniqueId()), true, floorInstance -> floorInstance.sendToServer(player));
        player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#00FF00✓ &fEdit mode started for floor &e" + floor.getId() + "&f."));
        player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&fPlease wait while the instance is being prepared..."));
    }

    @Command(
            names = {"dungeon admin edit stop", "dungeons admin edit stop", "nextdungeon admin edit stop", "nextdungeons admin edit stop", "nd admin edit stop"},
            permission = "nextdungeon.admin")
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
            if(!confirm.equalsIgnoreCase("--confirm")) {
                if (!Boolean.TRUE.equals(triggersExist)) {
                    player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&eWarning: no saved triggers found for this floor in database."));
                    player.sendMessage(ChatUtil.translate("&eIf you have trigger changes they will be lost on the next dungeon start / edit."));
                    player.sendMessage(ChatUtil.translate("&eIf you want to discard without saving use &#FF0000/dungeon admin edit stop --confirm"));
                } else {
                    player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&fAre you sure you want to end your edit mode?"));
                    player.sendMessage(ChatUtil.translate("&fUse &b/dungeon admin edit stop --confirm &fif &#00FF00yes"));
                }
            } else {
                saveAndShutdown(player, currentFloor);
            }
        }).exceptionally(ex -> {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Error checking triggers: " + ex.getMessage()));
            return null;
        });
    }

    @Command(names = {"dungeon admin run", "dungeons admin run", "nextdungeon admin run", "nextdungeons admin run", "nd admin run"}, permission = "nextdungeon.admin")
    public static void onDungeonJoinCommand(CommandSender sender, @Param(name = "Floor ID", tabCompleteFlags = {"floors"}) FloorData floorData) {
        Player player = (Player) sender;

        if (Main.getInstance().getQueueManager() == null) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Queue system is not available on this server"));
            return;
        }

        if (floorData == null) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000FloorData not found."));
            return;
        }

        Floor floor = Floor.getFloor(floorData.getId());
        if (floor == null) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Floor not found: " + floorData.getId()));
            return;
        }

        if(DungeonPartyImpl.getDungeonPartyOf(player) == null) {
            new DungeonPartyImpl.Builder()
                    .setDungeonId(floorData.getDungeonId())
                    .setFloorId(floorData.getId())
                    .setMinLevel(0)
                    .setDescription("")
                    .setLeader(player)
                    .build();
        }

        Main.getInstance().getQueueManager().requestInstance(player, floor);
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
            if (Boolean.TRUE.equals(success)) {
                player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#00FF00✓ &fWorld changes saved to template."));
                player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&7Provider: &e" + Main.getInstance().getInstanceProvider().getType()));
                player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&7Triggers saved in database: &e" + Objects.requireNonNull(Main.getInstance().getConfig().getString("DatabaseConfiguration.type"))));
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
            permission = "nextdungeon.admin")
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
            permission = "nextdungeon.admin")
    public static void adminDungeonWebEditorStopCommand(Player player) {
        Main.getInstance().getWebEditorManager().stopWebEditor(player);
    }

    // ======================= Utils Commands =======================

    @Command(
            names = {"dungeon admin status", "dungeons admin status", "nextdungeon admin status", "nextdungeons admin status", "nd admin status"},
            permission = "nextdungeon.admin")
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

    // ======================= Queue Commands =======================

    @Command(names = {"dungeon admin queue", "dungeons admin queue", "nextdungeon admin queue", "nextdungeons admin queue", "nd admin queue"}, permission = "nextdungeons.admin")
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

    @Command(names = {"dungeon admin queue clear", "dungeons admin queue clear", "nextdungeon admin queue clear", "nextdungeons admin queue clear", "nd admin queue clear"}, permission = "nextdungeons.admin")
    public static void adminQueueClearCommand(Player player, @Param(name = "Floor ID", tabCompleteFlags = {"floors"}) FloorData floorData) {
        if(floorData == null) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000FloorData not found."));
            return;
        }

        if (Main.getInstance().getDungeonQueueService() == null) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Queue service not available"));
            return;
        }

        Main.getInstance().getDungeonQueueService().clearQueue(floorData.getId());
        player.sendMessage(ChatUtil.translate(Main.getPrefix() + "Queue cleared for floor: " + floorData.getId()));
    }

    @Command(names = {"dungeon admin queue list", "dungeons admin queue list", "nd admin queue list"}, permission = "nextdungeons.admin")
    public static void adminQueueListCommand(Player player, @Param(name = "Floor ID", tabCompleteFlags = {"floors"}) FloorData floorData) {
        if(floorData == null) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000FloorData not found."));
            return;
        }

        if (Main.getInstance().getDungeonQueueService() == null) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Queue service not available"));
            return;
        }

        Floor floor = Floor.getFloor(floorData.getId());
        if (floor == null) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Floor not found: " + floorData.getId()));
            return;
        }

        player.sendMessage(ChatUtil.getBar());
        player.sendMessage(ChatUtil.translate("<gradient:#8B0000:bold>NextDungeon</gradient:#D10000> &8| &fQueue for " + floor.getName()));
        player.sendMessage("");

        var entries = Main.getInstance().getDungeonQueueService().getQueueEntries(floorData.getId());
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

    // ======================= Module Commands =======================

    @Command(names = {"dungeon admin module list", "dungeons admin module list", "nextdungeon admin module list", "nextdungeons admin module list", "nd admin module list"}, permission = "nextdungeons.admin")
    public static void adminModuleListCommand(Player player) {
        ModuleLoader loader = Main.getInstance().getModuleLoader();
        if (loader == null) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Module system not available."));
            return;
        }

        player.sendMessage(ChatUtil.getBar());
        player.sendMessage(ChatUtil.translate("<gradient:#8B0000:bold>NextDungeon</gradient:#D10000> &8| &fLoaded Modules"));
        player.sendMessage("");

        var modules = loader.getLoadedModules();
        if (modules.isEmpty()) {
            player.sendMessage(ChatUtil.translate("&7Aucun module chargé."));
        } else {
            for (NextDungeonModule module : modules.values()) {
                int blockCount = loader.getBlockRegistry().getBlocksByModule(module.getId()).size();
                player.sendMessage(ChatUtil.translate(String.format(
                    "&#00FF00● &f%s &7v%s &8(&e%s&8) &7— &b%d block(s)",
                    module.getName(),
                    module.getVersion(),
                    module.getId(),
                    blockCount
                )));
            }
        }

        player.sendMessage("");
        player.sendMessage(ChatUtil.translate("&7Total : &e" + modules.size() + " module(s)"));
        player.sendMessage(ChatUtil.getBar());
    }

    @Command(names = {"dungeon admin module load", "dungeons admin module load", "nextdungeon admin module load", "nextdungeons admin module load", "nd admin module load"}, permission = "nextdungeons.admin")
    public static void adminModuleLoadCommand(Player player, @Param(name = "JAR file name") String jarFileName) {
        ModuleLoader loader = Main.getInstance().getModuleLoader();
        if (loader == null) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Module system not available."));
            return;
        }

        player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&7Loading module from &e" + jarFileName + "&7..."));
        String moduleId = loader.loadModuleFromFile(jarFileName);
        if (moduleId != null) {
            NextDungeonModule module = loader.getLoadedModules().get(moduleId);
            String name = module != null ? module.getName() : moduleId;
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#00FF00✓ &fModule &e" + name + " &f(&7" + moduleId + "&f) chargé avec succès."));
        } else {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Échec du chargement. Vérifiez la console pour les détails."));
        }
    }

    @Command(names = {"dungeon admin module unload", "dungeons admin module unload", "nextdungeon admin module unload", "nextdungeons admin module unload", "nd admin module unload"}, permission = "nextdungeons.admin")
    public static void adminModuleUnloadCommand(Player player,  @Param(name = "Module ID", tabCompleteFlags = {"modules"}) NextDungeonModule module) {
        if(module == null) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Module not found."));
            return;
        }

        ModuleLoader loader = Main.getInstance().getModuleLoader();
        try {
            boolean success = loader.unloadModule(module.getId());
            if (success) {
                player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#00FF00✓ &fModule &e" + module.getName() + " &f(&7" + module.getId() + "&f) déchargé avec succès."));
            } else {
                player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Échec du déchargement. Vérifiez la console pour les détails."));
            }
        } catch (NullPointerException exception) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Module not found."));
        }
    }

    @Command(names = {"dungeon admin module reload", "dungeons admin module reload", "nextdungeon admin module reload", "nextdungeons admin module reload", "nd admin module reload"}, permission = "nextdungeons.admin")
    public static void adminModuleReloadCommand(Player player, @Param(name = "Module ID", tabCompleteFlags = {"modules"}) NextDungeonModule module) {
        if(module == null) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Module not found."));
            return;
        }

        ModuleLoader loader = Main.getInstance().getModuleLoader();
        player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&7Rechargement du module &e" + module.getName() + "&7..."));
        boolean success = loader.reloadModule(module.getId());

        if (success) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#00FF00✓ &fModule &e" + module.getName() + " &f(&7" + module.getId() + "&f) rechargé avec succès."));
        } else {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Échec du rechargement. Vérifiez la console pour les détails."));
        }
    }
}
