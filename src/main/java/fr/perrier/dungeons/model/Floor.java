package fr.perrier.dungeons.model;

import com.infernalsuite.asp.api.world.SlimeWorld;
import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.configuration.Requirements;
import fr.perrier.dungeons.configuration.Rules;
import fr.perrier.dungeons.configuration.WorldConfig;
import fr.perrier.dungeons.utils.ASWMUtil;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.HashMap;
import java.util.List;

@Getter
@Setter
public class Floor {

    @Getter
    private static final HashMap<String, Floor> floors = new HashMap<>();

    private String id;
    private String name;
    private WorldConfig worldConfig;
    private Requirements requirements;
    private Rules rules;
    private List<Step> steps;

    public Floor(String id, String name) {
        this.id = id;
        this.name = name;
        floors.put(id, this);
    }

    public static Floor getFloor(String id) {
        return floors.get(id);
    }

    public void updateMap() {
        floors.put(id, this);
    }

    public void generateTemplateWorld() {
        World world = Bukkit.getWorld(id);
        if (world == null) {
            if(ASWMUtil.isFloorWorldTemplateExists(this)) {
                ASWMUtil.loadFloorWorldTemplate(this);
            } else {
                Main.getInstance().getLogger().info("Template world " + id + " not found, creating it now.");
                SlimeWorld slimeWorld = Main.getInstance().getAspAPI().createEmptyWorld(
                        id,
                        false,
                        worldConfig.getProperties(),
                        Main.getInstance().getAspLoader()
                );
                ASWMUtil.saveFloorWorldTemplate(this, slimeWorld);
                Main.getInstance().getAspAPI().loadWorld(slimeWorld, true);
            }
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
