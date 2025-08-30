package fr.perrier.dungeons.commands;

import fr.perrier.cupcodeapi.commands.annotations.Command;
import fr.perrier.cupcodeapi.commands.annotations.Param;
import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.menu.dungeon.DungeonGateMenu;
import fr.perrier.dungeons.model.Dungeon;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class ConsoleCommands {

    @Command(names = {"dungeon console openmenu"}, permission = "dungeons.admin")
    public static void openMenu(CommandSender sender, @Param(name = "Dungeon ID") String dungeonId, @Param(name = "Player")String playerName) {
        if(!(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage("This command can only be executed by the console.");
            return;
        }
        Player player = Bukkit.getPlayerExact(playerName);
        if(player == null) {
            Main.getInstance().getLogger().severe("&#D10000Player " + playerName + " not found or not online.");
            return;
        }
        Dungeon dungeon = Dungeon.getDungeon(dungeonId);
        if(dungeon == null) {
            Main.getInstance().getLogger().severe("&#D10000Dungeon with ID " + dungeonId + " not found.");
            return;
        }
        new DungeonGateMenu(dungeon).openMenu(player);
    }
}
