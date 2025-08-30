package fr.perrier.dungeons.spigot.webeditor;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.manager.TriggerSaveManager;
import fr.perrier.dungeons.spigot.webserver.blockly.BlocklyJavaScriptGenerator;
import fr.perrier.dungeons.spigot.model.Floor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Handler pour traiter les requêtes de l'éditeur web depuis le proxy
 */
public class ProxyEditorMessageHandler {

    private final Gson gson = new Gson();
    private final TriggerSaveManager triggerSaveManager;
    private final BlocklyJavaScriptGenerator blocklyGenerator;

    public ProxyEditorMessageHandler() {
        this.triggerSaveManager = new TriggerSaveManager();
        this.blocklyGenerator = new BlocklyJavaScriptGenerator();
    }

    /**
     * Traite une requête de chargement de triggers
     */
    public String handleLoadTriggersRequest(String dungeonName, String floorId) {
        try {
            Main.getInstance().getLogger().info("📥 Requête proxy: chargement triggers pour " + floorId);
            return triggerSaveManager.loadTriggersAsJson(dungeonName, floorId);
        } catch (Exception e) {
            Main.getInstance().getLogger().severe("Erreur chargement triggers: " + e.getMessage());
            return createErrorResponse("Erreur lors du chargement des triggers: " + e.getMessage());
        }
    }

    /**
     * Traite une requête de sauvegarde de triggers
     */
    public String handleSaveTriggersRequest(String dungeonName, String floorId, String triggersJson, UUID editorUuid) {
        try {
            Main.getInstance().getLogger().info("💾 Requête proxy: sauvegarde triggers pour " + floorId);
            
            Player editor = Bukkit.getPlayer(editorUuid);
            boolean success = triggerSaveManager.saveTriggers(dungeonName, floorId, triggersJson, editor);
            
            JsonObject response = new JsonObject();
            response.addProperty("success", success);
            response.addProperty("message", success ? "Triggers sauvegardés avec succès" : "Erreur lors de la sauvegarde");
            
            return gson.toJson(response);
        } catch (Exception e) {
            Main.getInstance().getLogger().severe("Erreur sauvegarde triggers: " + e.getMessage());
            return createErrorResponse("Erreur lors de la sauvegarde: " + e.getMessage());
        }
    }

    /**
     * Traite une requête des types de triggers
     */
    public String handleGetTriggerTypesRequest() {
        try {
            Main.getInstance().getLogger().info("📋 Requête proxy: types de triggers");
            
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
            Main.getInstance().getLogger().severe("Erreur types triggers: " + e.getMessage());
            return createErrorResponse("Erreur lors du chargement des types: " + e.getMessage());
        }
    }

    /**
     * Traite une requête de génération JavaScript Blockly
     */
    public String handleGenerateBlocklyJsRequest(UUID editorUuid) {
        try {
            Main.getInstance().getLogger().info("🔧 Requête proxy: génération Blockly JS");
            
            Player editor = Bukkit.getPlayer(editorUuid);
            if (editor == null) {
                return "console.error('Éditeur non connecté');";
            }
            
            return blocklyGenerator.generateJavaScript(editor);
        } catch (Exception e) {
            Main.getInstance().getLogger().severe("Erreur génération JS: " + e.getMessage());
            return "console.error('Erreur génération Blockly');";
        }
    }

    /**
     * Traite une requête d'informations de floor
     */
    public String handleGetFloorInfoRequest(String dungeonName, String floorId, String editorName) {
        try {
            Main.getInstance().getLogger().info("ℹ️ Requête proxy: infos floor " + floorId);
            
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
            Main.getInstance().getLogger().severe("Erreur infos floor: " + e.getMessage());
            return createErrorResponse("Erreur lors du chargement des informations: " + e.getMessage());
        }
    }

    /**
     * Crée une réponse d'erreur
     */
    private String createErrorResponse(String message) {
        JsonObject error = new JsonObject();
        error.addProperty("success", false);
        error.addProperty("error", message);
        return gson.toJson(error);
    }
}