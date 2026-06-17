package fr.perrier.dungeons.spigot.workflow.trigger.impl;

import fr.perrier.dungeons.spigot.webeditor.blockly.BlocklyTrigger;
import fr.perrier.dungeons.spigot.workflow.trigger.Trigger;
import fr.perrier.dungeons.spigot.utils.ServerUtil;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyInfo;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Trigger qui se déclenche quand un joueur ramasse un item
 */
@Getter
@Setter
@BlocklyInfo(
        name = "item_pickup_trigger",
        color = "#059000",
        displayText = "Quand le joueur ramasse un item",
        tooltip = "Déclenche quand un joueur ramasse un item spécifique",
        category = "Triggers"
)
public class ItemPickupTrigger extends Trigger implements BlocklyTrigger {

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Type d'item:",
            defaultValue = "DIAMOND", order = 1)
    private String itemMaterial;

    // Transient cached enum to avoid Material.valueOf() parse on every trigger check
    private transient Material cachedMaterialEnum;

    public void setItemMaterial(String itemMaterial) {
        this.itemMaterial = itemMaterial;
        this.cachedMaterialEnum = null;
    }

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Quantité minimum:",
            defaultValue = "1", order = 2)
    private int minAmount;

    @BlocklyField(type = BlocklyField.FieldType.CHECKBOX, label = "Annuler le ramassage:",
            defaultValue = "false", order = 3)
    private boolean cancelPickup;

    public ItemPickupTrigger(String name) {
        super(name);
        this.itemMaterial = "DIAMOND";
        this.minAmount = 1;
        this.cancelPickup = false;
    }

    public ItemPickupTrigger() {
        super("Item Pickup Trigger");
        this.itemMaterial = "DIAMOND";
        this.minAmount = 1;
        this.cancelPickup = false;
    }

    @Override
    public String getType() {
        return "item_pickup_trigger";
    }

    @Override
    public boolean execute(Player player, Location location, Map<String, Object> data) {
        if (ServerUtil.isInEditMode()) return false;

        if (!checkConditions(player, data)) {
            return false;
        }
        return executeActions(player, location, data);
    }

    @Override
    public boolean checkConditions(Player player, Map<String, Object> data) {
        return enabled;
    }

    /**
     * Vérifie si ce trigger correspond à l'item ramassé
     */
    public boolean matchesItem(ItemStack item) {
        if (item == null) {
            return false;
        }

        // Vérifier le type d'item (ANY = tous les items)
        if (itemMaterial != null && !itemMaterial.isEmpty() && !itemMaterial.equals("ANY")) {
            try {
                if (cachedMaterialEnum == null) {
                    cachedMaterialEnum = Material.valueOf(itemMaterial.toUpperCase());
                }
                if (item.getType() != cachedMaterialEnum) {
                    return false;
                }
            } catch (IllegalArgumentException e) {
                return false;
            }
        }

        return item.getAmount() >= minAmount;
    }

    public boolean shouldCancelPickup() {
        return cancelPickup;
    }
}
