package fr.perrier.dungeons.spigot.utils;

import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.dungeons.spigot.Main;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;

public class LoggerUtil {
    private static LoggerUtil instance;
    @Getter
    @Setter
    private boolean debugEnabled;
    @Getter
    @Setter
    private LogBroadcastType logBroadcastType;


    private LoggerUtil() {
        debugEnabled = false;
        logBroadcastType = LogBroadcastType.CONSOLE;
    }

    public static LoggerUtil getInstance() {
        if (instance == null) {
            instance = new LoggerUtil();
        }
        return instance;
    }

    public void info(String message) {
        if(logBroadcastType == LogBroadcastType.CONSOLE || logBroadcastType == LogBroadcastType.BOTH)
            Main.getInstance().getLogger().info(message);
        if(logBroadcastType == LogBroadcastType.IN_GAME || logBroadcastType == LogBroadcastType.BOTH)
            sendLogMessageBukkit(message);
    }

    public void warning(String message) {
        if(logBroadcastType == LogBroadcastType.CONSOLE || logBroadcastType == LogBroadcastType.BOTH)
            Main.getInstance().getLogger().warning("&e" + message);
        if(logBroadcastType == LogBroadcastType.IN_GAME || logBroadcastType == LogBroadcastType.BOTH)
            sendLogMessageBukkit("&e" + message);
    }

    public void severe(String message) {
        if(logBroadcastType == LogBroadcastType.CONSOLE || logBroadcastType == LogBroadcastType.BOTH)
            Main.getInstance().getLogger().severe("&#FF0000" + message);
        if(logBroadcastType == LogBroadcastType.IN_GAME || logBroadcastType == LogBroadcastType.BOTH)
            sendLogMessageBukkit("&#FF0000" + message);
    }

    private void sendLogMessageBukkit(String message) {
        Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.hasPermission("nextdungeon.debug")||player.isOp())
                .forEach(player -> player.sendMessage(ChatUtil.translate(message)));
    }

    public enum LogBroadcastType {
        CONSOLE,
        IN_GAME,
        BOTH
    }
}
