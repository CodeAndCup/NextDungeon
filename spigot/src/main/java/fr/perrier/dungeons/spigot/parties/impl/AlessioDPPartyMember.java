package fr.perrier.dungeons.spigot.parties.impl;

import com.alessiodp.parties.api.interfaces.PartyPlayer;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.parties.IPartyMember;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Implementation of IPartyMember that wraps AlessioDPParties' PartyPlayer.
 */
public class AlessioDPPartyMember implements IPartyMember {

    private final PartyPlayer partyPlayer;

    public AlessioDPPartyMember(PartyPlayer partyPlayer) {
        this.partyPlayer = partyPlayer;
    }

    /**
     * Creates an AlessioDPPartyMember from a UUID.
     *
     * @param uuid the UUID of the player
     * @return the party member, or null if not found
     */
    public static AlessioDPPartyMember fromUUID(UUID uuid) {
        if (Main.getInstance().getPartiesAPI() == null) return null;
        PartyPlayer pp = Main.getInstance().getPartiesAPI().getPartyPlayer(uuid);
        return pp != null ? new AlessioDPPartyMember(pp) : null;
    }

    /**
     * Creates an AlessioDPPartyMember from a Player.
     *
     * @param player the player
     * @return the party member, or null if not found
     */
    public static AlessioDPPartyMember fromPlayer(Player player) {
        return fromUUID(player.getUniqueId());
    }

    @Override
    public UUID getUniqueId() {
        return partyPlayer.getPlayerUUID();
    }

    @Override
    public String getName() {
        return partyPlayer.getName();
    }

    @Override
    public boolean isOnline() {
        return Bukkit.getPlayer(partyPlayer.getPlayerUUID()) != null;
    }

    @Override
    public Player getPlayer() {
        return Bukkit.getPlayer(partyPlayer.getPlayerUUID());
    }

    /**
     * Gets the underlying PartyPlayer object.
     *
     * @return the PartyPlayer
     */
    public PartyPlayer getPartyPlayer() {
        return partyPlayer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlessioDPPartyMember that = (AlessioDPPartyMember) o;
        return partyPlayer.getPlayerUUID().equals(that.partyPlayer.getPlayerUUID());
    }

    @Override
    public int hashCode() {
        return partyPlayer.getPlayerUUID().hashCode();
    }
}
