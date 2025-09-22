package fr.perrier.dungeons.spigot.workflow.action.impl;

import fr.perrier.dungeons.spigot.workflow.action.Action;
import fr.perrier.dungeons.spigot.webserver.blockly.BlocklyAction;
import fr.perrier.dungeons.spigot.webserver.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.spigot.webserver.blockly.annotations.BlocklyInfo;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;

@Setter
@Getter
@BlocklyInfo(
        name = "delay_action",
        color = "#FFC107",
        displayText = "⏳ Attendre (délai)",
        tooltip = "Attend un certain nombre de ticks avant de continuer le workflow.",
        category = "Actions"
)
public class DelayAction extends Action implements BlocklyAction {
    private static final long serialVersionUID = 1L;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Ticks (20 = 1s):", defaultValue = "20", order = 1)
    private int ticks;

    public DelayAction(int ticks) {
        super("Delay", "delay_action");
        this.ticks = ticks;
    }

    @Override
    public boolean execute(Player player, Location location, Map<String, Object> data) {
        // Le système de workflow doit gérer la reprise différée après le délai.
        // Ici, on retourne true immédiatement, la logique réelle doit être gérée dans le moteur de workflow.
        return true;
    }
}

