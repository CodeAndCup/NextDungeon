package fr.perrier.dungeons.spigot.database;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.utils.LoggerUtil;

import java.util.Objects;

/**
 * Fabrique pour créer une instance de gestionnaire de base de données
 * selon la configuration (MySQL ou MongoDB).
 */
public class DatabaseFactory {

    private DatabaseFactory() {
        // Private constructor to prevent from instantiation
    }
    
    /**
     * Crée et connecte un gestionnaire de base de données selon la configuration.
     * @return une instance de DatabaseManager connectée
     */
    public static DatabaseManager createDatabase() {
        String dbType = Objects.requireNonNull(Main.getInstance().getConfig().getString("DatabaseConfiguration.type")).toLowerCase();
        LoggerUtil.getInstance().info("Initializing database manager of type: " + dbType);
        
        switch (dbType) {
            case "mysql":
                String host = Objects.requireNonNull(Main.getInstance().getConfig().getString("DatabaseConfiguration.mysql.host"));
                int port = Main.getInstance().getConfig().getInt("DatabaseConfiguration.mysql.port");
                String database = Objects.requireNonNull(Main.getInstance().getConfig().getString("DatabaseConfiguration.mysql.database"));
                String username = Objects.requireNonNull(Main.getInstance().getConfig().getString("DatabaseConfiguration.mysql.username"));
                String password = Objects.requireNonNull(Main.getInstance().getConfig().getString("DatabaseConfiguration.mysql.password"));
                
                MySQLManager mySQLManager = new MySQLManager(host, port, database, username, password);
                mySQLManager.connect();
                return mySQLManager;
                
            case "mongodb":
            default:
                MongoManager mongoManager = new MongoManager();
                mongoManager.connect();
                return mongoManager;
        }
    }
}
