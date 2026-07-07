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

    /**
     * Streams every stored profile. Used once at boot to (re)build the Redis
     * leaderboard sorted sets when they are missing (first deploy / Redis flush).
     * Implementations must iterate the result set (never materialise the whole
     * table as a map) to avoid OOM, mirroring {@link #getAllFloors(int)}.
     */
    CompletableFuture<List<ProfileData>> getAllProfiles();

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

    /**
     * Lists every dungeon stored in the dashboard table as a {@code [id, dataJson]}
     * pair. Used by the lobby boot to repopulate the {@code <topic>:dd:<id>} Redis
     * buckets the dashboard reads from when Redis has been flushed but the DB
     * still holds the source of truth.
     */
    CompletableFuture<List<String[]>> listAllDungeons();

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

    /**
     * Loads a save by primary key. Returns the JSON or {@code null}.
     */
    CompletableFuture<String> loadLabyrinthSave(String saveId);

    /**
     * Looks up a save by its (partyHash, floorId) compound key. Returns the
     * most recent {@code payload_json} or {@code null}. There is normally at
     * most one save per couple — see CDC §1.4 / §6.3.
     */
    CompletableFuture<String> findLabyrinthSaveByPartyHash(String partyHash, String floorId);

    /**
     * Lists every save for a {@code (partyHash, floorId)} couple as
     * {@code payload_json}, most recently updated first. Used to present the
     * resumable-run list for the infinite labyrinth.
     */
    CompletableFuture<List<String>> findLabyrinthSavesByPartyHash(String partyHash, String floorId);

    CompletableFuture<Void> saveLabyrinthSave(String saveId, String floorId, String partyHash, String payloadJson);

    CompletableFuture<Void> deleteLabyrinthSave(String saveId);

    /**
     * Lists all saves as {@code [id, floorId, partyHash]} (admin / debug usage).
     */
    CompletableFuture<List<String[]>> listLabyrinthSaves();
}
