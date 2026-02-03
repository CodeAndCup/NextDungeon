package fr.perrier.dungeons.spigot.workflow.action.impl;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.webserver.blockly.BlocklyAction;
import fr.perrier.dungeons.spigot.workflow.action.Action;
import fr.perrier.dungeons.spigot.webserver.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.spigot.webserver.blockly.annotations.BlocklyInfo;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;

/**
 * Action pour appliquer un effet de potion à un joueur
 */
@Setter
@Getter
@BlocklyInfo(
        name = "apply_potion_effect_action",
        color = "#9C27B0",
        displayText = "🧪 Appliquer effet de potion",
        tooltip = "Applique un effet de potion au joueur",
        category = "Actions"
)
public class ApplyPotionEffectAction extends Action implements BlocklyAction {
    private static final long serialVersionUID = 1L;

    @BlocklyField(type = BlocklyField.FieldType.DROPDOWN, label = "Effet:",
            options = "SPEED,SLOWNESS,HASTE,MINING_FATIGUE,STRENGTH,JUMP_BOOST,NAUSEA,REGENERATION,RESISTANCE,FIRE_RESISTANCE,WATER_BREATHING,INVISIBILITY,BLINDNESS,NIGHT_VISION,HUNGER,WEAKNESS,POISON,WITHER,HEALTH_BOOST,ABSORPTION,SATURATION,GLOWING,LEVITATION,LUCK,UNLUCK,SLOW_FALLING,CONDUIT_POWER,DOLPHINS_GRACE,BAD_OMEN,HERO_OF_THE_VILLAGE",
            defaultValue = "SPEED", order = 1)
    private String effectType;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Durée (secondes):",
            defaultValue = "10", order = 2)
    private int duration;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Niveau (1-255):",
            defaultValue = "1", order = 3)
    private int amplifier;

    @BlocklyField(type = BlocklyField.FieldType.CHECKBOX, label = "Effet ambiant:",
            defaultValue = "false", order = 4)
    private boolean ambient;

    @BlocklyField(type = BlocklyField.FieldType.CHECKBOX, label = "Afficher particules:",
            defaultValue = "true", order = 5)
    private boolean particles;

    @BlocklyField(type = BlocklyField.FieldType.DROPDOWN, label = "Appliquer à:",
            options = "player,@all", defaultValue = "player", order = 6)
    private String target;

    public ApplyPotionEffectAction() {
        super("Apply Potion Effect", "apply_potion_effect_action");
        this.effectType = "SPEED";
        this.duration = 10;
        this.amplifier = 1;
        this.ambient = false;
        this.particles = true;
        this.target = "player";
    }

    public ApplyPotionEffectAction(String effectType, int duration, int amplifier, boolean ambient, boolean particles, String target) {
        super("Apply Potion Effect", "apply_potion_effect_action");
        this.effectType = effectType;
        this.duration = duration;
        this.amplifier = amplifier;
        this.ambient = ambient;
        this.particles = particles;
        this.target = target;
    }

    @Override
    public boolean execute(Player triggerPlayer, Location location, Map<String, Object> data) {
        if (effectType == null || effectType.isEmpty()) {
            Main.getInstance().getLogger().warning("Effect type is empty in ApplyPotionEffectAction");
            return false;
        }

        try {
            PotionEffectType potionType = PotionEffectType.getByName(effectType.toUpperCase());
            if (potionType == null) {
                Main.getInstance().getLogger().warning("Invalid potion effect type: " + effectType);
                return false;
            }

            // Convertir la durée en ticks (20 ticks = 1 seconde)
            int durationTicks = duration * 20;

            // S'assurer que l'amplificateur est dans les limites (0-254, car on affiche 1-255)
            int amp = Math.max(0, Math.min(254, amplifier - 1));

            PotionEffect effect = new PotionEffect(potionType, durationTicks, amp, ambient, particles);

            if ("@all".equals(target)) {
                // Appliquer l'effet à tous les joueurs en ligne
                for (Player onlinePlayer : Main.getInstance().getServer().getOnlinePlayers()) {
                    onlinePlayer.addPotionEffect(effect);
                }
            } else {
                // Appliquer l'effet au joueur déclencheur
                if (triggerPlayer != null) {
                    triggerPlayer.addPotionEffect(effect);
                }
            }

            if (Main.isDebug()) {
                Main.getInstance().getLogger().info("Applied potion effect " + effectType + " (level " + amplifier + ", " + duration + "s) to " + target);
            }

            return true;

        } catch (Exception e) {
            Main.getInstance().getLogger().warning("Error applying potion effect: " + e.getMessage());
            return false;
        }
    }
}
