package fr.perrier.dungeons.common.model.dungeon;

import fr.perrier.dungeons.common.model.dungeon.config.Requirements;
import fr.perrier.dungeons.common.model.dungeon.config.Rules;
import fr.perrier.dungeons.common.model.dungeon.config.WorldConfig;
import fr.perrier.dungeons.common.workflow.trigger.TriggerData;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class FloorData {

    private String id;

    private String name;
    private String description;

    private WorldConfig worldConfig;
    private Requirements requirements;
    private Rules rules;
    private List<Step> steps;
    private List<TriggerData> triggers;

    public FloorData(String id, String name) {
        this.id = id;
        this.name = name;
        this.description = "&cNo description";
    }

    public FloorData(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public FloorData(String id, String name, String description,
                     WorldConfig worldConfig, Requirements requirements,
                     Rules rules, List<Step> steps, List<TriggerData> triggers) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.worldConfig = worldConfig;
        this.requirements = requirements;
        this.rules = rules;
        this.steps = steps;
        this.triggers = triggers;
    }

    @Override
    public String toString() {
        return "FloorData{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", worldConfig=" + worldConfig +
                ", requirements=" + requirements +
                ", rules=" + rules +
                ", steps=" + steps +
                ", triggers=" + triggers +
                '}';
    }
}
