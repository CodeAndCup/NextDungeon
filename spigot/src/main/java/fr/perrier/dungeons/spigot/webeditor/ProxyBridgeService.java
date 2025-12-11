package fr.perrier.dungeons.spigot.webeditor;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fr.perrier.dungeons.spigot.Main;
import org.bukkit.Bukkit;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Service de communication simple avec le proxy pour l'éditeur web
 */
public class ProxyBridgeService {

    private static final String PROXY_HOST = "localhost";
    private int proxyPort = 7734; // Port par défaut
    private static final int SPIGOT_BRIDGE_PORT = 8081;
    private final Gson gson = new Gson();

    public ProxyBridgeService() {
        // Essayer de lire le port depuis la config du plugin
        try {
            proxyPort = Main.getInstance().getConfig().getInt("webeditor.proxy-port", 7734);
        } catch (Exception e) {
            Main.getInstance().getLogger().warning("Impossible de lire le port proxy depuis la config, utilisation du port par défaut: 7734");
        }
    }

    /**
     * Demande la création d'une session d'édition au proxy
     */
    public String requestEditorSession(String dungeonName, String floorId, UUID playerUuid, String playerName) {
        try {
            JsonObject request = new JsonObject();
            request.addProperty("action", "create_session");
            request.addProperty("dungeonName", dungeonName);
            request.addProperty("floorId", floorId);
            request.addProperty("playerUuid", playerUuid.toString());
            request.addProperty("playerName", playerName);
            request.addProperty("spigotServer", getCurrentServerName());

            String response = sendPostRequest("/proxy-api/session", request.toString());
            
            if (response != null) {
                JsonObject result = gson.fromJson(response, JsonObject.class);
                if (result.has("success") && result.get("success").getAsBoolean()) {
                    return result.get("sessionId").getAsString();
                }
            }
            
            return null;
        } catch (Exception e) {
            Main.getInstance().getLogger().severe("Erreur communication proxy: " + e.getMessage());
            return null;
        }
    }

    /**
     * Demande la suppression d'une session au proxy
     */
    public boolean requestSessionStop(String sessionId) {
        try {
            JsonObject request = new JsonObject();
            request.addProperty("action", "stop_session");
            request.addProperty("sessionId", sessionId);

            String response = sendPostRequest("/proxy-api/session", request.toString());
            
            if (response != null) {
                JsonObject result = gson.fromJson(response, JsonObject.class);
                return result.has("success") && result.get("success").getAsBoolean();
            }
            
            return false;
        } catch (Exception e) {
            Main.getInstance().getLogger().severe("Erreur arrêt session proxy: " + e.getMessage());
            return false;
        }
    }

    /**
     * Envoie une requête POST au proxy
     */
    private String sendPostRequest(String endpoint, String jsonData) {
        try {
            URL url = new URL("http://" + PROXY_HOST + ":" + proxyPort + endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonData.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    return response.toString();
                }
            } else {
                Main.getInstance().getLogger().warning("Réponse proxy HTTP " + responseCode);
                return null;
            }
        } catch (Exception e) {
            Main.getInstance().getLogger().warning("Impossible de contacter le proxy: " + e.getMessage());
            return null;
        }
    }

    /**
     * Récupère le nom du serveur actuel
     */
    private String getCurrentServerName() {
        // Utiliser le nom du serveur Bukkit ou CloudNet
        return Bukkit.getServer().getName();
    }
}