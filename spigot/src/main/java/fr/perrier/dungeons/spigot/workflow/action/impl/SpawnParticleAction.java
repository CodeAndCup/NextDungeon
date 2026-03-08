package fr.perrier.dungeons.spigot.workflow.action.impl;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.webeditor.blockly.BlocklyAction;
import fr.perrier.dungeons.spigot.workflow.action.Action;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyInfo;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Action pour faire apparaître des particules
 */
@Setter
@Getter
@BlocklyInfo(
        name = "spawn_particle_action",
        color = "#673AB7",
        displayText = "✨ Faire apparaître des particules",
        tooltip = "Fait apparaître des particules à une position",
        category = "Actions"
)
public class SpawnParticleAction extends Action implements BlocklyAction {
    private static final long serialVersionUID = 1L;

    @BlocklyField(type = BlocklyField.FieldType.DROPDOWN, label = "Particule:",
            options = "FLAME,HEART,VILLAGER_HAPPY,EXPLOSION_NORMAL,ENCHANTMENT_TABLE,SPELL_WITCH,DRAGON_BREATH,END_ROD,FIREWORKS_SPARK,CLOUD,REDSTONE,SNOWBALL,WATER_DROP,LAVA,SMOKE_NORMAL,PORTAL,NOTE,CRIT,CRIT_MAGIC,SPELL_MOB,TOTEM",
            defaultValue = "FLAME", order = 1)
    private String particleType;

    // Transient cached enum to avoid Particle.valueOf() parse on every execute
    private transient Particle cachedParticleEnum;

    public void setParticleType(String particleType) {
        this.particleType = particleType;
        this.cachedParticleEnum = null;
    }

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Quantité:",
            defaultValue = "10", order = 2)
    private int count;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Offset X:",
            defaultValue = "0.5", order = 3)
    private double offsetX;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Offset Y:",
            defaultValue = "0.5", order = 4)
    private double offsetY;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Offset Z:",
            defaultValue = "0.5", order = 5)
    private double offsetZ;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Vitesse:",
            defaultValue = "0.1", order = 6)
    private double speed;

    @BlocklyField(type = BlocklyField.FieldType.DROPDOWN, label = "Position:",
            options = "player,trigger_location", defaultValue = "player", order = 7)
    private String locationSource;

    public SpawnParticleAction() {
        super("Spawn Particle", "spawn_particle_action");
        this.particleType = "FLAME";
        this.count = 10;
        this.offsetX = 0.5;
        this.offsetY = 0.5;
        this.offsetZ = 0.5;
        this.speed = 0.1;
        this.locationSource = "player";
    }

    public SpawnParticleAction(String particleType, int count, double offsetX, double offsetY, double offsetZ, double speed, String locationSource) {
        super("Spawn Particle", "spawn_particle_action");
        this.particleType = particleType;
        this.count = count;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.speed = speed;
        this.locationSource = locationSource;
    }

    @Override
    public boolean execute(Player triggerPlayer, Location location, Map<String, Object> data) {
        if (particleType == null || particleType.isEmpty()) {
            Main.getLoggerUtil().warning("Particle type is empty in SpawnParticleAction");
            return false;
        }

        try {
            if (cachedParticleEnum == null) {
                cachedParticleEnum = Particle.valueOf(particleType.toUpperCase());
            }
            Particle particle = cachedParticleEnum;

            // Déterminer la position
            Location spawnLocation;
            if ("trigger_location".equals(locationSource) && location != null) {
                spawnLocation = location;
            } else if (triggerPlayer != null) {
                spawnLocation = triggerPlayer.getLocation().add(0, 1, 0); // Légèrement au-dessus du joueur
            } else {
                Main.getLoggerUtil().warning("No valid location for particle spawn");
                return false;
            }

            // Faire apparaître les particules
            spawnLocation.getWorld().spawnParticle(
                particle,
                spawnLocation,
                count,
                offsetX,
                offsetY,
                offsetZ,
                speed
            );

            if (Main.getLoggerUtil().isDebugEnabled()) {
                Main.getLoggerUtil().info("Spawned " + count + "x " + particleType + " particles at " + spawnLocation);
            }

            return true;

        } catch (IllegalArgumentException e) {
            Main.getLoggerUtil().warning("Invalid particle type: " + particleType);
            return false;
        } catch (Exception e) {
            Main.getLoggerUtil().warning("Error spawning particles: " + e.getMessage());
            return false;
        }
    }
}
