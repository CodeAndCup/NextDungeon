package fr.perrier.dungeons.model;

import com.infernalsuite.asp.api.world.SlimeWorldInstance;
import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.configuration.Requirements;
import fr.perrier.dungeons.configuration.Rules;
import fr.perrier.dungeons.configuration.WorldConfig;
import fr.perrier.dungeons.utils.ASWMUtil;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;

@Getter
@Setter
public class Floor {

    private String id;
    private String name;
    private String difficulty;
    private WorldConfig worldConfig;
    private Requirements requirements;
    private Rules rules;
    private List<Step> steps;

    public Floor(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public void play(Player leader) {
        SlimeWorldInstance instance = ASWMUtil.makeFloorInstance(this);
        World world = instance.getBukkitWorld();
        leader.teleport(world.getSpawnLocation());
    }
}
