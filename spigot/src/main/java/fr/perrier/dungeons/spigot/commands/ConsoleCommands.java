package fr.perrier.dungeons.spigot.commands;

import fr.perrier.cupcodeapi.commands.annotations.Command;
import fr.perrier.cupcodeapi.commands.annotations.Param;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.menu.dungeon.DungeonGateMenu;
import fr.perrier.dungeons.spigot.model.Dungeon;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class ConsoleCommands {

    private ConsoleCommands() {
        // Private constructor to prevent instantiation
    }

    @Command(names = {"dungeon console openmenu", "dungeons console openmenu", "nextdungeon console openmenu", "nextdungeons console openmenu", "nd console openmenu"}, permission = "nextdungeon.console")
    public static void openMenu(CommandSender sender, @Param(name = "Dungeon ID", tabCompleteFlags = {"dungeons"}) Dungeon dungeon, @Param(name = "Player")String playerName) {
        if(!(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage("This command can only be executed by the console.");
            return;
        }

        Player player = Bukkit.getPlayerExact(playerName);
        if(player == null) {
            Main.getLoggerUtil().severe("Player " + playerName + " not found or not online.");
            return;
        }

        if(dungeon == null) {
            Main.getLoggerUtil().severe("Dungeon not found.");
            return;
        }

        new DungeonGateMenu(dungeon).openMenu(player);
    }

    @Command(names = {"dungeon console end", "dungeons console end", "nextdungeon console end", "nextdungeons console end", "nd console end"}, permission = "nextdungeon.console")
    public static void endDungeon(CommandSender sender, @Param(name = "completed") boolean completed) {
        if(!(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage("This command can only be executed by the console.");
            return;
        }

        Main.getInstance().getDungeonService().getCurrentInstance().complete(completed);
    }
}
