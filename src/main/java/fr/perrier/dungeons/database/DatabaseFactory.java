package fr.perrier.dungeons.database;

import fr.perrier.dungeons.Main;

/**
 * Fabrique pour créer une instance de gestionnaire de base de données
 * selon la configuration (MySQL ou MongoDB).
 */
public class DatabaseFactory {
    
    /**
     * Crée et connecte un gestionnaire de base de données selon la configuration.
     * @return une instance de DatabaseManager connectée
     */
    public static DatabaseManager createDatabase() {
        String dbType = Main.getInstance().getConfig().getString("DatabaseConfiguration.type", "mongodb").toLowerCase();
        
        switch (dbType) {
            case "mysql":
                String host = Main.getInstance().getConfig().getString("DatabaseConfiguration.mysql.host", "localhost");
                int port = Main.getInstance().getConfig().getInt("DatabaseConfiguration.mysql.port", 3306);
                String database = Main.getInstance().getConfig().getString("DatabaseConfiguration.mysql.database", "housing");
                String username = Main.getInstance().getConfig().getString("DatabaseConfiguration.mysql.username", "root");
                String password = Main.getInstance().getConfig().getString("DatabaseConfiguration.mysql.password", "");
                
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
