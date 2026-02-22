package fr.perrier.dungeons.module.cinematic.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * A timed event that fires at a specific tick during cinematic playback.
 * Events can run commands, spawn particles, play sounds, display text, etc.
 */
@Getter
@Setter
public class TimelineEvent implements Serializable {

    /** Tick offset from cinematic start */
    private int tick;

    /** Event type: COMMAND, TITLE, SOUND, PARTICLE, SPAWN_NPC, REMOVE_NPC */
    private String type;

    /** Event-specific parameters */
    private Map<String, String> parameters = new HashMap<>();

    public TimelineEvent() {}

    public TimelineEvent(int tick, String type) {
        this.tick = tick;
        this.type = type;
    }

    public TimelineEvent(int tick, String type, Map<String, String> parameters) {
        this.tick = tick;
        this.type = type;
        this.parameters = parameters;
    }
}
