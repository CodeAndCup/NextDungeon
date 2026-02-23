package fr.perrier.dungeons.module.cinematic.action;

import fr.perrier.dungeons.module.cinematic.clock.CinematicClock;
import fr.perrier.dungeons.module.cinematic.segment.CinematicSegment;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Interface pour une action cinématique pouvant être exécutée
 * dans le système de cinématiques.
 * <p>
 * Cycle de vie: onCinematicSetup → onCinematicTick (chaque frame) → onCinematicStop
 */
public interface CinematicAction {

    /**
     * Appelé une fois au démarrage de la cinématique
     * @param player le joueur concerné
     * @param clock l'horloge cinématique
     */
    void onCinematicSetup(Player player, CinematicClock clock) throws Exception;

    /**
     * Appelé à chaque frame de la cinématique
     * @param player le joueur concerné
     * @param frame le numéro de frame courant
     */
    void onCinematicTick(Player player, int frame) throws Exception;

    /**
     * Appelé à l'arrêt de la cinématique pour le nettoyage
     * @param player le joueur concerné
     */
    void onCinematicStop(Player player) throws Exception;

    /**
     * Vérifie si cette action a terminé son exécution au frame donné
     * @param frame le frame courant
     * @return true si l'action peut se terminer
     */
    boolean canCinematicFinish(int frame);

    /**
     * @return la liste des segments de cette action
     */
    List<? extends CinematicSegment> getCinematicSegments();
}
