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
 * Manages variables in the dungeon system, including global and player-specific variables.
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
     * Sets a global variable.
     *
     * @param name  the variable name
     * @param value the value to set
     */
    public void setGlobalVariable(String name, Object value) {
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        globalVariables.put(name.trim(), value);
        Main.getInstance().getLogger().info("Set global variable: " + name + " = " + value);
    }

    /**
     * Retrieves a global variable.
     *
     * @param name the variable name
     * @return the value of the global variable, or null if not found
     */
    public Object getGlobalVariable(String name) {
        if (name == null) return null;
        return globalVariables.get(name.trim());
    }

    /**
     * Removes a global variable.
     *
     * @param name the variable name
     */
    public void removeGlobalVariable(String name) {
        if (name != null) {
            globalVariables.remove(name.trim());
        }
    }

    // ===== PLAYER VARIABLES =====

    /**
     * Sets a player-specific variable.
     *
     * @param player the player
     * @param name   the variable name
     * @param value  the value to set
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
     * Retrieves a player-specific variable.
     *
     * @param player the player
     * @param name   the variable name
     * @return the value of the player variable, or null if not found
     */
    public Object getPlayerVariable(Player player, String name) {
        if (player == null || name == null) return null;

        Map<String, Object> vars = playerVariables.get(player.getUniqueId());
        return vars != null ? vars.get(name.trim()) : null;
    }

    /**
     * Removes a player-specific variable.
     *
     * @param player the player
     * @param name   the variable name
     */
    public void removePlayerVariable(Player player, String name) {
        if (player == null || name == null) return;

        Map<String, Object> vars = playerVariables.get(player.getUniqueId());
        if (vars != null) {
            vars.remove(name.trim());
        }
    }

    /**
     * Clears all variables for a specific player.
     *
     * @param player the player
     */
    public void clearPlayerVariables(Player player) {
        if (player != null) {
            playerVariables.remove(player.getUniqueId());
            Main.getInstance().getLogger().info("Cleared all variables for player: " + player.getName());
        }
    }

    // ===== GENERAL VARIABLE ACCESS =====

    /**
     * Retrieves a variable with scope priority: player, then global.
     *
     * @param player the player (can be null)
     * @param name   the variable name
     * @return the value of the variable, or null if not found
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
     * Sets a variable with the specified scope.
     *
     * @param player the player (can be null)
     * @param name   the variable name
     * @param value  the value to set
     * @param scope  the scope ("player" or "global")
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
     * Clears all variables, both global and player-specific.
     */
    public void clearAllVariables() {
        globalVariables.clear();
        playerVariables.clear();
        Main.getInstance().getLogger().info("Cleared all variables");
    }

    /**
     * Gets the total count of all variables (global and player-specific).
     *
     * @return the total number of variables
     */
    public int getVariableCount() {
        int total = globalVariables.size();
        for (Map<String, Object> playerVars : playerVariables.values()) {
            total += playerVars.size();
        }
        return total;
    }

    /**
     * Formats a message by replacing variable placeholders with their values.
     * Placeholders must be in the format {global.varName} or {player.varName}.
     *
     * @param message       the message containing placeholders
     * @param triggerPlayer the player for player-scoped variables
     * @return the formatted message with variables replaced
     */
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