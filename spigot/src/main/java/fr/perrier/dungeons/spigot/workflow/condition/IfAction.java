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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Action pour créer des conditions If/Else
 */
@Setter
@Getter
@BlocklyInfo(
        name = "if_action",
        color = "#FF9800",
        displayText = "🔀 Si",
        tooltip = "Exécute des actions si une condition est vraie",
        category = "Logic"
)
public class IfAction extends Action implements BlocklyAction {
    private static final long serialVersionUID = 1L;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Valeur #1:",
            defaultValue = "value1", order = 1)
    private Object leftValue;

    @BlocklyField(type = BlocklyField.FieldType.DROPDOWN, label = "Opérateur:",
            options = "==,!=,<,<=,>,>=,contains,startsWith,endsWith", defaultValue = "==", order = 2)
    private String operator;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Valeur #2:",
            defaultValue = "value2", order = 3)
    private Object rightValue;

    // Actions to execute
    private List<Action> ifActions;
    private List<Action> elseActions;

    public IfAction() {
        super("If", "if_action");
        this.operator = "==";
        this.ifActions = new ArrayList<>();
        this.elseActions = new ArrayList<>();
    }

    public IfAction(Object leftValue, String operator, Object rightValue) {
        super("If", "if_action");
        this.leftValue = leftValue;
        this.operator = operator;
        this.rightValue = rightValue;
        this.ifActions = new ArrayList<>();
        this.elseActions = new ArrayList<>();
    }

    @Override
    public boolean execute(Player triggerPlayer, Location location, Map<String, Object> data) {
        try {
            // Resolve values
            Object resolvedLeft = resolveValue(leftValue, triggerPlayer, data);
            Object resolvedRight = resolveValue(rightValue, triggerPlayer, data);

            // Evaluate condition
            boolean conditionResult = evaluateCondition(resolvedLeft, operator, resolvedRight);

            if (Main.isDebug()) {
                Main.getInstance().getLogger().info("If condition: " + resolvedLeft + " " + operator + " " + resolvedRight + " = " + conditionResult);
            }

            // Execute appropriate actions
            List<Action> actionsToExecute = conditionResult ? ifActions : elseActions;

            if (actionsToExecute != null && !actionsToExecute.isEmpty()) {
                for (Action action : actionsToExecute) {
                    if (!action.execute(triggerPlayer, location, data)) {
                        Main.getInstance().getLogger().warning("&eAction failed in If block: " + action.getClass().getSimpleName());
                    }
                }
            }

            return true;

        } catch (Exception e) {
            Main.getInstance().getLogger().severe("&#FF0000Error executing If action: " + e.getMessage());
            e.printStackTrace();
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
        Blockly.Blocks['if_action'] = {
            init: function() {
                this.appendValueInput('LEFTVALUE')
                    .setCheck(null)
                    .appendField('🔀 Si');
                this.appendDummyInput()
                    .appendField(new Blockly.FieldDropdown([
                        ['égal à', '=='],
                        ['différent de', '!='],
                        ['inférieur à', '<'],
                        ['inférieur ou égal à', '<='],
                        ['supérieur à', '>'],
                        ['supérieur ou égal à', '>='],
                        ['contient', 'contains'],
                        ['commence par', 'startsWith'],
                        ['finit par', 'endsWith']
                    ]), 'OPERATOR');
                this.appendValueInput('RIGHTVALUE')
                    .setCheck(null);
                this.appendStatementInput('IFACTIONS')
                    .setCheck('Action')
                    .appendField('Alors:');
                this.appendStatementInput('ELSEACTIONS')
                    .setCheck('Action')
                    .appendField('Sinon:');
                this.setPreviousStatement(true, 'Action');
                this.setNextStatement(true, 'Action');
                this.setColour('#FF9800');
                this.setTooltip('Exécute des actions si une condition est vraie');
            }
        };
        """);
    }

    @Override
    public void generateCustomActionCase(StringBuilder js) {
        js.append("""
                        if (actionBlock.type === 'if_action') {
                            const leftValue = actionBlock.getInputTargetBlock('LEFTVALUE') ? actionBlock.getInputTargetBlock('LEFTVALUE').getFieldValue('TEXT') || '' : '';
                            const rightValue = actionBlock.getInputTargetBlock('RIGHTVALUE') ? actionBlock.getInputTargetBlock('RIGHTVALUE').getFieldValue('TEXT') || '' : '';
                            const ifActions = getActionsFromStatementInput(actionBlock, 'IFACTIONS');
                            const elseActions = getActionsFromStatementInput(actionBlock, 'ELSEACTIONS');

                            actions.push({
                                type: 'if_action',
                                leftvalue: leftValue,
                                operator: actionBlock.getFieldValue('OPERATOR'),
                                rightvalue: rightValue,
                                ifactions: ifActions,
                                elseactions: elseActions
                            });
                        }
        """);
    }

    @Override
    public void generateCustomActionLoadingCase(StringBuilder js) {
        js.append("""
                            if (action.type === 'if_action') {
                                 actionBlock = workspace.newBlock('if_action');
                                     if (action.leftvalue) {
                                         const leftBlock = workspace.newBlock('text');
                                         leftBlock.setFieldValue(action.leftvalue, 'TEXT');
                                         leftBlock.initSvg();
                                         leftBlock.render();
                                         actionBlock.getInput('LEFTVALUE').connection.connect(leftBlock.outputConnection);
                                     }

                                     if (action.rightvalue) {
                                         const rightBlock = workspace.newBlock('text');
                                         rightBlock.setFieldValue(action.rightvalue, 'TEXT');
                                         rightBlock.initSvg();
                                         rightBlock.render();
                                         actionBlock.getInput('RIGHTVALUE').connection.connect(rightBlock.outputConnection);
                                     }

                                     actionBlock.setFieldValue(action.operator || '==', 'OPERATOR');

                                     if (action.ifactions && action.ifactions.length > 0) {
                                         loadActionsIntoStatement(actionBlock, action.ifactions, 'IFACTIONS');
                                     }

                                     if (action.elseactions && action.elseactions.length > 0) {
                                         loadActionsIntoStatement(actionBlock, action.elseactions, 'ELSEACTIONS');
                                     }
                                 }
        """);
    }

    /**
     * Resolve a value (handle variables, constants, etc.)
     */
    private Object resolveValue(Object value, Player player, Map<String, Object> data) {
        if (value == null) {
            return null;
        }

        if (value instanceof String) {
            String stringValue = (String) value;

            stringValue = Main.getInstance().getVariableManager().formatVariable(stringValue, player);

            // Handle variable references: {VAR:variable_name}
            if (stringValue.startsWith("{VAR:") && stringValue.endsWith("}")) {
                String varName = stringValue.substring(5, stringValue.length() - 1);

                // Try data context first
                if (data.containsKey(varName)) {
                    return data.get(varName);
                }

                // Try variable manager
                if (Main.getInstance().getVariableManager() != null) {
                    Object varValue = Main.getInstance().getVariableManager().getVariable(player, varName);
                    if (varValue != null) {
                        return varValue;
                    }
                }

                // Return empty string if not found
                return "";
            }

            // Handle player placeholders
            return stringValue.replace("{player}", player != null ? player.getName() : "Unknown");
        }

        return value;
    }

    /**
     * Evaluate condition based on operator
     */
    private boolean evaluateCondition(Object left, String op, Object right) {
        if (left == null && right == null) {
            return "==".equals(op) || "<=".equals(op) || ">=".equals(op);
        }
        if (left == null || right == null) {
            return "!=".equals(op);
        }

        try {
            switch (op) {
                case "==":
                    return compareEqual(left, right);
                case "!=":
                    return !compareEqual(left, right);
                case "<":
                    return compareNumeric(left, right) < 0;
                case "<=":
                    return compareNumeric(left, right) <= 0;
                case ">":
                    return compareNumeric(left, right) > 0;
                case ">=":
                    return compareNumeric(left, right) >= 0;
                case "contains":
                    return left.toString().toLowerCase().contains(right.toString().toLowerCase());
                case "startsWith":
                    return left.toString().toLowerCase().startsWith(right.toString().toLowerCase());
                case "endsWith":
                    return left.toString().toLowerCase().endsWith(right.toString().toLowerCase());
                default:
                    Main.getInstance().getLogger().warning("&eUnknown operator: " + op);
                    return false;
            }
        } catch (Exception e) {
            Main.getInstance().getLogger().warning("&eError evaluating condition: " + e.getMessage());
            return false;
        }
    }

    /**
     * Compare for equality (handles different types)
     */
    private boolean compareEqual(Object left, Object right) {
        // Try numeric comparison first
        try {
            return Double.compare(Double.parseDouble(left.toString()),
                    Double.parseDouble(right.toString())) == 0;
        } catch (NumberFormatException e) {
            // Fall back to string comparison
            return left.toString().equals(right.toString());
        }
    }

    /**
     * Compare numerically
     */
    private int compareNumeric(Object left, Object right) throws NumberFormatException {
        double leftNum = Double.parseDouble(left.toString());
        double rightNum = Double.parseDouble(right.toString());
        return Double.compare(leftNum, rightNum);
    }

    /**
     * Set condition parameters
     */
    public void setCondition(Object leftValue, String operator, Object rightValue) {
        this.leftValue = leftValue;
        this.operator = operator;
        this.rightValue = rightValue;
    }

    /**
     * Add action to IF branch
     */
    public void addIfAction(Action action) {
        if (this.ifActions == null) {
            this.ifActions = new ArrayList<>();
        }
        this.ifActions.add(action);
    }

    /**
     * Add action to ELSE branch
     */
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
