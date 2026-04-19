package fr.perrier.dungeons.common.model.party;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Serializable snapshot of a dungeon party shared between servers via Redis.
 * Carries the minimum information needed for:
 * - Cross-server party finder listings
 * - Routing join requests to the owner server via Pidgin packets
 *
 * The actual {@code IParty} (with Bukkit references) lives only on the owner server and is not
 * part of this payload.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DungeonPartyData implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID partyId;
    private UUID leaderId;
    private Set<UUID> memberIds;

    /**
     * Display name of the party leader. Cached in the payload because peer servers can't resolve
     * it locally via {@code Bukkit.getOfflinePlayer} when the leader has never joined them.
     */
    private String leaderName;

    /**
     * Mojang skin texture URL for the party leader. Extracted on the owner server (where the
     * leader is always online) so peer servers can render the real head in the party finder
     * instead of the default Steve/Alex skin they'd get from an unknown OfflinePlayer.
     */
    private String leaderSkinUrl;

    /**
     * Display names of party members, keyed by UUID. Same rationale as {@link #leaderName}.
     */
    private Map<UUID, String> memberNames;

    /**
     * MMOCore class name per member (owner server snapshot for local members, cached from the
     * join packet for cross-server members). Null when unknown.
     */
    private Map<UUID, String> memberClasses;

    /**
     * MMOCore level per member. Same provenance as {@link #memberClasses}.
     */
    private Map<UUID, Integer> memberLevels;

    private String partyName;
    private String dungeonId;
    private String floorId;
    private int minLevel;
    private String description;

    /**
     * Cloud service UUID of the server that actually holds the live IParty object.
     * Join/leave/disband mutations must be routed to this server.
     */
    private UUID ownerServiceId;

    /**
     * Millis timestamp of the last heartbeat from the owner server. Used to evict
     * parties whose owner has crashed without cleanly disbanding them.
     */
    private long lastHeartbeat;

    /**
     * Whether this party should be surfaced in the party finder UI. The party itself always
     * exists (members stay together between dungeons), but listing is a separate flag — set to
     * false when the leader starts a dungeon so peers don't try to join a party mid-run.
     * Re-listing is driven by the leader through the party builder.
     */
    private boolean listed = true;

    public DungeonPartyData(UUID partyId, UUID leaderId, Set<UUID> memberIds,
                            String leaderName, String leaderSkinUrl,
                            Map<UUID, String> memberNames,
                            Map<UUID, String> memberClasses, Map<UUID, Integer> memberLevels,
                            String partyName, String dungeonId, String floorId,
                            int minLevel, String description, UUID ownerServiceId) {
        this.partyId = partyId;
        this.leaderId = leaderId;
        this.memberIds = memberIds != null ? new HashSet<>(memberIds) : new HashSet<>();
        this.leaderName = leaderName;
        this.leaderSkinUrl = leaderSkinUrl;
        this.memberNames = memberNames != null ? new HashMap<>(memberNames) : new HashMap<>();
        this.memberClasses = memberClasses != null ? new HashMap<>(memberClasses) : new HashMap<>();
        this.memberLevels = memberLevels != null ? new HashMap<>(memberLevels) : new HashMap<>();
        this.partyName = partyName;
        this.dungeonId = dungeonId;
        this.floorId = floorId;
        this.minLevel = minLevel;
        this.description = description;
        this.ownerServiceId = ownerServiceId;
        this.lastHeartbeat = System.currentTimeMillis();
    }
}
