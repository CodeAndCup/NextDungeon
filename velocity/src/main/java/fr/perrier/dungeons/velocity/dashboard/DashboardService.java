package fr.perrier.dungeons.velocity.dashboard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.perrier.dungeons.velocity.NextDungeonVelocity;
import fr.perrier.dungeons.velocity.webeditor.EditorSessionManager;
import fr.perrier.dungeons.common.model.dungeon.FloorMetadata;
import fr.perrier.dungeons.common.model.dungeon.config.FloorInstanceData;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service pour gérer les données du tableau de bord
 */
@RequiredArgsConstructor
public class DashboardService {
    
    private final RedissonClient redissonClient;
    private final EditorSessionManager sessionManager;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    // Redis Maps keys (same as in RedisStorageService)
    private static final String FLOOR_METADATA_MAP = NextDungeonVelocity.getInstance().getConfigManager().getTable("redis").getString("topic") + ":floor_metadata";
    private static final String INSTANCE_MAP = NextDungeonVelocity.getInstance().getConfigManager().getTable("redis").getString("topic") + ":instances";
    
    /**
     * Récupère tous les floors depuis Redis
     */
    public String getFloorsJson() {
        RMap<String, FloorMetadata> floorsMap = redissonClient.getMap(FLOOR_METADATA_MAP);

        JsonArray floorsArray = new JsonArray();
        for (Map.Entry<String, FloorMetadata> entry : floorsMap.entrySet()) {
            FloorMetadata floor = entry.getValue();
            JsonObject floorJson = new JsonObject();
            floorJson.addProperty("id", floor.getId());
            floorJson.addProperty("name", floor.getName());
            floorJson.addProperty("description", floor.getDescription());
            
            // Compter les instances actives pour ce floor
            long instanceCount = countInstancesForFloor(floor.getId());
            floorJson.addProperty("activeInstances", instanceCount);
            
            // Compter les sessions d'édition actives pour ce floor
            long editSessions = countEditSessionsForFloor(floor.getId());
            floorJson.addProperty("activeSessions", editSessions);
            
            floorsArray.add(floorJson);
        }
        
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.add("floors", floorsArray);
        response.addProperty("total", floorsArray.size());
        
        return gson.toJson(response);
    }
    
    /**
     * Récupère toutes les instances actives depuis Redis
     */
    public String getInstancesJson() {
        RMap<UUID, FloorInstanceData> instancesMap = redissonClient.getMap(INSTANCE_MAP);
        
        JsonArray instancesArray = new JsonArray();
        for (Map.Entry<UUID, FloorInstanceData> entry : instancesMap.entrySet()) {
            FloorInstanceData instance = entry.getValue();
            JsonObject instanceJson = new JsonObject();
            instanceJson.addProperty("instanceId", entry.getKey().toString());
            instanceJson.addProperty("floorId", instance.getFloorId());
            instanceJson.addProperty("ready", instance.isReady());
            instanceJson.addProperty("playerCount", instance.getPlayers().size());
            instanceJson.addProperty("playerList", instance.getPlayers().stream().map(UUID::toString).collect(Collectors.joining(", ")));
            
            instancesArray.add(instanceJson);
        }
        
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.add("instances", instancesArray);
        response.addProperty("total", instancesArray.size());
        
        return gson.toJson(response);
    }
    
    /**
     * Récupère toutes les sessions d'édition actives
     */
    public String getSessionsJson() {
        Map<String, EditorSessionManager.EditorSession> sessions = sessionManager.getActiveSessions();
        
        JsonArray sessionsArray = new JsonArray();
        for (Map.Entry<String, EditorSessionManager.EditorSession> entry : sessions.entrySet()) {
            JsonObject sessionJson = getSessionJson(entry);
            sessionsArray.add(sessionJson);
        }
        
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.add("sessions", sessionsArray);
        response.addProperty("total", sessionsArray.size());
        
        return gson.toJson(response);
    }

    private static @NonNull JsonObject getSessionJson(Map.Entry<String, EditorSessionManager.EditorSession> entry) {
        EditorSessionManager.EditorSession session = entry.getValue();
        JsonObject sessionJson = new JsonObject();
        sessionJson.addProperty("sessionId", session.getSessionId());
        sessionJson.addProperty("floorId", session.getFloorId());
        sessionJson.addProperty("dungeonName", session.getDungeonName());
        sessionJson.addProperty("editorName", session.getEditorName());
        sessionJson.addProperty("spigotServer", session.getSpigotServer());
        sessionJson.addProperty("createdAt", session.getCreatedAt().toString());
        return sessionJson;
    }

