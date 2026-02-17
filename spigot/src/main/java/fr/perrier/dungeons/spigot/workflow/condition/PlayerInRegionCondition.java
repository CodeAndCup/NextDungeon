package fr.perrier.dungeons.spigot.workflow.condition;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.webeditor.blockly.BlocklyAction;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyInfo;
import fr.perrier.dungeons.spigot.workflow.action.Action;
import fr.perrier.dungeons.spigot.workflow.blocks.LocationBlock;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Condition pour vérifier si un joueur est dans une région définie
 */
@Setter
@Getter
@BlocklyInfo(
        name = "player_in_region_condition",
        color = "#FF9800",
        displayText = "📍 Si le joueur est dans la région",
        tooltip = "Vérifie si un joueur est dans une région cubique définie",
        category = "Conditions"
)
public class PlayerInRegionCondition extends Action implements BlocklyAction {
    private static final long serialVersionUID = 1L;

    @BlocklyField(type = BlocklyField.FieldType.LOCATION_INPUT, label = "Position 1:", defaultValue = "", order = 0)
    private LocationBlock pos1;

    @BlocklyField(type = BlocklyField.FieldType.LOCATION_INPUT, label = "Position 2:", defaultValue = "", order = 1)
    private LocationBlock pos2;

    @BlocklyField(type = BlocklyField.FieldType.DROPDOWN, label = "Comparaison:",
            options = "inside,outside", defaultValue = "inside", order = 2)
    private String comparison;

    private List<Action> ifActions;
    private List<Action> elseActions;

    public PlayerInRegionCondition() {
        super("PlayerInRegion", "player_in_region_condition");
        this.pos1 = new LocationBlock(0, 64, 0);
        this.pos2 = new LocationBlock(10, 74, 10);
        this.comparison = "inside";
        this.ifActions = new ArrayList<>();
        this.elseActions = new ArrayList<>();
    }

    @Override
    public boolean execute(Player triggerPlayer, Location location, Map<String, Object> data) {
        try {
            if (triggerPlayer == null) {
                return false;
            }

            boolean inRegion = isPlayerInRegion(triggerPlayer.getLocation());

            if (Main.getLoggerUtil().isDebugEnabled()) {
                Main.getLoggerUtil().info("PlayerInRegion condition: " + inRegion);
            }

            List<Action> actionsToExecute = inRegion ? ifActions : elseActions;

            if (actionsToExecute != null && !actionsToExecute.isEmpty()) {
                for (Action action : actionsToExecute) {
                    if (!action.execute(triggerPlayer, location, data)) {
                        Main.getLoggerUtil().warning("Action failed in PlayerInRegion condition: " + action.getClass().getSimpleName());
                    }
                }
            }

            return true;

        } catch (Exception e) {
            Main.getLoggerUtil().severe("Error executing PlayerInRegion condition: " + e.getMessage());
            e.printStackTrace(System.err);
            return false;
        }
    }

    private boolean isPlayerInRegion(Location playerLoc) {
        if (playerLoc == null) {
            return false;
        }

        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double maxY = Math.max(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());

        boolean inside = playerLoc.getX() >= minX && playerLoc.getX() <= maxX
                && playerLoc.getY() >= minY && playerLoc.getY() <= maxY
                && playerLoc.getZ() >= minZ && playerLoc.getZ() <= maxZ;

        // If comparison is "outside", invert the result
        if ("outside".equals(comparison)) {
            return !inside;
        }

        return inside;
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
