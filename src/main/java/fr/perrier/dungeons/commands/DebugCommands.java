package fr.perrier.dungeons.commands;

import fr.perrier.cupcodeapi.commands.annotations.Command;
import fr.perrier.cupcodeapi.commands.annotations.Param;
import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.menu.dungeon.DungeonGateMenu;
import fr.perrier.dungeons.menu.dungeon.PartyBuilderMenu;
import fr.perrier.dungeons.menu.dungeon.PartyFinderMenu;
import fr.perrier.dungeons.model.FloorInstance;
import fr.perrier.dungeons.model.Dungeon;
import fr.perrier.dungeons.model.Floor;
import fr.perrier.dungeons.parties.DungeonParty;
import lombok.SneakyThrows;
import org.bukkit.entity.Player;

public class DebugCommands {

    @Command(names = "dungeon debug help")
    public static void debugDungeonCommand(Player player) {
        player.sendMessage(ChatUtil.getBar());
        player.sendMessage(ChatUtil.translate("/dungeon debug help"));
        player.sendMessage(ChatUtil.getBar());
    }

    @Command(names = "dungeon debug list instances")
    public static void debugDungeonListInstancesCommand(Player player) {
        player.sendMessage(ChatUtil.getBar());
        player.sendMessage(ChatUtil.translate("&6Instances:"));
        for (FloorInstance instance : Main.getInstance().getRedisStorageService().getInstancesMap().values()) {
            player.sendMessage(ChatUtil.translate("  &8- &e" + instance.getInstanceName() + " &8(&7&o" + instance.getInstanceId() + "&8)"));
        }
        player.sendMessage(ChatUtil.getBar());
    }

    @Command(names = "dungeon debug list floors")
    public static void debugDungeonListFloorsCommand(Player player) {
        player.sendMessage(ChatUtil.getBar());
        player.sendMessage(ChatUtil.translate("&6Floors:"));
        for (Floor floor : Main.getInstance().getRedisStorageService().getFloorsMap().values()) {
            player.sendMessage(ChatUtil.translate("  &8- &e" + floor.getName() + " &8(&7&o" + floor.getId() + "&8)"));
        }
        player.sendMessage(ChatUtil.getBar());
    }

    @Command(names = "dungeon debug list dungeons")
    public static void debugDungeonListDungeonsCommand(Player player) {
        player.sendMessage(ChatUtil.getBar());
        player.sendMessage(ChatUtil.translate("&6Dungeons:"));
        for (Dungeon dungeon : Dungeon.getDungeons().values()) {
            player.sendMessage(ChatUtil.translate("  &8- &e" + dungeon.getName() + " &8(&7&o" + dungeon.getId() + "&8)"));
        }
        player.sendMessage(ChatUtil.getBar());
    }

    @SneakyThrows
    @Command(names = "dungeon debug openmenu")
    public static void debugDungeonOpenMenuCommand(Player player, @Param(name = "class name")String className) {

        Dungeon dungeon = new Dungeon("example","Example");
        dungeon.addFloor(new Floor("example_floor1","Floor 1"));
        dungeon.addFloor(new Floor("example_floor2","Floor 2"));

        switch (className) {
            case "DungeonGateMenu" -> new DungeonGateMenu(dungeon).openMenu(player);
            case "PartyBuilderMenu" -> new PartyBuilderMenu(null,"example").openMenu(player);
            case "PartyFinderMenu" -> new PartyFinderMenu(dungeon).openMenu(player);
        }
    }

    @Command(names = "dungeon debug loreparty")
    public static void debugDungeonLorePartyCommand(Player player) {
        player.sendMessage(ChatUtil.getBar());
        DungeonParty dungeonParty = DungeonParty.getParties().get(player.getUniqueId());
        for(String lore : PartyFinderMenu.PartyButton.getLore(dungeonParty)) {
            player.sendMessage(ChatUtil.translate(lore));
        }
        player.sendMessage(ChatUtil.getBar());
    }
}
