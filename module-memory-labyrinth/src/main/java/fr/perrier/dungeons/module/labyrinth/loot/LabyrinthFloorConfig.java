package fr.perrier.dungeons.module.labyrinth.loot;

import java.util.HashMap;
import java.util.Map;

/**
 * Static config of finite-floor lengths. {@link Integer#MAX_VALUE} marks
 * the floor as infinite — no auto end-of-run triggers ; the run only
 * ends on group wipe or voluntary exit.
 *
 * <p>Defaults follow CDC §1.3 ("ex." values). Admins can patch the map
 * at runtime via {@link #setMaxRooms(String, int)} — wiring this to a
 * config file is left for P11 polish.</p>
 */
public final class LabyrinthFloorConfig {

    public static final String FLOOR_INFINITE = "infinite";
    public static final int INFINITE = Integer.MAX_VALUE;

    private static final Map<String, Integer> MAX_ROOMS = new HashMap<>();

    static {
        MAX_ROOMS.put("easy", 30);
        MAX_ROOMS.put("normal", 50);
        MAX_ROOMS.put("hard", 70);
        MAX_ROOMS.put(FLOOR_INFINITE, INFINITE);
    }

    private LabyrinthFloorConfig() {}

    public static int getMaxRooms(String floorId) {
        if (floorId == null) return INFINITE;
        return MAX_ROOMS.getOrDefault(floorId.toLowerCase(), INFINITE);
    }

    public static boolean isFinite(String floorId) {
        return getMaxRooms(floorId) != INFINITE;
    }

    public static void setMaxRooms(String floorId, int maxRooms) {
        if (floorId == null) return;
        MAX_ROOMS.put(floorId.toLowerCase(), maxRooms);
    }
}