    /**
     * Génère les statistiques pour les graphiques
     */
    public String getStatsJson() {
        RMap<String, FloorMetadata> floorsMap = redissonClient.getMap(FLOOR_METADATA_MAP);
        RMap<UUID, FloorInstanceData> instancesMap = redissonClient.getMap(INSTANCE_MAP);
        
        // Distribution des instances par floor
        Map<String, Long> instanceDistribution = instancesMap.values().stream()
            .collect(Collectors.groupingBy(FloorInstanceData::getFloorId, Collectors.counting()));
        
        // Distribution des sessions d'édition par floor
        Map<String, Long> sessionDistribution = sessionManager.getActiveSessions().values().stream()
            .collect(Collectors.groupingBy(EditorSessionManager.EditorSession::getFloorId, Collectors.counting()));
        
        // Créer les données pour le graphique de distribution des instances
        JsonArray instanceChartData = new JsonArray();
        JsonArray instanceChartLabels = new JsonArray();
        for (Map.Entry<String, Long> entry : instanceDistribution.entrySet()) {
            String floorId = entry.getKey();
            FloorMetadata floor = floorsMap.get(floorId);
            String floorName = floor != null ? floor.getName() : floorId;
            
            instanceChartLabels.add(floorName);
            instanceChartData.add(entry.getValue());
        }
        
        // Créer les données pour le graphique des floors les plus édités
        JsonArray editChartData = new JsonArray();
        JsonArray editChartLabels = new JsonArray();
        
        // Trier les floors par nombre de sessions (du plus au moins édité)
        List<Map.Entry<String, Long>> sortedSessions = sessionDistribution.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(10)  // Top 10
            .toList();
        
        for (Map.Entry<String, Long> entry : sortedSessions) {
            String floorId = entry.getKey();
            FloorMetadata floor = floorsMap.get(floorId);
            String floorName = floor != null ? floor.getName() : floorId;
            
            editChartLabels.add(floorName);
            editChartData.add(entry.getValue());
        }
        
        // Assembler la réponse
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        
        JsonObject instanceChart = new JsonObject();
        instanceChart.add("labels", instanceChartLabels);
        instanceChart.add("data", instanceChartData);
        response.add("instanceDistribution", instanceChart);
        
        JsonObject editChart = new JsonObject();
        editChart.add("labels", editChartLabels);
        editChart.add("data", editChartData);
        response.add("mostEditedFloors", editChart);
        
        // Statistiques générales
        JsonObject summary = new JsonObject();
        summary.addProperty("totalFloors", floorsMap.size());
        summary.addProperty("totalInstances", instancesMap.size());
        summary.addProperty("totalSessions", sessionManager.getActiveSessionCount());
        response.add("summary", summary);
        
        return gson.toJson(response);
    }
    
    /**
     * Récupère la configuration complète d'un floor
     * NOTE: Temporairement désactivé pour éviter les problèmes de désérialisation avec les triggers Spigot
     */
    public String getFloorConfigJson(String floorId) {
        // Cette méthode n'est pas disponible sur Velocity car elle nécessite l'accès aux
        // classes spécifiques à Spigot (triggers, etc.)
        JsonObject error = new JsonObject();
        error.addProperty("success", false);
        error.addProperty("error", "Floor config not available on Velocity. Please use Spigot editor.");
        return gson.toJson(error);
    }
    
    /**
     * Compte le nombre d'instances actives pour un floor donné
     */
    private long countInstancesForFloor(String floorId) {
        RMap<UUID, FloorInstanceData> instancesMap = redissonClient.getMap(INSTANCE_MAP);
        return instancesMap.values().stream()
            .filter(instance -> instance.getFloorId().equals(floorId))
            .count();
    }
    
    /**
     * Compte le nombre de sessions d'édition actives pour un floor donné
     */
    private long countEditSessionsForFloor(String floorId) {
        return sessionManager.getActiveSessions().values().stream()
            .filter(session -> session.getFloorId().equals(floorId))
            .count();
    }
    
