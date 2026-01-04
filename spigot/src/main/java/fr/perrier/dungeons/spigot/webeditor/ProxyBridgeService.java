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
import java.util.concurrent.CompletableFuture;

/**
 * Simple communication service with the proxy for the web editor.
 */
public class ProxyBridgeService {

    private static final String PROXY_HOST = "localhost";
    private int proxyPort = 7734; // Default port
    private static final int SPIGOT_BRIDGE_PORT = 8081;
    private final Gson gson = new Gson();

    public ProxyBridgeService() {
        // Try to read the port from the plugin config
        try {
            proxyPort = Main.getInstance().getConfig().getInt("WebEditor.proxy-port", 7734);
            Main.getInstance().getLogger().info("Proxy port for web editor set to: " + proxyPort);
        } catch (Exception e) {
            Main.getInstance().getLogger().warning("Unable to read proxy port from config, using default port: 7734");
        }
    }

    /**
     * Requests the creation of an editor session from the proxy.
     *
     * @param dungeonName the name of the dungeon
     * @param floorId     the floor identifier
     * @param playerUuid  the UUID of the player
     * @param playerName  the name of the player
     * @return a CompletableFuture completed with the session ID, or null if failed
     */
    public CompletableFuture<String> requestEditorSession(String dungeonName, String floorId, UUID playerUuid, String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject request = new JsonObject();
                request.addProperty("action", "create_session");
                request.addProperty("dungeonName", dungeonName);
                request.addProperty("floorId", floorId);
                request.addProperty("playerUuid", playerUuid.toString());
                request.addProperty("playerName", playerName);
                request.addProperty("spigotServer", Main.getInstance().getServerNameService().getServerName().get());

                String response = sendPostRequest("/proxy-api/session", request.toString());

                if (response != null) {
                    JsonObject result = gson.fromJson(response, JsonObject.class);
                    if (result.has("success") && result.get("success").getAsBoolean()) {
                        return result.get("sessionId").getAsString();
                    }
                }

                return null;
            } catch (Exception e) {
                Main.getInstance().getLogger().severe("Proxy communication error: " + e.getMessage());
                return null;
            }
        });
    }

    /**
     * Requests the termination of a session from the proxy.
     *
     * @param sessionId the session identifier
     * @return true if the session was successfully stopped, false otherwise
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
            Main.getInstance().getLogger().severe("Error stopping proxy session: " + e.getMessage());
            return false;
        }
    }

    /**
     * Sends a POST request to the proxy.
     *
     * @param endpoint the API endpoint
     * @param jsonData the JSON data to send
     * @return the response as a string, or null if failed
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
                Main.getInstance().getLogger().warning("Proxy HTTP response " + responseCode);
                return null;
            }
        } catch (Exception e) {
            Main.getInstance().getLogger().warning("Unable to contact proxy: " + e.getMessage());
            return null;
        }
    }
}