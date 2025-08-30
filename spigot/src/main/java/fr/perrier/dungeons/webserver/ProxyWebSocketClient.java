package fr.perrier.dungeons.webserver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.manager.TriggerSaveManager;
import fr.perrier.dungeons.model.Floor;
import fr.perrier.dungeons.webserver.blockly.BlocklyJavaScriptGenerator;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ProxyWebSocketClient extends WebSocketClient {

    private final String serverName;
    private final Gson gson;
    private final TriggerSaveManager triggerSaveManager;
    private final BlocklyJavaScriptGenerator blocklyGenerator;
    private final ScheduledExecutorService executor;

    public ProxyWebSocketClient(String proxyHost, int proxyPort, String serverName) {
        super(URI.create("ws://" + proxyHost + ":" + proxyPort));
        this.serverName = serverName;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.triggerSaveManager = new TriggerSaveManager();
        this.blocklyGenerator = new BlocklyJavaScriptGenerator();
        this.executor = Executors.newScheduledThreadPool(2);

        // Envoyer le statut du serveur toutes les 30 secondes
        executor.scheduleAtFixedRate(this::sendStatusUpdate, 10, 30, TimeUnit.SECONDS);
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        Main.getInstance().getLogger().info("🔌 Connecté au proxy BungeeCord");

        // S'enregistrer auprès du proxy
        Map<String, Object> registration = new HashMap<>();
        registration.put("type", "REGISTER");
        registration.put("serverName", serverName);

        send(gson.toJson(registration));
    }

    @Override
    public void onMessage(String message) {
        // Traitement sur le thread principal de Bukkit
        new BukkitRunnable() {
            @Override
            public void run() {
                handleMessage(message);
            }
        }.runTask(Main.getInstance());
    }

    private void handleMessage(String message) {
        try {
            Map<String, Object> messageMap = gson.fromJson(message, Map.class);
            String messageType = (String) messageMap.get("type");

            if ("REGISTRATION_CONFIRMED".equals(messageType)) {
                Main.getInstance().getLogger().info("✅ Enregistrement confirmé par le proxy BungeeCord");
                return;
            }

            // Traiter les requêtes du proxy
            String requestId = (String) messageMap.get("id");
            String action = (String) messageMap.get("action");
            String data = (String) messageMap.get("data");

            if (requestId != null && action != null) {
                handleProxyRequest(requestId, action, data);
            }

        } catch (Exception e) {
            Main.getInstance().getLogger().severe("Erreur traitement message proxy: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Main.getInstance().getLogger().warning("❌ Connexion proxy fermée: " + reason);

        // Tentative de reconnexion après 5 secondes
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Main.getInstance().getLogger().info("🔄 Tentative de reconnexion au proxy...");
                    reconnect();
                } catch (Exception e) {
                    Main.getInstance().getLogger().severe("Erreur reconnexion: " + e.getMessage());
                }
            }
        }.runTaskLater(Main.getInstance(), 100L); // 5 secondes
    }

    @Override
    public void onError(Exception ex) {
        Main.getInstance().getLogger().severe("Erreur WebSocket proxy: " + ex.getMessage());
    }

    /**
     * Traite les requêtes reçues du proxy
     */
    private void handleProxyRequest(String requestId, String action, String data) {
        String response = "";

        try {
            switch (action) {
                case "GET_FLOORS":
                    response = handleGetFloors();
                    break;

                case "LOAD_TRIGGERS":
                    response = handleLoadTriggers(data);
                    break;

                case "SAVE_TRIGGERS":
                    response = handleSaveTriggers(data);
                    break;

                case "GENERATE_BLOCKLY":
                    response = handleGenerateBlockly();
                    break;

                case "PING":
                    response = handlePing();
                    break;

                default:
                    response = createErrorResponse("Action inconnue: " + action);
            }

        } catch (Exception e) {
            Main.getInstance().getLogger().severe("Erreur traitement action " + action + ": " + e.getMessage());
            response = createErrorResponse("Erreur interne: " + e.getMessage());
        }

        // Envoyer la réponse
        sendResponse(requestId, response);
    }

    /**
     * Récupère la liste des floors disponibles
     */
    private String handleGetFloors() {
        try {
            List<Map<String, Object>> floors = new ArrayList<>();

            // Récupérer tous les floors depuis votre système existant
            Collection<Floor> allFloors = Main.getInstance().getRedisStorageService().getFloorsMap().values();

            for (Floor floor : allFloors) {
                Map<String, Object> floorInfo = new HashMap<>();
                floorInfo.put("id", floor.getId());
                floorInfo.put("name", floor.getName());
                floorInfo.put("triggerCount", getTriggerCount(floor.getId()));
                floors.add(floorInfo);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("floors", floors);

            return gson.toJson(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur récupération floors: " + e.getMessage());
        }
    }

    /**
     * Charge les triggers d'un floor
     */
    private String handleLoadTriggers(String data) {
        try {
            Map<String, Object> requestData = gson.fromJson(data, Map.class);
            String floorId = (String) requestData.get("floorId");

            if (floorId == null) {
                return createErrorResponse("FloorId manquant");
            }

            // Utiliser votre TriggerSaveManager existant
            String triggersJson = triggerSaveManager.loadTriggersAsJson("dungeon", floorId);

            return triggersJson;

        } catch (Exception e) {
            return createErrorResponse("Erreur chargement triggers: " + e.getMessage());
        }
    }

    /**
     * Sauvegarde les triggers d'un floor
     */
    private String handleSaveTriggers(String data) {
        try {
            Map<String, Object> requestData = gson.fromJson(data, Map.class);
            String floorId = (String) requestData.get("floorId");
            String triggersData = (String) requestData.get("triggersData");

            if (floorId == null || triggersData == null) {
                return createErrorResponse("Données manquantes");
            }

            // Sauvegarder via votre système existant
            boolean success = triggerSaveManager.saveTriggers("dungeon", floorId, triggersData, null);

            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "Triggers sauvegardés avec succès" : "Erreur sauvegarde");

            return gson.toJson(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur sauvegarde triggers: " + e.getMessage());
        }
    }

    /**
     * Génère le JavaScript Blockly
     */
    private String handleGenerateBlockly() {
        try {
            String javascript = blocklyGenerator.generateJavaScript(null);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("javascript", javascript);

            return gson.toJson(response);

        } catch (Exception e) {
            return createErrorResponse("Erreur génération Blockly: " + e.getMessage());
        }
    }

    /**
     * Répond au ping
     */
    private String handlePing() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("timestamp", System.currentTimeMillis());
        response.put("playerCount", Bukkit.getOnlinePlayers().size());
        response.put("maxPlayers", Bukkit.getMaxPlayers());
        response.put("serverName", serverName);

        return gson.toJson(response);
    }

    /**
     * Envoie une réponse au proxy
     */
    private void sendResponse(String requestId, String responseData) {
        Map<String, Object> message = new HashMap<>();
        message.put("id", requestId);
        message.put("response", responseData);

        send(gson.toJson(message));
    }

    /**
     * Envoie une mise à jour de statut au proxy
     */
    private void sendStatusUpdate() {
        if (!isOpen()) return;

        try {
            Map<String, Object> status = new HashMap<>();
            status.put("type", "STATUS_UPDATE");
            status.put("playerCount", Bukkit.getOnlinePlayers().size());
            status.put("maxPlayers", Bukkit.getMaxPlayers());
            status.put("timestamp", System.currentTimeMillis());
            status.put("serverName", serverName);

            send(gson.toJson(status));

        } catch (Exception e) {
            Main.getInstance().getLogger().warning("Erreur envoi statut: " + e.getMessage());
        }
    }

    private String createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        return gson.toJson(error);
    }

    private int getTriggerCount(String floorId) {
        // À implémenter selon votre logique existante
        return 0;
    }

    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
        }
        close();
    }
}
