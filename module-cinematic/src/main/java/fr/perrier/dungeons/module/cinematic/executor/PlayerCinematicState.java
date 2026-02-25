package fr.perrier.dungeons.module.cinematic.executor;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Captures and restores the complete player state before/after a cinematic.
 * <p>
 * Mirrors Typewriter's {@code PlayerState.kt} provider-based approach:
 * <ul>
 *   <li>{@code LOCATION} — player location</li>
 *   <li>{@code ALLOW_FLIGHT} — flight permission</li>
 *   <li>{@code FLYING} — active flight status</li>
 *   <li>{@code VISIBLE_PLAYERS} — players visible to this player</li>
 *   <li>{@code SHOWING_PLAYER} — players that can see this player</li>
 *   <li>{@code EffectStateProvider(INVISIBILITY)} — invisibility effect</li>
 *   <li>{@code VELOCITY} — player velocity (optional, for advancedCameraSettings.restoreVelocity)</li>
 * </ul>
 *
 * @see <a href="https://github.com/gabber235/Typewriter">Typewriter PlayerState.kt</a>
 */
@Getter
@Setter
public class PlayerCinematicState {

    // ref: Typewriter GenericPlayerStateProvider.LOCATION
    private Location location;
    // ref: Typewriter GenericPlayerStateProvider.ALLOW_FLIGHT
    private boolean allowFlight;
    // ref: Typewriter GenericPlayerStateProvider.FLYING
    private boolean flying;
    // ref: Typewriter GenericPlayerStateProvider.VISIBLE_PLAYERS
    private List<UUID> visiblePlayers;
    // ref: Typewriter GenericPlayerStateProvider.SHOWING_PLAYER
    private List<UUID> showingPlayers;
    // ref: Typewriter EffectStateProvider(INVISIBILITY)
    private PotionEffect invisibilityEffect;
    // ref: Typewriter GenericPlayerStateProvider.VELOCITY
    private Vector velocity;

    /**
     * Captures the current player state.
     * Matches Typewriter's {@code player.state(LOCATION, ALLOW_FLIGHT, FLYING, VISIBLE_PLAYERS, SHOWING_PLAYER, EffectStateProvider(INVISIBILITY), VELOCITY)}.
     *
     * @param player the player whose state to capture
     */
    public void captureState(Player player) {
        // LOCATION
        this.location = player.getLocation().clone();

        // ALLOW_FLIGHT
        this.allowFlight = player.getAllowFlight();

        // FLYING
        this.flying = player.isFlying();

        // VISIBLE_PLAYERS — players visible TO this player (ref: Typewriter VISIBLE_PLAYERS store)
        this.visiblePlayers = new ArrayList<>();
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.getUniqueId().equals(player.getUniqueId()) && player.canSee(other)) {
                visiblePlayers.add(other.getUniqueId());
            }
        }

        // SHOWING_PLAYER — players that can see this player (ref: Typewriter SHOWING_PLAYER store)
        this.showingPlayers = new ArrayList<>();
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.getUniqueId().equals(player.getUniqueId()) && other.canSee(player)) {
                showingPlayers.add(other.getUniqueId());
            }
        }

        // EffectStateProvider(INVISIBILITY) — current invisibility effect if any
        this.invisibilityEffect = player.getPotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY);

        // VELOCITY
        this.velocity = player.getVelocity().clone();
    }

    /**
     * Restores the saved state to the player.
     * Executed on the main Bukkit thread for thread safety.
     * <p>
     * Matches Typewriter's {@code player.restore(state)} which calls each provider's restore.
     *
     * @param player the player to restore
     */
    public void restoreState(Player player) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("NextDungeon");
        if (plugin == null) return;

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            try {
                // LOCATION (ref: Typewriter LOCATION restore → teleport)
                if (location != null) {
                    player.teleport(location);
                }

                // ALLOW_FLIGHT (ref: Typewriter ALLOW_FLIGHT restore)
                player.setAllowFlight(allowFlight);

                // FLYING (ref: Typewriter FLYING restore)
                player.setFlying(flying);

                // VISIBLE_PLAYERS (ref: Typewriter VISIBLE_PLAYERS restore — show only those that were visible)
                if (visiblePlayers != null) {
                    for (Player other : Bukkit.getOnlinePlayers()) {
                        if (!other.getUniqueId().equals(player.getUniqueId())
                                && visiblePlayers.contains(other.getUniqueId())) {
                            player.showPlayer(plugin, other);
                        }
                    }
                }

                // SHOWING_PLAYER (ref: Typewriter SHOWING_PLAYER restore — show player to those that could see them)
                if (showingPlayers != null) {
                    for (Player other : Bukkit.getOnlinePlayers()) {
                        if (!other.getUniqueId().equals(player.getUniqueId())
                                && showingPlayers.contains(other.getUniqueId())) {
                            other.showPlayer(plugin, player);
                        }
                    }
                }

                // EffectStateProvider(INVISIBILITY) (ref: Typewriter EffectStateProvider restore)
                player.removePotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY);
                if (invisibilityEffect != null) {
                    player.addPotionEffect(invisibilityEffect);
                }

                // VELOCITY (ref: Typewriter VELOCITY restore)
                if (velocity != null) {
                    player.setVelocity(velocity);
                }
            } catch (Exception e) {
                System.err.println("[Cinematic] Restore player state error: " + e.getMessage());
            }
        });
    }
}
