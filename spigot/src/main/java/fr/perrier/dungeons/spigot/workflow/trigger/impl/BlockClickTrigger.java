package fr.perrier.dungeons.spigot.workflow.trigger.impl;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.webeditor.blockly.BlocklyTrigger;
import fr.perrier.dungeons.spigot.workflow.blocks.LocationBlock;
import fr.perrier.dungeons.spigot.workflow.trigger.Trigger;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyInfo;
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

    @BlocklyField(type = BlocklyField.FieldType.LOCATION_INPUT, label = "Position du bloc :", defaultValue = "", order = 4)
    private LocationBlock locationBlock;

    @BlocklyField(type = BlocklyField.FieldType.CHECKBOX, label = "Position exacte seulement:",
            defaultValue = "false", order = 8)
    private boolean exactPositionOnly;

    public BlockClickTrigger() {
        super("Block Click Trigger");
        this.clickType = "right_click";
        this.detectionType = "block";
        this.blockMaterial = "STONE";
        this.locationBlock = new LocationBlock(0, 64, 0, "world");
        this.exactPositionOnly = false;
    }

    public BlockClickTrigger(String name) {
        super(name);
        this.clickType = "right_click";
        this.detectionType = "block";
        this.blockMaterial = "STONE";
        this.locationBlock = new LocationBlock(0, 64, 0, "world");
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

        if(Main.isDebug()) {
            Main.getInstance().getLogger().info("Checking conditions for BlockClickTrigger: " + getName());
            Main.getInstance().getLogger().info("Trigger click type: " + clickType + ", detection type: " + detectionType);
            Main.getInstance().getLogger().info("Event data: " + data);
        }

        // Vérifier le type de clic
        String eventClickType = (String) data.get("click_type");
        if (eventClickType == null) {
            return false;
        }

        if(Main.isDebug()) {
            Main.getInstance().getLogger().info("Event click type: " + eventClickType);
        }

        if (!clickType.equals("both") && !clickType.equals(eventClickType)) {
            return false;
        }

        if(Main.isDebug()) {
            Main.getInstance().getLogger().info("Click type matches trigger: " + clickType);
        }

        // Vérifier le type de détection
        String eventDetectionType = (String) data.get("detection_type");
        if (eventDetectionType == null || !eventDetectionType.equals(detectionType)) {
            return false;
        }

        if(Main.isDebug()) {
            Main.getInstance().getLogger().info("Detection type matches trigger: " + detectionType);
        }

        // Vérifier le bloc cliqué
        Block clickedBlock = (Block) data.get("clicked_block");
        if (clickedBlock == null) {
            return false;
        }

        if(Main.isDebug()) {
            Main.getInstance().getLogger().info("Clicked block: " + clickedBlock.getType() + " at " + clickedBlock.getLocation());
        }

        // Vérifier le monde
        if (locationBlock.getWorldName() != null && !locationBlock.getWorldName().isEmpty() && !clickedBlock.getWorld().getName().equals(locationBlock.getWorldName())) {
            return false;
        }

        if(Main.isDebug()) {
            Main.getInstance().getLogger().info("Block world matches trigger: " + locationBlock.getWorldName());
        }

        // Vérifier la position si demandé
        if (exactPositionOnly) {
            Location blockLocation = clickedBlock.getLocation();
            if (blockLocation.getBlockX() != locationBlock.getX() ||
                blockLocation.getBlockY() != locationBlock.getY() ||
                blockLocation.getBlockZ() != locationBlock.getZ()) {
                return false;
            }
        }

        if(Main.isDebug()) {
            Main.getInstance().getLogger().info("Block position matches trigger: " + (exactPositionOnly ? "Yes" : "No, exact position not required"));
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

        if(Main.isDebug()) {
            Main.getInstance().getLogger().info("Block material matches trigger: " + blockMaterial);
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
        if(Main.isDebug()) {
           Main.getInstance().getLogger().info("BlockClickTrigger.matchesBlock() - Checking trigger: " + getName());
            Main.getInstance().getLogger().info("  clickType: " + clickType + " vs " + clickTypeEvent);
            Main.getInstance().getLogger().info("  detectionType: " + detectionType + " vs " + detectionTypeEvent);
            Main.getInstance().getLogger().info("  blockMaterial: " + blockMaterial + " vs " + block.getType());
            Main.getInstance().getLogger().info("  position: (" + locationBlock.getX() + "," + locationBlock.getY() + "," + locationBlock.getZ() + ") vs (" + block.getX() + "," + block.getY() + "," + block.getZ() + ")");
        }

        // Vérifier le type de clic
        if (!clickType.equals("both") && !clickType.equals(clickTypeEvent)) {
            if(Main.isDebug()) {
                Main.getInstance().getLogger().info("  ❌ Click type mismatch!");
            }
            return false;
        }

        // Vérifier le type de détection
        if (!detectionType.equals(detectionTypeEvent)) {
            if(Main.isDebug()) {
                Main.getInstance().getLogger().info("  ❌ Detection type mismatch! (" + detectionType + " != " + detectionTypeEvent + ")");
            }
            return false;
        }

        // Vérifier le monde
        if (locationBlock.getWorldName() != null && !locationBlock.getWorldName().isEmpty() && !block.getWorld().getName().equals(locationBlock.getWorldName())) {
            if(Main.isDebug()) {
                Main.getInstance().getLogger().info("  ❌ World mismatch!");
            }
            return false;
        }

        // Vérifier la position si demandé
        if (exactPositionOnly) {
            Location blockLocation = block.getLocation();
            if (blockLocation.getBlockX() != locationBlock.getX() ||
                blockLocation.getBlockY() != locationBlock.getY() ||
                blockLocation.getBlockZ() != locationBlock.getZ()) {
                if(Main.isDebug()) {
                    Main.getInstance().getLogger().info("  ❌ Position mismatch!");
                }
                return false;
            }
        }

        // Vérifier le matériau
        if (blockMaterial != null && !blockMaterial.isEmpty() && !blockMaterial.equals("ANY")) {
            try {
                Material expectedMaterial = Material.valueOf(blockMaterial.toUpperCase());
                boolean matches = block.getType() == expectedMaterial;
                if(fr.perrier.dungeons.spigot.Main.isDebug()) {
                    fr.perrier.dungeons.spigot.Main.getInstance().getLogger().info("  Material check: " + (matches ? "✅ MATCH" : "❌ NO MATCH"));
                }
                return matches;
            } catch (IllegalArgumentException e) {
                fr.perrier.dungeons.spigot.Main.getInstance().getLogger().warning("&eInvalid material: " + blockMaterial + " in BlockClickTrigger");
                return false;
            }
        }

        if(fr.perrier.dungeons.spigot.Main.isDebug()) {
            fr.perrier.dungeons.spigot.Main.getInstance().getLogger().info("  ✅ ALL CHECKS PASSED!");
        }
        return true;
    }
}
