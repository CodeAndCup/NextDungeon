package fr.perrier.dungeons.spigot.workflow.action.impl;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.webeditor.blockly.BlocklyAction;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyInfo;
import fr.perrier.dungeons.spigot.workflow.action.Action;
import fr.perrier.dungeons.spigot.workflow.blocks.LocationBlock;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.Serial;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Action pour invoquer un BlockDisplay
 */
@Setter
@Getter
@BlocklyInfo(
        name = "summon_block_display_action",
        color = "#2196F3",
        displayText = "🧱 Invoquer un affichage de bloc",
        tooltip = "Permet d'invoquer un BlockDisplay avec des transformations personnalisées.",
        category = "Actions"
)
public class SummonBlockDisplayAction extends Action implements BlocklyAction {
    @Serial
    private static final long serialVersionUID = 1L;

    // Stockage global des BlockDisplay par ID pour pouvoir les modifier ultérieurement
    private static final Map<String, BlockDisplay> blockDisplays = new HashMap<>();

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Type de bloc:",
            defaultValue = "DIAMOND_BLOCK", order = 1)
    private String blockType;

    @BlocklyField(type = BlocklyField.FieldType.LOCATION_INPUT, label = "Position:",
            defaultValue = "", order = 2)
    private LocationBlock location;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Scale X:",
            defaultValue = "1.0", order = 3)
    private float scaleX;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Scale Y:",
            defaultValue = "1.0", order = 4)
    private float scaleY;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Scale Z:",
            defaultValue = "1.0", order = 5)
    private float scaleZ;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Translation X:",
            defaultValue = "0", order = 6)
    private float translationX;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Translation Y:",
            defaultValue = "0", order = 7)
    private float translationY;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Translation Z:",
            defaultValue = "0", order = 8)
    private float translationZ;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Left Rotation (X):",
            defaultValue = "0", order = 9)
    private float leftRotationX;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Left Rotation (Y):",
            defaultValue = "0", order = 10)
    private float leftRotationY;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Left Rotation (Z):",
            defaultValue = "0", order = 11)
    private float leftRotationZ;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Left Rotation (W):",
            defaultValue = "1", order = 12)
    private float leftRotationW;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Right Rotation (X):",
            defaultValue = "0", order = 13)
    private float rightRotationX;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Right Rotation (Y):",
            defaultValue = "0", order = 14)
    private float rightRotationY;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Right Rotation (Z):",
            defaultValue = "0", order = 15)
    private float rightRotationZ;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Right Rotation (W):",
            defaultValue = "1", order = 16)
    private float rightRotationW;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "ID du BlockDisplay (optionnel):",
            defaultValue = "", order = 17)
    private String displayId;

    public SummonBlockDisplayAction() {
        super("Summon Block Display", "summon_block_display_action");
        this.blockType = "DIAMOND_BLOCK";
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
        this.displayId = "";
    }

    public SummonBlockDisplayAction(String blockType, LocationBlock location, float scaleX, float scaleY, float scaleZ,
                                     float translationX, float translationY, float translationZ,
                                     float leftRotationX, float leftRotationY, float leftRotationZ, float leftRotationW,
                                     float rightRotationX, float rightRotationY, float rightRotationZ, float rightRotationW,
                                     String displayId) {
        super("Summon Block Display", "summon_block_display_action");
        this.blockType = blockType;
        this.location = location;
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
        this.displayId = displayId != null ? displayId : "";
    }

    @Override
    public boolean execute(Player triggerPlayer, Location location, Map<String, Object> data) {
        if (this.location == null) {
            Main.getLoggerUtil().warning("Location is null in SummonBlockDisplayAction");
            return false;
        }

        World world = Bukkit.getWorld(this.location.getWorldName());
        if (world == null) {
            Main.getLoggerUtil().severe("World not found for SummonBlockDisplayAction: " + this.location.getWorldName());
            return false;
        }

        // Créer la location du spawn
        Location spawnLocation = new Location(
                world,
                this.location.getX(),
                this.location.getY(),
                this.location.getZ()
        );

        // Valider et récupérer le type de bloc
        Material material = null;
        try {
            String upperBlockType = blockType == null ? "DIAMOND_BLOCK" : blockType.toUpperCase();
            material = Material.valueOf(upperBlockType);
        } catch (IllegalArgumentException e) {
            Main.getLoggerUtil().warning("Invalid block type in SummonBlockDisplayAction: " + blockType);
            material = Material.DIAMOND_BLOCK;
        }

        try {
            // Invoquer le BlockDisplay
            BlockDisplay blockDisplay = (BlockDisplay) Objects.requireNonNull(world).spawnEntity(
                    spawnLocation,
                    EntityType.BLOCK_DISPLAY
            );

            // Configurer le bloc à afficher
            blockDisplay.setBlock(material.createBlockData());

            // Créer les quaternions pour les rotations
            Quaternionf leftRotation = new Quaternionf(leftRotationX, leftRotationY, leftRotationZ, leftRotationW);
            Quaternionf rightRotation = new Quaternionf(rightRotationX, rightRotationY, rightRotationZ, rightRotationW);

            // Créer la transformation
            Transformation transformation = new Transformation(
                    new Vector3f(translationX, translationY, translationZ),
                    leftRotation,
                    new Vector3f(scaleX, scaleY, scaleZ),
                    rightRotation
            );

            // Appliquer la transformation
            blockDisplay.setTransformation(transformation);

            // Enregistrer le BlockDisplay par ID si fourni
            if (displayId != null && !displayId.isEmpty()) {
                blockDisplays.put(displayId, blockDisplay);
            }

            Main.getLoggerUtil().info("BlockDisplay created successfully at " + spawnLocation + " with ID: " + displayId);
            return true;

        } catch (Exception e) {
            Main.getLoggerUtil().severe("Error spawning BlockDisplay: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Récupère un BlockDisplay par son ID
     * @param displayId L'ID du BlockDisplay
     * @return Le BlockDisplay ou null s'il n'existe pas
     */
    public static BlockDisplay getBlockDisplay(String displayId) {
        BlockDisplay display = blockDisplays.get(displayId);
        if (display != null && !display.isDead()) {
            return display;
        }
        blockDisplays.remove(displayId); // Nettoyer si l'entité est morte
        return null;
    }

    /**
     * Supprime un BlockDisplay de la cache
     * @param displayId L'ID du BlockDisplay
     */
    public static void removeBlockDisplay(String displayId) {
        blockDisplays.remove(displayId);
    }

    /**
     * Supprime tous les BlockDisplay en cache
     */
    public static void clearAllBlockDisplays() {
        blockDisplays.clear();
    }
}
