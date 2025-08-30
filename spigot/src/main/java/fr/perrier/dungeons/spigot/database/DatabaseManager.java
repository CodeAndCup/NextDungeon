package fr.perrier.dungeons.spigot.database;

import fr.perrier.dungeons.spigot.model.ProfileData;

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

    <T> CompletableFuture<T> handleAsyncOperation(CompletableFuture<T> future, String operationName);
}
