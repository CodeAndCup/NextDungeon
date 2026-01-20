package fr.perrier.dungeons.spigot.workflow.trigger.impl;

import fr.perrier.dungeons.spigot.utils.ServerUtil;
import fr.perrier.dungeons.spigot.webserver.blockly.BlocklyTrigger;
import fr.perrier.dungeons.spigot.webserver.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.spigot.webserver.blockly.annotations.BlocklyInfo;
import fr.perrier.dungeons.spigot.workflow.trigger.Trigger;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@BlocklyInfo(
        name = "interact_trigger",
        color = "#4CAF50",
        displayText = "🤝 Quand le joueur interagit",
        tooltip = "Déclenche quand un joueur interagit avec un objet ou un bloc",
        category = "Triggers"
)
public class InteractTrigger extends Trigger implements BlocklyTrigger {
    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Type: ", defaultValue = "OAK_BUTTON", order = 1)
    private String materialType;
    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "X:", defaultValue = "0", order = 2)
    private double posX;
    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Y:", defaultValue = "64", order = 3)
    private double posY;
    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Z:", defaultValue = "0", order = 4)
    private double posZ;
    @BlocklyField(type = BlocklyField.FieldType.CHECKBOX, label = "Une seule fois:", order = 9)
    private boolean onlyOnce = false;
    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Cooldown (sec):", defaultValue = "0", order = 10)
    private int cooldownSeconds = 0;

    private final Map<String, Long> playerTriggerHistory = new HashMap<>();

    public InteractTrigger(String name) {
        super(name);
    }

    @Override
    public boolean execute(Player player, Location location, Map<String, Object> data) {
        if(ServerUtil.isInEditMode()) return false;

        if(!checkConditions(player,data)) return false;

        if (cooldownSeconds > 0) {
            String playerId = player.getUniqueId().toString();
            long currentTime = System.currentTimeMillis();
            Long lastTrigger = playerTriggerHistory.get(playerId);

            if (lastTrigger != null && (currentTime - lastTrigger) < (cooldownSeconds * 1000L)) {
                return false;
            }

            playerTriggerHistory.put(playerId, currentTime);
        }

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
        return false;
    }

    @Override
    public String getType() {
        return "interact_trigger";
    }
}
