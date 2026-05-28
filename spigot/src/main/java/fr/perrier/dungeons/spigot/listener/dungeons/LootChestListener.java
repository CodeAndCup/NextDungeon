package fr.perrier.dungeons.spigot.listener.dungeons;

import fr.perrier.dungeons.spigot.loot.LootChestManager;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Drives the runtime behavior of loot chests placed by
 * {@code SpawnLootChestAction}.
 *
 * <ul>
 *   <li><b>PER_PLAYER</b> — intercepts the right-click, cancels the vanilla
 *       chest open, and shows the player their isolated virtual inventory.
 *       On close, the contents are saved back so partial looting persists.</li>
 *   <li><b>GLOBAL</b> — left to vanilla : the physical container is already
 *       filled, so the default open/share behavior is correct. This listener
 *       does not interfere.</li>
 * </ul>
 */
public class LootChestListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        LootChestManager.LootChest chest = LootChestManager.get().getChestAt(block.getLocation());
        if (chest == null) return;

        // GLOBAL chests use the real container — let vanilla handle the open.
        if (chest.getMode() != LootChestManager.LootMode.PER_PLAYER) return;

        // PER_PLAYER : suppress the vanilla open and show the virtual inventory.
        event.setCancelled(true);
        LootChestManager.get().openPerPlayer(event.getPlayer(), chest);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof LootChestManager.LootChestHolder lootHolder)) return;
        LootChestManager.get().savePerPlayer(
                event.getPlayer().getUniqueId(),
                lootHolder.getChestKey(),
                event.getInventory().getContents());
    }
}
