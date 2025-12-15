package fr.perrier.dungeons.velocity.dashboard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.perrier.dungeons.velocity.webeditor.EditorSessionManager;
import fr.perrier.dungeons.common.model.dungeon.FloorData;
import fr.perrier.dungeons.common.model.dungeon.config.FloorInstanceData;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    private static final String FLOOR_MAP = "dungeons:floors";
    private static final String INSTANCE_MAP = "dungeons:instances";
    
    /**
     * Récupère tous les floors depuis Redis
     */
    public String getFloorsJson() {
        RMap<String, FloorData> floorsMap = redissonClient.getMap(FLOOR_MAP);
        
        JsonArray floorsArray = new JsonArray();
        for (Map.Entry<String, FloorData> entry : floorsMap.entrySet()) {
            FloorData floor = entry.getValue();
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
            instanceJson.addProperty("playerCount", instance.getPlayerStats().size());
            
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
            EditorSessionManager.EditorSession session = entry.getValue();
            JsonObject sessionJson = new JsonObject();
            sessionJson.addProperty("sessionId", session.getSessionId());
            sessionJson.addProperty("floorId", session.getFloorId());
            sessionJson.addProperty("dungeonName", session.getDungeonName());
            sessionJson.addProperty("editorName", session.getEditorName());
            sessionJson.addProperty("spigotServer", session.getSpigotServer());
            sessionJson.addProperty("createdAt", session.getCreatedAt().toString());
            
            sessionsArray.add(sessionJson);
        }
        
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.add("sessions", sessionsArray);
        response.addProperty("total", sessionsArray.size());
        
        return gson.toJson(response);
    }
    
    /**
     * Génère les statistiques pour les graphiques
     */
    public String getStatsJson() {
        RMap<String, FloorData> floorsMap = redissonClient.getMap(FLOOR_MAP);
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
            FloorData floor = floorsMap.get(floorId);
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
            .collect(Collectors.toList());
        
        for (Map.Entry<String, Long> entry : sortedSessions) {
            String floorId = entry.getKey();
            FloorData floor = floorsMap.get(floorId);
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
     */
    public String getFloorConfigJson(String floorId) {
        RMap<String, FloorData> floorsMap = redissonClient.getMap(FLOOR_MAP);
        FloorData floor = floorsMap.get(floorId);
        
        if (floor == null) {
            JsonObject error = new JsonObject();
            error.addProperty("success", false);
            error.addProperty("error", "Floor not found: " + floorId);
            return gson.toJson(error);
        }
        
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.add("floor", gson.toJsonTree(floor));
        
        return gson.toJson(response);
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
}
