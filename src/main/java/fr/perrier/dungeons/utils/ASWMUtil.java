package fr.perrier.dungeons.utils;

import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.api.world.SlimeWorldInstance;
import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.model.Floor;

public class ASWMUtil {

    /**
     * Make a new SlimeWorldInstance from a floor.
     *
     * @param floor The floor to make the SlimeWorldInstance from.
     * @return A new SlimeWorldInstance.
     * @throws RuntimeException If unable to load world.
     */
    public static SlimeWorldInstance makeFloorInstance(Floor floor) {
        try {
            SlimeWorld world = Main.getInstance().getAspAPI().readWorld(
                    Main.getInstance().getAspLoader(),
                    floor.getWorldConfig().getFolderName(),
                    true,
                    floor.getWorldConfig().getProperties()
            );
            return Main.getInstance().getAspAPI().loadWorld(world,true);
        }catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Edit a SlimeWorldInstance from a floor.
     *
     * @param floor The floor to edit the SlimeWorldInstance from.
     * @return A new SlimeWorldInstance.
     * @throws RuntimeException If unable to load world.
     */
    public static SlimeWorldInstance editFloorInstance(Floor floor) {
        try {
            SlimeWorld world = Main.getInstance().getAspAPI().readWorld(
                    Main.getInstance().getAspLoader(),
                    floor.getWorldConfig().getFolderName(),
                    false,
                    floor.getWorldConfig().getProperties()
            );
            return Main.getInstance().getAspAPI().loadWorld(world,false);
        }catch (Exception e) {
            e.fillInStackTrace();
        }
        throw new RuntimeException("Unable to load world");
    }

    /**
     * Save the given SlimeWorldInstance.
     *
     * @param instance The SlimeWorldInstance to be saved.
     */
    public static void saveFloorInstance(SlimeWorldInstance instance) {
        try {
            Main.getInstance().getAspAPI().saveWorld(instance);
        }catch (Exception e) {
            e.fillInStackTrace();
        }
    }
}
