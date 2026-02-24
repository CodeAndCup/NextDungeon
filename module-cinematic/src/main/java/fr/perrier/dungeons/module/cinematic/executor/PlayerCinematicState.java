package fr.perrier.dungeons.module.cinematic.executor;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Capture et restaure l'état complet d'un joueur avant/après une cinématique.
 * Permet de revenir à l'état exact du joueur une fois la cinématique terminée.
 *
 * @see <a href="https://github.com/gabber235/Typewriter">Typewriter PlayerState.kt</a>
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
    private Vector velocity;
    private List<UUID> visiblePlayers;
    private List<UUID> showingPlayers;

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

        // Velocity (ref: Typewriter PlayerState.kt VELOCITY)
        this.velocity = player.getVelocity().clone();

        // Visible players - joueurs que ce joueur peut voir
        this.visiblePlayers = new ArrayList<>();
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.getUniqueId().equals(player.getUniqueId()) && player.canSee(other)) {
                visiblePlayers.add(other.getUniqueId());
            }
        }

        // Showing players - joueurs qui peuvent voir ce joueur
        this.showingPlayers = new ArrayList<>();
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.getUniqueId().equals(player.getUniqueId()) && other.canSee(player)) {
                showingPlayers.add(other.getUniqueId());
            }
        }
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

                // Restaurer velocity (ref: Typewriter PlayerState.kt)
                if (velocity != null) {
                    player.setVelocity(velocity);
                }

                // Restaurer visibilité des joueurs (ref: Typewriter VISIBLE_PLAYERS)
                if (visiblePlayers != null) {
                    for (Player other : Bukkit.getOnlinePlayers()) {
                        if (!other.getUniqueId().equals(player.getUniqueId())) {
                            if (visiblePlayers.contains(other.getUniqueId())) {
                                player.showPlayer(plugin, other);
                            } else {
                                player.hidePlayer(plugin, other);
                            }
                        }
                    }
                }

                // Restaurer qui peut voir ce joueur (ref: Typewriter SHOWING_PLAYER)
                if (showingPlayers != null) {
                    for (Player other : Bukkit.getOnlinePlayers()) {
                        if (!other.getUniqueId().equals(player.getUniqueId())) {
                            if (showingPlayers.contains(other.getUniqueId())) {
                                other.showPlayer(plugin, player);
                            } else {
                                other.hidePlayer(plugin, player);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[Cinematic] Restore player state error: " + e.getMessage());
            }
        });
    }
}
