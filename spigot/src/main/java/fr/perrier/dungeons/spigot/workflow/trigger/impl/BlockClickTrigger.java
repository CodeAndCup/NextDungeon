package fr.perrier.dungeons.spigot.workflow.trigger.impl;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.webserver.blockly.BlocklyTrigger;
import fr.perrier.dungeons.spigot.workflow.trigger.Trigger;
import fr.perrier.dungeons.spigot.webserver.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.spigot.webserver.blockly.annotations.BlocklyInfo;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Trigger pour détecter les clics sur les blocs ou interactions
 */
@Setter
@Getter
@BlocklyInfo(
        name = "block_click_trigger",
        color = "#795548",
        displayText = "🖱️ Clic sur bloc",
        tooltip = "Déclenché quand un joueur clique sur un bloc spécifique",
        category = "Triggers"
)
public class BlockClickTrigger extends Trigger implements BlocklyTrigger {
    private static final long serialVersionUID = 1L;

    @BlocklyField(type = BlocklyField.FieldType.DROPDOWN, label = "Type de clic:",
            options = "left_click,right_click,both", defaultValue = "right_click", order = 1)
    private String clickType;

    @BlocklyField(type = BlocklyField.FieldType.DROPDOWN, label = "Type de détection:",
            options = "block,interaction", defaultValue = "block", order = 2)
    private String detectionType;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Matériau du bloc:",
            defaultValue = "STONE", order = 3)
    private String blockMaterial;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Position X:",
            defaultValue = "0", order = 4)
    private int blockX;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Position Y:",
            defaultValue = "64", order = 5)
    private int blockY;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Position Z:",
            defaultValue = "0", order = 6)
    private int blockZ;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Monde:",
            defaultValue = "world", order = 7)
    private String worldName;

    @BlocklyField(type = BlocklyField.FieldType.CHECKBOX, label = "Position exacte seulement:",
            defaultValue = "false", order = 8)
    private boolean exactPositionOnly;

    public BlockClickTrigger() {
        super("Block Click Trigger");
        this.clickType = "right_click";
        this.detectionType = "block";
        this.blockMaterial = "STONE";
        this.blockX = 0;
        this.blockY = 64;
        this.blockZ = 0;
        this.worldName = "world";
        this.exactPositionOnly = false;
    }

    public BlockClickTrigger(String name) {
        super(name);
        this.clickType = "right_click";
        this.detectionType = "block";
        this.blockMaterial = "STONE";
        this.blockX = 0;
        this.blockY = 64;
        this.blockZ = 0;
        this.worldName = "world";
        this.exactPositionOnly = false;
    }

    @Override
    public boolean execute(Player player, Location location, Map<String, Object> data) {
        if (!isEnabled()) {
            return false;
        }

        // Exécuter les actions associées
        return executeActions(player, location, data);
    }

    @Override
    public boolean checkConditions(Player player, Map<String, Object> data) {
        if (!isEnabled()) {
            return false;
        }

        // Vérifier le type de clic
        String eventClickType = (String) data.get("click_type");
        if (eventClickType == null) {
            return false;
        }

        if (!clickType.equals("both") && !clickType.equals(eventClickType)) {
            return false;
        }

        // Vérifier le type de détection
        String eventDetectionType = (String) data.get("detection_type");
        if (eventDetectionType == null || !eventDetectionType.equals(detectionType)) {
            return false;
        }

        // Vérifier le bloc cliqué
        Block clickedBlock = (Block) data.get("clicked_block");
        if (clickedBlock == null) {
            return false;
        }

        // Vérifier le monde
        if (worldName != null && !worldName.isEmpty() && !clickedBlock.getWorld().getName().equals(worldName)) {
            return false;
        }

        // Vérifier la position si demandé
        if (exactPositionOnly) {
            Location blockLocation = clickedBlock.getLocation();
            if (blockLocation.getBlockX() != blockX ||
                blockLocation.getBlockY() != blockY ||
                blockLocation.getBlockZ() != blockZ) {
                return false;
            }
        }

        // Vérifier le matériau
        if (blockMaterial != null && !blockMaterial.isEmpty() && !blockMaterial.equals("ANY")) {
            try {
                Material expectedMaterial = Material.valueOf(blockMaterial.toUpperCase());
                if (clickedBlock.getType() != expectedMaterial) {
                    return false;
                }
            } catch (IllegalArgumentException e) {
                Main.getInstance().getLogger().warning("&eInvalid material: " + blockMaterial + " in BlockClickTrigger");
                return false;
            }
        }

        return true;
    }

    @Override
    public String getType() {
        return "block_click_trigger";
    }

    /**
     * Vérifie si ce trigger correspond à un bloc spécifique
     */
    public boolean matchesBlock(Block block, String clickTypeEvent, String detectionTypeEvent) {
        // Vérifier le type de clic
        if (!clickType.equals("both") && !clickType.equals(clickTypeEvent)) {
            return false;
        }

        // Vérifier le type de détection
        if (!detectionType.equals(detectionTypeEvent)) {
            return false;
        }

        // Vérifier le monde
        if (worldName != null && !worldName.isEmpty() && !block.getWorld().getName().equals(worldName)) {
            return false;
        }

        // Vérifier la position si demandé
        if (exactPositionOnly) {
            Location blockLocation = block.getLocation();
            if (blockLocation.getBlockX() != blockX ||
                blockLocation.getBlockY() != blockY ||
                blockLocation.getBlockZ() != blockZ) {
                return false;
            }
        }

        // Vérifier le matériau
        if (blockMaterial != null && !blockMaterial.isEmpty() && !blockMaterial.equals("ANY")) {
            try {
                Material expectedMaterial = Material.valueOf(blockMaterial.toUpperCase());
                return block.getType() == expectedMaterial;
            } catch (IllegalArgumentException e) {
                Main.getInstance().getLogger().warning("&eInvalid material: " + blockMaterial + " in BlockClickTrigger");
                return false;
            }
        }

        return true;
    }
}