    /**
     * Récupère les données de la queue depuis Redis
     */
    public String getQueueJson() {
        // Redis keys for queue data
        String queuePrefix = NextDungeonVelocity.getInstance().getConfigManager().getTable("redis").getString("topic") + ":queue:";
        String instanceCountPrefix = NextDungeonVelocity.getInstance().getConfigManager().getTable("redis").getString("topic") + ":instance_count:";
        
        try {
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            
            // Get all queue keys
            Iterable<String> queueKeys = redissonClient.getKeys().getKeysByPattern(queuePrefix + "*");
            
            JsonObject queuesData = new JsonObject();
            int totalQueued = 0;
            int totalInstances = 0;
            int activeFloors = 0;
            
            for (String queueKey : queueKeys) {
                String floorId = queueKey.substring(queuePrefix.length());
                
                // Get queue for this floor
                org.redisson.api.RDeque<Object> queue = redissonClient.getDeque(queueKey);
                int queueSize = queue.size();
                
                if (queueSize > 0) {
                    activeFloors++;
                }
                
                // Get instance count for this floor
                org.redisson.api.RMap<UUID, String> instanceMap = redissonClient.getMap(instanceCountPrefix + floorId);
                int activeInstances = instanceMap.size();
                
                totalQueued += queueSize;
                totalInstances += activeInstances;
                
                // Build queue info for this floor
                JsonObject queueInfo = new JsonObject();
                queueInfo.addProperty("queueSize", queueSize);
                queueInfo.addProperty("activeInstances", activeInstances);
                
                // Get player list
                JsonArray playersArray = new JsonArray();
                int position = 1;
                for (Object entry : queue) {
                    try {
                        // Parse the queue entry
                        JsonObject playerJson = new JsonObject();
                        
                        // Use reflection to get fields from QueueEntry
                        if (entry != null) {
                            try {
                                java.lang.reflect.Method getPlayerId = entry.getClass().getMethod("getPlayerId");
                                java.lang.reflect.Method getPlayerName = entry.getClass().getMethod("getPlayerName");
                                java.lang.reflect.Method getServerName = entry.getClass().getMethod("getServerName");
                                java.lang.reflect.Method getTimestamp = entry.getClass().getMethod("getTimestamp");
                                
                                playerJson.addProperty("position", position);
                                playerJson.addProperty("playerId", String.valueOf(getPlayerId.invoke(entry)));
                                playerJson.addProperty("playerName", String.valueOf(getPlayerName.invoke(entry)));
                                playerJson.addProperty("serverName", String.valueOf(getServerName.invoke(entry)));
                                playerJson.addProperty("timestamp", ((Long) getTimestamp.invoke(entry)));
                                
                                playersArray.add(playerJson);
                            } catch (Exception e) {
                                // If reflection fails, skip this entry
                                continue;
                            }
                        }
                        position++;
                    } catch (Exception e) {
                        // Skip invalid entries
                    }
                }
                
                queueInfo.add("players", playersArray);
                queuesData.add(floorId, queueInfo);
            }
            
            response.add("queues", queuesData);
            
            // Add statistics
            JsonObject stats = new JsonObject();
            stats.addProperty("activeFloors", activeFloors);
            stats.addProperty("totalQueued", totalQueued);
            stats.addProperty("totalInstances", totalInstances);
            response.add("stats", stats);
            
            return gson.toJson(response);
            
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("success", false);
            error.addProperty("error", "Failed to load queue data: " + e.getMessage());
            return gson.toJson(error);
        }
    }
    
    /**
     * Vide la queue pour un floor spécifique
     */
    public String clearQueueForFloor(String floorId) {
        String queueKey = NextDungeonVelocity.getInstance().getConfigManager().getTable("redis").getString("topic") + ":queue:" + floorId;
        
        try {
            org.redisson.api.RDeque<Object> queue = redissonClient.getDeque(queueKey);
            int size = queue.size();
            queue.clear();
            
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.addProperty("message", "Queue cleared for floor: " + floorId);
            response.addProperty("clearedCount", size);
            
            return gson.toJson(response);
            
        } catch (Exception e) {
            JsonObject error = new JsonObject();
            error.addProperty("success", false);
            error.addProperty("error", "Failed to clear queue: " + e.getMessage());
            return gson.toJson(error);
        }
    }
}
