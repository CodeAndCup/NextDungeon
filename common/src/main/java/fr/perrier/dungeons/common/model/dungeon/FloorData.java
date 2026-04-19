package fr.perrier.dungeons.common.model.dungeon;

import com.google.gson.Gson;
import fr.perrier.dungeons.common.model.dungeon.config.Requirements;
import fr.perrier.dungeons.common.model.dungeon.config.Rules;
import fr.perrier.dungeons.common.model.dungeon.config.WorldConfig;
import fr.perrier.dungeons.common.workflow.trigger.TriggerData;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Getter
@Setter
public class FloorData {

    public static final int CURRENT_SCHEMA_VERSION = 2;
    private static final Gson CHECKSUM_GSON = new Gson();

    private String id;
    /** ID du donjon parent. Utilisé par le dashboard pour grouper les floors sans regex fragile. */
    private String dungeonId;

    private String name;
    private String description;

    private WorldConfig worldConfig;
    private Requirements requirements;
    private Rules rules;
    private List<Step> steps;
    private List<TriggerData> triggers;

    private long version = 1L;
    private int schemaVersion = CURRENT_SCHEMA_VERSION;
    private long updatedAt = System.currentTimeMillis();
    private String updatedBy;
    private String checksum;

    public FloorData() {
    }

    public FloorData(String id, String name) {
        this.id = id;
        this.name = name;
        this.description = "&#FF0000No description";
    }

    public FloorData(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public FloorData(String id, String name, String description,
                     WorldConfig worldConfig, Requirements requirements,
                     Rules rules, List<Step> steps, List<TriggerData> triggers) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.worldConfig = worldConfig;
        this.requirements = requirements;
        this.rules = rules;
        this.steps = steps;
        this.triggers = triggers;
    }

    public void incrementVersion(String updater) {
        this.version += 1L;
        this.updatedAt = System.currentTimeMillis();
        this.updatedBy = updater;
    }

    /**
     * Calculates the checksum over every field EXCEPT {@code checksum} itself
     * AND {@code triggers}. Triggers live in their own table ({@code floor_triggers})
     * with an independent lifecycle — including them here causes three problems:
     * <ul>
     *   <li>The checksum persisted in {@code floors.checksum} is computed before
     *       triggers are loaded, so any later trigger attach makes it mismatch.</li>
     *   <li>Health monitor samples then log false-positive corruptions.</li>
     *   <li>Proxy (Velocity) reads of this payload blow up on Spigot-only
     *       trigger classes — they must never cross that boundary.</li>
     * </ul>
     * Trigger integrity is enforced separately by {@code DatabaseTriggersManager}.
     */
    public String calculateChecksum() {
        FloorData copy = new FloorData();
        copy.id = this.id;
        copy.dungeonId = this.dungeonId;
        copy.name = this.name;
        copy.description = this.description;
        copy.worldConfig = this.worldConfig;
        copy.requirements = this.requirements;
        copy.rules = this.rules;
        copy.steps = this.steps;
        copy.triggers = null;
        copy.version = this.version;
        copy.schemaVersion = this.schemaVersion;
        copy.updatedAt = this.updatedAt;
        copy.updatedBy = this.updatedBy;
        copy.checksum = null;
        return sha256(CHECKSUM_GSON.toJson(copy));
    }

    public boolean verifyChecksum() {
        if (checksum == null || checksum.isEmpty()) return false;
        return checksum.equals(calculateChecksum());
    }

    public boolean isValid() {
        return id != null && !id.isEmpty()
                && name != null
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
        return "FloorData{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", worldConfig=" + worldConfig +
                ", requirements=" + requirements +
                ", rules=" + rules +
                ", steps=" + steps +
                ", triggers=" + triggers +
                ", version=" + version +
                ", schemaVersion=" + schemaVersion +
                ", updatedAt=" + updatedAt +
                ", updatedBy='" + updatedBy + '\'' +
                '}';
    }
}
