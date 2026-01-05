package fr.perrier.dungeons.spigot.menu.utils;

import fr.perrier.cupcodeapi.menuapi.Button;
import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.cupcodeapi.utils.ItemBuilder;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.parties.IDungeonParty;
import fr.perrier.dungeons.spigot.parties.IParty;
import fr.perrier.dungeons.spigot.parties.IPartyMember;
import net.Indyuce.mmocore.api.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class PartyButton extends Button {
    private final IDungeonParty dungeonParty;
    private final OfflinePlayer leader;

    public PartyButton(IDungeonParty dungeonParty) {
        this.dungeonParty = dungeonParty;
        this.leader = Bukkit.getOfflinePlayer(dungeonParty.getLeaderId());
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
        IParty party = dungeonParty.getParty();
        if(party.isMember(player)) {
            player.sendMessage(ChatUtil.translate("&#FF0000You are already in this party!"));
            return;
        }
        dungeonParty.addMember(player);
        player.sendMessage(ChatUtil.translate("&#00FF00You have been added to " + leader.getName()  + "'s party!"));
        player.closeInventory();
    }

    /**
     * Generate the lore of the party button
     * @param dungeonParty The dungeon party
     * @return The lore
     */
    public static List<String> getLore(IDungeonParty dungeonParty) {
        List<String> lore = new ArrayList<>();
        lore.add("&7Dungeon: &#90FFFF" + dungeonParty.getDungeonId());
        lore.add("&7Floor: &#90FFFF" + Main.getInstance().getDungeonService().getFloor(dungeonParty.getFloorId()).getName());
        lore.add("&7Description: &f" + dungeonParty.getDescription());
        lore.add("&7Min Level: &#90FFFF" + dungeonParty.getMinLevel());
        lore.add("&7Members:");

        int maxMemberPrinted = 5;
        IPartyMember[] members = dungeonParty.getMembers().toArray(new IPartyMember[0]);
        for(int i = 0; i < maxMemberPrinted; i++) {
            if (members.length <= i)
                lore.add(" &8Empty");
            else {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(members[i].getUniqueId());
                PlayerData playerData = PlayerData.get(offlinePlayer);
                lore.add(" &b" + offlinePlayer.getName() + "&f: &e" + playerData.getProfess().getName() + " &7[&f" + playerData.getLevel() + "&7]");
            }
        }
        lore.add(" &8&o+ " + (dungeonParty.getSize() <= 5 ? 0 : dungeonParty.getSize() - 5) + " other players..");
        lore.add("");
        lore.add("&#FFC700Click to join");

        return lore;
    }
}

