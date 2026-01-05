package fr.perrier.dungeons.spigot.database;

/**
 * Utility class to build a MongoDB connection URI.
 */
public class MongoShared {

    private final String host;
    private final int port;
    private final boolean auth;
    private final String username;
    private final String password;

    /**
     * Creates an instance without authentication.
     * @param host MongoDB host
     * @param port MongoDB port
     */
    public MongoShared(String host, int port) {
        this.host = host;
        this.port = port;
        this.auth = false;
        this.username = null;
        this.password = null;
    }

    /**
     * Creates an instance with authentication.
     * @param host MongoDB host
     * @param port MongoDB port
     * @param username Username
     * @param password Password
     */
    public MongoShared(String host, int port, String username, String password) {
        this.host = host;
        this.port = port;
        this.auth = true;
        this.username = username;
        this.password = password;
    }

    /**
     * Returns the connection URI for the MongoDB server.
     * @return MongoDB connection URI
     */
    public String getURI() {
        if (!auth) return "mongodb://" + host + ":" + port;
        return "mongodb://" + username + ":" + password + "@" + host + ":" + port;
    }

}
