package fr.perrier.dungeons.spigot.workflow.action.impl;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
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
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Action WorldEdit //set avec support des patterns pondérés (ex: "70%stone,30%gravel")
 */
@Setter
@Getter
@BlocklyInfo(
        name = "worldedit_set_action",
        color = "#8BC34A",
        displayText = "🧱 WE Set Région (pos1/pos2)",
        tooltip = "Remplit une région (pos1 → pos2) avec un ou plusieurs blocs. Supporte les patterns pondérés: ex. \"70%stone,30%gravel\".",
        category = "Actions"
)
public class WorldEditSetAction extends Action implements BlocklyAction {
    private static final long serialVersionUID = 1L;

    @BlocklyField(type = BlocklyField.FieldType.LOCATION_INPUT, label = "Position 1:", defaultValue = "", order = 1)
    private LocationBlock pos1;

    @BlocklyField(type = BlocklyField.FieldType.LOCATION_INPUT, label = "Position 2:", defaultValue = "", order = 2)
    private LocationBlock pos2;

    /**
     * Pattern de blocs, ex : "stone", "70%stone,30%gravel"
     */
    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Pattern (ex: 70%stone,30%gravel):", defaultValue = "stone", order = 3)
    private String pattern;

    public WorldEditSetAction() {
        super("WorldEdit Set", "worldedit_set_action");
        this.pos1 = new LocationBlock();
        this.pos2 = new LocationBlock();
        this.pattern = "stone";
    }

    public WorldEditSetAction(LocationBlock pos1, LocationBlock pos2, String pattern) {
        super("WorldEdit Set", "worldedit_set_action");
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.pattern = pattern;
    }

    @Override
    public boolean execute(Player triggerPlayer, Location triggerLocation, Map<String, Object> data) {
        if (pos1 == null || pos2 == null || pattern == null || pattern.isBlank()) {
            Main.getLoggerUtil().warning("[WorldEditSetAction] pos1, pos2 ou pattern est null/vide.");
            return false;
        }

        World bukkitWorld = resolveWorld(triggerLocation);
        if (bukkitWorld == null) {
            Main.getLoggerUtil().warning("[WorldEditSetAction] Monde introuvable.");
            return false;
        }

        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(bukkitWorld);
        BlockVector3 min = BlockVector3.at(
                Math.min(pos1.getX(), pos2.getX()),
                Math.min(pos1.getY(), pos2.getY()),
                Math.min(pos1.getZ(), pos2.getZ()));
        BlockVector3 max = BlockVector3.at(
                Math.max(pos1.getX(), pos2.getX()),
                Math.max(pos1.getY(), pos2.getY()),
                Math.max(pos1.getZ(), pos2.getZ()));
        CuboidRegion region = new CuboidRegion(weWorld, min, max);

        List<WeightedBlock> weightedBlocks = parsePattern(pattern);
        if (weightedBlocks.isEmpty()) {
            Main.getLoggerUtil().warning("[WorldEditSetAction] Pattern invalide: " + pattern);
            return false;
        }

        try (EditSession editSession = com.sk89q.worldedit.WorldEdit.getInstance().newEditSession(weWorld)) {
            Random random = new Random();
            for (BlockVector3 pos : region) {
                BlockState block = pickBlock(weightedBlocks, random);
                if (block != null) {
                    editSession.setBlock(pos, block);
                }
            }
        } catch (Exception e) {
            Main.getLoggerUtil().severe("[WorldEditSetAction] Erreur: " + e.getMessage());
            e.printStackTrace(System.err);
            return false;
        }
        return true;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private World resolveWorld(Location triggerLocation) {
        // Priorité : pos1 world > pos2 world > triggerLocation world > "world"
        if (pos1 != null && pos1.isHasWorld() && pos1.getWorldName() != null) {
            return Bukkit.getWorld(pos1.getWorldName());
        }
        if (pos2 != null && pos2.isHasWorld() && pos2.getWorldName() != null) {
            return Bukkit.getWorld(pos2.getWorldName());
        }
        if (triggerLocation != null && triggerLocation.getWorld() != null) {
            return triggerLocation.getWorld();
        }
        return Bukkit.getWorld("world");
    }

    /**
     * Parse "70%stone,30%gravel" ou simplement "stone" en liste pondérée.
     */
    static List<WeightedBlock> parsePattern(String pattern) {
        List<WeightedBlock> list = new ArrayList<>();
        String[] parts = pattern.split(",");
        double totalWeight = 0;

        for (String part : parts) {
            part = part.trim();
            double weight = 1.0;
            String blockId = part;

            if (part.contains("%")) {
                String[] split = part.split("%", 2);
                try {
                    weight = Double.parseDouble(split[0].trim());
                    blockId = split[1].trim();
                } catch (NumberFormatException e) {
                    // Ignore le pourcentage malformé, on utilise 1
                    blockId = split.length > 1 ? split[1].trim() : part;
                }
            }

            // Ajoute le namespace minecraft: si absent
            if (!blockId.contains(":")) {
                blockId = "minecraft:" + blockId.toLowerCase(Locale.ROOT);
            } else {
                blockId = blockId.toLowerCase(Locale.ROOT);
            }

            BlockType blockType = BlockTypes.get(blockId);
            if (blockType == null) {
                Main.getLoggerUtil().warning("[WorldEditSetAction] Type de bloc inconnu ignoré: " + blockId);
                continue;
            }

            totalWeight += weight;
            list.add(new WeightedBlock(blockType.getDefaultState(), weight));
        }

        // Normaliser les poids
        if (totalWeight > 0) {
            for (WeightedBlock wb : list) {
                wb.normalizedWeight = wb.weight / totalWeight;
            }
        }
        return list;
    }

    private static BlockState pickBlock(List<WeightedBlock> blocks, Random random) {
        if (blocks.size() == 1) return blocks.get(0).blockState;
        double r = random.nextDouble();
        double cumul = 0;
        for (WeightedBlock wb : blocks) {
            cumul += wb.normalizedWeight;
            if (r <= cumul) return wb.blockState;
        }
        return blocks.get(blocks.size() - 1).blockState;
    }

    static class WeightedBlock {
        final BlockState blockState;
        final double weight;
        double normalizedWeight;

        WeightedBlock(BlockState blockState, double weight) {
            this.blockState = blockState;
            this.weight = weight;
            this.normalizedWeight = weight;
        }
    }
}

