package fr.perrier.dungeons.spigot.workflow.condition;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.webserver.blockly.BlocklyAction;
import fr.perrier.dungeons.spigot.webserver.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.spigot.webserver.blockly.annotations.BlocklyInfo;
import fr.perrier.dungeons.spigot.workflow.action.Action;
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

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "X1:",
            defaultValue = "0", order = 1)
    private double pos1X;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Y1:",
            defaultValue = "64", order = 2)
    private double pos1Y;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Z1:",
            defaultValue = "0", order = 3)
    private double pos1Z;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "X2:",
            defaultValue = "10", order = 4)
    private double pos2X;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Y2:",
            defaultValue = "74", order = 5)
    private double pos2Y;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Z2:",
            defaultValue = "10", order = 6)
    private double pos2Z;

    @BlocklyField(type = BlocklyField.FieldType.DROPDOWN, label = "Comparaison:",
            options = "inside,outside", defaultValue = "inside", order = 7)
    private String comparison;

    private List<Action> ifActions;
    private List<Action> elseActions;

    public PlayerInRegionCondition() {
        super("PlayerInRegion", "player_in_region_condition");
        this.pos1X = 0;
        this.pos1Y = 64;
        this.pos1Z = 0;
        this.pos2X = 10;
        this.pos2Y = 74;
        this.pos2Z = 10;
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

            if (Main.isDebug()) {
                Main.getInstance().getLogger().info("PlayerInRegion condition: " + inRegion);
            }

            List<Action> actionsToExecute = inRegion ? ifActions : elseActions;

            if (actionsToExecute != null && !actionsToExecute.isEmpty()) {
                for (Action action : actionsToExecute) {
                    if (!action.execute(triggerPlayer, location, data)) {
                        Main.getInstance().getLogger().warning("Action failed in PlayerInRegion condition: " + action.getClass().getSimpleName());
                    }
                }
            }

            return true;

        } catch (Exception e) {
            Main.getInstance().getLogger().severe("Error executing PlayerInRegion condition: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean isPlayerInRegion(Location playerLoc) {
        if (playerLoc == null) {
            return false;
        }

        double minX = Math.min(pos1X, pos2X);
        double maxX = Math.max(pos1X, pos2X);
        double minY = Math.min(pos1Y, pos2Y);
        double maxY = Math.max(pos1Y, pos2Y);
        double minZ = Math.min(pos1Z, pos2Z);
        double maxZ = Math.max(pos1Z, pos2Z);

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
