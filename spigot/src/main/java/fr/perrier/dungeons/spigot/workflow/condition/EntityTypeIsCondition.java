package fr.perrier.dungeons.spigot.workflow.condition;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.webeditor.blockly.BlocklyAction;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyInfo;
import fr.perrier.dungeons.spigot.workflow.action.Action;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Condition pour vérifier le type d'une entité
 */
@Setter
@Getter
@BlocklyInfo(
        name = "entity_type_is_condition",
        color = "#FF9800",
        displayText = "👹 Si l'entité est de type",
        tooltip = "Vérifie si une entité est d'un type spécifique",
        category = "Conditions"
)
public class EntityTypeIsCondition extends Action implements BlocklyAction {
    private static final long serialVersionUID = 1L;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Type d'entité:",
            defaultValue = "ZOMBIE", order = 1)
    private String entityType;

    @BlocklyField(type = BlocklyField.FieldType.DROPDOWN, label = "Comparaison:",
            options = "is,is_not", defaultValue = "is", order = 2)
    private String comparison;

    private List<Action> ifActions;
    private List<Action> elseActions;

    public EntityTypeIsCondition() {
        super("EntityTypeIs", "entity_type_is_condition");
        this.entityType = "ZOMBIE";
        this.comparison = "is";
        this.ifActions = new ArrayList<>();
        this.elseActions = new ArrayList<>();
    }

    @Override
    public boolean execute(Player triggerPlayer, Location location, Map<String, Object> data) {
        try {
            boolean matches = checkEntityType(data);

            if (Main.getLoggerUtil().isDebugEnabled()) {
                Main.getLoggerUtil().info("EntityTypeIs condition: " + matches);
            }

            List<Action> actionsToExecute = matches ? ifActions : elseActions;

            if (actionsToExecute != null && !actionsToExecute.isEmpty()) {
                for (Action action : actionsToExecute) {
                    if (!action.execute(triggerPlayer, location, data)) {
                        Main.getLoggerUtil().warning("Action failed in EntityTypeIs condition: " + action.getClass().getSimpleName());
                    }
                }
            }

            return true;

        } catch (Exception e) {
            Main.getLoggerUtil().severe("Error executing EntityTypeIs condition: " + e.getMessage());
            e.printStackTrace(System.err);
            return false;
        }
    }

    private boolean checkEntityType(Map<String, Object> data) {
        if (!data.containsKey("entity_type")) {
            return false;
        }

        Object entityTypeObj = data.get("entity_type");
        String actualEntityType;

        if (entityTypeObj instanceof EntityType entityTypeEnum) {
            actualEntityType = entityTypeEnum.name();
        } else {
            actualEntityType = entityTypeObj.toString();
        }

        boolean isMatch = actualEntityType.equalsIgnoreCase(entityType);

        // If comparison is "is_not", invert the result
        if ("is_not".equals(comparison)) {
            return !isMatch;
        }

        return isMatch;
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
        Blockly.Blocks['entity_type_is_condition'] = {
            init: function() {
                this.appendDummyInput()
                    .appendField('👹 Si l\\'entité');
                this.appendValueInput('ENTITYTYPE')
                    .setCheck(null)
                    .appendField('Type:');
                this.appendDummyInput()
                    .appendField(new Blockly.FieldDropdown([
                        ['est', 'is'],
                        ['n\\'est pas', 'is_not']
                    ]), 'COMPARISON');
                this.appendStatementInput('IFACTIONS')
                    .setCheck('Action')
                    .appendField('Alors:');
                this.appendStatementInput('ELSEACTIONS')
                    .setCheck('Action')
                    .appendField('Sinon:');
                this.setPreviousStatement(true, 'Action');
                this.setNextStatement(true, 'Action');
                this.setColour('#FF9800');
                this.setTooltip('Vérifie si une entité est d\\'un type spécifique');
            }
        };
        """);
    }

    @Override
    public void generateCustomActionCase(StringBuilder js) {
        js.append("""
                        if (actionBlock.type === 'entity_type_is_condition') {
                            const entityType = actionBlock.getInputTargetBlock('ENTITYTYPE') ? actionBlock.getInputTargetBlock('ENTITYTYPE').getFieldValue('TEXT') || 'ZOMBIE' : 'ZOMBIE';
                            const ifActions = getActionsFromStatementInput(actionBlock, 'IFACTIONS');
                            const elseActions = getActionsFromStatementInput(actionBlock, 'ELSEACTIONS');

                            actions.push({
                                type: 'entity_type_is_condition',
                                entitytype: entityType,
                                comparison: actionBlock.getFieldValue('COMPARISON'),
                                ifactions: ifActions,
                                elseactions: elseActions
                            });
                        }
        """);
    }

    @Override
    public void generateCustomActionLoadingCase(StringBuilder js) {
        js.append("""
                            if (action.type === 'entity_type_is_condition') {
                                actionBlock = workspace.newBlock('entity_type_is_condition');
                                
                                if (action.entitytype) {
                                    const typeBlock = workspace.newBlock('text');
                                    typeBlock.setFieldValue(action.entitytype, 'TEXT');
                                    typeBlock.initSvg();
                                    typeBlock.render();
                                    actionBlock.getInput('ENTITYTYPE').connection.connect(typeBlock.outputConnection);
                                }
                                
                                actionBlock.setFieldValue(action.comparison || 'is', 'COMPARISON');
                                
                                if (action.ifactions && action.ifactions.length > 0) {
                                    loadActionsIntoStatement(actionBlock, action.ifactions, 'IFACTIONS');
                                }
                                
                                if (action.elseactions && action.elseactions.length > 0) {
                                    loadActionsIntoStatement(actionBlock, action.elseactions, 'ELSEACTIONS');
                                }
                            }
        """);
    }

    @Override
    public boolean isChainable() {
        return true;
    }
}
