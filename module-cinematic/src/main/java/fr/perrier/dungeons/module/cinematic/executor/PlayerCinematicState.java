package fr.perrier.dungeons.module.cinematic.executor;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Capture et restaure l'état complet d'un joueur avant/après une cinématique.
 * Permet de revenir à l'état exact du joueur une fois la cinématique terminée.
 */
@Getter
@Setter
public class PlayerCinematicState {

    private GameMode gameMode;
    private Collection<PotionEffect> potionEffects;
    private Location location;
    private boolean allowFlight;
    private boolean flying;
    private double health;
    private int foodLevel;
    private float exhaustion;

    /**
     * Capture l'état courant du joueur
     * @param player le joueur dont l'état doit être sauvegardé
     */
    public void captureState(Player player) {
        this.gameMode = player.getGameMode();
        this.potionEffects = new ArrayList<>(player.getActivePotionEffects());
        this.location = player.getLocation().clone();
        this.allowFlight = player.getAllowFlight();
        this.flying = player.isFlying();
        this.health = player.getHealth();
        this.foodLevel = player.getFoodLevel();
        this.exhaustion = player.getExhaustion();
    }

    /**
     * Restaure l'état sauvegardé sur le joueur.
     * Exécuté sur le thread principal Bukkit pour la sécurité thread.
     * @param player le joueur dont l'état doit être restauré
     */
    public void restoreState(Player player) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("NextDungeon");
        if (plugin == null) return;

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            try {
                player.setGameMode(gameMode);

                // Enlever tous les effets actifs
                player.getActivePotionEffects().forEach(effect ->
                        player.removePotionEffect(effect.getType()));

                // Restaurer anciens effets
                potionEffects.forEach(player::addPotionEffect);

                player.teleport(location);
                player.setAllowFlight(allowFlight);
                player.setFlying(flying);
                player.setHealth(health);
                player.setFoodLevel(foodLevel);
                player.setExhaustion(exhaustion);
            } catch (Exception e) {
                System.err.println("[Cinematic] Restore player state error: " + e.getMessage());
            }
        });
    }
}
