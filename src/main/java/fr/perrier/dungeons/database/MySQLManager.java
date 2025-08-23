package fr.perrier.dungeons.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.model.ProfileData;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Gestionnaire de base de données MySQL utilisant HikariCP pour la gestion des connexions.
 * Toutes les opérations de base de données sont effectuées de manière asynchrone pour éviter de bloquer le thread principal.
 */
public class MySQLManager implements DatabaseManager {
    private HikariDataSource dataSource;
    private final ExecutorService executorService;
    private volatile boolean isShuttingDown = false;
    private final CountDownLatch shutdownLatch = new CountDownLatch(1);
    private final AtomicInteger activeOperations = new AtomicInteger(0);

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;

    /**
     * Initialise le gestionnaire MySQL avec les paramètres de connexion.
     * @param host hôte MySQL
     * @param port port MySQL
     * @param database nom de la base
     * @param username utilisateur
     * @param password mot de passe
     */
    public MySQLManager(String host, int port, String database, String username, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;

        // Create a thread pool for database operations
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "MySQL-Worker-" + ThreadLocalRandom.current().nextInt(1000));
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Établit la connexion à la base MySQL et crée les tables si besoin.
     */
    @Override
    public void connect() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        config.setUsername(username);
        config.setPassword(password);
       /* config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);*/

        this.dataSource = new HikariDataSource(config);
        createTables();
    }

    /**
     * Ferme la connexion MySQL et attend la fin des opérations en cours.
     */
    @Override
    public void disconnect() {
        isShuttingDown = true;

        // Wait for active operations to complete with a timeout
        try {
            if (!shutdownLatch.await(5, TimeUnit.SECONDS)) {
                Main.getInstance().getLogger().warning("Some database operations did not complete before shutdown");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Shutdown executor service
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executorService.shutdownNow();
        }

        // Close data source
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    /**
     * Crée les tables nécessaires dans la base MySQL si elles n'existent pas.
     */
    private void createTables() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Create profiles table
            stmt.execute("CREATE TABLE IF NOT EXISTS profiles (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "data TEXT NOT NULL" +
                    ")");


        } catch (SQLException e) {
            Main.getInstance().getLogger().severe("Failed to create database tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Méthode dépréciée. Utilisez les méthodes spécifiques de chargement.
     */
    @Deprecated
    @Override
    public void loadData() {
        throw new UnsupportedOperationException("Use specific load methods");
    }

    /**
     * Exécute une tâche asynchrone dans le pool de threads.
     * @param task tâche à exécuter
     * @param operationName nom de l'opération pour le log
     * @return un CompletableFuture du résultat
     */
    private <T> CompletableFuture<T> executeAsync(Callable<T> task, String operationName) {
        if (isShuttingDown) {
            return CompletableFuture.failedFuture(new IllegalStateException("Database is shutting down"));
        }

        activeOperations.incrementAndGet();
        CompletableFuture<T> future = new CompletableFuture<>();

        executorService.execute(() -> {
            try {
                if (isShuttingDown) {
                    future.completeExceptionally(new IllegalStateException("Database is shutting down"));
                    return;
                }

                T result = task.call();
                future.complete(result);
            } catch (Exception e) {
                if (!isShuttingDown) {
                    Main.getInstance().getLogger().severe("Error in " + operationName + ": " + e.getMessage());
                }
                future.completeExceptionally(e);
            } finally {
                if (activeOperations.decrementAndGet() == 0 && isShuttingDown) {
                    shutdownLatch.countDown();
                }
            }
        });

        return future;
    }

    /**
     * Exécute une requête de mise à jour (INSERT, UPDATE, DELETE) de façon asynchrone.
     * @param query requête SQL
     * @param params paramètres de la requête
     */
    private void executeUpdate(String query, Object... params) {
        executeAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {

                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }

                return (Integer) stmt.executeUpdate();
            }
        }, "Update: " + query).exceptionally(e -> {
            if (!isShuttingDown) {
                Main.getInstance().getLogger().severe("Error executing update: " + query);
                e.printStackTrace();
            }
            return (Integer) 0;
        });
    }

    /**
     * Exécute une requête SQL et traite le résultat avec un processeur.
     * @param query requête SQL
     * @param processor processeur de résultat
     * @param params paramètres de la requête
     * @return résultat du processeur
     * @throws SQLException en cas d'erreur SQL
     */
    private <T> T executeQuery(String query, ResultSetProcessor<T> processor, Object... params) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                return processor.process(rs);
            }
        }
    }

    /**
     * Interface fonctionnelle pour traiter un ResultSet.
     * @param <T> type de retour du traitement
     */
    @FunctionalInterface
    private interface ResultSetProcessor<T> {
        T process(ResultSet rs) throws SQLException;
    }

    /**
     * Gère une opération asynchrone et log les erreurs éventuelles.
     * @param future opération asynchrone
     * @param operationName nom de l'opération
     * @return le future traité
     */
    @Override
    public <T> CompletableFuture<T> handleAsyncOperation(CompletableFuture<T> future, String operationName) {
        if (isShuttingDown) {
            return CompletableFuture.failedFuture(new IllegalStateException("Server is shutting down"));
        }

        activeOperations.incrementAndGet();
        return future.whenComplete((result, error) -> {
            try {
                if (error != null && !isShuttingDown) {
                    Main.getInstance().getLogger().severe("Error in " + operationName + ": " + error.getMessage());
                }
            } finally {
                if (activeOperations.decrementAndGet() == 0 && isShuttingDown) {
                    shutdownLatch.countDown();
                }
            }
        });
    }

    /**
     * Charge les données de profil d'un joueur depuis la base MySQL.
     * Si aucune donnée n'existe, retourne un nouveau ProfileData.
     * @param playerId l'UUID du joueur
     * @return les données de profil du joueur
     */
    @Override
    public ProfileData loadProfileData(UUID playerId) {
        try {
            return executeQuery(
                    "SELECT data FROM profiles WHERE id = ?",
                    rs -> {
                        ProfileData profileData;
                        if (rs.next()) {
                            profileData = ProfileData.fromJson(rs.getString("data"));
                        } else {
                            // No existing profile, return a new one
                            profileData = new ProfileData();
                        }
                        return profileData;
                    },
                    playerId.toString()
            );
        }catch (SQLException e) {
            if (!isShuttingDown) {
                Main.getInstance().getLogger().severe("Error loading profile data for player " + playerId + ": " + e.getMessage());
                e.printStackTrace();
            }
            return null;
        }
    }

    /**
     * Sauvegarde les données de profil d'un joueur dans la base MySQL.
     * Utilise une requête INSERT ... ON DUPLICATE KEY UPDATE pour insérer ou mettre à jour les données.
     * @param playerId l'UUID du joueur
     * @param profileData les données de profil à sauvegarder
     */
    @Override
    public void saveProfileData(UUID playerId, ProfileData profileData) {
        executeUpdate(
                "INSERT INTO profiles (id, data) VALUES (?, ?) ON DUPLICATE KEY UPDATE data = ?",
                playerId.toString(),
                profileData.toJson(),
                profileData.toJson()
        );
    }
}
