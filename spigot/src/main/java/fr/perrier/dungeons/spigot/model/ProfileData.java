package fr.perrier.dungeons.spigot.model;

import com.google.gson.reflect.TypeToken;
import fr.perrier.cupcodeapi.utils.TimeUtil;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.utils.GsonProvider;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.units.qual.A;
import org.checkerframework.checker.units.qual.N;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Getter
@Setter
public class ProfileData {

    public static final int CURRENT_SCHEMA_VERSION = 2;

    private UUID playerId;
    private String displayName;

    private final List<String> completedFloors;
    private final List<FloorStats> floorStats;

    private boolean autoReady;

    private long version = 1L;
    private int schemaVersion = CURRENT_SCHEMA_VERSION;
    private long updatedAt = System.currentTimeMillis();
    private String updatedBy;
    private String checksum;

    public ProfileData(UUID playerId) {
        this.playerId = playerId;
        this.displayName = "";
        this.completedFloors = new ArrayList<>();
        this.floorStats = new ArrayList<>();
        this.autoReady = false;
    }

    private ProfileData(UUID playerId, String displayName, List<String> completedFloors, List<FloorStats> floorStats, boolean autoReady) {
        this.playerId = playerId;
        this.displayName = displayName;
        this.completedFloors = completedFloors;
        this.floorStats = floorStats;
        this.autoReady = autoReady;
    }

    public void incrementVersion(String updater) {
        this.version += 1L;
        this.updatedAt = System.currentTimeMillis();
        this.updatedBy = updater;
    }

    public String calculateChecksum() {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("playerId", playerId != null ? playerId.toString() : null);
        snapshot.put("displayName", displayName);
        snapshot.put("completedFloors", completedFloors);
        List<Map<String, Object>> stats = new ArrayList<>();
        for (FloorStats s : floorStats) {
            stats.add(s.serialize());
        }
        snapshot.put("floorStats", stats);
        snapshot.put("autoReady", autoReady);
        snapshot.put("version", version);
        snapshot.put("schemaVersion", schemaVersion);
        snapshot.put("updatedAt", updatedAt);
        snapshot.put("updatedBy", updatedBy);
        return sha256(GsonProvider.GSON.toJson(snapshot));
    }

    public boolean verifyChecksum() {
        if (checksum == null || checksum.isEmpty()) return false;
        return checksum.equals(calculateChecksum());
    }

