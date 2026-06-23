package fr.perrier.dungeons.spigot.model;

import fr.perrier.dungeons.common.model.dungeon.FloorData;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.database.DatabaseTriggersManager;
import fr.perrier.dungeons.spigot.utils.ServerUtil;
import io.lumine.mythic.lib.api.item.NBTItem;
import lombok.Getter;
import lombok.Setter;
import net.Indyuce.mmocore.api.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
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
                        .anyMatch(itemStack -> itemMatches(itemStack, requiredItem));
                if (!hasItem) {
                    return false;
                }
            }
        }
        // Vérification des items interdits
        if (this.getRequirements().getForbiddenItems() != null && !this.getRequirements().getForbiddenItems().isEmpty()) {
            for (String forbiddenItem : this.getRequirements().getForbiddenItems()) {
                boolean hasItem = Arrays.stream(player.getInventory().getContents())
                        .anyMatch(itemStack -> itemMatches(itemStack, forbiddenItem));
                if (hasItem) {
                    return false;
                }
            }
        }
        // Si tout est respecté
        return true;
    }

    /**
     * Vérifie si un {@link ItemStack} correspond à un identifiant de requirement.
     *
     * <p>La correspondance est tolérante : un item satisfait le requirement si son identifiant
     * MMOItems ({@code MMOITEMS_ITEM_ID}) <em>ou</em> son nom d'affichage est égal à la valeur
     * attendue. L'identifiant MMOItems est lu via l'API NBT de MythicLib (la couche sur laquelle
     * MMOItems stocke ses items), ce qui permet de cibler un item par son ID logique plutôt que
     * par un displayname qui peut varier (couleur, reforge, gemmes…).</p>
     *
     * @param itemStack   l'item de l'inventaire (peut être null)
     * @param requirement l'identifiant attendu (ID MMOItems ou displayname)
     * @return true si l'item correspond au requirement
     */
    private boolean itemMatches(@Nullable ItemStack itemStack, String requirement) {
        if (itemStack == null || itemStack.getType() == Material.AIR || requirement == null) {
            return false;
        }

        // Correspondance par ID MMOItems (insensible à la casse — les IDs sont en majuscules)
        NBTItem nbtItem = NBTItem.get(itemStack);
        if (nbtItem.hasType()) {
            String mmoId = nbtItem.getString("MMOITEMS_ITEM_ID");
            if (mmoId != null && !mmoId.isEmpty() && mmoId.equalsIgnoreCase(requirement)) {
                return true;
            }
        }

        // Correspondance par nom d'affichage (fallback / items vanilla)
        ItemMeta meta = itemStack.getItemMeta();
        return meta != null && meta.hasDisplayName() && meta.getDisplayName().equals(requirement);
    }

    /**
     * Consomme un exemplaire (N-1) de chaque item requis dans l'inventaire du joueur.
     *
     * <p>Appelé une fois le joueur effectivement entré dans le donjon : les requirements ayant
     * déjà été validés au lancement, on retire ici une unité de chacun des {@code requiredItems}
     * (matché par ID MMOItems ou displayname via {@link #itemMatches}). Si un item requis a une
     * pile {@code > 1}, seule une unité est décrémentée ; sinon la pile est entièrement retirée.
     * Doit être exécuté sur le thread principal (accès inventaire Bukkit).</p>
     *
     * @param player le joueur dont on décrémente les items requis (no-op si null)
     */
    public void consumeRequiredItems(Player player) {
        if (player == null || this.getRequirements() == null) return;

        java.util.List<String> requiredItems = this.getRequirements().getRequiredItems();
        if (requiredItems == null || requiredItems.isEmpty()) return;

        ItemStack[] contents = player.getInventory().getContents();
        for (String requiredItem : requiredItems) {
            for (int i = 0; i < contents.length; i++) {
                ItemStack itemStack = contents[i];
                if (!itemMatches(itemStack, requiredItem)) continue;

                int amount = itemStack.getAmount();
                if (amount <= 1) {
                    player.getInventory().setItem(i, null);
                    contents[i] = null;
                } else {
                    itemStack.setAmount(amount - 1);
                }
                break; // une seule unité retirée par item requis
            }
        }
        player.updateInventory();
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
