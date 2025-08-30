package fr.perrier.dungeons.spigot.menu.utils;

import com.alessiodp.parties.api.interfaces.Party;
import fr.perrier.cupcodeapi.menuapi.Button;
import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.cupcodeapi.utils.ItemBuilder;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.manager.PartyManager;
import fr.perrier.dungeons.spigot.parties.DungeonParty;
import net.Indyuce.mmocore.api.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PartyButton extends Button {
    private final DungeonParty dungeonParty;
    private final OfflinePlayer leader;

    public PartyButton(DungeonParty dungeonParty) {
        this.dungeonParty = dungeonParty;
        this.leader = Bukkit.getOfflinePlayer(dungeonParty.getLeader());
    }

    @Override
    public ItemStack getButtonItem(Player player) {

        return new ItemBuilder(Material.PLAYER_HEAD)
                .setSkullOwner(leader.getName())
                .setName("&f&l" + ChatUtil.toSmallCaps(leader.getName() + "'s Party"))
                .setLore(getLore(dungeonParty))
                .toItemStack();
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        Party party = dungeonParty.getParty();
        if(PartyManager.isInsideParty(player, party)) {
            player.sendMessage(ChatUtil.translate("&cYou are already in this party!"));
            return;
        }
        dungeonParty.addMember(player);
        player.sendMessage(ChatUtil.translate("&aYou have been added to " + leader.getName()  + "'s party!"));
        player.closeInventory();
    }

    /**
     * Generate the lore of the party button
     * @param dungeonParty The dungeon party
     * @return The lore
     */
    public static List<String> getLore(DungeonParty dungeonParty) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Dungeon: &#90FFFF" + dungeonParty.getDungeonId());
        lore.add("&7Floor: &#90FFFF" + Main.getInstance().getRedisStorageService().getFloor(dungeonParty.getFloorId()).getName());
        lore.add("&7Description: &f" + dungeonParty.getDescription());
        lore.add("&7Min Level: &#90FFFF" + dungeonParty.getMinLevel());
        lore.add("&7Members:");

        int maxMemberPrinted = 5;
        Object[] members = dungeonParty.getParty().getMembers().toArray();
        for(int i = 0; i < maxMemberPrinted; i++) {
            if (members.length <= i)
                lore.add(" &8Empty");
            else {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer((UUID) members[i]);
                PlayerData playerData = PlayerData.get(offlinePlayer);
                lore.add(" &b" + offlinePlayer.getName() + "&f: &e" + playerData.getProfess().getName() + " &7[&f" + playerData.getLevel() + "&7]");
            }
        }
        lore.add(" &8&o+ " + (dungeonParty.getParty().getMembers().size() <= 5 ? 0 : dungeonParty.getParty().getMembers().size()-5) + " other players..");
        lore.add("");
        lore.add("&#FFC700Click to join");

        return lore;
    }
}
