package fr.perrier.dungeons.commands;

import fr.perrier.cupcodeapi.commands.annotations.Command;
import fr.perrier.cupcodeapi.utils.ChatUtil;
import org.bukkit.entity.Player;

public class EditorCommands {

    @Command(names = "dungeon editor help")
    public static void editorDungeonCommand(Player player) {
        player.sendMessage(ChatUtil.getBar());
        player.sendMessage(ChatUtil.translate("/dungeon editor help"));
        player.sendMessage(ChatUtil.translate("/dungeon editor givetools"));
        player.sendMessage(ChatUtil.translate("/dungeon editor menu"));
        player.sendMessage(ChatUtil.translate("/dungeon editor setspawn"));
        player.sendMessage(ChatUtil.translate("/dungeon editor save"));
        player.sendMessage(ChatUtil.getBar());
    }
}
