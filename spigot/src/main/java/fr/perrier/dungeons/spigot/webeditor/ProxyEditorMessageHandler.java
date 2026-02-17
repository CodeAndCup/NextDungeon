package fr.perrier.dungeons.spigot.webeditor;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.workflow.serializer.EditorSerializer;
import fr.perrier.dungeons.spigot.webeditor.blockly.BlocklyJavaScriptGenerator;
import fr.perrier.dungeons.spigot.model.Floor;
import org.bson.internal.Base64;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Handler for processing web editor requests from the proxy.
 */
public class ProxyEditorMessageHandler {

    private final Gson gson = new Gson();
    private final EditorSerializer workflowSaveManager;
    private final BlocklyJavaScriptGenerator blocklyGenerator;

    public ProxyEditorMessageHandler() {
        this.workflowSaveManager = new EditorSerializer();
        this.blocklyGenerator = new BlocklyJavaScriptGenerator();
    }

    /**
     * Handles a trigger loading request.
     *
     * @param dungeonName the name of the dungeon
     * @param floorId     the floor identifier
     * @return the triggers as JSON, or an error response
     */
    public String handleLoadTriggersRequest(String dungeonName, String floorId) {
        try {
            Main.getLoggerUtil().info("📥 Requête proxy: chargement triggers pour " + floorId);
            return workflowSaveManager.loadTriggersAsJson(dungeonName, floorId);
        } catch (Exception e) {
            Main.getLoggerUtil().severe("Erreur chargement triggers: " + e.getMessage());
            return createErrorResponse("Erreur lors du chargement des triggers: " + e.getMessage());
        }
    }

    /**
     * Handles a trigger saving request.
     *
     * @param dungeonName   the name of the dungeon
     * @param floorId       the floor identifier
     * @param triggersJson  the triggers in JSON format
     * @param editorUuid    the UUID of the editor
     * @return a JSON response indicating success or error
     */
    public String handleSaveTriggersRequest(String dungeonName, String floorId, String triggersJson, UUID editorUuid) {
        try {
            Main.getLoggerUtil().info("💾 Requête proxy: sauvegarde triggers pour " + floorId);
            
            Player editor = Bukkit.getPlayer(editorUuid);
            boolean success = workflowSaveManager.saveWorkflows(dungeonName, floorId, triggersJson, editor);
            
            JsonObject response = new JsonObject();
            response.addProperty("success", success);
            response.addProperty("message", success ? "Triggers sauvegardés avec succès" : "Erreur lors de la sauvegarde");
            
            return gson.toJson(response);
        } catch (Exception e) {
            Main.getLoggerUtil().severe("Erreur sauvegarde triggers: " + e.getMessage());
            return createErrorResponse("Erreur lors de la sauvegarde: " + e.getMessage());
        }
    }

    /**
     * Handles a request for trigger types.
     *
     * @return a JSON response containing available trigger types
     */
    public String handleGetTriggerTypesRequest() {
        try {
            Main.getLoggerUtil().info("📋 Requête proxy: types de triggers");
            
            // Retourner les types de triggers disponibles (même logique que WebEditorServer)
            String typesJson = """
                {
                    "success": true,
                    "types": [
                        {
                            "id": "region_trigger",
                            "name": "Region Trigger",
                            "description": "Se déclenche quand un joueur entre dans une région",
                            "category": "Location"
                        },
                        {
                            "id": "debug_chat",
                            "name": "Debug Chat",
                            "description": "Se déclenche sur un message de chat (debug)",
                            "category": "Debug"
                        }
                    ]
                }
                """;
            
            return typesJson;
        } catch (Exception e) {
            Main.getLoggerUtil().severe("Erreur types triggers: " + e.getMessage());
            return createErrorResponse("Erreur lors du chargement des types: " + e.getMessage());
        }
    }

    /**
     * Handles a Blockly JavaScript generation request.
     *
     * @param editorUuid the UUID of the editor
     * @return the generated JavaScript as a Base64 string, or an error message
     */
    public String handleGenerateBlocklyJsRequest(UUID editorUuid) {
        try {
            Main.getLoggerUtil().info("🔧 Requête proxy: génération Blockly JS");
            
            Player editor = Bukkit.getPlayer(editorUuid);
            if (editor == null) {
                return "console.error('Éditeur non connecté');";
            }
            
            return Base64.encode(blocklyGenerator.generateJavaScript(editor).getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            Main.getLoggerUtil().severe("Erreur génération JS: " + e.getMessage());
            return "console.error('Erreur génération Blockly');";
        }
    }

    /**
     * Handles a floor information request.
     *
     * @param dungeonName the name of the dungeon
     * @param floorId     the floor identifier
     * @param editorName  the name of the editor
     * @return a JSON response containing floor information
     */
    public String handleGetFloorInfoRequest(String dungeonName, String floorId, String editorName) {
        try {
            Main.getLoggerUtil().info("ℹ️ Requête proxy: infos floor " + floorId);
            
            Floor floor = Floor.getFloor(floorId);
            String floorName = floor != null ? floor.getName() : "Inconnu";

            String infoJson = String.format("""
                {
                    "success": true,
                    "dungeon": "%s",
                    "floorId": "%s",
                    "floorName": "%s",
                    "editor": "%s"
                }
                """, dungeonName, floorId, floorName, editorName);

            return infoJson;
        } catch (Exception e) {
            Main.getLoggerUtil().severe("Erreur infos floor: " + e.getMessage());
            return createErrorResponse("Erreur lors du chargement des informations: " + e.getMessage());
        }
    }

    /**
     * Creates an error response.
     *
     * @param message the error message
     * @return a JSON error response
     */
    private String createErrorResponse(String message) {
        JsonObject error = new JsonObject();
        error.addProperty("success", false);
        error.addProperty("error", message);
        return gson.toJson(error);
    }
}