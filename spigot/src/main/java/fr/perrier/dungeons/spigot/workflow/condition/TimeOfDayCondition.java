package fr.perrier.dungeons.spigot.workflow.condition;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.webeditor.blockly.BlocklyAction;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyInfo;
import fr.perrier.dungeons.spigot.workflow.action.Action;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Condition pour vérifier l'heure du jour dans le monde
 */
@Setter
@Getter
@BlocklyInfo(
        name = "time_of_day_condition",
        color = "#FF9800",
        displayText = "🌞 Si l'heure est",
        tooltip = "Vérifie si l'heure actuelle du monde correspond à une valeur",
        category = "Conditions"
)
public class TimeOfDayCondition extends Action implements BlocklyAction {
    private static final long serialVersionUID = 1L;

    @BlocklyField(type = BlocklyField.FieldType.DROPDOWN, label = "Période:",
            options = "day,night,dawn,dusk,custom", defaultValue = "day", order = 1)
    private String timePeriod;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Heure personnalisée (ticks):",
            defaultValue = "6000", order = 2)
    private long customTime;

    @BlocklyField(type = BlocklyField.FieldType.DROPDOWN, label = "Opérateur:",
            options = "==,!=,<,<=,>,>=", defaultValue = "==", order = 3)
    private String operator;

    private List<Action> ifActions;
    private List<Action> elseActions;

    public TimeOfDayCondition() {
        super("TimeOfDay", "time_of_day_condition");
        this.timePeriod = "day";
        this.customTime = 6000;
        this.operator = "==";
        this.ifActions = new ArrayList<>();
        this.elseActions = new ArrayList<>();
    }

    @Override
    public boolean execute(Player triggerPlayer, Location location, Map<String, Object> data) {
        try {
            World world = location != null && location.getWorld() != null
                ? location.getWorld()
                : (triggerPlayer != null ? triggerPlayer.getWorld() : null);

            if (world == null) {
                return false;
            }

            boolean timeMatches = checkTime(world);

            if (Main.isDebug()) {
                Main.getInstance().getLogger().info("TimeOfDay condition: " + timeMatches + " (current: " + world.getTime() + ")");
            }

            List<Action> actionsToExecute = timeMatches ? ifActions : elseActions;

            if (actionsToExecute != null && !actionsToExecute.isEmpty()) {
                for (Action action : actionsToExecute) {
                    if (!action.execute(triggerPlayer, location, data)) {
                        Main.getInstance().getLogger().warning("Action failed in TimeOfDay condition: " + action.getClass().getSimpleName());
                    }
                }
            }

            return true;

        } catch (Exception e) {
            Main.getInstance().getLogger().severe("Error executing TimeOfDay condition: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private boolean checkTime(World world) {
        long currentTime = world.getTime();
        long targetTime = getTargetTime();

        switch (operator) {
            case "==":
                return isTimeInRange(currentTime, targetTime, 1000);
            case "!=":
                return !isTimeInRange(currentTime, targetTime, 1000);
            case "<":
                return currentTime < targetTime;
            case "<=":
                return currentTime <= targetTime;
            case ">":
                return currentTime > targetTime;
            case ">=":
                return currentTime >= targetTime;
            default:
                return false;
        }
    }

    private long getTargetTime() {
        switch (timePeriod.toLowerCase()) {
            case "day":
                return 1000; // Midi
            case "night":
                return 13000; // Minuit
            case "dawn":
                return 23000; // Aube
            case "dusk":
                return 12000; // Crépuscule
            case "custom":
                return customTime;
            default:
                return 6000;
        }
    }

    private boolean isTimeInRange(long currentTime, long targetTime, long range) {
        return Math.abs(currentTime - targetTime) <= range;
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
