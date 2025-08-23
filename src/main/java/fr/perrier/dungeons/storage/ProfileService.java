package fr.perrier.dungeons.storage;

import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.model.ProfileData;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import java.util.UUID;

@RequiredArgsConstructor
public class ProfileService {
    private final RedissonClient redissonClient;

    // Redis Maps and Topics
    private static final String PROFILE_MAP = "dungeons:profiles";

    // Redis Maps
    @Getter
    private RMap<UUID, ProfileData> profilesMap;

    /**
     * Initializes the ProfileService by setting up the Redis map for profile data.
     * This method should be called during the plugin's initialization phase.
     */
    public void initialize() {
        this.profilesMap = redissonClient.getMap(PROFILE_MAP);
    }

    /**
     * Synchronizes the given profile data to Redis.
     * This method will update the Redis profile map with the given profile data.
     *
     * @param playerId    the unique ID of the player whose profile data is being synchronized
     * @param profileData the profile data to synchronize
     */
    public void syncProfileData(UUID playerId, ProfileData profileData) {
        // Update Redis
        profilesMap.fastPut(playerId, profileData);

        Main.getInstance().getLogger().info(String.format(
                "Synced profile data for player %s to Redis",
                playerId
        ));
    }

    /**
     * Check if a player's profile data is cached in Redis.
     *
     * @param playerId the unique ID of the player to check
     * @return true if the player's profile data is cached, false otherwise
     */
    public boolean hasCachedProfileData(UUID playerId) {
        return profilesMap.containsKey(playerId);
    }

    /**
     * Retrieve a player's profile data by their unique ID.
     *
     * @param playerId the unique ID of the player whose profile data is being retrieved
     * @return the ProfileData object for the given player ID, or null if not found
     */
    public ProfileData getProfileData(UUID playerId) {
        if(!profilesMap.containsKey(playerId)) {
            ProfileData profileData = Main.getInstance().getDatabaseManager().loadProfileData(playerId);
            profilesMap.fastPut(playerId, profileData);
            return profileData;
        }
        return profilesMap.get(playerId);
    }

    /**
     * Save a player's profile data to the database and remove it from Redis cache.
     *
     * @param playerId the unique ID of the player whose profile data is being saved
     */
    public void saveProfileData(UUID playerId) {
        ProfileData profileData = profilesMap.get(playerId);
        if(profileData != null) {
            Main.getInstance().getDatabaseManager().saveProfileData(playerId, profileData);
            Main.getInstance().getLogger().info(String.format(
                    "Saved profile data for player %s to Database",
                    playerId
            ));
            profilesMap.fastRemove(playerId);
            Main.getInstance().getLogger().info(String.format(
                    "Removed profile data for player %s from Redis cache",
                    playerId
            ));
        } else {
            Main.getInstance().getLogger().warning(String.format(
                    "No profile data found for player %s to save",
                    playerId
            ));
        }
    }
}
