package fr.perrier.dungeons.module.labyrinth.ui;

import fr.perrier.dungeons.common.model.labyrinth.RewardIcon;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

/**
 * Floating item hologram displayed at a given location at proposal time.
 *
 * <p>Hades-style UX (CDC §1.2 / §1.6) — the player sees the reward bias of
 * the next room before choosing the door.</p>
 *
 * <p>This class is a pure renderer : it spawns at the exact {@link Location}
 * it is handed. Computing that location (door anchor + offset, or the door's
 * custom {@code iconAnchor}) is the {@code DoorController}'s job.</p>
 */
public class DoorIconHologram {

    private final ItemDisplay entity;

    private DoorIconHologram(ItemDisplay entity) {
        this.entity = entity;
    }

    /**
     * Spawn a hologram at {@code at} for the given icon. Returns
     * {@code null} when the icon is {@link RewardIcon#NONE} — no visual
     * is shown for neutral rooms.
     */
    public static DoorIconHologram spawn(Location at, RewardIcon icon) {
        if (icon == null || icon == RewardIcon.NONE) return null;
        Material material = materialFor(icon);
        if (material == null) return null;
        return spawnWithMaterial(at, material);
    }

    /**
     * Spawn the boss-door hologram — always a wither skeleton skull,
     * independent of the {@link RewardIcon} carried by the choice.
     * Used by the single-door boss layout (CDC §1.7).
     */
    public static DoorIconHologram spawnBoss(Location at) {
        return spawnWithMaterial(at, Material.WITHER_SKELETON_SKULL);
    }

    private static DoorIconHologram spawnWithMaterial(Location location, Material material) {
        if (location == null || location.getWorld() == null) return null;
        Location at = location.clone();
        ItemDisplay display = at.getWorld().spawn(at, ItemDisplay.class, d -> {
            d.setItemStack(new ItemStack(material));
            d.setBillboard(Display.Billboard.CENTER);
            // Slight upscale + idle bob via transformation matrix.
            d.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    new org.joml.Quaternionf(),
                    new Vector3f(0.6f, 0.6f, 0.6f),
                    new org.joml.Quaternionf()
            ));
            d.setPersistent(false);
            d.setInvulnerable(true);
        });
        return new DoorIconHologram(display);
    }

    public void despawn() {
        if (entity != null && !entity.isDead()) entity.remove();
    }

    private static Material materialFor(RewardIcon icon) {
        return switch (icon) {
            case GOLD -> Material.GOLD_INGOT;
            case BLESSING -> Material.AMETHYST_SHARD;
            case NONE -> null;
        };
    }
}
