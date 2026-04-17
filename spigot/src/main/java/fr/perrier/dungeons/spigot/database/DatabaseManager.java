package fr.perrier.dungeons.spigot.database;

import fr.perrier.dungeons.common.model.dungeon.FloorData;
import fr.perrier.dungeons.common.workflow.trigger.TriggerData;
import fr.perrier.dungeons.spigot.model.ProfileData;

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

    // Cinematic CRUD operations (stored as JSON in DB)
    CompletableFuture<String> loadCinematic(String cinematicId);
    CompletableFuture<Void> saveCinematic(String cinematicId, String name, String creator, String payloadJson);
    CompletableFuture<Void> deleteCinematic(String cinematicId);
    CompletableFuture<List<String[]>> listCinematics();

    // Workflow storage operations
    CompletableFuture<String> loadWorkflow(String workflowId);
    CompletableFuture<Void> saveWorkflow(String workflowId, String name, String graphJson);
    CompletableFuture<Void> deleteWorkflow(String workflowId);

    // Dungeon CRUD operations (for dashboard)
    CompletableFuture<String> loadDungeon(String dungeonId);
    CompletableFuture<Void> saveDungeon(String dungeonId, String name, String description, String dataJson);
    CompletableFuture<Void> deleteDungeon(String dungeonId);

    // Floor CRUD operations (for dashboard)
    CompletableFuture<String> loadFloor(String floorId);
    CompletableFuture<Void> saveFloor(String floorId, String dungeonId, String name, String dataJson);
    CompletableFuture<Void> deleteFloor(String floorId);

    /**
     * Persists a versioned {@link FloorData} with retry + transaction semantics.
     * <p>
     * The implementation must: validate input, recompute checksum BEFORE saving,
     * retry up to 3x with exponential backoff (500/1000/2000 ms), wrap in a
     * transaction and rollback on error.
     */
    CompletableFuture<Void> saveFloor(String floorId, String dungeonId, FloorData floorData);

    /**
     * Loads a single {@link FloorData} with checksum verification.
     * Returns {@code null} if the row is missing OR its checksum does not match.
     */
    CompletableFuture<FloorData> getFloor(String floorId);

    /**
     * Returns every floor from the database using an iterator (never
     * {@code readAllMap()}/{@code findAll().toList()}) to avoid OOM.
     * Rows whose checksum does not match are skipped with a severe log.
     *
     * @param limit maximum number of rows to return; pass a non-positive value to disable the cap.
     */
    CompletableFuture<List<FloorData>> getAllFloors(int limit);

    <T> CompletableFuture<T> handleAsyncOperation(CompletableFuture<T> future, String operationName);
}
