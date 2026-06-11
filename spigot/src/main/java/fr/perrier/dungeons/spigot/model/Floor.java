package fr.perrier.dungeons.spigot.model;

import fr.perrier.dungeons.common.model.dungeon.FloorData;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.database.DatabaseTriggersManager;
import fr.perrier.dungeons.spigot.utils.ServerUtil;
import lombok.Getter;
import lombok.Setter;
import net.Indyuce.mmocore.api.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Getter
@Setter
public class Floor extends FloorData {

    public Floor(String id, String name) {
        super(id, name);
        super.setTriggers(DatabaseTriggersManager.loadTriggers(id));
    }

    public Floor(String id, String name, String description) {
        super(id, name, description);
        super.setTriggers(DatabaseTriggersManager.loadTriggers(id));
    }

    public Floor(FloorData floorData) {
        this(floorData, true);
    }

    /**
     * @param floorData    the source floor data (triggers are stripped from the shared Redis map)
     * @param loadTriggers when {@code true}, lazily hydrates triggers from the
     *                     {@code floor_triggers} table if {@code floorData} carries none — what an
     *                     instance server needs to execute the workflow. Lobby boot passes
     *                     {@code false}: it only needs floor metadata for menus / queue and must
     *                     not fire a DB trigger query per floor (Phase 2 of
     *                     DUNGEON_LOADING_OPTIMIZATION — triggers are loaded lazily at the real
     *                     point of use, i.e. when an instance server builds its Floor).
     */
    public Floor(FloorData floorData, boolean loadTriggers) {
        super(floorData.getId(), floorData.getName(), floorData.getDescription(),
                floorData.getWorldConfig(), floorData.getRequirements(),
                floorData.getRules(), floorData.getSteps(), floorData.getTriggers());
        // The 8-arg FloorData constructor does NOT copy versioning metadata or the
        // labyrinth discriminator — replay them explicitly, otherwise every Floor wrapper
        // resets version=1/checksum=null/floorType=CLASSIC and wipes the data upstream
        // when it syncs back through updateMap().
        setDungeonId(floorData.getDungeonId());
        setFloorType(floorData.getFloorType());
        setLabyrinthFloorConfig(floorData.getLabyrinthFloorConfig());
        setVersion(floorData.getVersion());
        setSchemaVersion(floorData.getSchemaVersion());
        setUpdatedAt(floorData.getUpdatedAt());
        setUpdatedBy(floorData.getUpdatedBy());
        setChecksum(floorData.getChecksum());
        // Triggers are stripped from the shared Redis map (they contain Spigot-only classes
        // that blow up on the proxy). Load them from floor_triggers on demand so the
        // instance server always has its trigger set, matching the other Floor constructors.
        if (loadTriggers && getTriggers() == null && getId() != null) {
            super.setTriggers(DatabaseTriggersManager.loadTriggers(getId()));
        }
    }

    /**
     * Retrieves a floor from Redis by its unique ID.
     *
     * @param id the unique ID of the floor to retrieve
     * @return the floor with the given ID, or null if not found
     */
    public static @Nullable Floor getFloor(String id) {
        return Main.getInstance().getDungeonService().getFloor(id);
    }

    /**
     * Synchronizes this floor to Redis, updating the local reference and
     * notifying other servers of the update.
     */
    public void updateMap() {
        Main.getInstance().getDungeonService().syncFloor(this.toFloorData());
    }

    /**
     * Asynchronously generates a floor template for this floor if it doesn't already exist.
     *
     * @return a future that completes with true if the template was successfully generated, or false if it already existed
     */
    public CompletableFuture<Boolean> generateTemplate() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            if(!ServerUtil.isFloorTemplateExists(this)) {
                ServerUtil.createFloorTemplate(this).thenAccept(future::complete);
            } else {
                future.complete(true);
            }
        });
        return future;
    }

    /**
     * Converts this Floor object to a FloorData object.
     *
     * @return a FloorData representation of this Floor
     */
    public FloorData toFloorData() {
        FloorData data = new FloorData(
                getId(),
                getName(),
                getDescription(),
                getWorldConfig(),
                getRequirements(),
                getRules(),
                getSteps(),
                getTriggers()
        );
        data.setDungeonId(getDungeonId());
        data.setFloorType(getFloorType());
        data.setLabyrinthFloorConfig(getLabyrinthFloorConfig());
        data.setVersion(getVersion());
        data.setSchemaVersion(getSchemaVersion());
        data.setUpdatedAt(getUpdatedAt());
        data.setUpdatedBy(getUpdatedBy());
        data.setChecksum(getChecksum());
        return data;
    }

    /**
     * Vérifie si un joueur respecte tous les requirements d'un floor.
     *
     * <p>Requires the player to be online on this server — minLevel checks against MMOCore's
     * {@code PlayerData} and the inventory checks both need a live {@link Player}. For party
     * members that are offline or on another server, this returns {@code false} so callers can
     * abort the dungeon start cleanly instead of NPE'ing.</p>
     *
     * @param player Le joueur (peut être null pour un joueur offline / cross-serveur)
     * @return true si tous les requirements sont respectés, false sinon
     */
    public boolean isRequirementsValid(Player player) {
        if (player == null) return false;

        PlayerData playerData = PlayerData.get(player);
        ProfileData profileData = Main.getInstance().getProfileService().getProfileData(player.getUniqueId());

        // Vérification du niveau minimum
        if (this.getRequirements().getMinLevel() > 0) {
            if (playerData.getLevel() < this.getRequirements().getMinLevel()) {
                return false;
            }
        }
        // Vérification des floors requis
        if (this.getRequirements().getRequiredFloorsId() != null && !this.getRequirements().getRequiredFloorsId().isEmpty()) {
            for (String requiredFloorId : this.getRequirements().getRequiredFloorsId()) {
                if (!profileData.getCompletedFloors().contains(requiredFloorId)) {
                    return false;
                }
            }
        }
        // Vérification des items requis
        if (this.getRequirements().getRequiredItems() != null && !this.getRequirements().getRequiredItems().isEmpty()) {
            for (String requiredItem : this.getRequirements().getRequiredItems()) {
                boolean hasItem = Arrays.stream(player.getInventory().getContents())
                        .anyMatch(itemStack -> itemStack != null && Objects.requireNonNull(itemStack.getItemMeta()).getDisplayName().equals(requiredItem));
                if (!hasItem) {
                    return false;
                }
            }
        }
        // Vérification des items interdits
        if (this.getRequirements().getForbiddenItems() != null && !this.getRequirements().getForbiddenItems().isEmpty()) {
            for (String forbiddenItem : this.getRequirements().getForbiddenItems()) {
                boolean hasItem = Arrays.stream(player.getInventory().getContents())
                        .anyMatch(itemStack -> itemStack != null && Objects.requireNonNull(itemStack.getItemMeta()).getDisplayName().equals(forbiddenItem));
                if (hasItem) {
                    return false;
                }
            }
        }
        // Si tout est respecté
        return true;
    }


    /**
     * A string representation of the floor, including its ID, name, world configuration,
     * requirements, rules, and steps.
     *
     * @return a string representation of the floor
     */
    @Override
    public String toString() {
        return "Floor{" +
                "id='" + getId() + '\'' +
                ", name='" + getName() + '\'' +
                ", worldConfig=" + getWorldConfig() +
                ", requirements=" + getRequirements() +
                ", rules=" + getRules() +
                ", steps=" + getSteps() +
                '}';
    }
}
