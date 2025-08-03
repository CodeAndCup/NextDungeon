package fr.perrier.dungeons.utils;

import com.infernalsuite.asp.api.exceptions.CorruptedWorldException;
import com.infernalsuite.asp.api.exceptions.NewerFormatException;
import com.infernalsuite.asp.api.exceptions.UnknownWorldException;
import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.manager.FloorInstance;
import fr.perrier.dungeons.model.Floor;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class ASWMUtil {

    private static SlimeWorld getWorldForCloning(String name, SlimePropertyMap propertyMap) throws CorruptedWorldException, NewerFormatException, UnknownWorldException, IOException {
        return Main.getInstance().getAspAPI().readWorld(
                Main.getInstance().getAspLoader(),
                name,
                false,
                propertyMap
        );
    }

    /**
     * Make a new SlimeWorldInstance from a floor.
     *
     * @param floorInstance The floor to make the SlimeWorldInstance from.
     * @return A new SlimeWorldInstance.
     * @throws RuntimeException If unable to load world.
     */
    public static World makeFloorInstance(FloorInstance floorInstance) {
        Floor floor = floorInstance.getFloor();
        String templateName = floor.getId();
        String instanceName = floorInstance.getInstanceName();

        World world = Bukkit.getWorld(instanceName);
        if(world != null) {
            throw new RuntimeException("World " + world.getName() + " is already loaded.");
        }

        SlimeLoader loader = Main.getInstance().getAspLoader();

        try {
            SlimeWorld slimeWorld = getWorldForCloning(
                    templateName,
                    floor.getWorldConfig().getProperties()
            );
            SlimeWorld clonedWorld = slimeWorld.clone(instanceName, loader);

            Main.getInstance().getAspAPI().loadWorld(clonedWorld,true);

            return Main.getInstance().getAspAPI().getLoadedWorld(floorInstance.getInstanceName()).getBukkitWorld();

        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
