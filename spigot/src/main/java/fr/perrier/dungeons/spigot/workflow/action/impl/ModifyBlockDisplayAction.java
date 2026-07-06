package fr.perrier.dungeons.spigot.workflow.action.impl;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.webeditor.blockly.BlocklyAction;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyInfo;
import fr.perrier.dungeons.spigot.workflow.action.Action;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.Serial;
import java.util.Locale;
import java.util.Map;

/**
 * Action pour modifier un BlockDisplay existant
 */
@Setter
@Getter
@BlocklyInfo(
        name = "modify_block_display_action",
        color = "#2196F3",
        displayText = "🔧 Modifier un affichage de bloc",
        tooltip = "Permet de modifier un BlockDisplay existant via son ID.",
        category = "Actions"
)
public class ModifyBlockDisplayAction extends Action implements BlocklyAction {
    @Serial
    private static final long serialVersionUID = 1L;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "ID du BlockDisplay:",
            defaultValue = "", order = 1)
    private String displayId;

    @BlocklyField(type = BlocklyField.FieldType.DROPDOWN, label = "Propriété à modifier:",
            options = "BLOCK_TYPE,SCALE,TRANSLATION,ROTATION,ALL",
            defaultValue = "ALL", order = 2)
    private String propertyToModify;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Nouveau type de bloc (optionnel):",
            defaultValue = "", order = 3)
    private String blockType;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Scale X:",
            defaultValue = "1.0", order = 4)
    private float scaleX;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Scale Y:",
            defaultValue = "1.0", order = 5)
    private float scaleY;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Scale Z:",
            defaultValue = "1.0", order = 6)
    private float scaleZ;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Translation X:",
            defaultValue = "0", order = 7)
    private float translationX;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Translation Y:",
            defaultValue = "0", order = 8)
    private float translationY;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Translation Z:",
            defaultValue = "0", order = 9)
    private float translationZ;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Left Rotation (X):",
            defaultValue = "0", order = 10)
    private float leftRotationX;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Left Rotation (Y):",
            defaultValue = "0", order = 11)
    private float leftRotationY;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Left Rotation (Z):",
            defaultValue = "0", order = 12)
    private float leftRotationZ;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Left Rotation (W):",
            defaultValue = "1", order = 13)
    private float leftRotationW;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Right Rotation (X):",
            defaultValue = "0", order = 14)
    private float rightRotationX;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Right Rotation (Y):",
            defaultValue = "0", order = 15)
    private float rightRotationY;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Right Rotation (Z):",
            defaultValue = "0", order = 16)
    private float rightRotationZ;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Right Rotation (W):",
            defaultValue = "1", order = 17)
    private float rightRotationW;

    public ModifyBlockDisplayAction() {
        super("Modify Block Display", "modify_block_display_action");
        this.displayId = "";
        this.propertyToModify = "ALL";
        this.blockType = "";
        this.scaleX = 1.0f;
        this.scaleY = 1.0f;
        this.scaleZ = 1.0f;
        this.translationX = 0;
        this.translationY = 0;
        this.translationZ = 0;
        this.leftRotationX = 0;
        this.leftRotationY = 0;
        this.leftRotationZ = 0;
        this.leftRotationW = 1;
        this.rightRotationX = 0;
        this.rightRotationY = 0;
        this.rightRotationZ = 0;
        this.rightRotationW = 1;
    }

    public ModifyBlockDisplayAction(String displayId, String propertyToModify, String blockType,
                                    float scaleX, float scaleY, float scaleZ,
                                    float translationX, float translationY, float translationZ,
                                    float leftRotationX, float leftRotationY, float leftRotationZ, float leftRotationW,
                                    float rightRotationX, float rightRotationY, float rightRotationZ, float rightRotationW) {
        super("Modify Block Display", "modify_block_display_action");
        this.displayId = displayId;
        this.propertyToModify = propertyToModify != null ? propertyToModify : "ALL";
        this.blockType = blockType != null ? blockType : "";
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.scaleZ = scaleZ;
        this.translationX = translationX;
        this.translationY = translationY;
        this.translationZ = translationZ;
        this.leftRotationX = leftRotationX;
        this.leftRotationY = leftRotationY;
        this.leftRotationZ = leftRotationZ;
        this.leftRotationW = leftRotationW;
        this.rightRotationX = rightRotationX;
        this.rightRotationY = rightRotationY;
        this.rightRotationZ = rightRotationZ;
        this.rightRotationW = rightRotationW;
    }

    @Override
    public boolean execute(Player triggerPlayer, Location location, Map<String, Object> data) {
        if (displayId == null || displayId.isEmpty()) {
            Main.getLoggerUtil().warning("Display ID is empty in ModifyBlockDisplayAction");
            return false;
        }

        // Récupérer le BlockDisplay existant
        BlockDisplay blockDisplay = SummonBlockDisplayAction.getBlockDisplay(displayId);
        if (blockDisplay == null) {
            Main.getLoggerUtil().warning("BlockDisplay not found with ID: " + displayId);
            return false;
        }

        try {
            String modifyType = propertyToModify != null ? propertyToModify.toUpperCase(Locale.ROOT) : "ALL";

            // Modifier le type de bloc si demandé
            if ((modifyType.equals("BLOCK_TYPE") || modifyType.equals("ALL")) && blockType != null && !blockType.isEmpty()) {
                try {
                    Material material = Material.valueOf(blockType.toUpperCase(Locale.ROOT));
                    blockDisplay.setBlock(material.createBlockData());
                    Main.getLoggerUtil().info("BlockDisplay block type updated: " + blockType);
                } catch (IllegalArgumentException e) {
                    Main.getLoggerUtil().warning("Invalid block type in ModifyBlockDisplayAction: " + blockType);
                }
            }

            // Modifier la transformation (scale + rotations + translations) si demandé
            if ((modifyType.equals("SCALE") || modifyType.equals("TRANSLATION") || modifyType.equals("ROTATION") || modifyType.equals("ALL"))) {
                try {
                    Quaternionf leftRotation = new Quaternionf(leftRotationX, leftRotationY, leftRotationZ, leftRotationW);
                    Quaternionf rightRotation = new Quaternionf(rightRotationX, rightRotationY, rightRotationZ, rightRotationW);

                    Transformation transformation = new Transformation(
                            new Vector3f(translationX, translationY, translationZ),
                            leftRotation,
                            new Vector3f(scaleX, scaleY, scaleZ),
                            rightRotation
                    );

                    blockDisplay.setTransformation(transformation);
                    Main.getLoggerUtil().info("BlockDisplay transformation updated for ID: " + displayId);
                } catch (Exception e) {
                    Main.getLoggerUtil().severe("Error updating BlockDisplay transformation: " + e.getMessage());
                    e.printStackTrace();
                    return false;
                }
            }

            Main.getLoggerUtil().info("BlockDisplay modified successfully with ID: " + displayId);
            return true;

        } catch (Exception e) {
            Main.getLoggerUtil().severe("Error modifying BlockDisplay: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}

