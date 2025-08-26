package fr.perrier.dungeons.database;

import com.mongodb.*;
import com.mongodb.MongoClient;
import com.mongodb.client.*;
import fr.perrier.dungeons.model.ProfileData;
import lombok.Getter;
import org.apache.commons.lang.NotImplementedException;
import org.bson.Document;
import org.bukkit.Bukkit;

import java.util.concurrent.CompletableFuture;

/**
 * Gestionnaire de base de données MongoDB pour les maisons.
 * Permet la connexion, la déconnexion, le chargement, la sauvegarde et la suppression des maisons.
 */
@Getter
public class MongoManager implements DatabaseManager {
    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> playersCollection;

    /**
     * Se connecte à la base MongoDB et initialise la collection des maisons.
     */
    @Override
    public void connect() {
        MongoShared mongoShard = new MongoShared("localhost", 27017);
        this.mongoClient = new MongoClient(new MongoClientURI(mongoShard.getURI()));
        this.database = mongoClient.getDatabase("dungeons");
        
        this.playersCollection = database.getCollection("profiles");
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
     * Charge toutes les maisons depuis la base MongoDB dans le gestionnaire global.
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
            Bukkit.getLogger().severe("Erreur lors de l'operation " + operationName + ": " + ex.getMessage());
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

}
