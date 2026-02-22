package fr.perrier.dungeons.module.cinematic.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Root data model for a cinematic sequence, stored as JSON (payload_json) in the database.
 *
 * <p>Example SQL schema:
 * <pre>
 * CREATE TABLE cinematics (
 *     id UUID PRIMARY KEY,
 *     name VARCHAR(64) NOT NULL,
 *     creator VARCHAR(32),
 *     payload_json JSONB NOT NULL,
 *     created_at TIMESTAMP,
 *     updated_at TIMESTAMP
 * );
 * </pre>
 */
@Getter
@Setter
public class CinematicData implements Serializable {

    /** Unique cinematic identifier */
    private UUID id;

    /** Human-readable name */
    private String name;

    /** Creator username */
    private String creator;

    /** Total duration in ticks (20 ticks = 1 second) */
    private int durationTicks;

    /** Whether the player's camera is locked during the cinematic */
    private boolean lockCamera = true;

    /** Whether to hide the HUD during playback */
    private boolean hideHud = true;

    /** Camera timeline: ordered list of camera waypoints */
    private List<CameraWaypoint> cameraWaypoints = new ArrayList<>();

    /** NPC actors participating in the cinematic */
    private List<NpcActor> npcActors = new ArrayList<>();

    /** Timed events that fire during the timeline */
    private List<TimelineEvent> events = new ArrayList<>();

    public CinematicData() {
        this.id = UUID.randomUUID();
    }

    public CinematicData(String name, String creator) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.creator = creator;
    }
}
