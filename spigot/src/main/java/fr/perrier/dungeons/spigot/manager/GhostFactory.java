package fr.perrier.dungeons.spigot.manager;

import fr.perrier.dungeons.spigot.Main;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Manages ghost players that can see each other and are invisible to living players.
 *
 * @author <a href="https://bukkit.org/threads/lib-ghostfactory-make-players-look-like-ghosts.149088/">This Forum</a>
 */
public class GhostFactory {

    private static final String GHOST_TEAM_NAME = "Ghosts";
    private static final long UPDATE_DELAY = 20L;

    private static final OfflinePlayer[] EMPTY_PLAYERS = new OfflinePlayer[0];
    private Team ghostTeam;

    private BukkitTask task;
    @Getter
    private boolean closed;


    public GhostFactory() {
        // Initialize
        createTask();
        createGetTeam();
    }

    private void createGetTeam() {
        Scoreboard board = Objects.requireNonNull(Bukkit.getServer().getScoreboardManager()).getMainScoreboard();

        ghostTeam = board.getTeam(GHOST_TEAM_NAME);

        if (ghostTeam == null) {
            ghostTeam = board.registerNewTeam(GHOST_TEAM_NAME);
        }
        ghostTeam.setCanSeeFriendlyInvisibles(true);
    }

    private void createTask() {
        task = Bukkit.getScheduler().runTaskTimer(Main.getInstance(), () -> {
            OfflinePlayer[] members = getMembers();
            for (OfflinePlayer member : members) {
                Player player = member.getPlayer();

                if (player != null) {
                    setGhost(player, isGhost(player));
                } else {
                    ghostTeam.removeEntry(Objects.requireNonNull(member.getName()));
                }
            }
        }, UPDATE_DELAY, UPDATE_DELAY);
    }

    /**
     * Add the given player to this ghost manager. This ensures that it can see ghosts, and later become one.
     * @param player - the player to add to the ghost manager.
     */
    public void addPlayer(Player player) {
        validateState();
        if (!ghostTeam.hasEntry(player.getName())) {
            ghostTeam.addEntry(player.getName());
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 15,false, false,false));
        }
    }

    /**
     * Determine if the given player is tracked by this ghost manager and is a ghost.
     * @param player - the player to test.
     * @return TRUE if it is, FALSE otherwise.
     */
    public boolean isGhost(Player player) {
        return ghostTeam.hasEntry(player.getName());
    }

    /**
     * Determine if the current player is tracked by this ghost manager, or is a ghost.
     * @param player - the player to check.
     * @return TRUE if it is, FALSE otherwise.
     */
    public boolean hasPlayer(Player player) {
        validateState();
        return ghostTeam.hasEntry(player.getName());
    }

    /**
     * Set wheter or not a given player is a ghost.
     * @param player - the player to set as a ghost.
     * @param isGhost - TRUE to make the given player into a ghost, FALSE otherwise.
     */
    private void setGhost(Player player, boolean isGhost) {
        if (!hasPlayer(player))
            addPlayer(player);

        if (isGhost) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 15,false, false,false));
        } else {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
        }
    }

    /**
     * Remove the given player from the manager, turning it back into the living and making it unable to see ghosts.
     * @param player - the player to remove from the ghost manager.
     */
    public void removePlayer(Player player) {
        validateState();
        if (ghostTeam.removeEntry(player.getName())) {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
        }
    }

    /**
     * Retrieve every ghost and every player that can see ghosts.
     * @return Every ghost or every observer.
     */
    public OfflinePlayer[] getMembers() {
        validateState();
        return toArray(ghostTeam.getPlayers());
    }

    private OfflinePlayer[] toArray(Set<OfflinePlayer> players) {
        if (players != null) {
            return players.toArray(new OfflinePlayer[0]);
        } else {
            return EMPTY_PLAYERS;
        }
    }

    private void validateState() {
        if (closed) {
            throw new IllegalStateException("Ghost factory has closed. Cannot reuse instances.");
        }
    }

    /**
     * Close this ghost manager, removing every player from it and cleaning up resources.
     */
    public void close() {
        if (!closed) {
            closed = true;

            if(ghostTeam != null) {
                Set<OfflinePlayer> members = new HashSet<>(ghostTeam.getPlayers());
                for (OfflinePlayer member : members) {
                    Player player = member.getPlayer();
                    if (player != null) {
                        removePlayer(player);
                    }
                }
            }

            if (task != null) {
                task.cancel();
                task = null;
            }

            if (ghostTeam != null) {
                ghostTeam.unregister();
                ghostTeam = null;
            }
        }
    }
}
