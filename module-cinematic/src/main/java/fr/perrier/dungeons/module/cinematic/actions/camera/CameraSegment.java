package fr.perrier.dungeons.module.cinematic.actions.camera;

import fr.perrier.dungeons.module.cinematic.model.CameraWaypoint;
import fr.perrier.dungeons.module.cinematic.segment.CinematicSegmentData;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Segment de caméra contenant les points du chemin à parcourir.
 */
@Getter
@Setter
public class CameraSegment extends CinematicSegmentData {

    private static final long serialVersionUID = 1L;

    private List<CameraWaypoint> pathPoints = new ArrayList<>();

    public CameraSegment() {
        super();
    }

    public CameraSegment(int startFrame, int endFrame, List<CameraWaypoint> pathPoints) {
        super(startFrame, endFrame);
        this.pathPoints = pathPoints;
    }
}
