package fr.perrier.dungeons.spigot.workflow.action.impl;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.webeditor.blockly.BlocklyAction;
import fr.perrier.dungeons.spigot.workflow.action.Action;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyInfo;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.Location;

import java.io.Serial;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Action qui génère un nombre aléatoire entre min et max (inclus) et le stocke dans une variable.
 */
@Setter
@Getter
@BlocklyInfo(
        name = "random_number_action",
        color = "#FF5722",
        displayText = "🎲 Nombre aléatoire",
        tooltip = "Génère un nombre aléatoire entre min et max (inclus) et le stocke dans une variable",
        category = "Actions"
)
public class RandomNumberAction extends Action implements BlocklyAction {
    @Serial
    private static final long serialVersionUID = 1L;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Nom variable:",
            defaultValue = "ma_variable", order = 1)
    private String variableName;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Min:",
            defaultValue = "1", order = 2)
    private int min;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Max:",
            defaultValue = "10", order = 3)
    private int max;

    @BlocklyField(type = BlocklyField.FieldType.DROPDOWN, label = "Portée:",
            options = "player,global", defaultValue = "player", order = 4)
    private String scope;

    public RandomNumberAction() {
        super("Random Number", "random_number_action");
        this.variableName = "ma_variable";
        this.min = 1;
        this.max = 10;
        this.scope = "player";
    }

    public RandomNumberAction(String variableName, int min, int max, String scope) {
        super("Random Number", "random_number_action");
        this.variableName = variableName;
        this.min = min;
        this.max = max;
        this.scope = scope;
    }

    @Override
    public boolean execute(Player triggerPlayer, Location location, Map<String, Object> data) {
        if (variableName == null || variableName.trim().isEmpty()) {
            Main.getLoggerUtil().warning("Variable name is empty in RandomNumberAction");
            return false;
        }

        int lo = min;
        int hi = max;
        if (lo > hi) {
            int tmp = lo;
            lo = hi;
            hi = tmp;
        }

        String trimmedName = variableName.trim();
        String scopeToUse = scope != null ? scope : "player";

        // nextInt(origin, bound) est inclusif-exclusif → +1 pour rendre max inclusif
        int random = ThreadLocalRandom.current().nextInt(lo, hi + 1);

        Main.getInstance().getVariableRegistry().setVariable(triggerPlayer, trimmedName, random, scopeToUse);
        data.put(trimmedName, random);

        if (Main.getLoggerUtil().isDebugEnabled()) {
            Main.getLoggerUtil().info("Generated random number " + random + " in [" + lo + ", " + hi
                    + "] into variable " + trimmedName + " (scope: " + scopeToUse + ")");
        }

        return true;
    }
}
