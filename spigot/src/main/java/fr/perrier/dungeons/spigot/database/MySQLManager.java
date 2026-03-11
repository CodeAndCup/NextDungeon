package fr.perrier.dungeons.spigot.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.perrier.dungeons.common.workflow.trigger.TriggerData;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.workflow.serializer.InstanceSerializer;
import fr.perrier.dungeons.spigot.model.ProfileData;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MySQL database manager using HikariCP for connection pooling.
 * All database operations are performed asynchronously to avoid blocking the main thread.
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
     * Initializes the MySQL manager with connection parameters.
     * @param host MySQL host
     * @param port MySQL port
     * @param database database name
     * @param username user
     * @param password password
     */
    public MySQLManager(String host, int port, String database, String username, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;

        // Create a bounded thread pool for database operations
        this.executorService = new ThreadPoolExecutor(
                5,   // core pool size
                20,  // maximum pool size
                60L, TimeUnit.SECONDS,  // keep-alive time for idle threads
                new java.util.concurrent.LinkedBlockingQueue<>(200),  // bounded work queue
                r -> {
                    Thread thread = new Thread(r, "MySQL-Worker-" + ThreadLocalRandom.current().nextInt(1000));
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()  // back-pressure when queue is full
        );
    }

    /**
     * Establishes the connection to MySQL and creates tables if needed.
     */
    @Override
    public void connect() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        config.setUsername(username);
        config.setPassword(password);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setLeakDetectionThreshold(60000);

        this.dataSource = new HikariDataSource(config);
        createTables();
    }

    /**
     * Closes the MySQL connection and waits for ongoing operations to finish.
     */
    @Override
    public void disconnect() {
        isShuttingDown = true;

        // Wait for active operations to complete with a timeout
        try {
            if (!shutdownLatch.await(5, TimeUnit.SECONDS)) {
                Main.getLoggerUtil().warning("Some database operations did not complete before shutdown");
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
     * Creates the necessary tables in the MySQL database if they do not exist.
     */
    private void createTables() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Create profiles table
            stmt.execute("CREATE TABLE IF NOT EXISTS profiles (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "data TEXT NOT NULL" +
                    ")");

            // Create triggers table for dungeon floors
            stmt.execute("CREATE TABLE IF NOT EXISTS floor_triggers (" +
                    "floor_id VARCHAR(255) PRIMARY KEY, " +
                    "triggers_data MEDIUMTEXT NOT NULL, " +
                    "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                    ")");

            // Create cinematics table
            stmt.execute("CREATE TABLE IF NOT EXISTS cinematics (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "name VARCHAR(64) NOT NULL, " +
                    "creator VARCHAR(32), " +
                    "payload_json MEDIUMTEXT NOT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                    ")");

            // Create workflows table
            stmt.execute("CREATE TABLE IF NOT EXISTS workflows (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "name VARCHAR(128) NOT NULL, " +
                    "graph_json MEDIUMTEXT NOT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                    ")");

        } catch (SQLException e) {
            Main.getLoggerUtil().severe("Failed to create database tables: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    /**
     * Deprecated method. Use specific load methods instead.
     */
    @Deprecated
    @Override
    public void loadData() {
        throw new UnsupportedOperationException("Use specific load methods");
    }

    /**
     * Executes an asynchronous task in the thread pool.
     * @param task the task to execute
     * @param operationName operation name for logging
     * @return a CompletableFuture of the result
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
                    Main.getLoggerUtil().severe("Error in " + operationName + ": " + e.getMessage());
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
     * Executes an update query (INSERT, UPDATE, DELETE) asynchronously.
     * @param query SQL query
     * @param params query parameters
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
                Main.getLoggerUtil().severe("Error executing update: " + query);
                e.printStackTrace(System.err);
            }
            return (Integer) 0;
        });
    }

    /**
     * Executes an SQL query and processes the result with a processor.
     * @param query SQL query
     * @param processor result processor
     * @param params query parameters
     * @return result from the processor
     * @throws SQLException if an SQL error occurs
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
     * Functional interface for processing a ResultSet.
     * @param <T> return type of the processing
     */
    @FunctionalInterface
    private interface ResultSetProcessor<T> {
        T process(ResultSet rs) throws SQLException;
    }

    /**
     * Handles an asynchronous operation and logs any errors.
     * @param future asynchronous operation
     * @param operationName operation name
     * @return the processed future
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
                    Main.getLoggerUtil().severe("Error in " + operationName + ": " + error.getMessage());
                }
            } finally {
                if (activeOperations.decrementAndGet() == 0 && isShuttingDown) {
                    shutdownLatch.countDown();
                }
            }
        });
    }

    /**
     * Loads a player's profile data from the MySQL database.
     * If no data exists, returns a new ProfileData.
     * @param playerId the player's UUID
     * @return the player's profile data
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
                            profileData = new ProfileData(playerId);
                        }
                        return profileData;
                    },
                    playerId.toString()
            );
        }catch (SQLException e) {
            if (!isShuttingDown) {
                Main.getLoggerUtil().severe("Error loading profile data for player " + playerId + ": " + e.getMessage());
                e.printStackTrace(System.err);
            }
            return null;
        }
    }

    /**
     * Saves a player's profile data to the MySQL database.
     * Uses an INSERT ... ON DUPLICATE KEY UPDATE query to insert or update the data.
     * @param playerId the player's UUID
     * @param profileData the profile data to save
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

    // ==================== TRIGGER OPERATIONS ====================

    /**
     * Loads the triggers for a floor from the database.
     * @param floorId the floor ID
     * @return a CompletableFuture containing the list of triggers
     */
    @Override
    public CompletableFuture<List<TriggerData>> loadTriggers(String floorId) {
        return executeAsync(() -> {
            try {
                return executeQuery(
                        "SELECT triggers_data FROM floor_triggers WHERE floor_id = ?",
                        rs -> {
                            if (rs.next()) {
                                String json = rs.getString("triggers_data");
                                return InstanceSerializer.deserializeTriggers(json);
                            }
                            return new ArrayList<>();
                        },
                        floorId
                );
            } catch (SQLException e) {
                Main.getLoggerUtil().severe("Error loading triggers for floor " + floorId + ": " + e.getMessage());
                return new ArrayList<>();
            }
        }, "Load triggers for " + floorId);
    }

    /**
     * Saves the triggers for a floor to the database.
     * @param floorId the floor ID
     * @param triggers the list of triggers to save
     * @return a CompletableFuture indicating the completion of the operation
     */
    @Override
    public CompletableFuture<Void> saveTriggers(String floorId, List<TriggerData> triggers) {
        return executeAsync(() -> {
            String json = InstanceSerializer.serializeTriggers(triggers);
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "INSERT INTO floor_triggers (floor_id, triggers_data) VALUES (?, ?) " +
                         "ON DUPLICATE KEY UPDATE triggers_data = ?, last_updated = CURRENT_TIMESTAMP")) {

                stmt.setString(1, floorId);
                stmt.setString(2, json);
                stmt.setString(3, json);
                stmt.executeUpdate();

                Main.getLoggerUtil().info("Triggers saved for " + floorId + " (" + triggers.size() + " triggers)");
                return null;
            }
        }, "Save triggers for " + floorId);
    }

    /**
     * Checks if triggers exist for a given floor.
     * @param floorId the floor ID
     * @return a CompletableFuture containing true if triggers exist, false otherwise
     */
    @Override
    public CompletableFuture<Boolean> triggersExist(String floorId) {
        return executeAsync(() -> {
            try {
                return executeQuery(
                        "SELECT COUNT(*) as count FROM floor_triggers WHERE floor_id = ?",
                        rs -> {
                            if (rs.next()) {
                                return rs.getInt("count") > 0;
                            }
                            return false;
                        },
                        floorId
                );
            } catch (SQLException e) {
                Main.getLoggerUtil().severe("Error checking triggers existence for floor " + floorId + ": " + e.getMessage());
                return false;
            }
        }, "Check triggers existence for " + floorId);
    }

    /**
     * Deletes the triggers for a floor from the database.
     * @param floorId the floor ID
     * @return a CompletableFuture indicating the completion of the operation
     */
    @Override
    public CompletableFuture<Void> deleteTriggers(String floorId) {
        return executeAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM floor_triggers WHERE floor_id = ?")) {

                stmt.setString(1, floorId);
                stmt.executeUpdate();

                Main.getLoggerUtil().info("Triggers deleted for " + floorId);
                return null;
            }
        }, "Delete triggers for " + floorId);
    }

    // ===== Cinematic CRUD =====

    @Override
    public CompletableFuture<String> loadCinematic(String cinematicId) {
        return executeAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT payload_json FROM cinematics WHERE id = ?")) {

                stmt.setString(1, cinematicId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("payload_json");
                    }
                }
                return null;
            }
        }, "Load cinematic " + cinematicId);
    }

    @Override
    public CompletableFuture<Void> saveCinematic(String cinematicId, String name, String creator, String payloadJson) {
        return executeAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "INSERT INTO cinematics (id, name, creator, payload_json) VALUES (?, ?, ?, ?) " +
                         "ON DUPLICATE KEY UPDATE name = VALUES(name), creator = VALUES(creator), payload_json = VALUES(payload_json)")) {

                stmt.setString(1, cinematicId);
                stmt.setString(2, name);
                stmt.setString(3, creator);
                stmt.setString(4, payloadJson);
                stmt.executeUpdate();

                Main.getLoggerUtil().info("Cinematic saved: " + cinematicId);
                return null;
            }
        }, "Save cinematic " + cinematicId);
    }

    @Override
    public CompletableFuture<Void> deleteCinematic(String cinematicId) {
        return executeAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM cinematics WHERE id = ?")) {

                stmt.setString(1, cinematicId);
                stmt.executeUpdate();

                Main.getLoggerUtil().info("Cinematic deleted: " + cinematicId);
                return null;
            }
        }, "Delete cinematic " + cinematicId);
    }

    @Override
    public CompletableFuture<List<String[]>> listCinematics() {
        return executeAsync(() -> {
            List<String[]> result = new java.util.ArrayList<>();
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT id, name, creator FROM cinematics ORDER BY updated_at DESC")) {

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        result.add(new String[]{rs.getString("id"), rs.getString("name"), rs.getString("creator")});
                    }
                }
            }
            return result;
        }, "List cinematics");
    }

    // ===== Workflow CRUD =====

    @Override
    public CompletableFuture<String> loadWorkflow(String workflowId) {
        return executeAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT graph_json FROM workflows WHERE id = ?")) {

                stmt.setString(1, workflowId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("graph_json");
                    }
                }
                return null;
            }
        }, "Load workflow " + workflowId);
    }

    @Override
    public CompletableFuture<Void> saveWorkflow(String workflowId, String name, String graphJson) {
        return executeAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "INSERT INTO workflows (id, name, graph_json) VALUES (?, ?, ?) " +
                         "ON DUPLICATE KEY UPDATE name = VALUES(name), graph_json = VALUES(graph_json)")) {

                stmt.setString(1, workflowId);
                stmt.setString(2, name);
                stmt.setString(3, graphJson);
                stmt.executeUpdate();

                Main.getLoggerUtil().info("Workflow saved: " + workflowId);
                return null;
            }
        }, "Save workflow " + workflowId);
    }

    @Override
    public CompletableFuture<Void> deleteWorkflow(String workflowId) {
        return executeAsync(() -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM workflows WHERE id = ?")) {

                stmt.setString(1, workflowId);
                stmt.executeUpdate();

                Main.getLoggerUtil().info("Workflow deleted: " + workflowId);
                return null;
            }
        }, "Delete workflow " + workflowId);
    }
}
