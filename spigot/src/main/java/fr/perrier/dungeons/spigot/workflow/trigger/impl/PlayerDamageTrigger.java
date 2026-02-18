package fr.perrier.dungeons.spigot.workflow.trigger.impl;

import fr.perrier.dungeons.spigot.webeditor.blockly.BlocklyTrigger;
import fr.perrier.dungeons.spigot.workflow.trigger.Trigger;
import fr.perrier.dungeons.spigot.utils.ServerUtil;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyInfo;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Map;

/**
 * Trigger qui se déclenche quand un joueur prend des dégâts
 */
@Getter
@Setter
@BlocklyInfo(
        name = "player_damage_trigger",
        color = "#E91E63",
        displayText = "💔 Quand le joueur prend des dégâts",
        tooltip = "Déclenche quand un joueur prend des dégâts",
        category = "Triggers"
)
public class PlayerDamageTrigger extends Trigger implements BlocklyTrigger {

    @BlocklyField(type = BlocklyField.FieldType.DROPDOWN, label = "Type de dégâts:",
            options = "ALL,FALL,FIRE,ENTITY_ATTACK,PROJECTILE,DROWNING,LAVA,VOID",
            defaultValue = "ALL", order = 1)
    private String damageType;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Dégâts minimum:",
            defaultValue = "0", order = 2)
    private double minDamage;

    @BlocklyField(type = BlocklyField.FieldType.CHECKBOX, label = "Annuler les dégâts:",
            defaultValue = "false", order = 3)
    private boolean cancelDamage;

    public PlayerDamageTrigger(String name) {
        super(name);
        this.damageType = "ALL";
        this.minDamage = 0;
        this.cancelDamage = false;
    }

    public PlayerDamageTrigger() {
        super("Player Damage Trigger");
        this.damageType = "ALL";
        this.minDamage = 0;
        this.cancelDamage = false;
    }

    @Override
    public String getType() {
        return "player_damage_trigger";
    }

    @Override
    public boolean execute(Player player, Location location, Map<String, Object> data) {
        if (ServerUtil.isInEditMode()) return false;

        if (!checkConditions(player, data)) {
            return false;
        }
        return executeActions(player, location, data);
    }

    @Override
    public boolean checkConditions(Player player, Map<String, Object> data) {
        if (!enabled) {
            return false;
        }

        // Vérifier le type de dégâts
        if (data.containsKey("damage_cause") && !damageType.equals("ALL")) {
            String cause = data.get("damage_cause").toString();
            if (!cause.equals(damageType)) {
                return false;
            }
        }

        // Vérifier les dégâts minimum
        if (data.containsKey("damage_amount")) {
            double damage = ((Number) data.get("damage_amount")).doubleValue();
            if (damage < minDamage) {
                return false;
            }
        }

        return true;
    }

    /**
     * Vérifie si ce trigger correspond au type de dégâts
     */
    public boolean matchesDamageType(EntityDamageEvent.DamageCause cause) {
        if ("ALL".equals(damageType)) {
            return true;
        }
        return cause.name().equals(damageType);
    }

    public boolean shouldCancelDamage() {
        return cancelDamage;
    }
}
