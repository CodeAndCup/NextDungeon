package fr.perrier.dungeons.common.workflow.action;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
public class ActionData implements Serializable {
    private static final long serialVersionUID = 1L;

    @Setter
    protected String name;
    protected String type;

    public ActionData(String name, String type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String toString() {
        return "ActionData{" +
               "name='" + name + '\'' +
               ", type='" + type + '\'' +
               '}';
    }
}
