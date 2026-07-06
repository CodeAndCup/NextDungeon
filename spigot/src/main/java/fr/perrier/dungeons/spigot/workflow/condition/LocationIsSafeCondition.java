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
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Condition pour vérifier si une location est sûre pour la téléportation
 */
@Setter
@Getter
@BlocklyInfo(
        name = "location_is_safe_condition",
        color = "#FF9800",
        displayText = "🛡️ Si la location est sûre",
        tooltip = "Vérifie si une location est sûre pour la téléportation",
        category = "Conditions"
)
public class LocationIsSafeCondition extends Action implements BlocklyAction {
    private static final long serialVersionUID = 1L;

    @BlocklyField(type = BlocklyField.FieldType.LOCATION_INPUT, label = "Location:",
            defaultValue = "", order = 1)
    private LocationBlock location;

    @BlocklyField(type = BlocklyField.FieldType.CHECKBOX, label = "Vérifier le sol solide:",
            defaultValue = "true", order = 2)
    private boolean checkSolidGround;

    @BlocklyField(type = BlocklyField.FieldType.CHECKBOX, label = "Vérifier les blocs dangereux:",
            defaultValue = "true", order = 3)
    private boolean checkDangerousBlocks;

    private List<Action> ifActions;
    private List<Action> elseActions;

    public LocationIsSafeCondition() {
        super("LocationIsSafe", "location_is_safe_condition");
        this.checkSolidGround = true;
        this.checkDangerousBlocks = true;
        this.ifActions = new ArrayList<>();
        this.elseActions = new ArrayList<>();
    }

    @Override
    public boolean execute(Player triggerPlayer, Location targetLocation, Map<String, Object> data) {
        try {
            Location checkLocation = location != null ? location.toLocation() : targetLocation;
            boolean isSafe = isLocationSafe(checkLocation);

            if (Main.getLoggerUtil().isDebugEnabled()) {
                Main.getLoggerUtil().info("LocationIsSafe condition: " + isSafe);
            }

            List<Action> actionsToExecute = isSafe ? ifActions : elseActions;

            if (actionsToExecute != null && !actionsToExecute.isEmpty()) {
                for (Action action : actionsToExecute) {
                    if (!action.execute(triggerPlayer, targetLocation, data)) {
                        Main.getLoggerUtil().warning("Action failed in LocationIsSafe condition: " + action.getClass().getSimpleName());
                    }
                }
            }

            return true;

        } catch (Exception e) {
            Main.getLoggerUtil().severe("Error executing LocationIsSafe condition: " + e.getMessage());
            e.printStackTrace(System.err);
            return false;
        }
    }

    private boolean isLocationSafe(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return false;
        }

        Block blockAt = loc.getBlock();
        Block blockAbove = loc.clone().add(0, 1, 0).getBlock();
        Block blockBelow = loc.clone().subtract(0, 1, 0).getBlock();

        // Check if there's enough space (2 blocks high)
        if (!blockAt.getType().isAir() || !blockAbove.getType().isAir()) {
            return false;
        }

        // Check for solid ground
        if (checkSolidGround && !blockBelow.getType().isSolid()) {
            return false;
        }

        // Check for dangerous blocks
        if (checkDangerousBlocks) {
            if (isDangerousBlock(blockAt) || isDangerousBlock(blockAbove) || isDangerousBlock(blockBelow)) {
                return false;
            }
        }

        return true;
    }

    private boolean isDangerousBlock(Block block) {
        Material type = block.getType();
        return type == Material.LAVA ||
               type == Material.FIRE ||
               type == Material.MAGMA_BLOCK ||
               type == Material.CACTUS ||
               type == Material.SWEET_BERRY_BUSH ||
               type.name().contains("LAVA") ||
               type.name().contains("FIRE");
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
        Blockly.Blocks['location_is_safe_condition'] = {
            init: function() {
                this.appendDummyInput()
                    .appendField('🛡️ Si la location est sûre');
                this.appendValueInput('LOCATION')
                    .setCheck('LocationBlock')
                    .appendField('Location:');
                this.appendDummyInput()
                    .appendField('Sol solide:')
                    .appendField(new Blockly.FieldCheckbox('TRUE'), 'CHECKSOLIDGROUND');
                this.appendDummyInput()
                    .appendField('Blocs dangereux:')
                    .appendField(new Blockly.FieldCheckbox('TRUE'), 'CHECKDANGEROUSBLOCKS');
                this.appendStatementInput('IFACTIONS')
                    .setCheck('Action')
                    .appendField('Alors:');
                this.appendStatementInput('ELSEACTIONS')
                    .setCheck('Action')
                    .appendField('Sinon:');
                this.setPreviousStatement(true, 'Action');
                this.setNextStatement(true, 'Action');
                this.setColour('#FF9800');
                this.setTooltip('Vérifie si une location est sûre pour la téléportation');
            }
        };
        """);
    }

    @Override
    public void generateCustomActionCase(StringBuilder js) {
        js.append("""
                        if (actionBlock.type === 'location_is_safe_condition') {
                            const location = actionBlock.getInputTargetBlock('LOCATION') ? extractLocationBlock(actionBlock.getInputTargetBlock('LOCATION')) : null;
                            const ifActions = getActionsFromStatementInput(actionBlock, 'IFACTIONS');
                            const elseActions = getActionsFromStatementInput(actionBlock, 'ELSEACTIONS');

                            actions.push({
                                type: 'location_is_safe_condition',
                                location: location,
                                checkSolidGround: actionBlock.getFieldValue('CHECKSOLIDGROUND') === 'TRUE',
                                checkDangerousBlocks: actionBlock.getFieldValue('CHECKDANGEROUSBLOCKS') === 'TRUE',
                                ifActions: ifActions,
                                elseActions: elseActions
                            });
                        }
        """);
    }

    @Override
    public void generateCustomActionLoadingCase(StringBuilder js) {
        js.append("""
                            if (action.type === 'location_is_safe_condition') {
                                actionBlock = workspace.newBlock('location_is_safe_condition');
                                
                                if (action.location) {
                                    const locationBlock = createLocationBlock(workspace, action.location);
                                    actionBlock.getInput('LOCATION').connection.connect(locationBlock.outputConnection);
                                }
                                
                                actionBlock.setFieldValue(action.checkSolidGround ? 'TRUE' : 'FALSE', 'CHECKSOLIDGROUND');
                                actionBlock.setFieldValue(action.checkDangerousBlocks ? 'TRUE' : 'FALSE', 'CHECKDANGEROUSBLOCKS');
                                
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
