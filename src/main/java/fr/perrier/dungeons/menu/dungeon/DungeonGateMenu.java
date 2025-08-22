package fr.perrier.dungeons.menu.dungeon;

import fr.perrier.cupcodeapi.menuapi.Button;
import fr.perrier.cupcodeapi.menuapi.GlassMenu;
import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.cupcodeapi.utils.ItemBuilder;
import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.model.Dungeon;
import fr.perrier.dungeons.model.Floor;
import fr.perrier.dungeons.model.FloorInstance;
import fr.perrier.dungeons.parties.DungeonParty;
import fr.perrier.dungeons.utils.ServerUtil;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class DungeonGateMenu extends GlassMenu {
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
        return 15;
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
            buttons.put(40, new PartyFinderButton());
        } else {
            buttons.put(49, new PartyFinderButton());
        }

        return buttons;
    }

    @Override
    public String getTitle(Player player) {
        return "";
    }

    @RequiredArgsConstructor
    public static class FloorButton extends Button {
        private final Floor floor;

        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.OMINOUS_TRIAL_KEY)
                    .setName("&f" + floor.getName())
                    .setLore(
                            "&7Dungeon Floor",
                            "",
                            "&eClick to enter the floor."
                    )
                    .toItemStack();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
            if(DungeonParty.hasLeadParty(player)) {
                FloorInstance floorInstance = new FloorInstance(floor.getId());
                floorInstance.sendToServer(DungeonParty.getDungeonPartyOf(player));
            }
        }
    }

    private class PartyFinderButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.SPYGLASS)
                    .setName("&#FF8336&l" + ChatUtil.toSmallCaps("party finder"))
                    .setLore(
                            "&7Use the party finder to join a party",
                            "&7queued in the dungeon.",
                            "",
                            "&#FFC700Click to open the party finder."
                    )
                    .toItemStack();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
            new PartyFinderMenu(dungeon).openMenu(player);
        }
    }
}
