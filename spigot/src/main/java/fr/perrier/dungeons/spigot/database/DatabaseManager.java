package fr.perrier.dungeons.spigot.database;

import fr.perrier.dungeons.common.workflow.trigger.TriggerData;
import fr.perrier.dungeons.spigot.model.ProfileData;
import fr.perrier.dungeons.spigot.workflow.trigger.Trigger;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface DatabaseManager {
    
    // Initialize the database connection
    void connect();
    
    // Close the database connection
    void disconnect();
    
    // Load all data into the provider
    void loadData();

    // Profile operations
    ProfileData loadProfileData(UUID playerId);
    void saveProfileData(UUID playerId, ProfileData profileData);

    // Trigger operations (for dungeon floors)
    CompletableFuture<List<TriggerData>> loadTriggers(String floorId);
    CompletableFuture<Void> saveTriggers(String floorId, List<TriggerData> triggers);
    CompletableFuture<Boolean> triggersExist(String floorId);
    CompletableFuture<Void> deleteTriggers(String floorId);

    <T> CompletableFuture<T> handleAsyncOperation(CompletableFuture<T> future, String operationName);
}
