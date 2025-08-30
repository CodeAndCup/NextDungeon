package fr.perrier.dungeons.model;

import com.google.gson.reflect.TypeToken;
import fr.perrier.cupcodeapi.utils.TimeUtil;
import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.utils.GsonProvider;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.*;

@Getter
@Setter
public class ProfileData {

    private UUID playerId;
    private String displayName;

    private final List<String> completedFloors;
    private final List<FloorStats> floorStats;

    private boolean autoReady;

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

    public void addFloorStat(FloorStats stats) {
        Optional<FloorStats> existingStats = this.floorStats.stream()
                .filter(s -> s.getFloorId().equals(stats.getFloorId()))
                .findFirst();

        if (existingStats.isPresent()) {
            FloorStats existing = existingStats.get();
            existing.setTotalDeaths(existing.getTotalDeaths() + stats.getTotalDeaths());
            existing.setTotalEnemiesKilled(existing.getTotalEnemiesKilled() + stats.getTotalEnemiesKilled());
            if (existing.getFastestCompletionTime() == -1 || (stats.getFastestCompletionTime() != -1 && stats.getFastestCompletionTime() < existing.getFastestCompletionTime())) {
                existing.setFastestCompletionTime(stats.getFastestCompletionTime());
            }
        } else {
            this.floorStats.add(stats);
        }
        Main.getInstance().getProfileService().syncProfileData(playerId, this);
    }

    public void addCompletedFloor(String floorId) {
        if (!this.completedFloors.contains(floorId)) {
            this.completedFloors.add(floorId);
        }
        Main.getInstance().getProfileService().syncProfileData(playerId, this);
    }

    @Setter
    @Getter
    public static class FloorStats {
        private final String floorId;
        private long fastestCompletionTime;
        private int totalEnemiesKilled;
        private int totalDeaths;

        public FloorStats(String floorId) {
            this.floorId = floorId;
            this.fastestCompletionTime = -1;
            this.totalEnemiesKilled = 0;
            this.totalDeaths = 0;
        }

        public FloorStats(String floorId, long fastestCompletionTime, int totalEnemiesKilled, int totalDeaths) {
            this.floorId = floorId;
            this.fastestCompletionTime = fastestCompletionTime;
            this.totalEnemiesKilled = totalEnemiesKilled;
            this.totalDeaths = totalDeaths;
        }

        /**
         * Get a summary of the floor stats for display purposes.
         * @return A list of strings representing the stats summary.
         */
        public List<String> getStatsSummary() {
            return List.of(
                "&7Fastest Time: &a" + TimeUtil.getDuration(fastestCompletionTime),
                "&7Total Enemies Killed: &a" + totalEnemiesKilled,
                "&7Total Deaths: &a" + totalDeaths
            );
        }

        /** Serialization / Deserialization */
        private @NotNull Map<String, Object> serialize() {
            Map<String, Object> data = new HashMap<>();

            data.put("floorId", this.floorId);
            data.put("fastestCompletionTime", this.fastestCompletionTime);
            data.put("totalEnemiesKilled", this.totalEnemiesKilled);
            data.put("totalDeaths", this.totalDeaths);

            return data;
        }

        @SuppressWarnings("unchecked")
        private static @NotNull FloorStats deserialize(Map<String, Object> data) {
            return new FloorStats(
                    (String) data.get("floorId"),
                    ((Number) data.get("fastestCompletionTime")).longValue(),
                    ((Number) data.get("totalEnemiesKilled")).intValue(),
                    ((Number) data.get("totalDeaths")).intValue()
            );
        }
    }

    /** Serialization / Deserialization */
    public String toJson() {
        Map<String, Object> data = this.serialize();
        return GsonProvider.GSON.toJson(data);
    }

    public static ProfileData fromJson(String json) {
        Type type = new TypeToken<Map<String, Object>>(){}.getType();
        Map<String, Object> data = GsonProvider.GSON.fromJson(json, type);
        return deserialize(data);
    }

    private @NotNull Map<String, Object> serialize() {
        Map<String, Object> data = new HashMap<>();

        data.put("playerId", this.playerId.toString());
        data.put("displayName", this.displayName);
        data.put("completedFloors", this.completedFloors);
        data.put("floorStats", this.floorStats);
        data.put("autoReady", this.autoReady);

        return data;
    }

    @SuppressWarnings("unchecked")
    private static @NotNull ProfileData deserialize(Map<String, Object> data) {
        return new ProfileData(
                UUID.fromString((String) data.get("playerId")),
                (String) data.get("displayName"),
                (List<String>) data.get("completedFloors"),
                (List<FloorStats>) data.get("floorStats"),
                (boolean) data.get("autoReady")
        );
    }
}
