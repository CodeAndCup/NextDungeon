package fr.perrier.dungeons.module.cinematic.executor;

import fr.perrier.dungeons.module.cinematic.action.CinematicAction;
import fr.perrier.dungeons.module.cinematic.clock.CinematicClock;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Exécuteur orchestrant une cinématique: coordonne l'horloge,
 * les actions et l'état du joueur.
 * <p>
 * Les actions sont tickées en parallèle via CompletableFuture
 * pour supporter 100+ actions sans ralentissement.
 */
public class CinematicExecutor {

    private final List<CinematicAction> actions;
    private final Player player;
    private final CinematicClock clock;
    private PlayerCinematicState stateSnapshot;
    private Consumer<Integer> frameListener;
    private boolean isRunning = false;

    public CinematicExecutor(List<CinematicAction> actions, Player player, CinematicClock clock) {
        this.actions = actions;
        this.player = player;
        this.clock = clock;
    }

    /**
     * Démarre la cinématique: capture l'état joueur, setup les actions,
     * et s'abonne à l'horloge.
     */
    public void start() {
        if (isRunning) return;
        isRunning = true;

        // Capturer état joueur
        stateSnapshot = new PlayerCinematicState();
        stateSnapshot.captureState(player);

        // TIER 1A: Rendre joueur invisible durant cinématique (ref: Typewriter CameraCinematicEntry.kt:215)
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.INVISIBILITY,
                Integer.MAX_VALUE,
                0,
                false,  // pas de particules ambiantes
                false   // pas de particules du tout
        ));

        // TIER 1A: Cacher joueurs mutuellement (ref: Typewriter CameraCinematicEntry.kt:216-218)
        Plugin plugin = Bukkit.getPluginManager().getPlugin("NextDungeon");
        if (plugin != null) {
            for (Player other : Bukkit.getOnlinePlayers()) {
                if (!other.getUniqueId().equals(player.getUniqueId())) {
                    other.hidePlayer(plugin, player);
                    player.hidePlayer(plugin, other);
                }
            }
        }

        // Setup toutes les actions
        for (CinematicAction action : actions) {
            try {
                action.onCinematicSetup(player, clock);
            } catch (Exception e) {
                System.err.println("[Cinematic] Setup action error: " + e.getMessage());
            }
        }

        // Listener à chaque frame
        frameListener = this::tickFrame;
        clock.addFrameChangeListener(frameListener);
    }

    /**
     * Tick toutes les actions en parallèle pour le frame donné
     */
    private void tickFrame(int frame) {
        if (!isRunning) return;

        try {
            // Parallèle: toutes les actions
            List<CompletableFuture<Void>> futures = actions.stream()
                    .map(action -> CompletableFuture.runAsync(() -> {
                        try {
                            action.onCinematicTick(player, frame);
                        } catch (Exception e) {
                            System.err.println("[Cinematic] Tick action error: " + e.getMessage());
                        }
                    }))
                    .collect(Collectors.toList());

            // Attendre toutes les actions
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            System.err.println("[Cinematic] Tick error: " + e.getMessage());
        }

        // Vérifier fin
        if (canFinish(frame)) {
            stop();
        }
    }

    /**
     * Arrête la cinématique: nettoie les actions et restaure l'état joueur
     */
    public void stop() {
        if (!isRunning) return;
        isRunning = false;

        // Retirer le listener de l'horloge
        if (frameListener != null) {
            clock.removeFrameChangeListener(frameListener);
        }

        try {
            // Arrêter toutes les actions
            for (CinematicAction action : actions) {
                try {
                    action.onCinematicStop(player);
                } catch (Exception e) {
                    System.err.println("[Cinematic] Stop action error: " + e.getMessage());
                }
            }

            // TIER 1A: Retirer invisibilité (ref: Typewriter CameraCinematicEntry.kt teardown)
            player.removePotionEffect(PotionEffectType.INVISIBILITY);

            // TIER 1A: Remontrer joueurs mutuellement
            Plugin plugin = Bukkit.getPluginManager().getPlugin("NextDungeon");
            if (plugin != null) {
                for (Player other : Bukkit.getOnlinePlayers()) {
                    if (!other.getUniqueId().equals(player.getUniqueId())) {
                        other.showPlayer(plugin, player);
                        player.showPlayer(plugin, other);
                    }
                }
            }

            // Restaurer état joueur (inclut velocity, visibilité fine-grained)
            if (stateSnapshot != null) {
                stateSnapshot.restoreState(player);
            }
        } catch (Exception e) {
            System.err.println("[Cinematic] Stop error: " + e.getMessage());
        }
    }

    /**
     * Vérifie si toutes les actions ont terminé
     */
    private boolean canFinish(int frame) {
        for (CinematicAction action : actions) {
            if (!action.canCinematicFinish(frame)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return true si la cinématique est en cours d'exécution
     */
    public boolean isRunning() {
        return isRunning;
    }
}
