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

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Action WorldEdit //replace : remplace certains blocs par d'autres dans une région.
 * Supporte les patterns pondérés pour la destination (ex: "70%stone,30%gravel").
 */
@Setter
@Getter
@BlocklyInfo(
        name = "worldedit_replace_action",
        color = "#FF9800",
        displayText = "🔄 WE Replace Région (pos1/pos2)",
        tooltip = "Remplace les blocs d'un certain type (from) par un autre type ou pattern pondéré (to) dans la région pos1→pos2.",
        category = "Actions"
)
public class WorldEditReplaceAction extends Action implements BlocklyAction {
    private static final long serialVersionUID = 1L;

    @BlocklyField(type = BlocklyField.FieldType.LOCATION_INPUT, label = "Position 1:", defaultValue = "", order = 1)
    private LocationBlock pos1;

    @BlocklyField(type = BlocklyField.FieldType.LOCATION_INPUT, label = "Position 2:", defaultValue = "", order = 2)
    private LocationBlock pos2;

    /**
     * Bloc(s) source à remplacer, séparés par des virgules (sans pourcentage). Ex: "stone,cobblestone"
     */
    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Blocs à remplacer (ex: stone,cobblestone):", defaultValue = "stone", order = 3)
    private String fromPattern;

    /**
     * Bloc(s) destination avec support des pourcentages. Ex: "70%stone,30%gravel" ou simplement "gravel"
     */
    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Remplacer par (ex: 70%stone,30%gravel):", defaultValue = "gravel", order = 4)
    private String toPattern;

    public WorldEditReplaceAction() {
        super("WorldEdit Replace", "worldedit_replace_action");
        this.pos1 = new LocationBlock();
        this.pos2 = new LocationBlock();
        this.fromPattern = "stone";
        this.toPattern = "gravel";
    }

    public WorldEditReplaceAction(LocationBlock pos1, LocationBlock pos2, String fromPattern, String toPattern) {
        super("WorldEdit Replace", "worldedit_replace_action");
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.fromPattern = fromPattern;
        this.toPattern = toPattern;
    }

    @Override
    public boolean execute(Player triggerPlayer, Location triggerLocation, Map<String, Object> data) {
        if (pos1 == null || pos2 == null || fromPattern == null || fromPattern.isBlank()
                || toPattern == null || toPattern.isBlank()) {
            Main.getLoggerUtil().warning("[WorldEditReplaceAction] paramètres manquants.");
            return false;
        }

        World bukkitWorld = resolveWorld(triggerLocation);
        if (bukkitWorld == null) {
            Main.getLoggerUtil().warning("[WorldEditReplaceAction] Monde introuvable.");
            return false;
        }

        // Résoudre les blocs source (sans pourcentage)
        List<BlockType> fromTypes = parseFromPattern(fromPattern);
        if (fromTypes.isEmpty()) {
            Main.getLoggerUtil().warning("[WorldEditReplaceAction] Aucun bloc source valide: " + fromPattern);
            return false;
        }

        // Résoudre les blocs destination (avec pourcentages optionnels)
        List<WorldEditSetAction.WeightedBlock> toBlocks = WorldEditSetAction.parsePattern(toPattern);
        if (toBlocks.isEmpty()) {
            Main.getLoggerUtil().warning("[WorldEditReplaceAction] Pattern destination invalide: " + toPattern);
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

        Random random = new Random();

        try (EditSession editSession = com.sk89q.worldedit.WorldEdit.getInstance().newEditSession(weWorld)) {
            for (BlockVector3 pos : region) {
                BlockState current = editSession.getBlock(pos);
                if (fromTypes.contains(current.getBlockType())) {
                    BlockState replacement = pickBlock(toBlocks, random);
                    if (replacement != null) {
                        editSession.setBlock(pos, replacement);
                    }
                }
            }
        } catch (Exception e) {
            Main.getLoggerUtil().severe("[WorldEditReplaceAction] Erreur: " + e.getMessage());
            e.printStackTrace(System.err);
            return false;
        }
        return true;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private World resolveWorld(Location triggerLocation) {
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
     * Parse la liste des blocs source (simples, sans pourcentage). Ex: "stone,cobblestone"
     */
    private List<BlockType> parseFromPattern(String pattern) {
        List<BlockType> types = new java.util.ArrayList<>();
        for (String part : pattern.split(",")) {
            String blockId = part.trim();
            if (!blockId.contains(":")) {
                blockId = "minecraft:" + blockId.toLowerCase(Locale.ROOT);
            } else {
                blockId = blockId.toLowerCase(Locale.ROOT);
            }
            BlockType bt = BlockTypes.get(blockId);
            if (bt == null) {
                Main.getLoggerUtil().warning("[WorldEditReplaceAction] Bloc source inconnu ignoré: " + blockId);
            } else {
                types.add(bt);
            }
        }
        return types;
    }

    private static BlockState pickBlock(List<WorldEditSetAction.WeightedBlock> blocks, Random random) {
        if (blocks.size() == 1) return blocks.get(0).blockState;
        double r = random.nextDouble();
        double cumul = 0;
        for (WorldEditSetAction.WeightedBlock wb : blocks) {
            cumul += wb.normalizedWeight;
            if (r <= cumul) return wb.blockState;
        }
        return blocks.get(blocks.size() - 1).blockState;
    }
}

