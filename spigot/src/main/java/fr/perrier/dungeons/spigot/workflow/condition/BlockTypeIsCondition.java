package fr.perrier.dungeons.spigot.workflow.condition;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.webeditor.blockly.BlocklyAction;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyInfo;
import fr.perrier.dungeons.spigot.workflow.action.Action;
import fr.perrier.dungeons.spigot.workflow.action.conditional.ConditionalAction;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Condition pour vérifier le type d'un bloc à une position
 */
@Setter
@Getter
@BlocklyInfo(
        name = "block_type_is_condition",
        color = "#FF9800",
        displayText = "🧱 Si le bloc est de type",
        tooltip = "Vérifie si un bloc à une position est d'un type spécifique",
        category = "Conditions"
)
public class BlockTypeIsCondition extends Action implements BlocklyAction, ConditionalAction {
    private static final long serialVersionUID = 1L;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "X:",
            defaultValue = "0", order = 1)
    private double x;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Y:",
            defaultValue = "64", order = 2)
    private double y;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Z:",
            defaultValue = "0", order = 3)
    private double z;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Type de bloc:",
            defaultValue = "STONE", order = 4)
    private String blockType;

    @BlocklyField(type = BlocklyField.FieldType.DROPDOWN, label = "Comparaison:",
            options = "is,is_not", defaultValue = "is", order = 5)
    private String comparison;

    private List<Action> ifActions;
    private List<Action> elseActions;

    public BlockTypeIsCondition() {
        super("BlockTypeIs", "block_type_is_condition");
        this.x = 0;
        this.y = 64;
        this.z = 0;
        this.blockType = "STONE";
        this.comparison = "is";
        this.ifActions = new ArrayList<>();
        this.elseActions = new ArrayList<>();
    }

    @Override
    public boolean execute(Player triggerPlayer, Location location, Map<String, Object> data) {
        try {
            Location checkLocation = location != null && location.getWorld() != null
                ? new Location(location.getWorld(), x, y, z)
                : (triggerPlayer != null ? new Location(triggerPlayer.getWorld(), x, y, z) : null);

            if (checkLocation == null) {
                return false;
            }

            boolean matches = checkBlockType(checkLocation);

            if (Main.getLoggerUtil().isDebugEnabled()) {
                Main.getLoggerUtil().info("BlockTypeIs condition: " + matches);
            }

            List<Action> actionsToExecute = matches ? ifActions : elseActions;

            if (actionsToExecute != null && !actionsToExecute.isEmpty()) {
                for (Action action : actionsToExecute) {
                    if (!action.execute(triggerPlayer, location, data)) {
                        Main.getLoggerUtil().warning("Action failed in BlockTypeIs condition: " + action.getClass().getSimpleName());
                    }
                }
            }

            return true;

        } catch (Exception e) {
            Main.getLoggerUtil().severe("Error executing BlockTypeIs condition: " + e.getMessage());
            e.printStackTrace(System.err);
            return false;
        }
    }

    private boolean checkBlockType(Location loc) {
        try {
            Block block = loc.getBlock();
            Material actualType = block.getType();
            Material expectedType = Material.valueOf(blockType.toUpperCase());

            boolean isMatch = actualType == expectedType;

            // If comparison is "is_not", invert the result
            if ("is_not".equals(comparison)) {
                return !isMatch;
            }

            return isMatch;

        } catch (IllegalArgumentException e) {
            Main.getLoggerUtil().warning("Invalid block type: " + blockType);
            return false;
        }
    }

    public void addIfAction(Action action) {
        if (this.ifActions == null) {
            this.ifActions = new ArrayList<>();
        }
        this.ifActions.add(action);
    }

    public void addElseAction(Action action) {
        if (this.elseActions == null) {
            this.elseActions = new ArrayList<>();
        }
        this.elseActions.add(action);
    }

    @Override
    public boolean isChainable() {
        return true;
    }
}
