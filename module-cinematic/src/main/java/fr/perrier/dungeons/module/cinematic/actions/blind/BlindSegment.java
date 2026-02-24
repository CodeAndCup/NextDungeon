package fr.perrier.dungeons.module.cinematic.actions.blind;

import fr.perrier.dungeons.module.cinematic.segment.CinematicSegmentData;
import lombok.Getter;
import lombok.Setter;

/**
 * Segment d'aveuglement (écran noir) pendant la cinématique.
 */
@Getter
@Setter
public class BlindSegment extends CinematicSegmentData {

    private static final long serialVersionUID = 1L;

    public BlindSegment() {
        super();
    }

    public BlindSegment(int startFrame, int endFrame) {
        super(startFrame, endFrame);
    }
}
