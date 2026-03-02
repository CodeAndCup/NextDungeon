package fr.perrier.dungeons.spigot.database;

import com.mongodb.*;
import com.mongodb.MongoClient;
import com.mongodb.client.*;
import fr.perrier.dungeons.common.workflow.trigger.TriggerData;
import fr.perrier.dungeons.spigot.Main;
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

    /**
     * Connects to the MongoDB database and initializes collections.
     */
    @Override
    public void connect() {
        MongoShared mongoShard = new MongoShared("localhost", 27017);
        this.mongoClient = new MongoClient(new MongoClientURI(mongoShard.getURI()));
        this.database = mongoClient.getDatabase("dungeons");
        
        this.playersCollection = database.getCollection("profiles");
        this.triggersCollection = database.getCollection("floor_triggers");
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

    // ==================== TRIGGER OPERATIONS ====================

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

    // ===== Cinematic CRUD (MongoDB) =====

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

    // ===== Workflow CRUD (MongoDB) =====

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
}
