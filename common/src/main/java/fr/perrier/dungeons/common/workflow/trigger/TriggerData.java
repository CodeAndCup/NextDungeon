package fr.perrier.dungeons.common.workflow.trigger;

import fr.perrier.dungeons.common.workflow.action.ActionData;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
public class TriggerData implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    protected UUID triggerId;
    @Setter
    protected String name;
    @Setter
    protected boolean enabled;
    @Setter
    protected Map<String, Object> properties;
    @Setter
    protected List<ActionData> actions;

    public TriggerData(String name) {
        this.triggerId = UUID.randomUUID();
        this.name = name;
        this.enabled = true;
        this.actions = new ArrayList<>();
    }

    public TriggerData(UUID triggerId, String name, boolean enabled, Map<String, Object> properties, List<ActionData> actions) {
        this.triggerId = triggerId;
        this.name = name;
        this.enabled = enabled;
        this.properties = properties;
        this.actions = actions;
    }

    @Override
    public String toString() {
        return "TriggerData{" +
               "triggerId=" + triggerId +
               ", name='" + name + '\'' +
               ", enabled=" + enabled +
               ", properties=" + properties +
               ", actions=" + actions +
               '}';
    }
}
