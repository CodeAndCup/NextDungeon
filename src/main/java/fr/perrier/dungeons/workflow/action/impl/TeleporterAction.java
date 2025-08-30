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
        name = "teleporter_action",
        color = "#2196F3",
        displayText = "\uD83C\uDF0C\u200B Téléporter joueur",
        tooltip = "Téléporter joueur spécifique\n{player} = joueur déclencheur\n{target} = joueur cible",
        category = "Actions"
)
public class TeleporterAction extends Action implements BlocklyAction {
    private static final long serialVersionUID = 1L;

    @BlocklyField(type = BlocklyField.FieldType.DROPDOWN, label = "Cible:",
            options = "player,@all", defaultValue = "player", order = 1)
    private String targetPlayer;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "X:",
            defaultValue = "0", order = 2)
    private float x;
    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Y:",
            defaultValue = "0", order = 3)
    private float y;
    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Z:",
            defaultValue = "0", order = 4)
    private float z;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Yaw:",
            defaultValue = "0", order = 6)
    private float yaw;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Pitch:",
            defaultValue = "0", order = 7)
    private float pitch;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Monde:",
            defaultValue = "world", order = 5)
    private String worldName;

    public TeleporterAction(String targetPlayer, float x, float y, float z, float yaw, float pitch, String worldName) {
        super("Teleporter", "teleporter_action");
        this.targetPlayer = targetPlayer;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.worldName = worldName;
    }

    @Override
    public boolean execute(Player triggerPlayer, Location location, Map<String, Object> data) {
        if(worldName == null || worldName.isEmpty()) {
            worldName = "world";
        }

        Player target = null;

        // Déterminer le joueur cible
        if (targetPlayer == null || targetPlayer.isEmpty() || "player".equals(targetPlayer)) {
            target = triggerPlayer; // Le joueur qui a déclenché le trigger
        } else if("@all".equals(targetPlayer)) {
            // Téléporter tous les joueurs en ligne
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.teleport(new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch));
            }
            return true;
        } else {
            // Chercher un joueur par nom exact
            target = Bukkit.getPlayerExact(targetPlayer);
        }

        if (target != null && target.isOnline()) {
            target.teleport(new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch));
            return true;
        }

        return false;
    }
}