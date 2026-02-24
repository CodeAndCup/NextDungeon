package fr.perrier.dungeons.module.cinematic.action;

import fr.perrier.dungeons.module.cinematic.clock.CinematicClock;
import fr.perrier.dungeons.module.cinematic.segment.CinematicSegment;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

/**
 * Classe helper abstraite pour les actions cinématiques avec gestion automatique
 * des segments (startSegment → tickSegment → stopSegment).
 * <p>
 * Inspiré du pattern SimpleCinematicAction de Typewriter.
 *
 * @param <S> le type de segment géré par cette action
 */
public abstract class SimpleCinematicAction<S extends CinematicSegment> implements CinematicAction {

    private final transient Set<S> activeSegments = new HashSet<>();

    @Override
    public void onCinematicSetup(Player player, CinematicClock clock) throws Exception {
        // Override si besoin d'initialisation globale
    }

    @Override
    public void onCinematicTick(Player player, int frame) throws Exception {
        for (S segment : getCinematicSegments()) {
            boolean wasActive = activeSegments.contains(segment);
            boolean isActive = segment.isActiveAt(frame);

            if (!wasActive && isActive) {
                // Segment vient de démarrer
                activeSegments.add(segment);
                onSegmentStart(player, segment);
            }

            if (isActive) {
                // Segment actif → tick
                onSegmentTick(player, segment, frame);
            }

            if (wasActive && !isActive) {
                // Segment vient de se terminer
                activeSegments.remove(segment);
                onSegmentStop(player, segment);
            }
        }
    }

    @Override
    public void onCinematicStop(Player player) throws Exception {
        // Arrêter tous les segments encore actifs
        for (S segment : activeSegments) {
            onSegmentStop(player, segment);
        }
        activeSegments.clear();
    }

    @Override
    public boolean canCinematicFinish(int frame) {
        for (CinematicSegment segment : getCinematicSegments()) {
            if (frame < segment.getEndFrame()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Appelé quand un segment démarre (frame == startFrame)
     */
    protected void onSegmentStart(Player player, S segment) throws Exception {
        // Override optionnel
    }

    /**
     * Appelé à chaque frame pendant que le segment est actif
     */
    protected void onSegmentTick(Player player, S segment, int frame) throws Exception {
        // Override optionnel
    }

    /**
     * Appelé quand un segment se termine (frame &gt; endFrame)
     */
    protected void onSegmentStop(Player player, S segment) throws Exception {
        // Override optionnel
    }

    @Override
    public abstract java.util.List<S> getCinematicSegments();
}
