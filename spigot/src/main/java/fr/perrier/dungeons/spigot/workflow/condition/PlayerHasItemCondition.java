package fr.perrier.dungeons.spigot.workflow.condition;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.webeditor.blockly.BlocklyAction;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyInfo;
import fr.perrier.dungeons.spigot.workflow.action.Action;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Condition pour vérifier si un joueur possède un item spécifique
 */
@Setter
@Getter
@BlocklyInfo(
        name = "player_has_item_condition",
        color = "#FF9800",
        displayText = "🎒 Si le joueur possède",
        tooltip = "Vérifie si le joueur possède un item spécifique",
        category = "Conditions"
)
public class PlayerHasItemCondition extends Action implements BlocklyAction {
    private static final long serialVersionUID = 1L;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Type d'item:",
            defaultValue = "DIAMOND", order = 1)
    private String itemMaterial;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Quantité minimum:",
            defaultValue = "1", order = 2)
    private int minAmount;

    @BlocklyField(type = BlocklyField.FieldType.CHECKBOX, label = "Vérifier le nom:",
            defaultValue = "false", order = 3)
    private boolean checkName;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Nom de l'item:",
            defaultValue = "", order = 4)
    private String itemName;

    private List<Action> ifActions;
    private List<Action> elseActions;

    public PlayerHasItemCondition() {
        super("PlayerHasItem", "player_has_item_condition");
        this.minAmount = 1;
        this.checkName = false;
        this.ifActions = new ArrayList<>();
        this.elseActions = new ArrayList<>();
    }

    @Override
    public boolean execute(Player triggerPlayer, Location location, Map<String, Object> data) {
        try {
            boolean hasItem = checkPlayerHasItem(triggerPlayer);

            if (Main.getLoggerUtil().isDebugEnabled()) {
                Main.getLoggerUtil().info("PlayerHasItem condition: " + hasItem);
            }

            List<Action> actionsToExecute = hasItem ? ifActions : elseActions;

            if (actionsToExecute != null && !actionsToExecute.isEmpty()) {
                for (Action action : actionsToExecute) {
                    if (!action.execute(triggerPlayer, location, data)) {
                        Main.getLoggerUtil().warning("Action failed in PlayerHasItem condition: " + action.getClass().getSimpleName());
                    }
                }
            }

            return true;

        } catch (Exception e) {
            Main.getLoggerUtil().severe("Error executing PlayerHasItem condition: " + e.getMessage());
            e.printStackTrace(System.err);
            return false;
        }
    }

    private boolean checkPlayerHasItem(Player player) {
        if (player == null) {
            return false;
        }

        try {
            Material material = Material.valueOf(itemMaterial.toUpperCase());
            int totalCount = 0;

            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == material) {
                    // If we need to check the name
                    if (checkName && itemName != null && !itemName.isEmpty()) {
                        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                            String displayName = item.getItemMeta().getDisplayName();
                            if (!displayName.contains(itemName)) {
                                continue;
                            }
                        } else {
                            continue;
                        }
                    }
                    totalCount += item.getAmount();
                }
            }

            return totalCount >= minAmount;

        } catch (IllegalArgumentException e) {
            Main.getLoggerUtil().warning("Invalid material type: " + itemMaterial);
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
    public boolean requiresCustomBlockGeneration() {
        return true;
    }

    @Override
    public void generateCustomBlock(StringBuilder js) {
        js.append("""
        Blockly.Blocks['player_has_item_condition'] = {
            init: function() {
                this.appendDummyInput()
                    .appendField('🎒 Si le joueur possède');
                this.appendValueInput('ITEMMATERIAL')
                    .setCheck(null)
                    .appendField('Type d\\'item:');
                this.appendValueInput('MINAMOUNT')
                    .setCheck('Number')
                    .appendField('Quantité min:');
                this.appendDummyInput()
                    .appendField('Vérifier nom:')
                    .appendField(new Blockly.FieldCheckbox('FALSE'), 'CHECKNAME');
                this.appendValueInput('ITEMNAME')
                    .setCheck(null)
                    .appendField('Nom item:');
                this.appendStatementInput('IFACTIONS')
                    .setCheck('Action')
                    .appendField('Alors:');
                this.appendStatementInput('ELSEACTIONS')
                    .setCheck('Action')
                    .appendField('Sinon:');
                this.setPreviousStatement(true, 'Action');
                this.setNextStatement(true, 'Action');
                this.setColour('#FF9800');
                this.setTooltip('Vérifie si le joueur possède un item spécifique');
            }
        };
        """);
    }

    @Override
    public void generateCustomActionCase(StringBuilder js) {
        js.append("""
                        if (actionBlock.type === 'player_has_item_condition') {
                            const itemMaterial = actionBlock.getInputTargetBlock('ITEMMATERIAL') ? actionBlock.getInputTargetBlock('ITEMMATERIAL').getFieldValue('TEXT') || 'DIAMOND' : 'DIAMOND';
                            const minAmount = actionBlock.getInputTargetBlock('MINAMOUNT') ? actionBlock.getInputTargetBlock('MINAMOUNT').getFieldValue('NUM') || '1' : '1';
                            const itemName = actionBlock.getInputTargetBlock('ITEMNAME') ? actionBlock.getInputTargetBlock('ITEMNAME').getFieldValue('TEXT') || '' : '';
                            const ifActions = getActionsFromStatementInput(actionBlock, 'IFACTIONS');
                            const elseActions = getActionsFromStatementInput(actionBlock, 'ELSEACTIONS');

                            actions.push({
                                type: 'player_has_item_condition',
                                itemmaterial: itemMaterial,
                                minamount: parseInt(minAmount),
                                checkname: actionBlock.getFieldValue('CHECKNAME') === 'TRUE',
                                itemname: itemName,
                                ifactions: ifActions,
                                elseactions: elseActions
                            });
                        }
        """);
    }

    @Override
    public void generateCustomActionLoadingCase(StringBuilder js) {
        js.append("""
                            if (action.type === 'player_has_item_condition') {
                                actionBlock = workspace.newBlock('player_has_item_condition');
                                
                                if (action.itemmaterial) {
                                    const materialBlock = workspace.newBlock('text');
                                    materialBlock.setFieldValue(action.itemmaterial, 'TEXT');
                                    materialBlock.initSvg();
                                    materialBlock.render();
                                    actionBlock.getInput('ITEMMATERIAL').connection.connect(materialBlock.outputConnection);
                                }
                                
                                if (action.minamount !== undefined) {
                                    const amountBlock = workspace.newBlock('math_number');
                                    amountBlock.setFieldValue(action.minamount.toString(), 'NUM');
                                    amountBlock.initSvg();
                                    amountBlock.render();
                                    actionBlock.getInput('MINAMOUNT').connection.connect(amountBlock.outputConnection);
                                }
                                
                                actionBlock.setFieldValue(action.checkname ? 'TRUE' : 'FALSE', 'CHECKNAME');
                                
                                if (action.itemname) {
                                    const nameBlock = workspace.newBlock('text');
                                    nameBlock.setFieldValue(action.itemname, 'TEXT');
                                    nameBlock.initSvg();
                                    nameBlock.render();
                                    actionBlock.getInput('ITEMNAME').connection.connect(nameBlock.outputConnection);
                                }
                                
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
