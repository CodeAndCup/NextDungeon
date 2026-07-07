package fr.perrier.dungeons.spigot.workflow.action.impl;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.workflow.action.Action;
import fr.perrier.dungeons.spigot.webeditor.blockly.BlocklyAction;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyInfo;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.Serial;
import java.util.Map;

@Setter
@Getter
@BlocklyInfo(
        name = "broadcast_command_action",
        color = "#2196F3",
        displayText = "⌨️ Exécuter commande console",
        tooltip = "Exécute une commande en tant que console serveur.",
        category = "Actions"
)
public class BroadcastCommandAction extends Action implements BlocklyAction {
    @Serial
    private static final long serialVersionUID = 1L;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Commande:", defaultValue = "say Hello World!", order = 1)
    private String command;

    public BroadcastCommandAction() {
        super("BroadcastCommand", "broadcast_command_action");
        this.command = "say Hello World!";
    }

    public BroadcastCommandAction(String command) {
        super("BroadcastCommand", "broadcast_command_action");
        this.command = command;
    }

    @Override
    public boolean execute(Player player, Location location, Map<String, Object> data) {
        if (command == null || command.isEmpty()) {
            return false;
        }

        if(Main.getLoggerUtil().isDebugEnabled())
            Main.getLoggerUtil().info("Executing command: " + command);

        if (command.contains("@player") && command.contains("@all")) {
            Main.getLoggerUtil().warning("Warning: The command cannot contain both @player and @all placeholders.");
            return false;
        }

        if (command.contains("@player")) {
            if (player == null) {
                Main.getLoggerUtil().warning("Warning: The command contains @player placeholder but the player is null.");
                return false;
            }
            String resolved = command.replace("@player", player.getName());
            if(Main.getLoggerUtil().isDebugEnabled())
                Main.getLoggerUtil().info("Executing command '" + resolved + "' for player: " + player.getName());
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), resolved);
        } else if (command.contains("@all")) {
            for (Player target : Bukkit.getOnlinePlayers()) {
                String resolved = command.replace("@all", target.getName());
                if(Main.getLoggerUtil().isDebugEnabled())
                    Main.getLoggerUtil().info("Executing command '" + resolved + "' for player: " + target.getName());
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), resolved);
            }
        } else {
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
        }

        return true;
    }
}

