package fr.perrier.dungeons.module.cinematic.segment;

import lombok.Setter;

import java.io.Serializable;

/**
 * Classe de base pour les données de segment cinématique.
 * Sérialisable via Gson pour la persistance.
 */
@Setter
public class CinematicSegmentData implements CinematicSegment, Serializable {

    private static final long serialVersionUID = 1L;

    private int startFrame = 0;
    private int endFrame = 100;

    public CinematicSegmentData() {
    }

    public CinematicSegmentData(int startFrame, int endFrame) {
        this.startFrame = startFrame;
        this.endFrame = endFrame;
    }

    @Override
    public int getStartFrame() {
        return startFrame;
    }

    @Override
    public int getEndFrame() {
        return endFrame;
    }
}
