package fr.perrier.dungeons.spigot.workflow.action.impl;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.webserver.blockly.BlocklyAction;
import fr.perrier.dungeons.spigot.workflow.action.Action;
import fr.perrier.dungeons.spigot.webserver.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.spigot.webserver.blockly.annotations.BlocklyInfo;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Action pour modifier la santé d'un joueur
 */
@Setter
@Getter
@BlocklyInfo(
        name = "set_health_action",
        color = "#F44336",
        displayText = "❤️ Modifier la santé",
        tooltip = "Modifie la santé du joueur",
        category = "Actions"
)
public class SetHealthAction extends Action implements BlocklyAction {
    private static final long serialVersionUID = 1L;

    @BlocklyField(type = BlocklyField.FieldType.DROPDOWN, label = "Opération:",
            options = "set,add,subtract", defaultValue = "set", order = 1)
    private String operation;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Valeur:",
            defaultValue = "20", order = 2)
    private double value;

    @BlocklyField(type = BlocklyField.FieldType.DROPDOWN, label = "Appliquer à:",
            options = "player,@all", defaultValue = "player", order = 3)
    private String target;

    public SetHealthAction() {
        super("Set Health", "set_health_action");
        this.operation = "set";
        this.value = 20;
        this.target = "player";
    }

    public SetHealthAction(String operation, double value, String target) {
        super("Set Health", "set_health_action");
        this.operation = operation;
        this.value = value;
        this.target = target;
    }

    @Override
    public boolean execute(Player triggerPlayer, Location location, Map<String, Object> data) {
        try {
            if ("@all".equals(target)) {
                // Appliquer à tous les joueurs en ligne
                for (Player onlinePlayer : Main.getInstance().getServer().getOnlinePlayers()) {
                    applyHealthChange(onlinePlayer);
                }
            } else {
                // Appliquer au joueur déclencheur
                if (triggerPlayer != null) {
                    applyHealthChange(triggerPlayer);
                }
            }

            if (Main.isDebug()) {
                Main.getInstance().getLogger().info("Applied health change: " + operation + " " + value + " to " + target);
            }

            return true;

        } catch (Exception e) {
            Main.getInstance().getLogger().warning("Error modifying health: " + e.getMessage());
            return false;
        }
    }

    private void applyHealthChange(Player player) {
        double currentHealth = player.getHealth();
        double maxHealth = player.getMaxHealth();
        double newHealth;

        newHealth = switch (operation) {
            case "set" -> value;
            case "add" -> currentHealth + value;
            case "subtract" -> currentHealth - value;
            default -> value;
        };

        // S'assurer que la santé reste dans les limites
        newHealth = Math.max(0, Math.min(maxHealth, newHealth));

        player.setHealth(newHealth);
    }
}
