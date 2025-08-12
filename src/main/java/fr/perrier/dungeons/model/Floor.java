package fr.perrier.dungeons.model;

import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.configuration.Requirements;
import fr.perrier.dungeons.configuration.Rules;
import fr.perrier.dungeons.configuration.WorldConfig;
import fr.perrier.dungeons.utils.ServerUtil;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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

    public CompletableFuture<Boolean> generateTemplate() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            System.out.println("Is floor template exists : " + ServerUtil.isFloorTemplateExists(this));
            if(!ServerUtil.isFloorTemplateExists(this)) {
                ServerUtil.createFloorTemplate(this).thenAccept(success -> {
                    future.complete(success);
                });
            } else {
                System.out.println("Floor template already exists");
                future.complete(true);
            }
        });
        return future;
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
