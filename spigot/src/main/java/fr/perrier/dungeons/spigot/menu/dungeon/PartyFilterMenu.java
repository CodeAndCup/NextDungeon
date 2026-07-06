package fr.perrier.dungeons.spigot.menu.dungeon;

import fr.perrier.cupcodeapi.menuapi.Button;
import fr.perrier.cupcodeapi.menuapi.GlassMenu;
import fr.perrier.cupcodeapi.menuapi.Menu;
import fr.perrier.cupcodeapi.menuapi.buttons.BackButton;
import fr.perrier.cupcodeapi.menuapi.buttons.ConversationButton;
import fr.perrier.cupcodeapi.menuapi.buttons.DisplayButton;
import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.cupcodeapi.utils.ItemBuilder;
import fr.perrier.dungeons.spigot.menu.utils.MenuTitle;
import fr.perrier.dungeons.spigot.model.Dungeon;
import fr.perrier.dungeons.spigot.model.Floor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class PartyFilterMenu extends GlassMenu {
    private final Menu oldMenu;
    private final Dungeon dungeon;

    @Override
    public String getTitle(Player player) {
        return MenuTitle.ofRows(3, "&#8B0000&l" + ChatUtil.toSmallCaps("search settings"));
    }

    @Override
    public int getGlassColor() {
        return 0;
    }

    @Override
    public Map<Integer, Button> getAllButtons(Player player) {
        HashMap<Integer, Button> buttons = new HashMap<>();

        buttons.put(12, new FilterFloorButton());
        buttons.put(13, FilterDescriptionButton(player));
        buttons.put(14, FilterMinLevelButton(player));

        buttons.put(22, new BackButton(oldMenu));

        return buttons;
    }

    public class FilterFloorButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            PartyFinderConfiguration config = PartyFinderConfiguration.getConfigForPlayer(player.getUniqueId(),dungeon.getId());

            return new ItemBuilder(Material.RIB_ARMOR_TRIM_SMITHING_TEMPLATE)
                    .hideItemFlags()
                    .setName("<gradient:#8B0000:bold>" + ChatUtil.toSmallCaps("floor selector") + "</gradient:#D10000>")
                    .setLore(
                            "&7Select the floor you want to play on.",
                            "",
                            "&7Current floor: &#90FFFF" + (config.getFloorFilter().isEmpty() ? "&#FF0000None" : config.getFloorFilter()),
                            "",
                            "&#FFC700Click to select a floor."
                    ).toItemStack();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
            new FloorSelectorMenu(PartyFilterMenu.this, dungeon).openMenu(player);
        }
    }

    public Button FilterDescriptionButton(Player player) {
        PartyFinderConfiguration config = PartyFinderConfiguration.getConfigForPlayer(player.getUniqueId(),dungeon.getId());

        ItemStack item = new ItemBuilder(Material.PAPER)
                .setName("<gradient:#8B0000:bold>" + ChatUtil.toSmallCaps("description") + "</gradient:#D10000>")
                .setLore(
                        "&7Write a description to let everyone",
                        "&7know what your party to do.",
                        "",
                        "&7Current description:",
                        "&#90FFFF" + (config.getDescriptionFilter().isEmpty() ? "&#FF0000None" : config.getDescriptionFilter()),
                        "",
                        "&#FFC700Left-click to edit the description.",
                        "&#FFC700Right-click to clear the filter."
                )
                .toItemStack();

        // Right-click bypasses the conversation and clears the filter in place — ConversationButton
        // always prompts for input on click, so we override to branch on click type.
        return new ConversationButton<Player>(
                item,
                player,
                ChatUtil.translate("&fPlease enter the description"),
                (target, result) -> {
                    String description = result.getRight();
                    if(description.length() <= 32) {
                        config.setDescriptionFilter(description);
                        player.sendRawMessage(ChatUtil.translate("&#00FF00Description updated to: &f" + description));
                        this.openMenu(player);
                    } else {
                        player.sendRawMessage(ChatUtil.translate("&#FF0000You can not have more than 32 characters in the description."));
                    }
                }
        ) {
            @Override
            public void clicked(Player p, int slot, ClickType clickType, int hotbarButton) {
                if (clickType.isRightClick()) {
                    config.setDescriptionFilter("");
                    p.sendRawMessage(ChatUtil.translate("&#00FF00Description filter cleared."));
                    PartyFilterMenu.this.openMenu(p);
                    return;
                }
                super.clicked(p, slot, clickType, hotbarButton);
            }
        };
    }

    public Button FilterMinLevelButton(Player player) {
        PartyFinderConfiguration config = PartyFinderConfiguration.getConfigForPlayer(player.getUniqueId(),dungeon.getId());

        return new ConversationButton<>(
                new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                        .setName("<gradient:#8B0000:bold>" + ChatUtil.toSmallCaps("minimum player level") + "</gradient:#D10000>")
                        .setLore(
                                "&7Add a minimum player level requirement",
                                "&7to join your party so only players with",
                                "&7at least this level are able to join.",
                                "",
                                "&7Current minimum player level: " + (config.getMinimumLevelFilter() == -1 ? "&#FF0000None" : "&#90FFFF" + config.getMinimumLevelFilter()),
                                "",
                                "&#FFC700Click to edit the minimum player level."
                        )
                        .toItemStack(),
                player,
                ChatUtil.translate("&fPlease enter the minimum player level"),
                (target, result) -> {
                    String minLevel = result.getRight();
                    if(StringUtils.isNumeric(minLevel)) {
                        try {
                            int minLevelInt = Integer.parseInt(minLevel);
                            if (minLevelInt < 0) {
                                player.sendRawMessage(ChatUtil.translate("&#FF0000You can not have a negative minimum player level."));
                                return;
                            }
                            config.setMinimumLevelFilter(minLevelInt);
                            player.sendRawMessage(ChatUtil.translate("&#00FF00Minimum player level updated to: &f" + minLevel));
                            this.openMenu(player);
                        }catch (NumberFormatException e) {
                            player.sendRawMessage(ChatUtil.translate("&#FF0000You must enter a valid number."));
                        }
                    } else {
                        player.sendRawMessage(ChatUtil.translate("&#FF0000You must enter a number."));
                    }
                }
        );
    }

    private static class FloorSelectorMenu extends GlassMenu {
        private final Menu oldMenu;
        private final Dungeon dungeon;

        private final HashMap<Integer, List<Integer>> floorsSlots = new HashMap<>() {
            {
                put(1, List.of(22));
                put(2, List.of(21,23));
                put(3, List.of(20,22,24 ));
                put(4, List.of(20,21,23,24));
                put(5, List.of(20,21,22,23,24));
                put(6, List.of(21,22,23,30,31,32));
                put(7, List.of(20,21,23,24,30,31,32));
                put(8, List.of(20,21,22,23,24,30,31,32));
                put(9, List.of(20,21,22,23,24,29,30,32,33));
                put(10, List.of(20,21,22,23,24,29,30,31,32,33));
            }
        };

        @Getter
        private Floor selectedFloor;

        public FloorSelectorMenu(Menu oldMenu, Dungeon dungeon) {
            this.oldMenu = oldMenu;
            this.dungeon = dungeon;
        }

        @Override
        public int getGlassColor() {
            return 0;
        }

        @Override
        public Map<Integer, Button> getAllButtons(Player player) {
            HashMap<Integer, Button> buttons = new HashMap<>();

            List<Integer> slots = floorsSlots.get(dungeon.getFloors().size());

            int index = 0;
            for(Integer slot : slots) {
                buttons.put(slot, new FloorButton(dungeon.getFloors().get(index)));
                index++;
            }

            if(dungeon.getFloors().size() <= 5) {
                buttons.put(40, new BackButton(oldMenu));
            } else {
                buttons.put(49, new BackButton(oldMenu));
            }

            return buttons;
        }

        @Override
        public String getTitle(Player player) {
            // Back button sits at slot 40 (<=5 floors) or 49 (>5), giving a 5- or 6-row inventory.
            int rows = dungeon.getFloors().size() <= 5 ? 5 : 6;
            return MenuTitle.ofRows(rows, "&#8B0000&l" + ChatUtil.toSmallCaps("select a floor"));
        }

        @RequiredArgsConstructor
        public class FloorButton extends Button {
            private final Floor floor;

            @Override
            public ItemStack getButtonItem(Player player) {
                List<String> lore = new ArrayList<>();
                String[] description = floor.getDescription().split("\n");
                for(String line : description)
                    lore.add("&7" + ChatUtil.translate(line));
                lore.add("");
                lore.add("&#FFC700Click to select this floor.");

                return new ItemBuilder(Material.OMINOUS_TRIAL_KEY)
                        .setName("&f" + floor.getName())
                        .setLore(lore)
                        .toItemStack();
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
                PartyFinderConfiguration config = PartyFinderConfiguration.getConfigForPlayer(player.getUniqueId(),dungeon.getId());
                config.setFloorFilter(floor.getId());
                oldMenu.openMenu(player);
            }
        }
    }
}
