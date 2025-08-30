package fr.perrier.dungeons.database;

/**
 * Classe utilitaire pour construire une URI de connexion MongoDB.
 */
public class MongoShared {

    private final String host;
    private final int port;
    private final boolean auth;
    private final String username;
    private final String password;

    /**
     * Crée une instance sans authentification.
     * @param host hôte MongoDB
     * @param port port MongoDB
     */
    public MongoShared(String host, int port) {
        this.host = host;
        this.port = port;
        this.auth = false;
        this.username = null;
        this.password = null;
    }

    /**
     * Crée une instance avec authentification.
     * @param host hôte MongoDB
     * @param port port MongoDB
     * @param username nom d'utilisateur
     * @param password mot de passe
     */
    public MongoShared(String host, int port, String username, String password) {
        this.host = host;
        this.port = port;
        this.auth = true;
        this.username = username;
        this.password = password;
    }

    /**
     * Retourne l'URI de connexion pour le serveur MongoDB.
     * @return URI de connexion MongoDB
     */
    public String getURI() {
        if (!auth) return "mongodb://" + host + ":" + port;
        return "mongodb://" + username + ":" + password + "@" + host + ":" + port;
    }

}
