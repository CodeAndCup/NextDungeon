package fr.perrier.dungeons.spigot.workflow.condition;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.webeditor.blockly.BlocklyAction;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyInfo;
import fr.perrier.dungeons.spigot.workflow.action.Action;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.Location;

import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Action pour créer des boucles For
 */
@Setter
@Getter
@BlocklyInfo(
        name = "for_loop_action",
        color = "#9C27B0",
        displayText = "🔁 Pour",
        tooltip = "Répète des actions un nombre de fois",
        category = "Logic"
)
public class ForLoopAction extends Action implements BlocklyAction {
    @Serial
    private static final long serialVersionUID = 1L;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Variable de boucle:",
            defaultValue = "i", order = 1)
    private String variableName;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Valeur initiale:",
            defaultValue = "0", order = 2)
    private int startValue;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Valeur finale:",
            defaultValue = "10", order = 3)
    private int endValue;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Incrément:",
            defaultValue = "1", order = 4)
    private int step;

    // Actions to execute in the loop
    private List<Action> loopActions;

    public ForLoopAction() {
        super("For Loop", "for_loop_action");
        this.variableName = "i";
        this.startValue = 0;
        this.endValue = 10;
        this.step = 1;
        this.loopActions = new ArrayList<>();
    }

    public ForLoopAction(String variableName, int startValue, int endValue, int step) {
        super("For Loop", "for_loop_action");
        this.variableName = variableName != null ? variableName : "i";
        this.startValue = startValue;
        this.endValue = endValue;
        this.step = step != 0 ? step : 1;
        this.loopActions = new ArrayList<>();
    }

    @Override
    public boolean execute(Player triggerPlayer, Location location, Map<String, Object> data) {
        try {
            if (variableName == null || variableName.isEmpty()) {
                Main.getLoggerUtil().warning("Loop variable name is empty in ForLoopAction");
                return false;
            }

            if (step == 0) {
                Main.getLoggerUtil().warning("Step value cannot be 0 in ForLoopAction");
                return false;
            }

            if (loopActions == null || loopActions.isEmpty()) {
                if (Main.getLoggerUtil().isDebugEnabled()) {
                    Main.getLoggerUtil().info("For loop has no actions to execute");
                }
                return true;
            }

            // Create a copy of data to avoid modifying the original
            Map<String, Object> loopData = new HashMap<>(data);

            // Execute loop
            if (step > 0) {
                for (int i = startValue; i <= endValue; i += step) {
                    // Set the loop variable in the data context
                    loopData.put(variableName, i);

                    // Execute all actions in the loop
                    for (Action action : loopActions) {
                        if (!action.execute(triggerPlayer, location, loopData)) {
                            Main.getLoggerUtil().warning("Action failed in For loop iteration " + i + ": " + action.getClass().getSimpleName());
                        }
                    }
                }
            } else {
                // Negative step (counting down)
                for (int i = startValue; i >= endValue; i += step) {
                    loopData.put(variableName, i);

                    for (Action action : loopActions) {
                        if (!action.execute(triggerPlayer, location, loopData)) {
                            Main.getLoggerUtil().warning("Action failed in For loop iteration " + i + ": " + action.getClass().getSimpleName());
                        }
                    }
                }
            }

            if (Main.getLoggerUtil().isDebugEnabled()) {
                Main.getLoggerUtil().info("For loop completed: " + variableName + " from " + startValue + " to " + endValue + " step " + step);
            }

            return true;

        } catch (Exception e) {
            Main.getLoggerUtil().severe("Error executing For loop action: " + e.getMessage());
            e.printStackTrace(System.err);
            return false;
        }
    }

    @Override
    public boolean requiresCustomBlockGeneration() {
        return true;
    }

    @Override
    public void generateCustomBlock(StringBuilder js) {
        js.append("""
        Blockly.Blocks['for_loop_action'] = {
            init: function() {
                this.appendDummyInput()
                    .appendField('🔁 Pour')
                    .appendField(new Blockly.FieldTextInput('i'), 'VARIABLE')
                    .appendField('de');
                this.appendValueInput('START')
                    .setCheck('Number')
                    .setAlign(Blockly.ALIGN_RIGHT);
                this.appendValueInput('END')
                    .setCheck('Number')
                    .appendField('à')
                    .setAlign(Blockly.ALIGN_RIGHT);
                this.appendValueInput('STEP')
                    .setCheck('Number')
                    .appendField('pas')
                    .setAlign(Blockly.ALIGN_RIGHT);
                this.appendStatementInput('ACTIONS')
                    .setCheck('Action')
                    .appendField('faire:');
                this.setPreviousStatement(true, 'Action');
                this.setNextStatement(true, 'Action');
                this.setColour('#9C27B0');
                this.setTooltip('Répète des actions un nombre de fois');
            }
        };
        """);
    }

    @Override
    public void generateCustomActionCase(StringBuilder js) {
        js.append("""
                        if (actionBlock.type === 'for_loop_action') {
                            const loopActions = getActionsFromStatementInput(actionBlock, 'ACTIONS');
                            const startBlock = actionBlock.getInputTargetBlock('START');
                            const endBlock = actionBlock.getInputTargetBlock('END');
                            const stepBlock = actionBlock.getInputTargetBlock('STEP');

                            let startValue = 0;
                            let endValue = 10;
                            let stepValue = 1;

                            if (startBlock) {
                                startValue = startBlock.getFieldValue('NUM') || 0;
                            }
                            if (endBlock) {
                                endValue = endBlock.getFieldValue('NUM') || 10;
                            }
                            if (stepBlock) {
                                stepValue = stepBlock.getFieldValue('NUM') || 1;
                            }

                            actions.push({
                                type: 'for_loop_action',
                                variableName: actionBlock.getFieldValue('VARIABLE') || 'i',
                                startValue: parseInt(startValue),
                                endValue: parseInt(endValue),
                                step: parseInt(stepValue),
                                loopActions: loopActions
                            });
                        }
        """);
    }

    @Override
    public void generateCustomActionLoadingCase(StringBuilder js) {
        js.append("""
                            if (action.type === 'for_loop_action') {
                                 actionBlock = workspace.newBlock('for_loop_action');
                                 actionBlock.setFieldValue(action.variableName || 'i', 'VARIABLE');

                                     const startBlock = workspace.newBlock('math_number');
                                     startBlock.setFieldValue(action.startValue || 0, 'NUM');
                                     startBlock.initSvg();
                                     startBlock.render();
                                     actionBlock.getInput('START').connection.connect(startBlock.outputConnection);

                                     const endBlock = workspace.newBlock('math_number');
                                     endBlock.setFieldValue(action.endValue || 10, 'NUM');
                                     endBlock.initSvg();
                                     endBlock.render();
                                     actionBlock.getInput('END').connection.connect(endBlock.outputConnection);

                                     const stepBlock = workspace.newBlock('math_number');
                                     stepBlock.setFieldValue(action.step || 1, 'NUM');
                                     stepBlock.initSvg();
                                     stepBlock.render();
                                     actionBlock.getInput('STEP').connection.connect(stepBlock.outputConnection);

                                     if (action.loopActions && action.loopActions.length > 0) {
                                         loadActionsIntoStatement(actionBlock, action.loopActions, 'ACTIONS');
                                     }
                                 }
        """);
    }

    /**
     * Add action to loop body
     */
    public void addLoopAction(Action action) {
        if (this.loopActions == null) {
            this.loopActions = new ArrayList<>();
        }
        this.loopActions.add(action);
    }

    /**
     * Get loop actions
     */
    public List<Action> getLoopActions() {
        if (this.loopActions == null) {
            this.loopActions = new ArrayList<>();
        }
        return this.loopActions;
    }

}

