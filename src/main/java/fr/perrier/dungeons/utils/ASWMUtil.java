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

import java.io.File;
import java.io.IOException;

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

    public static void saveFloorWorldTemplate(Floor floor, SlimeWorld slimeWorld) {
        try {
            Main.getInstance().getAspAPI().saveWorld(slimeWorld);
            Main.getInstance().getAspLoader().saveWorld(floor.getId(),Main.getInstance().getAspAPI().getSerializer().serializeWorld(slimeWorld));
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Deprecated
    public static void loadAllFloorWorldTemplate() {
        File file = new File(Main.getInstance().getDataFolder() + "/../../slime_worlds/");
        File[] files = file.listFiles();
        if(files != null) {
            for(File f : files) {
                if(f.isFile() && f.getName().endsWith(".slime")) {
                    String name = f.getName().replace(".slime","");
                    try {
                        byte[] data = Main.getInstance().getAspLoader().readWorld(name);
                        SlimeWorld slimeWorld = Main.getInstance().getAspAPI().getSerializer().deserializeWorld(
                                name,
                                data,
                                Main.getInstance().getAspLoader(),
                                new SlimePropertyMap(),
                                false
                        );
                        Main.getInstance().getAspAPI().loadWorld(slimeWorld,true);
                    }catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static boolean isFloorWorldTemplateExists(Floor floor) {
        File file = new File(Main.getInstance().getDataFolder() + "/../../slime_worlds/" + floor.getId() + ".slime");
        return file.exists();
    }

    public static void loadFloorWorldTemplate(Floor floor) {
        try {
            byte[] data = Main.getInstance().getAspLoader().readWorld(floor.getId());
            SlimeWorld slimeWorld = Main.getInstance().getAspAPI().getSerializer().deserializeWorld(
                    floor.getId(),
                    data,
                    Main.getInstance().getAspLoader(),
                    new SlimePropertyMap(),
                    false
            );
            Main.getInstance().getAspAPI().loadWorld(slimeWorld,true);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}
