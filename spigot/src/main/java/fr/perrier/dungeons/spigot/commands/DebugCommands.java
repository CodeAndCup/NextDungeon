package fr.perrier.dungeons.spigot.commands;

import fr.perrier.cupcodeapi.commands.annotations.Command;
import fr.perrier.cupcodeapi.commands.annotations.Param;
import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.dungeons.common.model.dungeon.FloorData;
import fr.perrier.dungeons.common.model.dungeon.config.FloorInstanceData;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.menu.dungeon.DungeonGateMenu;
import fr.perrier.dungeons.spigot.model.FloorInstance;
import fr.perrier.dungeons.spigot.model.Dungeon;
import fr.perrier.dungeons.spigot.model.Floor;
import org.bukkit.entity.Player;

public class DebugCommands {

    @Command(names = {"dungeon debug help", "dungeons debug help", "nextdungeon debug help", "nextdungeons debug help", "nd debug help"})
    public static void debugDungeonCommand(Player player) {
        player.sendMessage(ChatUtil.getBar());
        player.sendMessage(ChatUtil.translate("<gradient:#8B0000:bold>NextDungeon</gradient:#D10000> &8| &fDebug Commands"));
        player.sendMessage("");
        player.sendMessage(ChatUtil.translate("&#D10000/dungeon debug help &8- &fGet the list of available commands"));
        player.sendMessage(ChatUtil.translate("&#D10000/dungeon debug list dungeons &8- &fList all dungeons"));
        player.sendMessage(ChatUtil.translate("&#D10000/dungeon debug list floors &8- &fList all floors"));
        player.sendMessage(ChatUtil.translate("&#D10000/dungeon debug list instances &8- &fList all instances"));
        player.sendMessage(ChatUtil.translate("&#D10000/dungeon debug openmenu &8- &fOpen the dungeon example menu"));
        player.sendMessage("");
        player.sendMessage(ChatUtil.getBar());
    }

    @Command(names = {"dungeon debug list instances", "dungeons debug list instances", "nextdungeon debug list instances", "nextdungeons debug list instances", "nd debug list instances"})
    public static void debugDungeonListInstancesCommand(Player player) {
        player.sendMessage(ChatUtil.getBar());
        player.sendMessage(ChatUtil.translate("&6Instances:"));
        for (FloorInstanceData instanceData : Main.getInstance().getRedisStorageService().getInstancesMap().values()) {
            player.sendMessage(ChatUtil.translate("  &8- &e" + instanceData.getInstanceName() + " &8(&7&o" + instanceData.getInstanceId() + "&8)"));
        }
        player.sendMessage(ChatUtil.getBar());
    }

    @Command(names = {"dungeon debug list floors", "dungeons debug list floors", "nextdungeon debug list floors", "nextdungeons debug list floors", "nd debug list floors"})
    public static void debugDungeonListFloorsCommand(Player player) {
        player.sendMessage(ChatUtil.getBar());
        player.sendMessage(ChatUtil.translate("&6Floors:"));
        for (FloorData floorData : Main.getInstance().getRedisStorageService().getFloorsMap().values()) {
            player.sendMessage(ChatUtil.translate("  &8- &e" + floorData.getName() + " &8(&7&o" + floorData.getId() + "&8)"));
            player.sendMessage(ChatUtil.translate("      &8- &eDescription: &f" + floorData.getDescription()));
            player.sendMessage(ChatUtil.translate("      &8- &eNumber of Steps: &f" + floorData.getSteps().size()));
            player.sendMessage(ChatUtil.translate("      &8- &eNumber of Triggers: &f" + floorData.getTriggers().size()));
        }
        player.sendMessage(ChatUtil.getBar());
    }

    @Command(names = {"dungeon debug list dungeons", "dungeons debug list dungeons", "nextdungeon debug list dungeons", "nextdungeons debug list dungeons", "nd debug list dungeons"})
    public static void debugDungeonListDungeonsCommand(Player player) {
        player.sendMessage(ChatUtil.getBar());
        player.sendMessage(ChatUtil.translate("&6Dungeons:"));
        for (Dungeon dungeon : Dungeon.getDungeons()) {
            player.sendMessage(ChatUtil.translate("  &8- &e" + dungeon.getName() + " &8(&7&o" + dungeon.getId() + "&8)"));
        }
        player.sendMessage(ChatUtil.getBar());
    }

    @Command(names = {"dungeon debug openmenu", "dungeons debug openmenu", "nextdungeon debug openmenu", "nextdungeons debug openmenu", "nd debug openmenu"})
    public static void debugDungeonOpenMenuCommand(Player player) {

        Dungeon dungeon = Dungeon.getDungeon("example");
        if(dungeon == null) {
            player.sendMessage(ChatUtil.translate("&cDungeon 'example' not found."));
            return;
        }
        new DungeonGateMenu(dungeon).openMenu(player);
    }

    @Command(names = "dungeon debug print")
    public static void debugDungeonPrintCommand(Player player, @Param(name = "message", wildcard = true) String message) {
        player.sendMessage(ChatUtil.translate(message));
    }

    @Command(names = "dungeon debug floor")
    public static void debugDungeonFloorCommand(Player player, @Param(name = "dungeonId")String dungeonId, @Param(name = "floorId") String floorId) {
        Floor floor = Floor.getFloor(dungeonId + "_" + floorId);
        if (floor == null) {
            player.sendMessage(ChatUtil.translate("&cFloor with ID '" + floorId + "' not found."));
            return;
        }
        player.sendMessage(ChatUtil.getBar());
        player.sendMessage(ChatUtil.translate("&6Floor Info:"));
        player.sendMessage(ChatUtil.translate("  &8- &eID: &f" + floor.getId()));
        player.sendMessage(ChatUtil.translate("  &8- &eName: &f" + floor.getName()));
        player.sendMessage(ChatUtil.translate("  &8- &eDescription: &f" + floor.getDescription()));
        player.sendMessage(ChatUtil.translate("  &8- &eNumber of Steps: &f" + floor.getSteps().size()));
        player.sendMessage(ChatUtil.translate("  &8- &eNumber of Triggers: &f" + floor.getTriggers().size()));
        player.sendMessage(ChatUtil.getBar());
    }
}
