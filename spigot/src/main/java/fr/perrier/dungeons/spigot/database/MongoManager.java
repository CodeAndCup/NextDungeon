package fr.perrier.dungeons.spigot.database;

import com.mongodb.*;
import com.mongodb.MongoClient;
import com.mongodb.client.*;
import fr.perrier.dungeons.common.model.dungeon.FloorData;
import fr.perrier.dungeons.common.workflow.trigger.TriggerData;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.utils.GsonProvider;
import fr.perrier.dungeons.spigot.workflow.serializer.InstanceSerializer;
import fr.perrier.dungeons.spigot.model.ProfileData;
import lombok.Getter;
import org.bson.Document;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * MongoDB database manager for dungeons.
 * Handles connection, disconnection, loading, saving, and deletion of data.
 */
@Getter
public class MongoManager implements DatabaseManager {
    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> playersCollection;
    private MongoCollection<Document> triggersCollection;
    private MongoCollection<Document> dungeonsCollection;
    private MongoCollection<Document> floorsCollection;
    private MongoCollection<Document> labyrinthSavesCollection;

    /**
     * Connects to the MongoDB database and initializes collections.
     */
    @Override
    public void connect() {
        String host = Main.getInstance().getConfig().getString("DatabaseConfiguration.mongodb.host", "localhost");
        int port = Main.getInstance().getConfig().getInt("DatabaseConfiguration.mongodb.port", 27017);
        MongoShared mongoShard = new MongoShared(host, port);
        this.mongoClient = new MongoClient(new MongoClientURI(mongoShard.getURI()));
        this.database = mongoClient.getDatabase("dungeons");

        this.playersCollection = database.getCollection("profiles");
        this.triggersCollection = database.getCollection("floor_triggers");
        this.dungeonsCollection = database.getCollection("dungeons");
        this.floorsCollection = database.getCollection("floors");

        this.labyrinthSavesCollection = database.getCollection("labyrinth_saves");

        // Create index on floor_id for efficient queries
        triggersCollection.createIndex(new Document("floor_id", 1));
        dungeonsCollection.createIndex(new Document("id", 1));
        floorsCollection.createIndex(new Document("dungeon_id", 1));
        labyrinthSavesCollection.createIndex(new Document("party_hash", 1).append("floor_id", 1));
    }

    /**
     * Closes the connection to the MongoDB server.
     */
    @Override
    public void disconnect() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    /**
     * Loads all data from the MongoDB database.
     */
    @Override
    public void loadData() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Handles an asynchronous operation and logs any errors.
     * @param future the asynchronous operation
     * @param operationName the name of the operation
     * @return the processed future
     */
    @Override
    public <T> CompletableFuture<T> handleAsyncOperation(CompletableFuture<T> future, String operationName) {
        return future.exceptionally(ex -> {
            Bukkit.getLogger().severe("Erreur lors de l'operation " + operationName + ": " + ex.getMessage());
            ex.printStackTrace(System.err);
            return null;
        });
    }

