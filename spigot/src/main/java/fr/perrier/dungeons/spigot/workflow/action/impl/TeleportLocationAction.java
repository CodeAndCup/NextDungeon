package fr.perrier.dungeons.spigot.workflow.action.impl;

import fr.perrier.dungeons.spigot.webeditor.blockly.BlocklyAction;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyInfo;
import fr.perrier.dungeons.spigot.workflow.action.Action;
import fr.perrier.dungeons.spigot.workflow.blocks.LocationBlock;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Action pour téléporter un joueur en utilisant un LocationBlock
 * Version améliorée avec support des blocs de location
 */
@Setter
@Getter
@BlocklyInfo(
        name = "teleport_location_action",
        color = "#2196F3",
        displayText = "🌐 Téléporter (Location)",
        tooltip = "Téléporte un joueur à une location définie par un bloc de location",
        category = "Actions"
)
public class TeleportLocationAction extends Action implements BlocklyAction {
    private static final long serialVersionUID = 1L;

    @BlocklyField(type = BlocklyField.FieldType.DROPDOWN, label = "Cible:",
            options = "player,@all", defaultValue = "player", order = 1)
    private String targetPlayer;

    @BlocklyField(type = BlocklyField.FieldType.LOCATION_INPUT, label = "Location:",
            defaultValue = "", order = 2)
    private LocationBlock location;

    public TeleportLocationAction() {
        super("Teleport Location", "teleport_location_action");
        this.targetPlayer = "player";
        this.location = new LocationBlock();
    }

    public TeleportLocationAction(String targetPlayer, LocationBlock location) {
        super("Teleport Location", "teleport_location_action");
        this.targetPlayer = targetPlayer;
        this.location = location;
    }

    @Override
    public boolean execute(Player triggerPlayer, Location triggerLocation, Map<String, Object> data) {
        if (location == null) {
            return false;
        }

        Player target = null;

        // Déterminer le joueur cible
        if (targetPlayer == null || targetPlayer.isEmpty() || "player".equals(targetPlayer)) {
            target = triggerPlayer;
        } else if ("@all".equals(targetPlayer)) {
            // Téléporter tous les joueurs en ligne
            Location dest = location.toLocation(triggerLocation != null ? triggerLocation.getWorld() : null);
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.teleport(dest);
            }
            return true;
        } else {
            // Chercher un joueur par nom exact
            target = Bukkit.getPlayerExact(targetPlayer);
        }

        if (target != null && target.isOnline()) {
            Location dest = location.toLocation(triggerLocation != null ? triggerLocation.getWorld() : null);
            target.teleport(dest);
            return true;
        }

        return false;
    }
}
