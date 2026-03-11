package fr.perrier.dungeons.module.cinematic.actions.message;

import fr.perrier.dungeons.module.cinematic.segment.CinematicSegmentData;
import lombok.Getter;
import lombok.Setter;

/**
 * Segment de message avec texte et type d'affichage (CHAT ou ACTION_BAR).
 */
@Getter
@Setter
public class MessageSegment extends CinematicSegmentData {

    private static final long serialVersionUID = 1L;

    private String message = "";
    private String displayType = "CHAT";

    public MessageSegment() {
        super();
    }

    public MessageSegment(int startFrame, int endFrame, String message, String displayType) {
        super(startFrame, endFrame);
        this.message = message;
        this.displayType = displayType;
    }
}
