package fr.perrier.dungeons.module.cinematic.clock;

import java.time.Duration;
import java.util.function.IntConsumer;

/**
 * Horloge centralisée pour le système cinématique.
 * Basée sur le temps réel (Duration), jamais sur les ticks Bukkit.
 * Formula: frame = playTime.toMillis() / 50 → 20 fps constant (50ms/frame)
 */
public interface CinematicClock {

    /**
     * @return le frame courant basé sur le temps de lecture
     */
    int getCurrentFrame();

    /**
     * Avance l'horloge du deltaTime donné
     * @param deltaTime temps écoulé depuis le dernier tick
     */
    void tick(Duration deltaTime);

    /**
     * Met l'horloge en pause
     */
    void pause();

    /**
     * Reprend l'horloge après une pause
     */
    void resume();

    /**
     * Saute à un frame spécifique
     * @param frame le frame cible
     */
    void setFrame(int frame);

    /**
     * Ajoute un listener appelé à chaque changement de frame
     * @param listener le consumer recevant le numéro de frame
     */
    void addFrameChangeListener(IntConsumer listener);

    /**
     * Retire un listener de changement de frame
     * @param listener le consumer à retirer
     */
    void removeFrameChangeListener(IntConsumer listener);

    /**
     * @return true si l'horloge tourne (non pausée)
     */
    boolean isRunning();
}
