package fr.perrier.dungeons.menu.dungeon;


import com.alessiodp.parties.api.interfaces.Party;
import fr.perrier.cupcodeapi.menuapi.Button;
import fr.perrier.cupcodeapi.menuapi.GlassMenu;
import fr.perrier.cupcodeapi.menuapi.Menu;
import fr.perrier.cupcodeapi.menuapi.buttons.BackButton;
import fr.perrier.cupcodeapi.menuapi.buttons.ConversationButton;
import fr.perrier.cupcodeapi.menuapi.buttons.DisplayButton;
import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.cupcodeapi.utils.ItemBuilder;
import fr.perrier.dungeons.model.Dungeon;
import fr.perrier.dungeons.model.Floor;
import fr.perrier.dungeons.parties.DungeonParty;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PartyBuilderMenu extends GlassMenu {
    private final Menu oldMenu;
    private final String dungeonId;
    private String floorId;
    private int minLevel = 0;
    private String description = "";

    public PartyBuilderMenu(Menu oldMenu, String dungeonId) {
        this.oldMenu = oldMenu;
        this.dungeonId = dungeonId;

        // Set by default the first floor of the dungeon
        this.floorId = Dungeon.getDungeon(dungeonId).getFloors().getFirst().getId();
    }

    @Override
    public String getTitle(Player player) {
        return "Party Builder";
    }

    @Override
    public int getGlassColor() {
        return 0;
    }

    @Override
    public Map<Integer, Button> getAllButtons(Player player) {
        HashMap<Integer, Button> buttons = new HashMap<>();

        buttons.put(12, new SelectFloorButton());
        buttons.put(13, EditDescriptionButton(player));
        buttons.put(14, EditMinLevelButton(player));

        buttons.put(30, new CancelButton());
        buttons.put(32, new ConfirmButton());

        return buttons;
    }

    public class SelectFloorButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.RIB_ARMOR_TRIM_SMITHING_TEMPLATE)
                    .hideItemFlags()
                    .setName("&3Floor selector")
                    .setLore(
                            "&7Select the floor you want to play on.",
                            "",
                            "&7Current floor&f: &b" + (floorId.isEmpty() ? "&cNone" : floorId),
                            "",
                            "&eClick to select a floor."
                    ).toItemStack();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
            new FloorSelectorMenu(PartyBuilderMenu.this, Dungeon.getDungeon(dungeonId)).openMenu(player);
        }
    }

    public Button EditDescriptionButton(Player player) {
        return new ConversationButton<>(
                new ItemBuilder(Material.PAPER)
                        .setName("&3Description")
                        .setLore(
                                "&7Write a description to let everyone",
                                "&7know what your party to do.",
                                "",
                                "&bCurrent description:",
                                "&f" + (description.isEmpty() ? "&cNone" : description),
                                "",
                                "&eClick to edit the description."
                        )
                        .toItemStack(),
                player,
                ChatUtil.translate("&fPlease enter the description"),
                (target, result) -> {
                    String description = result.getRight();
                    if(description.length() <= 32) {
                        this.description = description;
                        player.sendRawMessage(ChatUtil.translate("&aDescription updated to: &f" + description));
                        this.openMenu(player);
                    } else {
                        player.sendRawMessage(ChatUtil.translate("&cYou can not have more than 32 characters in the description."));
                    }
                }
        );
    }

    public Button EditMinLevelButton(Player player) {
        return new ConversationButton<>(
                new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                        .setName("&3Minimum player level")
                        .setLore(
                                "&7Add a minimum player level requirement",
                                "&7to join your party so only players with",
                                "&7at least this level are able to join.",
                                "",
                                "&bCurrent minimum player level:",
                                "&f" + (minLevel == -1 ? "&cNone" : minLevel),
                                "",
                                "&eClick to edit the minimum player level."
                        )
                        .toItemStack(),
                player,
                ChatUtil.translate("&fPlease enter the minimum player level"),
                (target, result) -> {
                    String minLevel = result.getRight();
                    if(StringUtils.isNumeric(minLevel)) {
                        int minLevelInt = Integer.parseInt(minLevel);
                        if(minLevelInt < 0) {
                            player.sendRawMessage(ChatUtil.translate("&cYou can not have a negative minimum player level."));
                            return;
                        }
                        this.minLevel = Integer.parseInt(minLevel);
                        player.sendRawMessage(ChatUtil.translate("&aMinimum player level updated to: &f" + minLevel));
                        this.openMenu(player);
                    } else {
                        player.sendRawMessage(ChatUtil.translate("&cYou must enter a number."));
                    }
                }
        );
    }

    public class CancelButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.REDSTONE_BLOCK)
                    .setName("&cCancel")
                    .setLore(
                            "&7Close the party builder.",
                            "",
                            "&eClick to cancel."
                    )
                    .toItemStack();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
            oldMenu.openMenu(player);
        }
    }

    public class ConfirmButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.EMERALD_BLOCK)
                    .setName("&aConfirm")
                    .setLore(
                            "&7Open up your party so",
                            "&7other players can start joining.",
                            "",
                            "&eClick to confirm."
                    )
                    .toItemStack();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
            DungeonParty party = new DungeonParty.Builder()
                    .setDungeonId(dungeonId)
                    .setFloorId(floorId)
                    .setMinLevel(minLevel)
                    .setDescription(description)
                    .setLeader(player)
                    .build();
            player.closeInventory();
            player.sendMessage(ChatUtil.translate("&aYour party has been created and has been queued in the dungeon finder!"));
        }
    }

    @RequiredArgsConstructor
    private class FloorSelectorMenu extends GlassMenu {
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
                buttons.put(40, new DisplayButton(new ItemStack(Material.AIR)));
            } else {
                buttons.put(49, new DisplayButton(new ItemStack(Material.AIR)));
            }

            return buttons;
        }

        @Override
        public String getTitle(Player player) {
            return "Select a floor";
        }

        @RequiredArgsConstructor
        public class FloorButton extends Button {
            private final Floor floor;

            @Override
            public ItemStack getButtonItem(Player player) {
                return new ItemBuilder(Material.OMINOUS_TRIAL_KEY)
                        .setName("&f" + floor.getName())
                        .setLore(
                                "&7Dungeon Floor",
                                "",
                                "&eClick to select this floor."
                        )
                        .toItemStack();
            }

            @Override
            public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
                PartyBuilderMenu.this.floorId = floor.getId();
                oldMenu.openMenu(player);
            }
        }
    }
}
