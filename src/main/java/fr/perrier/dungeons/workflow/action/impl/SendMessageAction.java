package fr.perrier.dungeons.workflow.action.impl;

import fr.perrier.cupcodeapi.utils.ChatUtil;
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
public class SendMessageAction extends Action {
    private static final long serialVersionUID = 1L;

    // Getters/Setters
    private String targetPlayer;
    private String message;

    public SendMessageAction(String targetPlayer, String message) {
        super("SendMessage", "send_message");
        this.targetPlayer = targetPlayer;
        this.message = message;
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
        } else {
            target = Bukkit.getPlayer(targetPlayer); // Joueur spécifique
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