package fr.perrier.dungeons.menu.dungeon;

import fr.perrier.cupcodeapi.menuapi.Button;
import fr.perrier.cupcodeapi.menuapi.pagination.PaginatedMenu;
import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.cupcodeapi.utils.ItemBuilder;
import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.menu.dungeon.utils.PartyButton;
import fr.perrier.dungeons.model.Dungeon;
import fr.perrier.dungeons.parties.DungeonParty;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RequiredArgsConstructor
public class PartyFinderMenu extends PaginatedMenu {
    private final Dungeon dungeon;

    @Override
    public String getPrePaginatedTitle(Player player) {
        return "&#FF8336&l" + ChatUtil.toSmallCaps("party finder");
    }

    @Override
    public Map<Integer, Button> getAllPagesButtons(Player player) {
        HashMap<Integer, Button> buttons = new HashMap<>();

        PartyFinderConfiguration config = PartyFinderConfiguration.getConfigForPlayer(player.getUniqueId(),dungeon.getId());

        for(DungeonParty dungeonParty : DungeonParty.getParties().values()) {
            if(!dungeonParty.getDungeonId().equals(dungeon.getId())) continue;

            if(!Objects.equals(dungeonParty.getFloorId(), config.getFloorFilter())) continue;
            if(!config.getDescriptionFilter().isEmpty() && !dungeonParty.getDescription().contains(config.getDescriptionFilter())) continue;
            if(dungeonParty.getMinLevel() > config.getMinimumLevelFilter()) continue;

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

    public class PartyBuilderButton extends Button {

        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.NETHER_STAR)
                    .setName("&#FF8336&l" + ChatUtil.toSmallCaps("party builder"))
                    .setLore(
                            "&7Use the party builder to create",
                            "&7a new party.",
                            "",
                            "&#FFC700Click to open the party builder."
                    ).toItemStack();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
            new PartyBuilderMenu(new PartyFinderMenu(dungeon),dungeon.getId()).openMenu(player);
        }
    }

    public class FilterButton extends Button {

        @Override
        public ItemStack getButtonItem(Player player) {
            PartyFinderConfiguration config = PartyFinderConfiguration.getConfigForPlayer(player.getUniqueId(),dungeon.getId());

            return new ItemBuilder(Material.COMPARATOR)
                    .setName("&#FF8336&l" + ChatUtil.toSmallCaps("search settings"))
                    .setLore(
                            "&#9C9C9CChange your search settings to",
                            "&7parties suited to you!",
                            "",
                            "&7Floor&f: &#90FFFF" + (config.getFloorFilter().isEmpty() ? "&cNone" : Main.getInstance().getRedisStorageService().getFloor(config.getFloorFilter()).getName()),
                            "&7Description&f &#90FFFF" + (config.getDescriptionFilter().isEmpty() ? "&cNone" : config.getDescriptionFilter()),
                            "&7Minimum Level&f: &#90FFFF" + config.getMinimumLevelFilter(),
                            ""
                    ).toItemStack();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
            new PartyFilterMenu(new PartyFinderMenu(dungeon),dungeon).openMenu(player);
        }
    }


}
