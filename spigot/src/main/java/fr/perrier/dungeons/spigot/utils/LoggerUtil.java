package fr.perrier.dungeons.spigot.utils;

import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.dungeons.spigot.Main;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;

/**
 * Centralized logging utility for the NextDungeon plugin with multi-destination support.
 * <p>
 * This singleton class provides a unified interface for logging at multiple severity levels
 * (info, warning, severe) with flexible output routing. Logs can be directed to:
 * <ul>
 *   <li><strong>Console:</strong> Standard server console output via Bukkit logger</li>
 *   <li><strong>In-game:</strong> Chat messages to authorized players (permission: {@code nextdungeon.debug} or op)</li>
 *   <li><strong>Both:</strong> Simultaneously to console and in-game chat</li>
 * </ul>
 * <p>
 * <strong>Color Coding:</strong> Log levels are color-coded for clarity:
 * <ul>
 *   <li>Info: No color prefix</li>
 *   <li>Warning: Yellow ({@code &e})</li>
 *   <li>Severe: Red ({@code &#FF0000})</li>
 * </ul>
 * <p>
 * <strong>Thread Safety:</strong> This class uses a simple singleton pattern without explicit
 * synchronization. For thread-safe initialization in multi-threaded environments, consider
 * using eager initialization or synchronized access.
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>
 * LoggerUtil logger = LoggerUtil.getInstance();
 * logger.setLogBroadcastType(LogBroadcastType.BOTH);
 * logger.info("Server is ready");
 * logger.warning("Low memory detected");
 * logger.severe("Critical error occurred");
 * </pre>
 *
 * @see LogBroadcastType
 * @see ChatUtil
 */
public class LoggerUtil {

    /**
     * Static instance for singleton pattern.
     * Initialized lazily on first call to {@link #getInstance()}.
     */
    private static LoggerUtil instance;

    /**
     * Flag controlling whether debug-level logging is enabled.
     * Currently unused in the current implementation but reserved for future debug-level logging support.
     * <p>
     * Accessible via {@code getDebugEnabled()} and {@code setDebugEnabled(boolean)}.
     */
    @Getter
    @Setter
    private boolean debugEnabled;

    /**
     * Determines where log messages are sent.
     * <p>
     * Controls the routing of {@code info()}, {@code warning()}, and {@code severe()} messages:
     * <ul>
     *   <li>{@link LogBroadcastType#CONSOLE} — to server console only</li>
     *   <li>{@link LogBroadcastType#IN_GAME} — to authorized players only</li>
     *   <li>{@link LogBroadcastType#BOTH} — to both console and authorized players</li>
     * </ul>
     * <p>
     * Default value: {@link LogBroadcastType#CONSOLE}
     * <p>
     * Accessible via {@code getLogBroadcastType()} and {@code setLogBroadcastType(LogBroadcastType)}.
     */
    @Getter
    @Setter
    private LogBroadcastType logBroadcastType;

    /**
     * Private constructor initializing the logger with default settings.
     * <p>
     * Debug logging is disabled by default, and broadcast type is set to {@link LogBroadcastType#CONSOLE}.
     * This constructor is private to enforce singleton pattern via {@link #getInstance()}.
     */
    private LoggerUtil() {
        debugEnabled = false;
        logBroadcastType = LogBroadcastType.CONSOLE;
    }

    /**
     * Retrieves the singleton instance of LoggerUtil.
     * <p>
     * Creates the instance on first invocation and returns the same instance on subsequent calls.
     * This method uses lazy initialization without synchronization; for thread-safe access in
     * highly concurrent environments, synchronization should be added or the pattern refactored.
     *
     * @return the singleton {@link LoggerUtil} instance
     */
    public static LoggerUtil getInstance() {
        if (instance == null) {
            instance = new LoggerUtil();
        }
        return instance;
    }

    /**
     * Logs an informational message.
     * <p>
     * Routes the message according to the current {@link #logBroadcastType}:
     * <ul>
     *   <li>To server console if broadcast type is {@link LogBroadcastType#CONSOLE} or {@link LogBroadcastType#BOTH}</li>
     *   <li>To authorized in-game players if broadcast type is {@link LogBroadcastType#IN_GAME} or {@link LogBroadcastType#BOTH}</li>
     * </ul>
     * <p>
     * The message is sent without color prefix for info-level logs.
     *
     * @param message the log message to send; should not be null to avoid exceptions
     */
    public void info(String message) {
        if(logBroadcastType == LogBroadcastType.CONSOLE || logBroadcastType == LogBroadcastType.BOTH)
            Main.getInstance().getLogger().info(message);
        if(logBroadcastType == LogBroadcastType.IN_GAME || logBroadcastType == LogBroadcastType.BOTH)
            sendLogMessageBukkit(message);
    }

