package fr.perrier.dungeons.commands;

import fr.perrier.cupcodeapi.commands.annotations.Command;
import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.dungeons.Main;
import org.bukkit.entity.Player;

public class PlayerCommands {

    @Command(names = {"dungeon","dungeons","nextdungeon","nextdungeons","nd"})
    public static void onDungeonCommand(Player player) {
        player.sendMessage(ChatUtil.getBar());
        player.sendMessage(ChatUtil.translate("<gradient:#8B0000:bold>NextDungeon</gradient:#D10000>&8| &fPlayer Commands"));
        player.sendMessage("");
        player.sendMessage(ChatUtil.translate("&#D10000/dungeon &8- &fGet the list of available commands"));
        player.sendMessage("");
        player.sendMessage(ChatUtil.getBar());
    }

    @Command(names = {"dungeon help","dungeons help","nextdungeon help","nextdungeons help","nd help"})
    public static  void onDungeonHelpCommand(Player player) {
        onDungeonCommand(player);
    }
}
