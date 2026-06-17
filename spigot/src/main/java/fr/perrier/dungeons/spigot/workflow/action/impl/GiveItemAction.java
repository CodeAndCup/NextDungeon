package fr.perrier.dungeons.spigot.workflow.action.impl;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.webeditor.blockly.BlocklyAction;
import fr.perrier.dungeons.spigot.workflow.action.Action;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyInfo;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.Serial;
import java.util.Map;

/**
 * Action pour donner un item à un joueur
 */
@Setter
@Getter
@BlocklyInfo(
        name = "give_item_action",
        color = "#2196F3",
        displayText = "🎁 Donner un item",
        tooltip = "Donne un item au joueur",
        category = "Actions"
)
public class GiveItemAction extends Action implements BlocklyAction {
    @Serial
    private static final long serialVersionUID = 1L;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Type d'item:",
            defaultValue = "DIAMOND", order = 1)
    private String itemMaterial;

    // Transient cached enum to avoid Material.valueOf() parse on every execute
    private transient org.bukkit.Material cachedMaterialEnum;

    public void setItemMaterial(String itemMaterial) {
        this.itemMaterial = itemMaterial;
        this.cachedMaterialEnum = null;
    }

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Quantité:",
            defaultValue = "1", order = 2)
    private int amount;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Nom personnalisé:",
            defaultValue = "", order = 3)
    private String customName;

    @BlocklyField(type = BlocklyField.FieldType.DROPDOWN, label = "Donner à:",
            options = "player,@all", defaultValue = "player", order = 4)
    private String target;

    public GiveItemAction() {
        super("Give Item", "give_item_action");
        this.itemMaterial = "DIAMOND";
        this.amount = 1;
        this.customName = "";
        this.target = "player";
    }

    public GiveItemAction(String itemMaterial, int amount, String customName, String target) {
        super("Give Item", "give_item_action");
        this.itemMaterial = itemMaterial;
        this.amount = amount;
        this.customName = customName;
        this.target = target;
    }

    @Override
    public boolean execute(Player triggerPlayer, Location location, Map<String, Object> data) {
        if (itemMaterial == null || itemMaterial.isEmpty()) {
            Main.getLoggerUtil().warning("Item material is empty in GiveItemAction");
            return false;
        }

        try {
            if (cachedMaterialEnum == null) {
                cachedMaterialEnum = Material.valueOf(itemMaterial.toUpperCase());
            }
            Material material = cachedMaterialEnum;
            ItemStack item = new ItemStack(material, Math.max(1, amount));

            // Appliquer un nom personnalisé si fourni
            if (customName != null && !customName.isEmpty()) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    String formattedName = fr.perrier.cupcodeapi.utils.ChatUtil.translate(customName);
                    formattedName = Main.getInstance().getVariableRegistry().formatVariable(formattedName, triggerPlayer);
                    meta.setDisplayName(formattedName);
                    item.setItemMeta(meta);
                }
            }

            if ("@all".equals(target)) {
                // Donner l'item à tous les joueurs en ligne
                for (Player onlinePlayer : Main.getInstance().getServer().getOnlinePlayers()) {
                    onlinePlayer.getInventory().addItem(item.clone());
                }
            } else {
                // Donner l'item au joueur déclencheur
                if (triggerPlayer != null) {
                    triggerPlayer.getInventory().addItem(item);
                }
            }

            if (Main.getLoggerUtil().isDebugEnabled()) {
                Main.getLoggerUtil().info("Gave " + amount + "x " + itemMaterial + " to " + target);
            }

            return true;

        } catch (IllegalArgumentException e) {
            Main.getLoggerUtil().warning("Invalid material: " + itemMaterial);
            return false;
        }
    }
}
