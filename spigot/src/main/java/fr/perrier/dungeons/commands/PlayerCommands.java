package fr.perrier.dungeons.commands;

import fr.perrier.cupcodeapi.commands.annotations.Command;
import fr.perrier.cupcodeapi.utils.ChatUtil;
import org.bukkit.command.CommandSender;

public class PlayerCommands {

    @Command(names = {"dungeon","dungeons","nextdungeon","nextdungeons","nd"})
    public static void onDungeonCommand(CommandSender sender) {
        sender.sendMessage(ChatUtil.getBar());
        sender.sendMessage(ChatUtil.translate("<gradient:#8B0000:bold>NextDungeon</gradient:#D10000> &8| &fPlayer Commands"));
        sender.sendMessage("");
        sender.sendMessage(ChatUtil.translate("&#D10000/dungeon &8- &fGet the list of available commands"));
        sender.sendMessage("");
        sender.sendMessage(ChatUtil.getBar());
    }

    @Command(names = {"dungeon help","dungeons help","nextdungeon help","nextdungeons help","nd help"})
    public static  void onDungeonHelpCommand(CommandSender sender) {
        onDungeonCommand(sender);
    }
}
