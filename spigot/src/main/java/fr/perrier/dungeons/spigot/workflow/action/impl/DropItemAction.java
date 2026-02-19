package fr.perrier.dungeons.spigot.workflow.action.impl;

import fr.perrier.dungeons.spigot.webeditor.blockly.BlocklyAction;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyInfo;
import fr.perrier.dungeons.spigot.workflow.action.Action;
import fr.perrier.dungeons.spigot.workflow.blocks.LocationBlock;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Objects;

@Setter
@Getter
@BlocklyInfo(
        name = "drop_item_action",
        color = "#2196F3",
        displayText = "Drop Item",
        tooltip = "Fait tomber un item à l'emplacement donné",
        category = "Actions"
)
public class DropItemAction extends Action implements BlocklyAction {
    private static final long serialVersionUID = 1L;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Item:",
            defaultValue = "STONE", order = 1)
    private String item;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Quantity:",
            defaultValue = "1", order = 2, max = 64, min = 1)
    private int quantity;

    @BlocklyField(type = BlocklyField.FieldType.LOCATION_INPUT, label = "Location:",
            defaultValue = "", order = 3)
    private LocationBlock locationBlock;

    public DropItemAction() {
        super("Drop Item", "drop_item_action");
        this.item = "STONE";
        this.quantity = 1;
        this.locationBlock = new LocationBlock();
    }

    public DropItemAction(String item, int quantity, LocationBlock location) {
        super("Drop Item", "drop_item_action");
        this.item = item;
        this.quantity = quantity;
        this.locationBlock = location;
    }

    @Override
    public boolean execute(Player triggerPlayer, Location location, Map<String, Object> data) {
        Location dropLocation = this.locationBlock.toLocation();
        if (dropLocation == null) {
            return false;
        }
        Objects.requireNonNull(dropLocation.getWorld()).dropItemNaturally(dropLocation, new ItemStack(Objects.requireNonNull(Material.getMaterial(item)), quantity));
        return true;
    }
}
