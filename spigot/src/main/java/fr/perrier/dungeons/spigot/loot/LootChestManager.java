package fr.perrier.dungeons.spigot.loot;

import fr.perrier.dungeons.spigot.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Tracks loot chests spawned at runtime by {@code SpawnLootChestAction} and
 * resolves the per-player vs global looting behavior.
 *
 * <ul>
 *   <li><b>GLOBAL</b> — the physical chest block is filled once ; vanilla
 *       inventory sharing applies (first-come-first-served).</li>
 *   <li><b>PER_PLAYER</b> — the physical block is only a visual ; each player
 *       gets an isolated virtual inventory seeded from the same loot. Partial
 *       looting is persisted per player so reopening shows what they left.</li>
 * </ul>
 *
 * <p>State is in-memory only — chests live for the lifetime of the instance
 * (the CloudNet floor world is never saved), so no DB persistence is needed.</p>
 */
public class LootChestManager {

    /** Inventory rows × 9. A single/double chest is 27 ; we cap loot to one chest. */
    private static final int INV_SIZE = 27;

    private static final LootChestManager INSTANCE = new LootChestManager();

    public static LootChestManager get() {
        return INSTANCE;
    }

    private final Map<String, LootChest> chests = new ConcurrentHashMap<>();

    private LootChestManager() {
    }

    /**
     * Places a loot chest of {@code chestType} at {@code location} and registers
     * it under {@code mode}. The {@code lootSpec} is the inline format
     * {@code MATERIAL:qty} or {@code MATERIAL:min-max}, comma-separated
     * (e.g. {@code "DIAMOND:3,GOLD_INGOT:1-5"}). Ranges are rolled once here so
     * every player sees the same contents (CDC owner decision: identical loot).
     *
     * @return {@code true} when the chest was placed and registered.
     */
    public boolean placeChest(Location location, Material chestType, LootMode mode, String lootSpec) {
        if (location == null || location.getWorld() == null) {
            Main.getLoggerUtil().warning("[LootChest] placeChest: null location/world");
            return false;
        }
        Material type = chestType != null ? chestType : Material.CHEST;
        List<ItemStack> loot = parseLoot(lootSpec);

        Block block = location.getBlock();
        block.setType(type);

        LootChest chest = new LootChest(blockKey(location), mode, loot);
        chests.put(chest.key, chest);

        if (mode == LootMode.GLOBAL) {
            // Fill the real container so vanilla handles the shared inventory.
            BlockState state = block.getState();
            if (state instanceof Container container) {
                Inventory inv = container.getInventory();
                inv.clear();
                for (ItemStack item : loot) inv.addItem(item.clone());
            } else {
                Main.getLoggerUtil().warning("[LootChest] " + type + " is not a Container at " + chest.key);
            }
        }
        Main.getLoggerUtil().info("[LootChest] Placed " + mode + " chest (" + type + ") at " + chest.key
                + " with " + loot.size() + " stack(s)");
        return true;
    }

    /**
     * Returns the registered chest at a block location, or {@code null}.
     */
    public LootChest getChestAt(Location location) {
        if (location == null) return null;
        return chests.get(blockKey(location));
    }

    /**
     * Opens the per-player virtual inventory for {@code player} at {@code chest}.
     * Seeds a fresh copy of the loot on first open ; subsequent opens restore
     * the player's saved contents (partial looting persists).
     */
    public void openPerPlayer(Player player, LootChest chest) {
        if (player == null || chest == null) return;
        ItemStack[] saved = chest.perPlayerContents.get(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(new LootChestHolder(chest.key), INV_SIZE, "§8Coffre de butin");
        if (saved != null) {
            inv.setContents(saved);
        } else {
            for (ItemStack item : chest.loot)
                inv.addItem(item.clone());
            chest.perPlayerContents.put(player.getUniqueId(), inv.getContents());
        }
        player.openInventory(inv);
    }

    /**
     * Persists the player's virtual inventory back into the chest state when
     * the per-player inventory is closed.
     */
    public void savePerPlayer(UUID playerId, String chestKey, ItemStack[] contents) {
        if (playerId == null || chestKey == null) return;
        LootChest chest = chests.get(chestKey);
        if (chest == null) return;
        chest.perPlayerContents.put(playerId, contents);
    }

    /**
     * Drops all tracked chests (called on instance teardown if ever needed).
     */
    public void clear() {
        chests.clear();
    }


    private List<ItemStack> parseLoot(String spec) {
        List<ItemStack> out = new ArrayList<>();
        if (spec == null || spec.isBlank()) return out;
        for (String part : spec.split(",")) {
            String token = part.trim();
            if (token.isEmpty()) continue;
            String matName = token;
            int qty = 1;
            int colon = token.indexOf(':');
            if (colon >= 0) {
                matName = token.substring(0, colon).trim();
                qty = parseQuantity(token.substring(colon + 1).trim());
            }
            Material mat = Material.matchMaterial(matName.toUpperCase(Locale.ROOT));
            if (mat == null) {
                Main.getLoggerUtil().warning("[LootChest] Unknown material in loot spec: '" + matName + "'");
                continue;
            }
            qty = Math.clamp(qty, 1, mat.getMaxStackSize() * 9);

            int remaining = qty;
            int max = Math.max(1, mat.getMaxStackSize());
            while (remaining > 0) {
                int stack = Math.min(remaining, max);
                out.add(new ItemStack(mat, stack));
                remaining -= stack;
            }
        }
        return out;
    }

    private int parseQuantity(String raw) {
        try {
            int dash = raw.indexOf('-');
            if (dash > 0) {
                int min = Integer.parseInt(raw.substring(0, dash).trim());
                int max = Integer.parseInt(raw.substring(dash + 1).trim());
                if (max < min) { int t = min; min = max; max = t; }
                return ThreadLocalRandom.current().nextInt(min, max + 1);
            }
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private static String blockKey(Location loc) {
        return Objects.requireNonNull(loc.getWorld()).getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    /**
     * A registered loot chest. Identified by its block key.
     */
    public static final class LootChest {
        final String key;
        final LootMode mode;
        final List<ItemStack> loot;
        final Map<UUID, ItemStack[]> perPlayerContents = new HashMap<>();

        LootChest(String key, LootMode mode, List<ItemStack> loot) {
            this.key = key;
            this.mode = mode;
            this.loot = loot;
        }

        public LootMode getMode() {
            return mode;
        }

        public String getKey() {
            return key;
        }
    }

    /**
     * Marker holder so {@code LootChestListener} can recognise our virtual
     * per-player inventories on {@link org.bukkit.event.inventory.InventoryCloseEvent}.
     */
    public static final class LootChestHolder implements InventoryHolder {
        private final String chestKey;

        LootChestHolder(String chestKey) {
            this.chestKey = chestKey;
        }

        public String getChestKey() {
            return chestKey;
        }

        @Override
        public Inventory getInventory() {
            return null; // not used — holder is only an identity tag
        }
    }
}
