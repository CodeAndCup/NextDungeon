package fr.perrier.dungeons.commands;

import fr.perrier.cupcodeapi.commands.annotations.Command;
import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.dungeons.manager.FloorInstance;
import fr.perrier.dungeons.model.Dungeon;
import fr.perrier.dungeons.model.Floor;
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
        player.sendMessage(FloorInstance.getInstances().toString());
        player.sendMessage(ChatUtil.getBar());
    }

    @Command(names = "dungeon debug list floors")
    public static void debugDungeonListFloorsCommand(Player player) {
        player.sendMessage(ChatUtil.getBar());
        player.sendMessage(Floor.getFloors().toString());
        player.sendMessage(ChatUtil.getBar());
    }

    @Command(names = "dungeon debug list dungeons")
    public static void debugDungeonListDungeonsCommand(Player player) {
        player.sendMessage(ChatUtil.getBar());
        player.sendMessage(Dungeon.getDungeons().toString());
        player.sendMessage(ChatUtil.getBar());
    }
}
