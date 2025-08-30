package fr.perrier.dungeons.webserver.blockly;

import org.bukkit.entity.Player;

import java.util.Map;

public interface BlocklyCondition {
    /**
     * Évalue la condition
     */
    boolean evaluate(Player player, Map<String, Object> data);

    /**
     * @return True si cette condition peut être inversée (NOT)
     */
    default boolean canBeNegated() {
        return true;
    }
}