package fr.perrier.dungeons.spigot.parties;

import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Interface representing a member of a party.
 * Implementations can wrap different party API player objects.
 */
public interface IPartyMember {

    /**
     * Gets the unique identifier of the party member.
     *
     * @return the UUID of the member
     */
    UUID getUniqueId();

    /**
     * Gets the name of the party member.
     *
     * @return the name of the member
     */
    String getName();

    /**
     * Checks if the party member is currently online.
     *
     * @return true if the member is online, false otherwise
     */
    boolean isOnline();

    /**
     * Gets the Bukkit Player object if the member is online.
     *
     * @return the Player object, or null if not online
     */
    Player getPlayer();
}

