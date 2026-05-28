package fr.perrier.dungeons.spigot.workflow.action.impl;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.loot.LootChestManager;
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

import java.io.Serial;
import java.util.Locale;
import java.util.Map;

/**
 * Spawns a loot chest at a location with either GLOBAL (shared, vanilla) or
 * PER_PLAYER (instanced) loot semantics. The actual block placement and the
 * per-player virtual inventories are handled by {@link LootChestManager}.
 *
 * <p>Loot is declared inline as {@code MATERIAL:qty} / {@code MATERIAL:min-max}
 * comma-separated, e.g. {@code "DIAMOND:3,GOLD_INGOT:1-5,IRON_INGOT:10"}.</p>
 */
@Setter
@Getter
@BlocklyInfo(
        name = "spawn_loot_chest_action",
        color = "#795548",
        displayText = "📦 Générer un coffre de loot",
        tooltip = "Place un coffre rempli de loot.\n"
                + "GLOBAL = coffre partagé (premier arrivé premier servi).\n"
                + "PER_PLAYER = chaque joueur a son propre loot dans le même coffre.\n"
                + "Loot: MATERIAL:qty ou MATERIAL:min-max, séparés par des virgules.",
        category = "Actions"
)
public class SpawnLootChestAction extends Action implements BlocklyAction {
    @Serial
    private static final long serialVersionUID = 1L;

    @BlocklyField(type = BlocklyField.FieldType.LOCATION_INPUT, label = "Position:",
            defaultValue = "", order = 1)
    private LocationBlock location;

    @BlocklyField(type = BlocklyField.FieldType.DROPDOWN, label = "Type de coffre:",
            options = "CHEST,TRAPPED_CHEST,BARREL", defaultValue = "CHEST", order = 2)
    private String chestType;

    @BlocklyField(type = BlocklyField.FieldType.DROPDOWN, label = "Mode de loot:",
            options = "GLOBAL,PER_PLAYER", defaultValue = "GLOBAL", order = 3)
    private String lootMode;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Loot (MATERIAL:qty,...):",
            defaultValue = "DIAMOND:3,GOLD_INGOT:1-5", order = 4)
    private String loot;

    public SpawnLootChestAction() {
        super("Spawn Loot Chest", "spawn_loot_chest_action");
        this.location = new LocationBlock();
        this.chestType = "CHEST";
        this.lootMode = "GLOBAL";
        this.loot = "DIAMOND:3,GOLD_INGOT:1-5";
    }

    public SpawnLootChestAction(LocationBlock location, String chestType, String lootMode, String loot) {
        super("Spawn Loot Chest", "spawn_loot_chest_action");
        this.location = location;
        this.chestType = chestType;
        this.lootMode = lootMode;
        this.loot = loot;
    }

    @Override
    public boolean execute(Player triggerPlayer, Location location, Map<String, Object> data) {
        Location target = this.location != null ? this.location.toLocation() : null;
        if (target == null || target.getWorld() == null) {
            Main.getLoggerUtil().warning("[LootChest] SpawnLootChestAction: invalid location");
            return false;
        }

        Material material;
        try {
            material = Material.valueOf((chestType == null ? "CHEST" : chestType).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            Main.getLoggerUtil().warning("[LootChest] Invalid chest type '" + chestType + "', defaulting to CHEST");
            material = Material.CHEST;
        }

        LootChestManager.LootMode mode;
        try {
            mode = LootChestManager.LootMode.valueOf((lootMode == null ? "GLOBAL" : lootMode).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            mode = LootChestManager.LootMode.GLOBAL;
        }

        return LootChestManager.get().placeChest(target, material, mode, loot);
    }
}
