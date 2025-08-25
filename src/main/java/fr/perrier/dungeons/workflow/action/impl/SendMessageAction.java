package fr.perrier.dungeons.workflow.action.impl;

import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.dungeons.webserver.blockly.BlocklyAction;
import fr.perrier.dungeons.webserver.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.webserver.blockly.annotations.BlocklyInfo;
import fr.perrier.dungeons.workflow.action.Action;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.Bukkit;

import java.util.Map;

/**
 * Action pour envoyer un message à un joueur
 */
@Setter
@Getter
@BlocklyInfo(
        name = "send_message_action",
        color = "#2196F3",
        displayText = "💬 Envoyer message",
        tooltip = "Envoie un message à un joueur spécifique\n{player} = joueur déclencheur\n{target} = joueur cible\n& = codes couleur",
        category = "Actions"
)
public class SendMessageAction extends Action implements BlocklyAction {
    private static final long serialVersionUID = 1L;

    @BlocklyField(type = BlocklyField.FieldType.DROPDOWN, label = "À:",
            options = "player,@all", defaultValue = "player", order = 1)
    private String targetPlayer;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Message:",
            defaultValue = "&aBonjour {player}!", order = 2)
    private String message;

    public SendMessageAction(String targetPlayer, String message) {
        super("SendMessage", "send_message_action");
        this.targetPlayer = targetPlayer;
        this.message = message;
    }

    @Override
    public String getBlockName() {
        return "send_message_action";
    }

    @Override
    public String getColor() {
        return "#2196F3";
    }

    @Override
    public String getTooltip() {
        return "Envoie un message à un joueur spécifique";
    }

    @Override
    public String getDisplayText() {
        return "💬 Envoyer message";
    }

    @Override
    public boolean execute(Player triggerPlayer, Location location, Map<String, Object> data) {
        if (message == null || message.isEmpty()) {
            return false;
        }

        Player target = null;

        // Déterminer le joueur cible
        if (targetPlayer == null || targetPlayer.isEmpty() || "player".equals(targetPlayer)) {
            target = triggerPlayer; // Le joueur qui a déclenché le trigger
        } else if("@all".equals(targetPlayer)) {
            // Envoyer à tous les joueurs en ligne
            String finalMessage = message
                    .replace("{player}", triggerPlayer != null ? triggerPlayer.getName() : "Unknown")
                    .replace("{target}", "Tous")
                    .replace("{trigger}", data.getOrDefault("trigger_name", "Unknown").toString());
            String translatedMessage = ChatUtil.translate(finalMessage);
            Bukkit.broadcastMessage(translatedMessage);
            return true;
        } else {
            // Chercher un joueur par nom exact
            target = Bukkit.getPlayerExact(targetPlayer);
        }

        if (target != null && target.isOnline()) {
            // Remplacer les variables dans le message
            String finalMessage = message
                    .replace("{player}", triggerPlayer != null ? triggerPlayer.getName() : "Unknown")
                    .replace("{target}", target.getName())
                    .replace("{trigger}", data.getOrDefault("trigger_name", "Unknown").toString());

            target.sendMessage(ChatUtil.translate(finalMessage));
            return true;
        }

        return false;
    }
}