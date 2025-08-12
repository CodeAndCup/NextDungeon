package fr.perrier.dungeons.model;

import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.configuration.Requirements;
import fr.perrier.dungeons.configuration.Rules;
import fr.perrier.dungeons.configuration.WorldConfig;
import fr.perrier.dungeons.utils.ServerUtil;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;

@Getter
@Setter
public class Floor {

    private String id;
    private String name;
    private WorldConfig worldConfig;
    private Requirements requirements;
    private Rules rules;
    private List<Step> steps;

    public Floor(String id, String name) {
        this.id = id;
        this.name = name;
        updateMap();
    }

    public static Floor getFloor(String id) {
        return Main.getInstance().getRedisStorageService().getFloor(id);
    }

    public void updateMap() {
        Main.getInstance().getRedisStorageService().syncFloor(this);
    }

    public void generateTemplate() {
        System.out.println("Is floor template exists : " + ServerUtil.isFloorTemplateExists(this));
        if(!ServerUtil.isFloorTemplateExists(this)) {
            ServerUtil.createFloorTemplate(this);
        }
    }


    @Override
    public String toString() {
        return "Floor{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", worldConfig=" + worldConfig +
                ", requirements=" + requirements +
                ", rules=" + rules +
                ", steps=" + steps +
                '}';
    }
}
