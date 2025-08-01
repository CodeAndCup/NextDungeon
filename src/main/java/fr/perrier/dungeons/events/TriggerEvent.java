package fr.perrier.dungeons.events;

import fr.perrier.dungeons.trigger.Trigger;
import org.bukkit.event.Event;

public abstract class TriggerEvent extends Event {

    protected Trigger trigger;

    public TriggerEvent(Trigger trigger) {
        this.trigger = trigger;
    }

    public Trigger getTrigger() {
        return trigger;
    }
}