    public boolean isValid() {
        return playerId != null && version >= 1L && schemaVersion >= 1 && verifyChecksum();
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

    /**
     * Add or update floor stats.
     * If the floor stats for the given floorId already exist, update them by adding the new stats.
     * Otherwise, add the new stats to the list.
     *
     * @param stats The FloorStats to add or update.
     */
    public void addFloorStat(FloorStats stats) {
        Optional<FloorStats> existingStats = this.floorStats.stream()
                .filter(s -> s.getFloorId().equals(stats.getFloorId()))
                .findFirst();

        if (existingStats.isPresent()) {
            FloorStats existing = existingStats.get();
            if (existing.getFastestCompletionTime() == -1 || (stats.getFastestCompletionTime() != -1 && stats.getFastestCompletionTime() < existing.getFastestCompletionTime())) {
                existing.setFastestCompletionTime(stats.getFastestCompletionTime());
            }
            existing.setTotalEnemiesKilled(existing.getTotalEnemiesKilled() + stats.getTotalEnemiesKilled());
            if(existing.getMostEnemiesKilledInRun() < stats.getMostEnemiesKilledInRun()) {
                existing.setMostEnemiesKilledInRun(stats.getMostEnemiesKilledInRun());
            }
            existing.setTotalDeaths(existing.getTotalDeaths() + stats.getTotalDeaths());
            if(existing.getMostDeathsInRun() < stats.getMostDeathsInRun()) {
                existing.setMostDeathsInRun(stats.getMostDeathsInRun());
            }
            existing.setTotalRuns(existing.getTotalRuns() + stats.getTotalRuns());
            existing.setTotalCompletions(existing.getTotalCompletions() + stats.getTotalCompletions());
        } else {
            this.floorStats.add(stats);
        }
        Main.getInstance().getProfileService().syncProfileData(playerId, this);
    }

    /**
     * Add a completed floor to the profile.
     * If the floor is already marked as completed, do nothing.
     *
     * @param floorId The ID of the completed floor.
     */
    public void addCompletedFloor(String floorId) {
        if (!this.completedFloors.contains(floorId)) {
            this.completedFloors.add(floorId);
        }
        Main.getInstance().getProfileService().syncProfileData(playerId, this);
    }

    @Setter
    @Getter
    @AllArgsConstructor
    public static class FloorStats {
        private final String floorId;
        private long fastestCompletionTime;
        private int totalEnemiesKilled;
        private int mostEnemiesKilledInRun;
        private int totalDeaths;
        private int mostDeathsInRun;
        private int totalRuns;
        private int totalCompletions;

        public FloorStats(String floorId) {
            this.floorId = floorId;
            this.fastestCompletionTime = -2L;
            this.totalEnemiesKilled = 0;
            this.mostEnemiesKilledInRun = 0;
            this.totalDeaths = 0;
            this.mostDeathsInRun = 0;
            this.totalRuns = 0;
            this.totalCompletions = 0;
        }

        /**
         * Get a summary of the floor stats for display purposes.
         * @return A list of strings representing the stats summary.
         */
        public List<String> getStatsSummary() {
            return List.of(
                "&7Fastest Time: &#00FF00" + TimeUtil.getDuration(fastestCompletionTime),
                "&7Total Enemies Killed: &#00FF00" + totalEnemiesKilled,
                "&7Most Enemies Killed in a Run: &#00FF00" + mostEnemiesKilledInRun,
                "&7Total Deaths: &#00FF00" + totalDeaths,
                "&7Most Deaths in a Run: &#00FF00" + mostDeathsInRun,
                "&7Total Runs: &#00FF00" + totalRuns,
                "&7Total Completions: &#00FF00" + totalCompletions
            );
        }

        /** Serialization / Deserialization */
        @NotNull Map<String, Object> serialize() {
            Map<String, Object> data = new HashMap<>();

            data.put("floorId", this.floorId);
            data.put("fastestCompletionTime", this.fastestCompletionTime);
            data.put("totalEnemiesKilled", this.totalEnemiesKilled);
            data.put("mostEnemiesKilledInRun", this.mostEnemiesKilledInRun);
            data.put("totalDeaths", this.totalDeaths);
            data.put("mostDeathsInRun", this.mostDeathsInRun);
            data.put("totalRuns", this.totalRuns);
            data.put("totalCompletions", this.totalCompletions);

            return data;
        }

        /**
         * Deserialize a FloorStats object from a map of data.
         *
         * @param data The map containing the serialized data.
         * @return A FloorStats object.
         */
        private static @NotNull FloorStats deserialize(Map<String, Object> data) {
            return new FloorStats(
                    (String) data.get("floorId"),
                    ((Number) data.get("fastestCompletionTime")).longValue(),
                    ((Number) data.get("totalEnemiesKilled")).intValue(),
                    ((Number) data.get("mostEnemiesKilledInRun")).intValue(),
                    ((Number) data.get("totalDeaths")).intValue(),
                    ((Number) data.get("mostDeathsInRun")).intValue(),
                    ((Number) data.get("totalRuns")).intValue(),
                    ((Number) data.get("totalCompletions")).intValue()
            );
        }
    }

    /** Serialization / Deserialization */
    /**
     * Serialize the ProfileData object to a JSON string.
     *
     * @return A JSON string representing the serialized data.
     */
    public String toJson() {
        Map<String, Object> data = this.serialize();
        return GsonProvider.GSON.toJson(data);
    }

    /**
     * Deserialize a ProfileData object from a JSON string.
     *
     * @param json The JSON string containing the serialized data.
     * @return A ProfileData object.
     */
    public static ProfileData fromJson(String json) {
        Type type = new TypeToken<Map<String, Object>>(){}.getType();
        Map<String, Object> data = GsonProvider.GSON.fromJson(json, type);
        return deserialize(data);
    }

    /**
     * Serialize the ProfileData object to a map of data.
     *
     * @return A map representing the serialized data.
     */
    private @NotNull Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();

        data.put("playerId", this.playerId.toString());
        data.put("displayName", this.displayName);
        data.put("completedFloors", this.completedFloors);
        data.put("floorStats", this.floorStats);
        data.put("autoReady", this.autoReady);
        data.put("version", this.version);
        data.put("schemaVersion", this.schemaVersion);
        data.put("updatedAt", this.updatedAt);
        data.put("updatedBy", this.updatedBy);
        data.put("checksum", this.checksum);

        return data;
    }

    /**
     * Deserialize a ProfileData object from a map of data.
     *
     * @param data The map containing the serialized data.
     * @return A ProfileData object.
     */
    @SuppressWarnings("unchecked")
    private static @NotNull ProfileData deserialize(Map<String, Object> data) {
        List<FloorStats> floorStats = new ArrayList<>();
        List<?> rawList = (List<?>) data.get("floorStats");
        for(Object obj : rawList) {
            if(obj instanceof Map map) {
                floorStats.add(FloorStats.deserialize((Map<String, Object>) map));
            }
        }

        ProfileData profile = new ProfileData(
                UUID.fromString((String) data.get("playerId")),
                (String) data.get("displayName"),
                (List<String>) data.get("completedFloors"),
                floorStats,
                (boolean) data.get("autoReady")
        );
        Object versionObj = data.get("version");
        if (versionObj instanceof Number) profile.version = ((Number) versionObj).longValue();
        Object schemaObj = data.get("schemaVersion");
        if (schemaObj instanceof Number) profile.schemaVersion = ((Number) schemaObj).intValue();
        Object updatedAtObj = data.get("updatedAt");
        if (updatedAtObj instanceof Number) profile.updatedAt = ((Number) updatedAtObj).longValue();
        profile.updatedBy = (String) data.get("updatedBy");
        profile.checksum = (String) data.get("checksum");
        return profile;
    }
}
