package fr.perrier.dungeons.listener.editor;

import fr.perrier.cupcodeapi.utils.ItemBuilder;
import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.model.Floor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

public class EditorJoinListener implements Listener {

    private final ItemStack wandItem = new ItemBuilder(Material.WOODEN_AXE)
            .setName("&#964B00World Edit Wand")
            .setLore(
                    "&#7B7B7BRight click to select pos1",
                    "&#7B7B7BLeft click to select pos2"
            )
            .hideItemFlags()
            .toItemStack();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        Floor floor = Main.getInstance().getRedisStorageService().getCurrentFloor().get();
        player.teleport(floor.getWorldConfig().getSpawn().toLocation());
        player.setGameMode(GameMode.CREATIVE);
    }
}
