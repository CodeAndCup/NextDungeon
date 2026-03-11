package fr.perrier.dungeons.module.cinematic.factory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import fr.perrier.dungeons.module.cinematic.action.CinematicAction;
import fr.perrier.dungeons.module.cinematic.actions.blind.CinematicBlindAction;
import fr.perrier.dungeons.module.cinematic.actions.camera.CameraMoveAction;
import fr.perrier.dungeons.module.cinematic.actions.message.CinematicMessageAction;
import fr.perrier.dungeons.module.cinematic.actions.sound.CinematicSoundAction;
import fr.perrier.dungeons.module.cinematic.actions.title.CinematicTitleAction;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory pour créer des actions cinématiques depuis JSON.
 * Pattern extensible: ajouter un nouveau type = ajouter une entrée dans CINEMATIC_ACTIONS.
 */
public class CinematicActionFactory {

    private static final Map<String, Class<? extends CinematicAction>> CINEMATIC_ACTIONS = new HashMap<>();

    static {
        CINEMATIC_ACTIONS.put("cinematic_camera_move", CameraMoveAction.class);
        CINEMATIC_ACTIONS.put("cinematic_title", CinematicTitleAction.class);
        CINEMATIC_ACTIONS.put("cinematic_sound", CinematicSoundAction.class);
        CINEMATIC_ACTIONS.put("cinematic_message", CinematicMessageAction.class);
        CINEMATIC_ACTIONS.put("cinematic_blind", CinematicBlindAction.class);
    }

    private static final Gson GSON = new GsonBuilder().create();

    private CinematicActionFactory() {
    }

    /**
     * Vérifie si un type d'action est un type cinématique
     * @param type le type d'action
     * @return true si c'est un type cinématique connu
     */
    public static boolean isCinematicType(String type) {
        return type != null && CINEMATIC_ACTIONS.containsKey(type);
    }

    /**
     * Désérialise une action cinématique depuis un JsonObject
     * @param json l'objet JSON contenant les données de l'action
     * @return l'instance de CinematicAction
     * @throws IllegalArgumentException si le type est inconnu
     */
    public static CinematicAction deserialize(JsonObject json) {
        String type = json.has("type") ? json.get("type").getAsString() : null;
        Class<? extends CinematicAction> clazz = CINEMATIC_ACTIONS.get(type);

        if (clazz == null) {
            throw new IllegalArgumentException("Unknown cinematic action type: " + type);
        }

        return GSON.fromJson(json, clazz);
    }
}
