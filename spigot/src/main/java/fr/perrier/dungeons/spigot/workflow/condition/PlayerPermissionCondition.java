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
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Condition pour vérifier si un joueur possède une permission
 */
@Setter
@Getter
@BlocklyInfo(
        name = "player_permission_condition",
        color = "#FF9800",
        displayText = "🔐 Si le joueur a la permission",
        tooltip = "Vérifie si un joueur possède une permission spécifique",
        category = "Conditions"
)
public class PlayerPermissionCondition extends Action implements BlocklyAction, ConditionalAction {
    private static final long serialVersionUID = 1L;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Permission:",
            defaultValue = "dungeons.admin", order = 1)
    private String permission;

    @BlocklyField(type = BlocklyField.FieldType.DROPDOWN, label = "Comparaison:",
            options = "has,has_not", defaultValue = "has", order = 2)
    private String comparison;

    private List<Action> ifActions;
    private List<Action> elseActions;

    public PlayerPermissionCondition() {
        super("PlayerPermission", "player_permission_condition");
        this.permission = "dungeons.admin";
        this.comparison = "has";
        this.ifActions = new ArrayList<>();
        this.elseActions = new ArrayList<>();
    }

    @Override
    public boolean execute(Player triggerPlayer, Location location, Map<String, Object> data) {
        try {
            if (triggerPlayer == null) {
                return false;
            }

            boolean hasPermission = checkPermission(triggerPlayer);

            if (Main.getLoggerUtil().isDebugEnabled()) {
                Main.getLoggerUtil().info("PlayerPermission condition: " + hasPermission + " for " + permission);
            }

            List<Action> actionsToExecute = hasPermission ? ifActions : elseActions;

            if (actionsToExecute != null && !actionsToExecute.isEmpty()) {
                for (Action action : actionsToExecute) {
                    if (!action.execute(triggerPlayer, location, data)) {
                        Main.getLoggerUtil().warning("Action failed in PlayerPermission condition: " + action.getClass().getSimpleName());
                    }
                }
            }

            return true;

        } catch (Exception e) {
            Main.getLoggerUtil().severe("Error executing PlayerPermission condition: " + e.getMessage());
            e.printStackTrace(System.err);
            return false;
        }
    }

    private boolean checkPermission(Player player) {
        boolean hasPermission = player.hasPermission(permission);

        // If comparison is "has_not", invert the result
        if ("has_not".equals(comparison)) {
            return !hasPermission;
        }

        return hasPermission;
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
