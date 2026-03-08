package fr.perrier.dungeons.spigot.workflow.blocks;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.Serializable;

/**
 * Représente un bloc de location réutilisable dans Blockly
 * Peut contenir différents niveaux d'information (coordonnées, monde, rotation)
 */
@Getter
@Setter
public class LocationBlock implements Serializable {
    private static final long serialVersionUID = 1L;

    private double x;
    private double y;
    private double z;
    private String worldName;
    private float yaw;
    private float pitch;
    private boolean hasWorld;
    private boolean hasRotation;

    // Transient cached world reference to avoid repeated Bukkit.getWorld() lookups
    private transient World cachedWorld;

    public LocationBlock() {
        this.x = 0;
        this.y = 64;
        this.z = 0;
        this.worldName = "world";
        this.yaw = 0;
        this.pitch = 0;
        this.hasWorld = false;
        this.hasRotation = false;
    }

    public LocationBlock(double x, double y, double z) {
        this();
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public LocationBlock(double x, double y, double z, String worldName) {
        this(x, y, z);
        this.worldName = worldName;
        this.hasWorld = true;
    }

    public LocationBlock(double x, double y, double z, String worldName, float yaw, float pitch) {
        this(x, y, z, worldName);
        this.yaw = yaw;
        this.pitch = pitch;
        this.hasRotation = true;
    }

    /**
     * Invalidates the cached world reference when worldName changes.
     */
    public void setWorldName(String worldName) {
        this.worldName = worldName;
        this.cachedWorld = null;
    }

    /**
     * Convertit ce LocationBlock en Location Bukkit
     * @param defaultWorld Le monde par défaut si aucun monde n'est spécifié
     * @return La Location Bukkit correspondante
     */
    public Location toLocation(World defaultWorld) {
        World world;
        if (hasWorld && worldName != null && !worldName.isEmpty()) {
            if (cachedWorld == null || !cachedWorld.getName().equals(worldName)) {
                cachedWorld = Bukkit.getWorld(worldName);
            }
            world = cachedWorld;
        } else {
            world = defaultWorld;
        }

        if (world == null) {
            world = Bukkit.getWorlds().get(0); // Fallback au premier monde
        }

        return new Location(world, x, y, z, yaw, pitch);
    }

    /**
     * Convertit ce LocationBlock en Location Bukkit avec un monde par défaut "world"
     * @return La Location Bukkit correspondante
     */
    public Location toLocation() {
        return toLocation(Bukkit.getWorld("world"));
    }

    /**
     * Crée un LocationBlock depuis une Location Bukkit
     * @param location La location source
     * @param includeWorld Si le monde doit être inclus
     * @param includeRotation Si la rotation doit être incluse
     * @return Le LocationBlock créé
     */
    public static LocationBlock fromLocation(Location location, boolean includeWorld, boolean includeRotation) {
        LocationBlock block = new LocationBlock();
        block.setX(location.getX());
        block.setY(location.getY());
        block.setZ(location.getZ());

        if (includeWorld && location.getWorld() != null) {
            block.setWorldName(location.getWorld().getName());
            block.setHasWorld(true);
        }

        if (includeRotation) {
            block.setYaw(location.getYaw());
            block.setPitch(location.getPitch());
            block.setHasRotation(true);
        }

        return block;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Location{x=").append(x)
          .append(", y=").append(y)
          .append(", z=").append(z);

        if (hasWorld) {
            sb.append(", world=").append(worldName);
        }

        if (hasRotation) {
            sb.append(", yaw=").append(yaw)
              .append(", pitch=").append(pitch);
        }

        sb.append("}");
        return sb.toString();
    }
}
