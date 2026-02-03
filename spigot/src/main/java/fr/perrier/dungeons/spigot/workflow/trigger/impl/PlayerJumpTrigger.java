package fr.perrier.dungeons.spigot.workflow.trigger.impl;

import fr.perrier.dungeons.spigot.webserver.blockly.BlocklyTrigger;
import fr.perrier.dungeons.spigot.workflow.trigger.Trigger;
import fr.perrier.dungeons.spigot.utils.ServerUtil;
import fr.perrier.dungeons.spigot.webserver.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.spigot.webserver.blockly.annotations.BlocklyInfo;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Trigger qui se déclenche quand un joueur saute
 */
@Getter
@Setter
@BlocklyInfo(
        name = "player_jump_trigger",
        color = "#00BCD4",
        displayText = "🦘 Quand le joueur saute",
        tooltip = "Déclenche quand un joueur saute",
        category = "Triggers"
)
public class PlayerJumpTrigger extends Trigger implements BlocklyTrigger {

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Cooldown (sec):",
            defaultValue = "0", order = 1)
    private int cooldownSeconds;

    @BlocklyField(type = BlocklyField.FieldType.CHECKBOX, label = "Une seule fois:",
            defaultValue = "false", order = 2)
    private boolean onlyOnce;

    private final Map<String, Long> playerTriggerHistory = new HashMap<>();

    public PlayerJumpTrigger(String name) {
        super(name);
        this.cooldownSeconds = 0;
        this.onlyOnce = false;
    }

    public PlayerJumpTrigger() {
        super("Player Jump Trigger");
        this.cooldownSeconds = 0;
        this.onlyOnce = false;
    }

    @Override
    public String getType() {
        return "player_jump_trigger";
    }

    @Override
    public boolean execute(Player player, Location location, Map<String, Object> data) {
        if (ServerUtil.isInEditMode()) return false;

        if (!checkConditions(player, data)) {
            return false;
        }

        // Vérifier le cooldown
        if (cooldownSeconds > 0) {
            String playerId = player.getUniqueId().toString();
            long currentTime = System.currentTimeMillis();
            Long lastTrigger = playerTriggerHistory.get(playerId);

            if (lastTrigger != null && (currentTime - lastTrigger) < (cooldownSeconds * 1000L)) {
                return false;
            }

            playerTriggerHistory.put(playerId, currentTime);
        }

        // Vérifier si une seule fois
        if (onlyOnce) {
            String playerId = player.getUniqueId().toString() + "_once";
            if (playerTriggerHistory.containsKey(playerId)) {
                return false;
            }
            playerTriggerHistory.put(playerId, System.currentTimeMillis());
        }

        return executeActions(player, location, data);
    }

    @Override
    public boolean checkConditions(Player player, Map<String, Object> data) {
        return true;
    }
}
