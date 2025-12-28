package fr.perrier.dungeons.spigot.database;

import com.mongodb.*;
import com.mongodb.MongoClient;
import com.mongodb.client.*;
import fr.perrier.dungeons.common.workflow.trigger.TriggerData;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.model.ProfileData;
import fr.perrier.dungeons.spigot.workflow.trigger.Trigger;
import lombok.Getter;
import org.apache.commons.lang.NotImplementedException;
import org.bson.Document;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Gestionnaire de base de données MongoDB pour les donjons.
 * Permet la connexion, la déconnexion, le chargement, la sauvegarde et la suppression des données.
 */
@Getter
public class MongoManager implements DatabaseManager {
    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> playersCollection;
    private MongoCollection<Document> triggersCollection;

    /**
     * Se connecte à la base MongoDB et initialise les collections.
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
     * Ferme la connexion au serveur MongoDB.
     */
    @Override
    public void disconnect() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    /**
     * Charge toutes les données depuis la base MongoDB.
     */
    @Override
    public void loadData() {
        throw new NotImplementedException("Not implemented yet");
    }

    /**
     * Gère une opération asynchrone et log les erreurs éventuelles.
     * @param future opération asynchrone
     * @param operationName nom de l'opération
     * @return le future traité
     */
    @Override
    public <T> CompletableFuture<T> handleAsyncOperation(CompletableFuture<T> future, String operationName) {
        return future.exceptionally(ex -> {
            Bukkit.getLogger().severe("&cErreur lors de l'operation " + operationName + ": " + ex.getMessage());
            ex.printStackTrace();
            return null;
        });
    }

    /**
     * Charge les données de profil d'un joueur depuis la base de données.
     * @param playerId l'UUID du joueur
     * @return les données de profil du joueur
     */
    @Override
    public ProfileData loadProfileData(java.util.UUID playerId) {
        throw new NotImplementedException("Not implemented yet");
    }

    /**
     * Sauvegarde les données de profil d'un joueur dans la base de données.
     * @param playerId l'UUID du joueur
     * @param profileData les données de profil à sauvegarder
     */
    @Override
    public void saveProfileData(java.util.UUID playerId, ProfileData profileData) {
        throw new NotImplementedException("Not implemented yet");
    }

    // ==================== TRIGGER OPERATIONS ====================

    /**
     * Charge les triggers d'un floor depuis MongoDB.
     * @param floorId l'ID du floor
     * @return un CompletableFuture contenant la liste des triggers
     */
    @Override
    public CompletableFuture<List<TriggerData>> loadTriggers(String floorId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Document query = new Document("floor_id", floorId);
                Document result = triggersCollection.find(query).first();

                if (result != null && result.containsKey("triggers_data")) {
                    String json = result.getString("triggers_data");
                    List<TriggerData> triggers = TriggerSerializer.deserializeTriggers(json);
                    Main.getInstance().getLogger().info("Triggers chargés pour " + floorId + " (" + triggers.size() + " triggers)");
                    return triggers;
                }

                Main.getInstance().getLogger().warning("&eNo trigger found for " + floorId + " in the trigger list.");
                return new ArrayList<>();
            } catch (Exception e) {
                Main.getInstance().getLogger().severe("&cError loading triggers for " + floorId + ": " + e.getMessage());
                e.printStackTrace();
                return new ArrayList<>();
            }
        });
    }

    /**
     * Sauvegarde les triggers d'un floor dans MongoDB.
     * @param floorId l'ID du floor
     * @param triggers la liste des triggers à sauvegarder
     * @return un CompletableFuture indiquant la fin de l'opération
     */
    @Override
    public CompletableFuture<Void> saveTriggers(String floorId, List<TriggerData> triggers) {
        return CompletableFuture.runAsync(() -> {
            try {
                String json = TriggerSerializer.serializeTriggers(triggers);

                Document query = new Document("floor_id", floorId);
                Document update = new Document("$set", new Document()
                        .append("floor_id", floorId)
                        .append("triggers_data", json)
                        .append("last_updated", System.currentTimeMillis()));

                triggersCollection.updateOne(query, update, new com.mongodb.client.model.UpdateOptions().upsert(true));

                Main.getInstance().getLogger().info("Triggers sauvegardés pour " + floorId + " (" + triggers.size() + " triggers)");
            } catch (Exception e) {
                Main.getInstance().getLogger().severe("&cErreur lors de la sauvegarde des triggers pour " + floorId + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Vérifie si des triggers existent pour un floor donné.
     * @param floorId l'ID du floor
     * @return un CompletableFuture contenant true si des triggers existent, false sinon
     */
    @Override
    public CompletableFuture<Boolean> triggersExist(String floorId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Document query = new Document("floor_id", floorId);
                long count = triggersCollection.countDocuments(query);
                return count > 0;
            } catch (Exception e) {
                Main.getInstance().getLogger().severe("&cErreur lors de la vérification des triggers pour " + floorId + ": " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Supprime les triggers d'un floor de MongoDB.
     * @param floorId l'ID du floor
     * @return un CompletableFuture indiquant la fin de l'opération
     */
    @Override
    public CompletableFuture<Void> deleteTriggers(String floorId) {
        return CompletableFuture.runAsync(() -> {
            try {
                Document query = new Document("floor_id", floorId);
                triggersCollection.deleteOne(query);

                Main.getInstance().getLogger().info("Triggers supprimés pour " + floorId);
            } catch (Exception e) {
                Main.getInstance().getLogger().severe("&cErreur lors de la suppression des triggers pour " + floorId + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}
