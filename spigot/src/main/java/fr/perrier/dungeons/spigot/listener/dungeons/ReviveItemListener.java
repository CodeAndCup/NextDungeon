package fr.perrier.dungeons.spigot.listener.dungeons;

import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.dungeons.spigot.Main;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Objects;

/**
 * Listener for the revive item system.
 * Handles player interactions with the revive item and revives nearby ghost players.
 */
public class ReviveItemListener implements Listener {

    /**
     * Handles player interaction events.
     * If the player uses a revive item, attempts to revive the nearest ghost player within a 10-block radius.
     *
     * @param event the player interaction event
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // Vérifier si c'est un item de résurrection
        if(isReviveItem(item)) {
            event.setCancelled(true);

            // Chercher un joueur fantôme dans les alentours (radius de 10 blocs)
            Player deadPlayer = findNearestGhostPlayer(player);

            if(deadPlayer != null) {
                // Consommer l'item
                if(item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    player.getInventory().setItemInMainHand(null);
                }

                // Ressusciter le joueur
                InstancePlayerDeathListener.revivePlayer(deadPlayer);

                // Message de confirmation
                String reviveMessage = Objects.requireNonNull(Main.getInstance().getConfig().getString("ReviveSystem.reviveMessage"));
                player.sendMessage(ChatUtil.translate("&#00FF00✓ " + reviveMessage.replace("{player}", deadPlayer.getName())));
            } else {
                player.sendMessage(ChatUtil.translate("&#FF0000✗ &fNo ghost player nearby to revive!"));
            }
        }
    }

    /**
     * Checks if the given item is a revive item based on type and display name.
     *
     * @param item the item to check
     * @return true if the item is a revive item, false otherwise
     */
    private boolean isReviveItem(ItemStack item) {
        if(item == null || item.getType() == Material.AIR) {
            return false;
        }

        // Vérifier le type d'item
        String configuredType = Objects.requireNonNull(Main.getInstance().getConfig().getString("ReviveSystem.ReviveItem.type"));
        if(!item.getType().name().equalsIgnoreCase(configuredType)) {
            return false;
        }

        // Vérifier le nom de l'item (optionnel)
        ItemMeta meta = item.getItemMeta();
        if(meta != null && meta.hasDisplayName()) {
            String configuredName = Objects.requireNonNull(Main.getInstance().getConfig().getString("ReviveSystem.ReviveItem.displayName"));
            String translatedConfigName = ChatUtil.translate(configuredName);

            // Comparer les noms après traduction
            return meta.getDisplayName().equals(translatedConfigName);
        }

        return true;
    }

    /**
     * Finds the nearest ghost player (not yet revived) within a 10-block radius.
     *
     * @param reviver the player attempting to revive
     * @return the nearest ghost player, or null if none found
     */
    private Player findNearestGhostPlayer(Player reviver) {
        double searchRadius = 10.0;
        Player nearestGhost = null;
        double nearestDistance = searchRadius;

        for(Player potentialGhost : reviver.getWorld().getPlayers()) {
            // Vérifier si le joueur est en mode fantôme (est dans GHOST_DATA et pas ressuscité)
            if(InstancePlayerDeathListener.getGHOST_DATA().containsKey(potentialGhost.getUniqueId())) {
                InstancePlayerDeathListener.GhostData ghostData = InstancePlayerDeathListener.getGHOST_DATA().get(potentialGhost.getUniqueId());

                if(!ghostData.isRevived()) {
                    double distance = reviver.getLocation().distance(potentialGhost.getLocation());

                    if(distance < nearestDistance) {
                        nearestGhost = potentialGhost;
                        nearestDistance = distance;
                    }
                }
            }
        }

        return nearestGhost;
    }
}