    /**
     * Loads a player's profile data from the database.
     * @param playerId the player's UUID
     * @return the player's profile data
     */
    @Override
    public ProfileData loadProfileData(java.util.UUID playerId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Saves a player's profile data to the database.
     * @param playerId the player's UUID
     * @param profileData the profile data to save
     */
    @Override
    public void saveProfileData(java.util.UUID playerId, ProfileData profileData) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public java.util.concurrent.CompletableFuture<java.util.List<ProfileData>> getAllProfiles() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Loads the triggers for a floor from MongoDB.
     * @param floorId the floor ID
     * @return a CompletableFuture containing the list of triggers
     */
    @Override
    public CompletableFuture<List<TriggerData>> loadTriggers(String floorId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Document query = new Document("floor_id", floorId);
                Document result = triggersCollection.find(query).first();

                if (result != null && result.containsKey("triggers_data")) {
                    String json = result.getString("triggers_data");
                    List<TriggerData> triggers = InstanceSerializer.deserializeTriggers(json);
                    Main.getLoggerUtil().info("Triggers loaded for " + floorId + " (" + triggers.size() + " triggers)");
                    return triggers;
                }

                Main.getLoggerUtil().warning("No trigger found for " + floorId + " in the trigger list.");
                return new ArrayList<>();
            } catch (Exception e) {
                Main.getLoggerUtil().severe("An error occurred during the loading phase of triggers for " + floorId + ": " + e.getMessage());
                e.printStackTrace(System.err);
                return new ArrayList<>();
            }
        });
    }

    /**
     * Saves the triggers for a floor to MongoDB.
     * @param floorId the floor ID
     * @param triggers the list of triggers to save
     * @return a CompletableFuture indicating the completion of the operation
     */
    @Override
    public CompletableFuture<Void> saveTriggers(String floorId, List<TriggerData> triggers) {
        return CompletableFuture.runAsync(() -> {
            try {
                String json = InstanceSerializer.serializeTriggers(triggers);

                Document query = new Document("floor_id", floorId);
                Document update = new Document("$set", new Document()
                        .append("floor_id", floorId)
                        .append("triggers_data", json)
                        .append("last_updated", System.currentTimeMillis()));

                triggersCollection.updateOne(query, update, new com.mongodb.client.model.UpdateOptions().upsert(true));

                Main.getLoggerUtil().info("Triggers saved for " + floorId + " (" + triggers.size() + " triggers)");
            } catch (Exception e) {
                Main.getLoggerUtil().severe("An error occurred during the saving phase of triggers for " + floorId + ": " + e.getMessage());
                e.printStackTrace(System.err);
            }
        });
    }

    /**
     * Checks if triggers exist for a given floor.
     * @param floorId the floor ID
     * @return a CompletableFuture containing true if triggers exist, false otherwise
     */
    @Override
    public CompletableFuture<Boolean> triggersExist(String floorId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Document query = new Document("floor_id", floorId);
                long count = triggersCollection.countDocuments(query);
                return count > 0;
            } catch (Exception e) {
                Main.getLoggerUtil().severe("An error occurred during the verification of triggers for " + floorId + ": " + e.getMessage());
                e.printStackTrace(System.err);
                return false;
            }
        });
    }

    /**
     * Deletes the triggers for a floor from MongoDB.
     * @param floorId the floor ID
     * @return a CompletableFuture indicating the completion of the operation
     */
    @Override
    public CompletableFuture<Void> deleteTriggers(String floorId) {
        return CompletableFuture.runAsync(() -> {
            try {
                Document query = new Document("floor_id", floorId);
                triggersCollection.deleteOne(query);

                Main.getLoggerUtil().info("Triggers delete for " + floorId);
            } catch (Exception e) {
                Main.getLoggerUtil().severe("An error occurred during the deletion of triggers for " + floorId + ": " + e.getMessage());
                e.printStackTrace(System.err);
            }
        });
    }

    @Override
    public CompletableFuture<String> loadCinematic(String cinematicId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                MongoCollection<Document> col = database.getCollection("cinematics");
                Document result = col.find(new Document("_id", cinematicId)).first();
                if (result != null && result.containsKey("payload_json")) {
                    return result.getString("payload_json");
                }
                return null;
            } catch (Exception e) {
                Main.getLoggerUtil().severe("Error loading cinematic " + cinematicId + ": " + e.getMessage());
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<Void> saveCinematic(String cinematicId, String name, String creator, String payloadJson) {
        return CompletableFuture.runAsync(() -> {
            try {
                MongoCollection<Document> col = database.getCollection("cinematics");
                Document doc = new Document("$set", new Document()
                        .append("_id", cinematicId)
                        .append("name", name)
                        .append("creator", creator)
                        .append("payload_json", payloadJson)
                        .append("updated_at", System.currentTimeMillis()));
                col.updateOne(new Document("_id", cinematicId), doc,
                        new com.mongodb.client.model.UpdateOptions().upsert(true));
            } catch (Exception e) {
                Main.getLoggerUtil().severe("Error saving cinematic " + cinematicId + ": " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteCinematic(String cinematicId) {
        return CompletableFuture.runAsync(() -> {
            try {
                MongoCollection<Document> col = database.getCollection("cinematics");
                col.deleteOne(new Document("_id", cinematicId));
            } catch (Exception e) {
                Main.getLoggerUtil().severe("Error deleting cinematic " + cinematicId + ": " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<List<String[]>> listCinematics() {
        return CompletableFuture.supplyAsync(() -> {
            List<String[]> result = new ArrayList<>();
            try {
                MongoCollection<Document> col = database.getCollection("cinematics");
                for (Document doc : col.find()) {
                    result.add(new String[]{
                            doc.getString("_id"),
                            doc.getString("name"),
                            doc.getString("creator")
                    });
                }
            } catch (Exception e) {
                Main.getLoggerUtil().severe("Error listing cinematics: " + e.getMessage());
            }
            return result;
        });
    }

    @Override
    public CompletableFuture<String> loadWorkflow(String workflowId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                MongoCollection<Document> col = database.getCollection("workflows");
                Document result = col.find(new Document("_id", workflowId)).first();
                if (result != null && result.containsKey("graph_json")) {
                    return result.getString("graph_json");
                }
                return null;
            } catch (Exception e) {
                Main.getLoggerUtil().severe("Error loading workflow " + workflowId + ": " + e.getMessage());
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<Void> saveWorkflow(String workflowId, String name, String graphJson) {
        return CompletableFuture.runAsync(() -> {
            try {
                MongoCollection<Document> col = database.getCollection("workflows");
                Document doc = new Document("$set", new Document()
                        .append("_id", workflowId)
                        .append("name", name)
                        .append("graph_json", graphJson)
                        .append("updated_at", System.currentTimeMillis()));
                col.updateOne(new Document("_id", workflowId), doc,
                        new com.mongodb.client.model.UpdateOptions().upsert(true));
            } catch (Exception e) {
                Main.getLoggerUtil().severe("Error saving workflow " + workflowId + ": " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteWorkflow(String workflowId) {
        return CompletableFuture.runAsync(() -> {
            try {
                MongoCollection<Document> col = database.getCollection("workflows");
                col.deleteOne(new Document("_id", workflowId));
            } catch (Exception e) {
                Main.getLoggerUtil().severe("Error deleting workflow " + workflowId + ": " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<String> loadDungeon(String dungeonId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Document doc = dungeonsCollection.find(new Document("_id", dungeonId)).first();
                if (doc != null) {
                    return doc.getString("data");
                }
                return null;
            } catch (Exception e) {
                Main.getLoggerUtil().severe("Error loading dungeon " + dungeonId + ": " + e.getMessage());
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<Void> saveDungeon(String dungeonId, String name, String description, String dataJson) {
        return CompletableFuture.runAsync(() -> {
            try {
                Document doc = new Document("$set", new Document()
                        .append("_id", dungeonId)
                        .append("name", name)
                        .append("description", description)
                        .append("data", dataJson)
                        .append("updated_at", System.currentTimeMillis()));
                dungeonsCollection.updateOne(new Document("_id", dungeonId), doc,
                        new com.mongodb.client.model.UpdateOptions().upsert(true));
                Main.getLoggerUtil().info("Dungeon saved to MongoDB: " + dungeonId);
            } catch (Exception e) {
                Main.getLoggerUtil().severe("Error saving dungeon " + dungeonId + ": " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteDungeon(String dungeonId) {
        return CompletableFuture.runAsync(() -> {
            try {
                dungeonsCollection.deleteOne(new Document("_id", dungeonId));
                Main.getLoggerUtil().info("Dungeon deleted from MongoDB: " + dungeonId);
            } catch (Exception e) {
                Main.getLoggerUtil().severe("Error deleting dungeon " + dungeonId + ": " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<List<String[]>> listAllDungeons() {
        return CompletableFuture.supplyAsync(() -> {
            List<String[]> result = new ArrayList<>();
            try {
                for (Document doc : dungeonsCollection.find()) {
                    String id = doc.getString("_id");
                    String data = doc.getString("data");
                    if (id != null) result.add(new String[] { id, data });
                }
            } catch (Exception e) {
                Main.getLoggerUtil().severe("Error listing dungeons: " + e.getMessage());
            }
            return result;
        });
    }

    @Override
    public CompletableFuture<String> loadFloor(String floorId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Document doc = floorsCollection.find(new Document("_id", floorId)).first();
                if (doc != null) {
                    return doc.getString("data");
                }
                return null;
            } catch (Exception e) {
                Main.getLoggerUtil().severe("Error loading floor " + floorId + ": " + e.getMessage());
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<Void> saveFloor(String floorId, String dungeonId, String name, String dataJson) {
        return CompletableFuture.runAsync(() -> {
            try {
                Document doc = new Document("$set", new Document()
                        .append("_id", floorId)
                        .append("dungeon_id", dungeonId)
                        .append("name", name)
                        .append("data", dataJson)
                        .append("updated_at", System.currentTimeMillis()));
                floorsCollection.updateOne(new Document("_id", floorId), doc,
                        new com.mongodb.client.model.UpdateOptions().upsert(true));
                Main.getLoggerUtil().info("Floor saved to MongoDB: " + floorId);
            } catch (Exception e) {
                Main.getLoggerUtil().severe("Error saving floor " + floorId + ": " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteFloor(String floorId) {
        return CompletableFuture.runAsync(() -> {
            try {
                floorsCollection.deleteOne(new Document("_id", floorId));
                Main.getLoggerUtil().info("Floor deleted from MongoDB: " + floorId);
            } catch (Exception e) {
                Main.getLoggerUtil().severe("Error deleting floor " + floorId + ": " + e.getMessage());
            }
        });
    }

    private static final int SAVE_FLOOR_RETRIES = 3;
    private static final long[] SAVE_FLOOR_BACKOFF_MS = {500L, 1000L, 2000L};

    @Override
    public CompletableFuture<Void> saveFloor(String floorId, String dungeonId, FloorData floorData) {
        if (floorId == null || floorId.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("floorId must not be empty"));
        }
        if (dungeonId == null || dungeonId.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("dungeonId must not be empty"));
        }
        if (floorData == null) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("floorData must not be null"));
        }
        floorData.setDungeonId(dungeonId);
        floorData.setChecksum(floorData.calculateChecksum());
        String dataJson = GsonProvider.GSON.toJson(floorData);

        return CompletableFuture.runAsync(() -> {
            Exception lastError = null;
            for (int attempt = 0; attempt < SAVE_FLOOR_RETRIES; attempt++) {
                try {
                    Document set = new Document("$set", new Document()
                            .append("_id", floorId)
                            .append("dungeon_id", dungeonId)
                            .append("name", floorData.getName())
                            .append("data", dataJson)
                            .append("version", floorData.getVersion())
                            .append("schema_version", floorData.getSchemaVersion())
                            .append("updated_by", floorData.getUpdatedBy())
                            .append("updated_at", floorData.getUpdatedAt())
                            .append("checksum", floorData.getChecksum()));
                    floorsCollection.updateOne(new Document("_id", floorId), set,
                            new com.mongodb.client.model.UpdateOptions().upsert(true));
                    Main.getLoggerUtil().info("[MongoManager] Floor saved: " + floorId + " v" + floorData.getVersion());
                    return;
                } catch (Exception e) {
                    lastError = e;
                    long backoff = SAVE_FLOOR_BACKOFF_MS[attempt];
                    Main.getLoggerUtil().warning("[MongoManager] saveFloor attempt " + (attempt + 1)
                            + " failed for " + floorId + ": " + e.getMessage() + " (retry in " + backoff + "ms)");
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                }
            }
            throw new RuntimeException("[MongoManager] saveFloor gave up after " + SAVE_FLOOR_RETRIES
                    + " attempts for " + floorId, lastError);
        });
    }

    @Override
    public CompletableFuture<FloorData> getFloor(String floorId) {
        if (floorId == null || floorId.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                Document doc = floorsCollection.find(new Document("_id", floorId)).first();
                if (doc == null) return null;
                String json = doc.getString("data");
                String persistedChecksum = doc.getString("checksum");
                FloorData floor = GsonProvider.GSON.fromJson(json, FloorData.class);
                if (floor == null) return null;
                if (persistedChecksum != null && !persistedChecksum.isEmpty()
                        && !persistedChecksum.equals(floor.calculateChecksum())) {
                    Main.getLoggerUtil().warning("[MongoManager] Stale checksum for floor " + floorId
                            + " (schema likely evolved) — returning row for caller to heal");
                }
                return floor;
            } catch (Exception e) {
                Main.getLoggerUtil().severe("Error loading versioned floor " + floorId + ": " + e.getMessage());
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<List<FloorData>> getAllFloors(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<FloorData> result = new ArrayList<>();
            try {
                FindIterable<Document> cursor = floorsCollection.find();
                if (limit > 0) cursor = cursor.limit(limit);
                cursor = cursor.batchSize(100);
                for (Document doc : cursor) {
                    String id = doc.getString("_id");
                    String json = doc.getString("data");
                    String persistedChecksum = doc.getString("checksum");
                    FloorData floor;
                    try {
                        floor = GsonProvider.GSON.fromJson(json, FloorData.class);
                    } catch (Exception e) {
                        Main.getLoggerUtil().severe("[MongoManager] Skipping unparseable floor " + id + ": " + e.getMessage());
                        continue;
                    }
                    if (floor == null) continue;
                    if (persistedChecksum != null && !persistedChecksum.isEmpty()
                            && !persistedChecksum.equals(floor.calculateChecksum())) {
                        Main.getLoggerUtil().warning("[MongoManager] Stale checksum for floor " + id
                                + " (schema likely evolved) — loading row for self-heal");
                    }
                    result.add(floor);
                }
            } catch (Exception e) {
                Main.getLoggerUtil().severe("Error listing floors: " + e.getMessage());
            }
            return result;
        });
    }

    @Override
    public CompletableFuture<String> loadLabyrinthSave(String saveId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Document doc = labyrinthSavesCollection.find(new Document("_id", saveId)).first();
                if (doc != null) return doc.getString("payload_json");
                return null;
            } catch (Exception e) {
                Main.getLoggerUtil().severe("Error loading labyrinth save " + saveId + ": " + e.getMessage());
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<String> findLabyrinthSaveByPartyHash(String partyHash, String floorId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Document query = new Document("party_hash", partyHash).append("floor_id", floorId);
                Document doc = labyrinthSavesCollection.find(query)
                        .sort(new Document("updated_at", -1))
                        .first();
                if (doc != null) return doc.getString("payload_json");
                return null;
            } catch (Exception e) {
                Main.getLoggerUtil().severe("Error finding labyrinth save by partyHash: " + e.getMessage());
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<List<String>> findLabyrinthSavesByPartyHash(String partyHash, String floorId) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> result = new ArrayList<>();
            try {
                Document query = new Document("party_hash", partyHash).append("floor_id", floorId);
                FindIterable<Document> cursor = labyrinthSavesCollection.find(query)
                        .sort(new Document("updated_at", -1));
                for (Document doc : cursor) {
                    String payload = doc.getString("payload_json");
                    if (payload != null) result.add(payload);
                }
            } catch (Exception e) {
                Main.getLoggerUtil().severe("Error listing labyrinth saves by partyHash: " + e.getMessage());
            }
            return result;
        });
    }

    @Override
    public CompletableFuture<Void> saveLabyrinthSave(String saveId, String floorId, String partyHash, String payloadJson) {
        return CompletableFuture.runAsync(() -> {
            try {
                Document doc = new Document("$set", new Document()
                        .append("_id", saveId)
                        .append("floor_id", floorId)
                        .append("party_hash", partyHash)
                        .append("payload_json", payloadJson)
                        .append("updated_at", System.currentTimeMillis()));
                labyrinthSavesCollection.updateOne(new Document("_id", saveId), doc,
                        new com.mongodb.client.model.UpdateOptions().upsert(true));
            } catch (Exception e) {
                Main.getLoggerUtil().severe("Error saving labyrinth save " + saveId + ": " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteLabyrinthSave(String saveId) {
        return CompletableFuture.runAsync(() -> {
            try {
                labyrinthSavesCollection.deleteOne(new Document("_id", saveId));
            } catch (Exception e) {
                Main.getLoggerUtil().severe("Error deleting labyrinth save " + saveId + ": " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<List<String[]>> listLabyrinthSaves() {
        return CompletableFuture.supplyAsync(() -> {
            List<String[]> result = new ArrayList<>();
            try {
                FindIterable<Document> cursor = labyrinthSavesCollection.find()
                        .sort(new Document("updated_at", -1));
                for (Document doc : cursor) {
                    result.add(new String[]{
                            doc.getString("_id"),
                            doc.getString("floor_id"),
                            doc.getString("party_hash")
                    });
                }
            } catch (Exception e) {
                Main.getLoggerUtil().severe("Error listing labyrinth saves: " + e.getMessage());
            }
            return result;
        });
    }

}
