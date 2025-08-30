package fr.perrier.dungeons.spigot.manager;

import fr.perrier.dungeons.spigot.Main;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manager for variables in the dungeon system
 */
@Getter
public class VariableManager {

    // Global variables (shared across all contexts)
    private final Map<String, Object> globalVariables = new ConcurrentHashMap<>();

    // Player-specific variables
    private final Map<UUID, Map<String, Object>> playerVariables = new ConcurrentHashMap<>();


    public VariableManager() {
        Main.getInstance().getLogger().info("Variable Manager initialized");
    }

    // ===== GLOBAL VARIABLES =====

    /**
     * Set a global variable
     */
    public void setGlobalVariable(String name, Object value) {
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        globalVariables.put(name.trim(), value);
        Main.getInstance().getLogger().info("Set global variable: " + name + " = " + value);
    }

    /**
     * Get a global variable
     */
    public Object getGlobalVariable(String name) {
        if (name == null) return null;
        return globalVariables.get(name.trim());
    }

    /**
     * Remove a global variable
     */
    public void removeGlobalVariable(String name) {
        if (name != null) {
            globalVariables.remove(name.trim());
        }
    }

    // ===== PLAYER VARIABLES =====

    /**
     * Set a player-specific variable
     */
    public void setPlayerVariable(Player player, String name, Object value) {
        if (player == null || name == null || name.trim().isEmpty()) {
            return;
        }

        UUID playerId = player.getUniqueId();
        playerVariables.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        playerVariables.get(playerId).put(name.trim(), value);

        Main.getInstance().getLogger().info("Set player variable for " + player.getName() + ": " + name + " = " + value);
    }

    /**
     * Get a player-specific variable
     */
    public Object getPlayerVariable(Player player, String name) {
        if (player == null || name == null) return null;

        Map<String, Object> vars = playerVariables.get(player.getUniqueId());
        return vars != null ? vars.get(name.trim()) : null;
    }

    /**
     * Remove a player-specific variable
     */
    public void removePlayerVariable(Player player, String name) {
        if (player == null || name == null) return;

        Map<String, Object> vars = playerVariables.get(player.getUniqueId());
        if (vars != null) {
            vars.remove(name.trim());
        }
    }

    /**
     * Clear all variables for a player
     */
    public void clearPlayerVariables(Player player) {
        if (player != null) {
            playerVariables.remove(player.getUniqueId());
            Main.getInstance().getLogger().info("Cleared all variables for player: " + player.getName());
        }
    }

    // ===== GENERAL VARIABLE ACCESS =====

    /**
     * Get a variable with scope priority: player -> instance -> global
     */
    public Object getVariable(Player player, String name) {
        if (name == null) return null;

        String trimmedName = name.trim();

        // Check player variables first
        if (player != null) {
            Object playerValue = getPlayerVariable(player, trimmedName);
            if (playerValue != null) {
                return playerValue;
            }
        }

        // Check global variables
        return getGlobalVariable(trimmedName);
    }

    /**
     * Set a variable with specified scope
     */
    public void setVariable(Player player, String name, Object value, String scope) {
        if (name == null || name.trim().isEmpty()) {
            return;
        }

        switch (scope.toLowerCase()) {
            case "player":
                if (player != null) {
                    setPlayerVariable(player, name, value);
                }
                break;
            case "global":
                setGlobalVariable(name, value);
                break;
            default:
                // Default to player if player exists, otherwise global
                if (player != null) {
                    setPlayerVariable(player, name, value);
                } else {
                    setGlobalVariable(name, value);
                }
                break;
        }
    }

    /**
     * Clear all variables
     */
    public void clearAllVariables() {
        globalVariables.clear();
        playerVariables.clear();
        Main.getInstance().getLogger().info("Cleared all variables");
    }

    /**
     * Get variable count for debugging
     */
    public int getVariableCount() {
        int total = globalVariables.size();
        for (Map<String, Object> playerVars : playerVariables.values()) {
            total += playerVars.size();
        }
        return total;
    }

    public String formatVariable(String message, Player triggerPlayer) {
        String regex = "\\{(global|player)\\.([a-zA-Z0-9_]+)\\}";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(message);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String scope = matcher.group(1);
            String varName = matcher.group(2);
            Object value = null;
            if ("global".equalsIgnoreCase(scope)) {
                value = getGlobalVariable(varName);
            } else if ("player".equalsIgnoreCase(scope) && triggerPlayer != null) {
                value = getPlayerVariable(triggerPlayer, varName);
            }
            matcher.appendReplacement(sb, value != null ? value.toString() : "null");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}