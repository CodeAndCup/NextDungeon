package fr.perrier.dungeons.menu.dungeon;

import com.alessiodp.parties.api.interfaces.Party;
import fr.perrier.cupcodeapi.menuapi.Button;
import fr.perrier.cupcodeapi.menuapi.pagination.PaginatedMenu;
import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.cupcodeapi.utils.ItemBuilder;
import fr.perrier.dungeons.manager.PartyManager;
import fr.perrier.dungeons.model.Dungeon;
import fr.perrier.dungeons.parties.DungeonParty;
import lombok.RequiredArgsConstructor;
import net.Indyuce.mmocore.api.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.*;

@RequiredArgsConstructor
public class PartyFinderMenu extends PaginatedMenu {
    private final Dungeon dungeon;

    private String floorFilter = "";
    private String descriptionFilter = "";
    private int minimumLevelFilter = 0;

    @Override
    public String getPrePaginatedTitle(Player player) {
        return "";
    }

    @Override
    public Map<Integer, Button> getAllPagesButtons(Player player) {
        HashMap<Integer, Button> buttons = new HashMap<>();

        for(DungeonParty dungeonParty : DungeonParty.getParties().values()) {
            if(!dungeonParty.getDungeonId().equals(dungeon.getId())) continue;
            buttons.put(buttons.size(), new PartyButton(dungeonParty));
        }

        return buttons;
    }

    @Override
    public Map<Integer, Button> getGlobalButtons(Player player) {
        HashMap<Integer, Button> buttons = new HashMap<>();

        buttons.put(40, new PartyBuilderButton());
        buttons.put(42, new FilterButton());

        return buttons;
    }


    public static class PartyButton extends Button {
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


        public static List<String> getLore(DungeonParty dungeonParty) {
            List<String> lore = new ArrayList<>();
            lore.add("&7Dungeon: &#90FFFF" + dungeonParty.getDungeonId());
            lore.add("&7Floor: &#90FFFF" + dungeonParty.getFloorId());
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

    public class PartyBuilderButton extends Button {

        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.NETHER_STAR)
                    .setName("&#FF8336&l" + ChatUtil.toSmallCaps("Party Builder"))
                    .setLore(
                            "&7Use the party builder to create",
                            "&7a new party.",
                            "",
                            "&#FFC700Click to open the party builder."
                    ).toItemStack();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
            new PartyBuilderMenu(PartyFinderMenu.this,dungeon.getId()).openMenu(player);
        }
    }

    public class FilterButton extends Button {

        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.COMPARATOR)
                    .setName("&#FF8336&l" + ChatUtil.toSmallCaps("Search Settings"))
                    .setLore(
                            "&#9C9C9CChange your search settings to",
                            "&7parties suited to you!",
                            "",
                            "&7Floor&f: &#90FFFF" + floorFilter,
                            "&7Description&f &#90FFFF" + descriptionFilter,
                            "&7Level&f: &#90FFFF" + minimumLevelFilter,
                            ""
                    ).toItemStack();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
            //TODO: Create filter menu
        }
    }
}
