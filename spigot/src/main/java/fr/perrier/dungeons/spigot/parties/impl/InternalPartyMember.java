package fr.perrier.dungeons.spigot.parties.impl;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;
import org.bukkit.Bukkit;
import fr.perrier.dungeons.spigot.parties.IPartyMember;
/**
 * Implementation of IPartyMember for the internal party system.
 */
public class InternalPartyMember implements IPartyMember {
    private final UUID uuid;
    private String cachedName;

    public InternalPartyMember(UUID uuid) {
        this.uuid = uuid;
        this.cachedName = null;
    }

    public InternalPartyMember(Player player) {
        this.uuid = player.getUniqueId();
        this.cachedName = player.getName();
    }

    @Override
    public UUID getUniqueId() {
        return uuid;
    }

    @Override
    public String getName() {
        if (cachedName == null) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                cachedName = offlinePlayer.getName();
            } else {
                cachedName = player.getName();
            }
        }
        return cachedName;
    }

    @Override
    public boolean isOnline() {
        return Bukkit.getPlayer(uuid) != null;
    }

    @Override
    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public void refreshName() {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            cachedName = player.getName();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InternalPartyMember that = (InternalPartyMember) o;
        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return uuid.hashCode();
    }

    @Override
    public String toString() {
        return "InternalPartyMember{" +
               "uuid=" + uuid +
               ", name='" + getName() + '\'' +
               ", online=" + isOnline() +
               '}';
    }
}














