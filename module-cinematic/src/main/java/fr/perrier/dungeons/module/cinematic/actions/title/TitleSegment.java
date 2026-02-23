package fr.perrier.dungeons.module.cinematic.actions.title;

import fr.perrier.dungeons.module.cinematic.segment.CinematicSegmentData;
import lombok.Getter;
import lombok.Setter;

/**
 * Segment de titre avec texte, sous-titre et durées de fade.
 */
@Getter
@Setter
public class TitleSegment extends CinematicSegmentData {

    private static final long serialVersionUID = 1L;

    private String title = "";
    private String subtitle = "";
    private int fadeIn = 10;
    private int fadeOut = 10;

    public TitleSegment() {
        super();
    }

    public TitleSegment(int startFrame, int endFrame, String title, String subtitle, int fadeIn, int fadeOut) {
        super(startFrame, endFrame);
        this.title = title;
        this.subtitle = subtitle;
        this.fadeIn = fadeIn;
        this.fadeOut = fadeOut;
    }
}
