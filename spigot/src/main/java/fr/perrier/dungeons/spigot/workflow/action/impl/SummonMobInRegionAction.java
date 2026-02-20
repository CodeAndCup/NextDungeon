package fr.perrier.dungeons.spigot.workflow.action.impl;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.webeditor.blockly.BlocklyAction;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyInfo;
import fr.perrier.dungeons.spigot.workflow.action.Action;
import fr.perrier.dungeons.spigot.workflow.blocks.LocationBlock;
import io.lumine.mythic.bukkit.MythicBukkit;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Invoque un certain nombre de mobs dans une région (pos1→pos2) sur des emplacements sûrs.
 * Un emplacement sûr = 2 blocs d'air au-dessus, bloc solide en dessous, pas de lave/feu.
 */
@Setter
@Getter
@BlocklyInfo(
        name = "summon_mob_in_region_action",
        color = "#2196F3",
        displayText = "👾 Invoquer mobs dans une région",
        tooltip = "Invoque un nombre donné de mobs à des positions sûres aléatoires dans la région pos1→pos2.",
        category = "Actions"
)
public class SummonMobInRegionAction extends Action implements BlocklyAction {
    private static final long serialVersionUID = 1L;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Mob Type:",
            defaultValue = "ZOMBIE", order = 1)
    private String mobType;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Nombre de mobs:",
            defaultValue = "1", min = 1, max = 100, order = 2)
    private int amount;

    @BlocklyField(type = BlocklyField.FieldType.LOCATION_INPUT, label = "Position 1:", order = 3)
    private LocationBlock pos1;

    @BlocklyField(type = BlocklyField.FieldType.LOCATION_INPUT, label = "Position 2:", order = 4)
    private LocationBlock pos2;

    public SummonMobInRegionAction() {
        super("SummonMobInRegion", "summon_mob_in_region_action");
        this.mobType = "ZOMBIE";
        this.amount = 1;
        this.pos1 = new LocationBlock();
        this.pos2 = new LocationBlock();
    }

    public SummonMobInRegionAction(String mobType, int amount, LocationBlock pos1, LocationBlock pos2) {
        super("SummonMobInRegion", "summon_mob_in_region_action");
        this.mobType = mobType;
        this.amount = amount;
        this.pos1 = pos1;
        this.pos2 = pos2;
    }

    @Override
    public boolean execute(Player triggerPlayer, Location triggerLocation, Map<String, Object> data) {
        if (pos1 == null || pos2 == null) {
            Main.getLoggerUtil().warning("[SummonMobInRegionAction] pos1 ou pos2 est null.");
            return false;
        }

        World world = resolveWorld(triggerLocation);
        if (world == null) {
            Main.getLoggerUtil().warning("[SummonMobInRegionAction] Monde introuvable.");
            return false;
        }

        int minX = (int) Math.min(pos1.getX(), pos2.getX());
        int minY = (int) Math.min(pos1.getY(), pos2.getY());
        int minZ = (int) Math.min(pos1.getZ(), pos2.getZ());
        int maxX = (int) Math.max(pos1.getX(), pos2.getX());
        int maxY = (int) Math.max(pos1.getY(), pos2.getY());
        int maxZ = (int) Math.max(pos1.getZ(), pos2.getZ());

        // Collecte toutes les positions sûres dans la région
        List<Location> safeLocations = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Location candidate = new Location(world, x + 0.5, y, z + 0.5);
                    if (isSafe(candidate)) {
                        safeLocations.add(candidate);
                    }
                }
            }
        }

        if (safeLocations.isEmpty()) {
            Main.getLoggerUtil().warning("[SummonMobInRegionAction] Aucune position sûre trouvée dans la région.");
            return false;
        }

        // Détermine le type d'entité vanilla (peut être null si MythicMobs)
        EntityType entityType = null;
        try {
            entityType = EntityType.valueOf(mobType.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {}

        Random random = new Random();
        int spawned = 0;
        int attempts = 0;
        int maxAttempts = amount * 10; // Evite une boucle infinie si peu de positions

        while (spawned < amount && attempts < maxAttempts) {
            attempts++;
            Location spawnLoc = safeLocations.get(random.nextInt(safeLocations.size()));

            try {
                if (entityType == null) {
                    // MythicMobs
                    MythicBukkit.inst().getMobManager().spawnMob(mobType, spawnLoc);
                } else {
                    world.spawnEntity(spawnLoc, entityType);
                }
                spawned++;
            } catch (Exception e) {
                Main.getLoggerUtil().warning("[SummonMobInRegionAction] Erreur spawn: " + e.getMessage());
            }
        }

        if (Main.getLoggerUtil().isDebugEnabled()) {
            Main.getLoggerUtil().info("[SummonMobInRegionAction] " + spawned + "/" + amount + " mobs spawnés.");
        }

        return spawned > 0;
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
     * Vérifie si une location est sûre pour spawner un mob :
     * - Le bloc à Y est de l'air
     * - Le bloc à Y+1 est de l'air
     * - Le bloc à Y-1 est solide
     * - Pas de lave/feu à proximité
     */
    private boolean isSafe(Location loc) {
        if (loc.getWorld() == null) return false;

        Block feet   = loc.getBlock();
        Block head   = loc.clone().add(0, 1, 0).getBlock();
        Block ground = loc.clone().subtract(0, 1, 0).getBlock();

        if (!feet.getType().isAir())  return false;
        if (!head.getType().isAir())  return false;
        if (!ground.getType().isSolid()) return false;

        return !isDangerous(feet) && !isDangerous(head) && !isDangerous(ground);
    }

    private boolean isDangerous(Block block) {
        Material type = block.getType();
        return type == Material.LAVA
                || type == Material.FIRE
                || type == Material.MAGMA_BLOCK
                || type == Material.CACTUS
                || type == Material.SWEET_BERRY_BUSH
                || type.name().contains("LAVA")
                || type.name().contains("FIRE");
    }
}

