package fr.perrier.dungeons.workflow.trigger.impl;

import fr.perrier.dungeons.webserver.blockly.BlocklyTrigger;
import fr.perrier.dungeons.webserver.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.webserver.blockly.annotations.BlocklyInfo;
import fr.perrier.dungeons.workflow.trigger.Trigger;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;

@Getter
@Setter
@BlocklyInfo(
        name = "entity_death_trigger",
        color = "#4CAF50",
        displayText = "📍 Quand une entité meurt",
        tooltip = "Déclenche lorsque une entité meurt",
        category = "Triggers"
)
public class EntityDeathTrigger extends Trigger implements BlocklyTrigger {

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Entity:", defaultValue = "ZOMBIE", order = 1)
    private String entityType;

    public EntityDeathTrigger(String name) {
        super(name);
    }

    @Override
    public String getType() {
        return "entity_death_trigger";
    }

    @Override
    public boolean execute(Player player, Location location, Map<String, Object> data) {
        if(!checkConditions(player, data)) {
            return false;
        }
        return executeActions(player, location, data);
    }

    @Override
    public boolean checkConditions(Player player, Map<String, Object> data) {
        // Le EntityDeathTriggerHandler s'occupe déjà de vérifier si le joueur est dans la région
        // Ici on peut ajouter des conditions supplémentaires si nécessaire
        return true;
    }
}
