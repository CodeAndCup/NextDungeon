package fr.perrier.dungeons.module.cinematic.segment;

/**
 * Interface définissant un segment temporel dans une cinématique.
 * Un segment a un frame de début et un frame de fin.
 */
public interface CinematicSegment {

    /**
     * @return le frame de début du segment
     */
    int getStartFrame();

    /**
     * @return le frame de fin du segment
     */
    int getEndFrame();

    /**
     * Vérifie si un frame donné est dans la plage de ce segment
     * @param frame le frame à vérifier
     * @return true si startFrame &lt;= frame &lt;= endFrame
     */
    default boolean isActiveAt(int frame) {
        return frame >= getStartFrame() && frame <= getEndFrame();
    }

    /**
     * Calcule le pourcentage de progression dans ce segment à un frame donné
     * @param frame le frame courant
     * @return valeur entre 0.0 et 1.0
     */
    default double getPercentageAt(int frame) {
        int duration = getEndFrame() - getStartFrame();
        if (duration <= 0) return 1.0;
        return Math.max(0.0, Math.min(1.0,
                (double) (frame - getStartFrame()) / duration));
    }
}
