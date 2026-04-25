package fr.perrier.dungeons.module.labyrinth.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persisted Infinite-only checkpoint snapshot.
 *
 * <p>Saved automatically at every boss kill (CDC §6.3) and loaded at the
 * lobby of an Infinite run if {@code partyHash} matches the initial party
 * composition (CDC §1.4, Q2 = A).</p>
 *
 * <p>{@code checksum} is a sha256 of the canonical payload (every field
 * except {@code checksum} itself). It is recomputed at every save and
 * verified at every load — any tampering refuses the resume (CDC §8).</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class LabyrinthSave implements Serializable {

    private static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .disableHtmlEscaping()
            .create();

    private String id;
    private String floorId;

    /** sha256(sorted(initialPlayerUuids)) — see {@link #computePartyHash(java.util.Collection)}. */
    private String partyHash;

    private List<UUID> playerUuids = new ArrayList<>();

    /** Always a multiple of 10 — saves are created at boss kill (CDC §4.3). */
    private int lastBossClearedRoom;

    private int difficultyTier;

    private long seed;

    private Map<RewardIcon, Integer> iconCounts = new EnumMap<>(RewardIcon.class);

    private long createdAt;
    private long updatedAt;

    /** sha256 of the canonical payload (all fields except this one). */
    private String checksum;

    /**
     * Compute the party hash that keys this save.
     *
     * <p>Sort UUIDs to make the hash deterministic regardless of join order ;
     * use lowercase string form for stability across JVM versions.</p>
     */
    public static String computePartyHash(java.util.Collection<UUID> playerUuids) {
        List<String> sorted = new ArrayList<>();
        for (UUID id : playerUuids) sorted.add(id.toString());
        Collections.sort(sorted);
        return sha256(String.join(",", sorted));
    }

    /**
     * Recompute and store the checksum. Call this right before persisting
     * the save (and never trust an externally-provided checksum on load).
     */
    public void recomputeChecksum() {
        this.checksum = null;
        this.checksum = sha256(GSON.toJson(this));
    }

    /**
     * Verify the stored checksum matches the canonical payload. Returns
     * {@code false} if the save was tampered with after persistence.
     */
    public boolean verifyChecksum() {
        String stored = this.checksum;
        if (stored == null) return false;
        this.checksum = null;
        String recomputed = sha256(GSON.toJson(this));
        this.checksum = stored;
        return stored.equals(recomputed);
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