    /**
     * Logs a warning message with yellow color highlighting.
     * <p>
     * Routes the message according to the current {@link #logBroadcastType}:
     * <ul>
     *   <li>To server console if broadcast type is {@link LogBroadcastType#CONSOLE} or {@link LogBroadcastType#BOTH}</li>
     *   <li>To authorized in-game players if broadcast type is {@link LogBroadcastType#IN_GAME} or {@link LogBroadcastType#BOTH}</li>
     * </ul>
     * <p>
     * The message is automatically prefixed with the yellow color code ({@code &e}) for emphasis.
     *
     * @param message the warning message to send; should not be null to avoid exceptions
     */
    public void warning(String message) {
        if(logBroadcastType == LogBroadcastType.CONSOLE || logBroadcastType == LogBroadcastType.BOTH)
            Main.getInstance().getLogger().warning("&e" + message);
        if(logBroadcastType == LogBroadcastType.IN_GAME || logBroadcastType == LogBroadcastType.BOTH)
            sendLogMessageBukkit("&e" + message);
    }

    /**
     * Logs a severe error message with red color highlighting.
     * <p>
     * Routes the message according to the current {@link #logBroadcastType}:
     * <ul>
     *   <li>To server console if broadcast type is {@link LogBroadcastType#CONSOLE} or {@link LogBroadcastType#BOTH}</li>
     *   <li>To authorized in-game players if broadcast type is {@link LogBroadcastType#IN_GAME} or {@link LogBroadcastType#BOTH}</li>
     * </ul>
     * <p>
     * The message is automatically prefixed with the red color code ({@code &#FF0000}) to indicate severity.
     *
     * @param message the error message to send; should not be null to avoid exceptions
     */
    public void severe(String message) {
        if(logBroadcastType == LogBroadcastType.CONSOLE || logBroadcastType == LogBroadcastType.BOTH)
            Main.getInstance().getLogger().severe("&#FF0000" + message);
        if(logBroadcastType == LogBroadcastType.IN_GAME || logBroadcastType == LogBroadcastType.BOTH)
            sendLogMessageBukkit("&#FF0000" + message);
    }

    /**
     * Broadcasts a log message to all connected players with appropriate permissions.
     * <p>
     * Iterates through all online players and sends the message only to those who have
     * either the {@code nextdungeon.debug} permission or are server operators. The message
     * is translated using {@link ChatUtil#translate(String)} to support color codes and
     * formatting.
     * <p>
     * <strong>Permission Check:</strong> A player receives the message if:
     * <ul>
     *   <li>They have the {@code nextdungeon.debug} permission, OR</li>
     *   <li>They are a server operator ({@code isOp()})</li>
     * </ul>
     * <p>
     * This method is private and called internally by {@code info()}, {@code warning()},
     * and {@code severe()} when in-game broadcasting is enabled.
     *
     * @param message the message to send to players; color codes (e.g., {@code &e}, {@code &#FF0000})
     *                are translated by {@link ChatUtil#translate(String)}
     * @see ChatUtil#translate(String)
     */
    private void sendLogMessageBukkit(String message) {
        Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.hasPermission("nextdungeon.debug")||player.isOp())
                .forEach(player -> player.sendMessage(ChatUtil.translate(message)));
    }

    /**
     * Enumeration defining where log messages are broadcast.
     * <p>
     * This enum controls the routing destination for all logging methods in {@link LoggerUtil}.
     */
    public enum LogBroadcastType {
        /**
         * Logs are sent only to the server console.
         * <p>
         * Messages are passed to the Bukkit logger (info, warning, severe methods depending on level).
         */
        CONSOLE,

        /**
         * Logs are sent only to authorized in-game players.
         * <p>
         * Messages are broadcast via chat to players with the {@code nextdungeon.debug}
         * permission or operator status.
         */
        IN_GAME,

        /**
         * Logs are sent to both the server console and authorized in-game players.
         * <p>
         * This is the most comprehensive logging mode, useful for development and debugging
         * with multiple audience channels.
         */
        BOTH
    }
}
