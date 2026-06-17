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

import java.util.HashMap;
import java.util.Map;

/**
 * Trigger pour détecter les clics sur les blocs ou interactions
 */
@Setter
@Getter
@BlocklyInfo(
        name = "block_click_trigger",
        color = "#059000",
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

    // Transient cached enum to avoid Material.valueOf() parse on every trigger check
    private transient Material cachedMaterialEnum;

    public void setBlockMaterial(String blockMaterial) {
        this.blockMaterial = blockMaterial;
        this.cachedMaterialEnum = null;
    }

    @BlocklyField(type = BlocklyField.FieldType.LOCATION_INPUT, label = "Position du bloc :", defaultValue = "", order = 4)
    private LocationBlock locationBlock;

    @BlocklyField(type = BlocklyField.FieldType.CHECKBOX, label = "Position exacte seulement:",
            defaultValue = "false", order = 8)
    private boolean exactPositionOnly;

    @BlocklyField(type = BlocklyField.FieldType.CHECKBOX, label = "Une seule fois:",
            defaultValue = "false", order = 9)
    private boolean onlyOnce = false;

    private transient Map<String, Long> playerTriggerHistory;

    public BlockClickTrigger() {
        super("Block Click Trigger");
        this.clickType = "right_click";
        this.detectionType = "block";
        this.blockMaterial = "STONE";
        this.locationBlock = new LocationBlock(0, 64, 0, "world");
        this.exactPositionOnly = false;
        this.onlyOnce = false;
        this.playerTriggerHistory = new HashMap<>();
    }

    public BlockClickTrigger(String name) {
        super(name);
        this.clickType = "right_click";
        this.detectionType = "block";
        this.blockMaterial = "STONE";
        this.locationBlock = new LocationBlock(0, 64, 0, "world");
        this.exactPositionOnly = false;
        this.onlyOnce = false;
        this.playerTriggerHistory = new HashMap<>();
    }

    private void ensurePlayerTriggerHistoryInitialized() {
        if (this.playerTriggerHistory == null) {
            this.playerTriggerHistory = new HashMap<>();
        }
    }

    @Override
    public boolean execute(Player player, Location location, Map<String, Object> data) {
        if (!isEnabled()) {
            return false;
        }

        ensurePlayerTriggerHistoryInitialized();

        if (onlyOnce) {
            String playerId = player.getUniqueId().toString() + "_once";
            if (playerTriggerHistory.containsKey(playerId)) {
                return false;
            }
            playerTriggerHistory.put(playerId, System.currentTimeMillis());
        }

        // Exécuter les actions associées
        return executeActions(player, location, data);
    }

    /**
     * Remet à zéro l'historique d'un joueur pour ce trigger
     */
    public void resetPlayerHistory(Player player) {
        if (playerTriggerHistory != null) {
            playerTriggerHistory.remove(player.getUniqueId().toString() + "_once");
        }
    }

    /**
     * Remet à zéro tout l'historique du trigger
     */
    public void resetAllHistory() {
        if (playerTriggerHistory != null) {
            playerTriggerHistory.clear();
        }
    }

    @Override
    public boolean checkConditions(Player player, Map<String, Object> data) {
        if (!isEnabled()) {
            return false;
        }

        if(Main.getLoggerUtil().isDebugEnabled()) {
            Main.getLoggerUtil().info("Checking conditions for BlockClickTrigger: " + getName());
            Main.getLoggerUtil().info("Trigger click type: " + clickType + ", detection type: " + detectionType);
            Main.getLoggerUtil().info("Event data: " + data);
        }

        // Vérifier le type de clic
        String eventClickType = (String) data.get("click_type");
        if (eventClickType == null) {
            return false;
        }

        if(Main.getLoggerUtil().isDebugEnabled()) {
            Main.getLoggerUtil().info("Event click type: " + eventClickType);
        }

        if (!clickType.equals("both") && !clickType.equals(eventClickType)) {
            return false;
        }

        if(Main.getLoggerUtil().isDebugEnabled()) {
            Main.getLoggerUtil().info("Click type matches trigger: " + clickType);
        }

        // Vérifier le type de détection
        String eventDetectionType = (String) data.get("detection_type");
        if (eventDetectionType == null || !eventDetectionType.equals(detectionType)) {
            return false;
        }

        if(Main.getLoggerUtil().isDebugEnabled()) {
            Main.getLoggerUtil().info("Detection type matches trigger: " + detectionType);
        }

        // Vérifier le bloc cliqué
        Block clickedBlock = (Block) data.get("clicked_block");
        if (clickedBlock == null) {
            return false;
        }

        if(Main.getLoggerUtil().isDebugEnabled()) {
            Main.getLoggerUtil().info("Clicked block: " + clickedBlock.getType() + " at " + clickedBlock.getLocation());
        }

        // Vérifier le monde
        if (locationBlock.getWorldName() != null && !locationBlock.getWorldName().isEmpty() && !clickedBlock.getWorld().getName().equals(locationBlock.getWorldName())) {
            return false;
        }

        if(Main.getLoggerUtil().isDebugEnabled()) {
            Main.getLoggerUtil().info("Block world matches trigger: " + locationBlock.getWorldName());
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

        if(Main.getLoggerUtil().isDebugEnabled()) {
            Main.getLoggerUtil().info("Block position matches trigger: " + (exactPositionOnly ? "Yes" : "No, exact position not required"));
        }

        // Vérifier le matériau
        if (blockMaterial != null && !blockMaterial.isEmpty() && !blockMaterial.equals("ANY")) {
            try {
                if (cachedMaterialEnum == null) {
                    cachedMaterialEnum = Material.valueOf(blockMaterial.toUpperCase());
                }
                if (clickedBlock.getType() != cachedMaterialEnum) {
                    return false;
                }
            } catch (IllegalArgumentException e) {
                Main.getLoggerUtil().warning("Invalid material: " + blockMaterial + " in BlockClickTrigger");
                return false;
            }
        }

        if(Main.getLoggerUtil().isDebugEnabled()) {
            Main.getLoggerUtil().info("Block material matches trigger: " + blockMaterial);
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
        if(Main.getLoggerUtil().isDebugEnabled()) {
           Main.getLoggerUtil().info("BlockClickTrigger.matchesBlock() - Checking trigger: " + getName());
            Main.getLoggerUtil().info("  clickType: " + clickType + " vs " + clickTypeEvent);
            Main.getLoggerUtil().info("  detectionType: " + detectionType + " vs " + detectionTypeEvent);
            Main.getLoggerUtil().info("  blockMaterial: " + blockMaterial + " vs " + block.getType());
            Main.getLoggerUtil().info("  position: (" + locationBlock.getX() + "," + locationBlock.getY() + "," + locationBlock.getZ() + ") vs (" + block.getX() + "," + block.getY() + "," + block.getZ() + ")");
        }

        // Vérifier le type de clic
        if (!clickType.equals("both") && !clickType.equals(clickTypeEvent)) {
            if(Main.getLoggerUtil().isDebugEnabled()) {
                Main.getLoggerUtil().info("  ❌ Click type mismatch!");
            }
            return false;
        }

        // Vérifier le type de détection
        if (!detectionType.equals(detectionTypeEvent)) {
            if(Main.getLoggerUtil().isDebugEnabled()) {
                Main.getLoggerUtil().info("  ❌ Detection type mismatch! (" + detectionType + " != " + detectionTypeEvent + ")");
            }
            return false;
        }

        // Vérifier le monde
        if (locationBlock.getWorldName() != null && !locationBlock.getWorldName().isEmpty() && !block.getWorld().getName().equals(locationBlock.getWorldName())) {
            if(Main.getLoggerUtil().isDebugEnabled()) {
                Main.getLoggerUtil().info("  ❌ World mismatch!");
            }
            return false;
        }

        // Vérifier la position si demandé
        if (exactPositionOnly) {
            Location blockLocation = block.getLocation();
            if (blockLocation.getBlockX() != locationBlock.getX() ||
                blockLocation.getBlockY() != locationBlock.getY() ||
                blockLocation.getBlockZ() != locationBlock.getZ()) {
                if(Main.getLoggerUtil().isDebugEnabled()) {
                    Main.getLoggerUtil().info("  ❌ Position mismatch!");
                }
                return false;
            }
        }

        // Vérifier le matériau
        if (blockMaterial != null && !blockMaterial.isEmpty() && !blockMaterial.equals("ANY")) {
            try {
                if (cachedMaterialEnum == null) {
                    cachedMaterialEnum = Material.valueOf(blockMaterial.toUpperCase());
                }
                boolean matches = block.getType() == cachedMaterialEnum;
                if(fr.perrier.dungeons.spigot.Main.getLoggerUtil().isDebugEnabled()) {
                    fr.perrier.dungeons.spigot.Main.getLoggerUtil().info("  Material check: " + (matches ? "✅ MATCH" : "❌ NO MATCH"));
                }
                return matches;
            } catch (IllegalArgumentException e) {
                fr.perrier.dungeons.spigot.Main.getLoggerUtil().warning("Invalid material: " + blockMaterial + " in BlockClickTrigger");
                return false;
            }
        }

        if(fr.perrier.dungeons.spigot.Main.getLoggerUtil().isDebugEnabled()) {
            fr.perrier.dungeons.spigot.Main.getLoggerUtil().info("  ✅ ALL CHECKS PASSED!");
        }
        return true;
    }
}
