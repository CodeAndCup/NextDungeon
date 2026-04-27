package fr.perrier.dungeons.spigot.event;

import fr.perrier.dungeons.spigot.model.FloorInstance;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Bukkit event fired exactly once per {@link FloorInstance} when its
 * {@code ready} flag transitions from {@code false} to {@code true}.
 *
 * <p>Modules subscribe to this event to take over the runtime behaviour
 * of a freshly-prepared instance — typically to detect a non-classic
 * floor type (e.g. {@code LABYRINTH}) and start their own engine
 * (see {@code module-memory-labyrinth}).</p>
 */
@Getter
public class FloorInstanceReadyEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final FloorInstance instance;

    public FloorInstanceReadyEvent(FloorInstance instance) {
        this.instance = instance;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
