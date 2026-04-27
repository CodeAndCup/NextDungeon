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
 * Floating item hologram displayed above a door anchor at proposal time.
 *
 * <p>Hades-style UX (CDC §1.2 / §1.6) — the player sees the reward bias of
 * the next room before choosing the door.</p>
 */
public class DoorIconHologram {

    /** Vertical offset (in blocks) above the door anchor. */
    public static final double Y_OFFSET = 2.0;

    private final ItemDisplay entity;

    private DoorIconHologram(ItemDisplay entity) {
        this.entity = entity;
    }

    /**
     * Spawn a hologram at {@code anchor} for the given icon. Returns
     * {@code null} when the icon is {@link RewardIcon#NONE} — no visual
     * is shown for neutral rooms.
     */
    public static DoorIconHologram spawn(Location anchor, RewardIcon icon) {
        if (icon == null || icon == RewardIcon.NONE) return null;
        if (anchor == null || anchor.getWorld() == null) return null;
        Material material = materialFor(icon);
        if (material == null) return null;

        Location at = anchor.clone().add(0.5, Y_OFFSET, 0.5);
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
