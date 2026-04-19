package fr.perrier.dungeons.common.model.dungeon.config;

import com.google.gson.Gson;
import fr.perrier.dungeons.common.model.player.PlayerStats;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Getter
public class FloorInstanceData {

    public static final int CURRENT_SCHEMA_VERSION = 3;
    private static final Gson CHECKSUM_GSON = new Gson();

    protected final String floorId;
    protected UUID instanceId;
    @Setter
    protected boolean ready;

    protected final Set<UUID> players;
    protected final Map<UUID, PlayerStats> playerStats;
    protected final Map<UUID, Integer> playerCurrentLives;
    /**
     * Maps a player UUID to the cloud service UUID they were on when they entered this instance.
     * Used to send players back to their origin server when the dungeon completes instead of kicking them.
     */
    protected final Map<UUID, UUID> originInstances;

    @Setter
    protected long version = 1L;
    @Setter
    protected int schemaVersion = CURRENT_SCHEMA_VERSION;
    @Setter
    protected long updatedAt = System.currentTimeMillis();
    @Setter
    protected String updatedBy;
    @Setter
    protected String checksum;

    public FloorInstanceData(String floorId, Set<UUID> players) {
        this.floorId = floorId;
        this.ready = false;
        this.players = new HashSet<>(players);
        this.playerStats = new HashMap<>();
        this.playerCurrentLives = new HashMap<>();
        this.originInstances = new HashMap<>();
    }

    public FloorInstanceData(UUID instanceId, String floorId) {
        this.instanceId = instanceId;
        this.floorId = floorId;
        this.ready = false;
        this.players = new HashSet<>();
        this.playerStats = new HashMap<>();
        this.playerCurrentLives = new HashMap<>();
        this.originInstances = new HashMap<>();
    }

    /**
     * Gets the name of this instance.
     * <p>
     * The name is in the format of {@code <floorId>_<instanceId>}.
     * @return the name of this instance
     */
    public String getInstanceName() {
        return floorId + "_" + instanceId.toString();
    }

    public void incrementVersion(String updater) {
        this.version += 1L;
        this.updatedAt = System.currentTimeMillis();
        this.updatedBy = updater;
    }

    public String calculateChecksum() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("floorId", this.floorId);
        snapshot.put("instanceId", this.instanceId != null ? this.instanceId.toString() : null);
        snapshot.put("ready", this.ready);
        snapshot.put("players", this.players);
        snapshot.put("playerStats", this.playerStats);
        snapshot.put("playerCurrentLives", this.playerCurrentLives);
        snapshot.put("originInstances", this.originInstances);
        snapshot.put("version", this.version);
        snapshot.put("schemaVersion", this.schemaVersion);
        snapshot.put("updatedAt", this.updatedAt);
        snapshot.put("updatedBy", this.updatedBy);
        return sha256(CHECKSUM_GSON.toJson(snapshot));
    }

    public boolean verifyChecksum() {
        if (checksum == null || checksum.isEmpty()) return false;
        return checksum.equals(calculateChecksum());
    }

    public boolean isValid() {
        return floorId != null && !floorId.isEmpty()
                && version >= 1L
                && schemaVersion >= 1
                && verifyChecksum();
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    @Override
    public String toString() {
        return "FloorInstanceData{" +
               "floorId='" + floorId + '\'' +
               ", instanceId=" + instanceId +
               ", ready=" + ready +
               ", players=" + players +
               ", playerStats=" + playerStats +
               ", playerCurrentLives=" + playerCurrentLives +
               ", version=" + version +
               '}';
    }
}
