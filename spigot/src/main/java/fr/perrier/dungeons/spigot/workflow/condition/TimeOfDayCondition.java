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

            if (Main.getLoggerUtil().isDebugEnabled()) {
                Main.getLoggerUtil().info("TimeOfDay condition: " + timeMatches + " (current: " + world.getTime() + ")");
            }

            List<Action> actionsToExecute = timeMatches ? ifActions : elseActions;

            if (actionsToExecute != null && !actionsToExecute.isEmpty()) {
                for (Action action : actionsToExecute) {
                    if (!action.execute(triggerPlayer, location, data)) {
                        Main.getLoggerUtil().warning("Action failed in TimeOfDay condition: " + action.getClass().getSimpleName());
                    }
                }
            }

            return true;

        } catch (Exception e) {
            Main.getLoggerUtil().severe("Error executing TimeOfDay condition: " + e.getMessage());
            e.printStackTrace(System.err);
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
    public boolean requiresCustomBlockGeneration() {
        return true;
    }

    @Override
    public void generateCustomBlock(StringBuilder js) {
        js.append("""
        Blockly.Blocks['time_of_day_condition'] = {
            init: function() {
                this.appendDummyInput()
                    .appendField('🌞 Si l\\'heure est')
                    .appendField(new Blockly.FieldDropdown([
                        ['Jour', 'day'],
                        ['Nuit', 'night'],
                        ['Aube', 'dawn'],
                        ['Crépuscule', 'dusk'],
                        ['Personnalisée', 'custom']
                    ]), 'TIMEPERIOD');
                this.appendValueInput('CUSTOMTIME')
                    .setCheck('Number')
                    .appendField('Heure (ticks):');
                this.appendDummyInput()
                    .appendField('Opérateur:')
                    .appendField(new Blockly.FieldDropdown([
                        ['égal à', '=='],
                        ['différent de', '!='],
                        ['inférieur à', '<'],
                        ['inférieur ou égal à', '<='],
                        ['supérieur à', '>'],
                        ['supérieur ou égal à', '>=']
                    ]), 'OPERATOR');
                this.appendStatementInput('IFACTIONS')
                    .setCheck('Action')
                    .appendField('Alors:');
                this.appendStatementInput('ELSEACTIONS')
                    .setCheck('Action')
                    .appendField('Sinon:');
                this.setPreviousStatement(true, 'Action');
                this.setNextStatement(true, 'Action');
                this.setColour('#FF9800');
                this.setTooltip('Vérifie si l\\'heure actuelle du monde correspond à une valeur');
            }
        };
        """);
    }

    @Override
    public void generateCustomActionCase(StringBuilder js) {
        js.append("""
                        if (actionBlock.type === 'time_of_day_condition') {
                            const customTime = actionBlock.getInputTargetBlock('CUSTOMTIME') ? actionBlock.getInputTargetBlock('CUSTOMTIME').getFieldValue('NUM') || '6000' : '6000';
                            const ifActions = getActionsFromStatementInput(actionBlock, 'IFACTIONS');
                            const elseActions = getActionsFromStatementInput(actionBlock, 'ELSEACTIONS');

                            actions.push({
                                type: 'time_of_day_condition',
                                timePeriod: actionBlock.getFieldValue('TIMEPERIOD'),
                                customTime: parseInt(customTime),
                                operator: actionBlock.getFieldValue('OPERATOR'),
                                ifActions: ifActions,
                                elseActions: elseActions
                            });
                        }
        """);
    }

    @Override
    public void generateCustomActionLoadingCase(StringBuilder js) {
        js.append("""
                            if (action.type === 'time_of_day_condition') {
                                actionBlock = workspace.newBlock('time_of_day_condition');
                                
                                actionBlock.setFieldValue(action.timePeriod || 'day', 'TIMEPERIOD');
                                actionBlock.setFieldValue(action.operator || '==', 'OPERATOR');
                                
                                if (action.customTime !== undefined) {
                                    const timeBlock = workspace.newBlock('math_number');
                                    timeBlock.setFieldValue(action.customTime.toString(), 'NUM');
                                    timeBlock.initSvg();
                                    timeBlock.render();
                                    actionBlock.getInput('CUSTOMTIME').connection.connect(timeBlock.outputConnection);
                                }
                                
                                if (action.ifActions && action.ifActions.length > 0) {
                                    loadActionsIntoStatement(actionBlock, action.ifActions, 'IFACTIONS');
                                }
                                
                                if (action.elseActions && action.elseActions.length > 0) {
                                    loadActionsIntoStatement(actionBlock, action.elseActions, 'ELSEACTIONS');
                                }
                            }
        """);
    }

    @Override
    public boolean isChainable() {
        return true;
    }
}
