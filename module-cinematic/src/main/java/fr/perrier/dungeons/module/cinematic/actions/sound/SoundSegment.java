package fr.perrier.dungeons.module.cinematic.actions.sound;

import fr.perrier.dungeons.module.cinematic.segment.CinematicSegmentData;
import lombok.Getter;
import lombok.Setter;

/**
 * Segment de son avec type, volume et pitch.
 */
@Getter
@Setter
public class SoundSegment extends CinematicSegmentData {

    private static final long serialVersionUID = 1L;

    private String soundType = "ENTITY_GENERIC_EXPLODE";
    private float volume = 1.0f;
    private float pitch = 1.0f;

    public SoundSegment() {
        super();
    }

    public SoundSegment(int startFrame, int endFrame, String soundType, float volume, float pitch) {
        super(startFrame, endFrame);
        this.soundType = soundType;
        this.volume = volume;
        this.pitch = pitch;
    }
}
