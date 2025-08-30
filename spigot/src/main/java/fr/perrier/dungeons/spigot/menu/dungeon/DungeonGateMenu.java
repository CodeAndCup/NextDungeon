package fr.perrier.dungeons.spigot.menu.dungeon;

import fr.perrier.cupcodeapi.menuapi.Button;
import fr.perrier.cupcodeapi.menuapi.GlassMenu;
import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.cupcodeapi.utils.ItemBuilder;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.model.Dungeon;
import fr.perrier.dungeons.spigot.model.Floor;
import fr.perrier.dungeons.spigot.model.FloorInstance;
import fr.perrier.dungeons.spigot.model.ProfileData;
import fr.perrier.dungeons.spigot.parties.DungeonParty;
import lombok.RequiredArgsConstructor;
import net.Indyuce.mmocore.api.player.PlayerData;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.*;

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
    public String getTitle(Player player) {
        return "&#8B0000&l" + ChatUtil.toSmallCaps(dungeon.getName());
    }

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
            buttons.put(38, new ProfileButton());
            buttons.put(40, new PartyFinderButton());
            buttons.put(42, new InformationButton());
        } else {
            buttons.put(47, new ProfileButton());
            buttons.put(49, new PartyFinderButton());
            buttons.put(51, new InformationButton());
        }

        return buttons;
    }

    @RequiredArgsConstructor
    public static class FloorButton extends Button {
        private final Floor floor;

        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.OMINOUS_TRIAL_KEY)
                    .setName("&f" + floor.getName())
                    .setLore(getFloorLore(floor, player))
                    .toItemStack();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
            //TODO: Make that can't be click if dungeon already loading
            if(Objects.requireNonNull(Objects.requireNonNull(getButtonItem(player).getItemMeta()).getLore()).stream().anyMatch(s -> s.contains("✘"))) {
                player.sendRawMessage(ChatUtil.translate(Main.getPrefix() + "&cYou do not meet the requirements to enter this floor."));
                return;
            }
            if(DungeonParty.hasLeadParty(player)) {
                if(DungeonParty.getDungeonPartyOf(player).getMembers().size() > floor.getRequirements().getPartyRequirements().getMaxSize()) {
                    player.sendRawMessage(ChatUtil.translate(Main.getPrefix() + "&cYour party is too big to enter this floor."));
                    return;
                }
                if (DungeonParty.getDungeonPartyOf(player).getMembers().size() < floor.getRequirements().getPartyRequirements().getMinSize()) {
                    player.sendRawMessage(ChatUtil.translate(Main.getPrefix() + "&cYour party is too small to enter this floor."));
                    return;
                }
                FloorInstance floorInstance = new FloorInstance(floor.getId());
                player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&fPlease wait while the instance is being prepared..."));
                floorInstance.sendToServer(DungeonParty.getDungeonPartyOf(player));
            }
        }
    }

    private class PartyFinderButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.SPYGLASS)
                    .setName("<gradient:#8B0000:bold>" + ChatUtil.toSmallCaps("party finder") + "</gradient:#D10000>")
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
            new PartyFinderMenu(DungeonGateMenu.this,dungeon).openMenu(player);
        }
    }

    private static class InformationButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.REDSTONE_TORCH)
                    .setName("<gradient:#8B0000:bold>" + ChatUtil.toSmallCaps("information") + "</gradient:#D10000>")
                    .setLore(
                            "&7Information about dungeons.",
                            "",
                            "&#FFC700Click to view more details."
                    )
                    .toItemStack();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {

        }
    }

    private class ProfileButton extends Button {
        @Override
        public ItemStack getButtonItem(Player player) {

            return new ItemBuilder(Material.PLAYER_HEAD)
                    .setSkullOwner(player.getName())
                    .setName("&f" + ChatUtil.toSmallCaps(player.getName()))
                    .setLore(
                            "&7View your statistics, best",
                            "&7performances and more.",
                            "",
                            "&#FFC700Click to view your profile."
                    )
                    .toItemStack();
        }

        @Override
        public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
            new ProfileMenu(DungeonGateMenu.this,dungeon).openMenu(player);
        }
    }

    private static List<String> getFloorLore(Floor floor, Player player) {
        PlayerData playerData = PlayerData.get(player);
        ProfileData profileData = Main.getInstance().getProfileService().getProfileData(player.getUniqueId());

        ArrayList<String> lore = new ArrayList<>();

        lore.add("&7Party size: &e" + floor.getRequirements().getPartyRequirements().getMinSize() + " - " + floor.getRequirements().getPartyRequirements().getMaxSize());
        lore.add("");

        String[] description = floor.getDescription().split("\n");
        for(String line : description)
            lore.add("&7" + ChatUtil.translate(line));
        lore.add("");
        lore.add("&7Requirements:");
        if(floor.getRequirements().getMinLevel() > 0) {
            if(playerData.getLevel() >= floor.getRequirements().getMinLevel()) {
                lore.add("&a✔ Level: " + floor.getRequirements().getMinLevel());
            } else {
                lore.add("&c✘ Level: " + floor.getRequirements().getMinLevel());
            }
        }
        if(floor.getRequirements().getRequiredFloorsId() != null && !floor.getRequirements().getRequiredFloorsId().isEmpty()) {
            for(String requiredFloorId : floor.getRequirements().getRequiredFloorsId()) {
                Floor requiredFloor = Floor.getFloor(requiredFloorId);
                if(profileData.getCompletedFloors().contains(requiredFloorId)) {
                    lore.add("&a✔ " + requiredFloor.getName() + " Completion.");
                } else {
                    lore.add("&c✘ " + requiredFloor.getName() + " Completion.");
                }
            }
        }
        if(floor.getRequirements().getRequiredItems() != null && !floor.getRequirements().getRequiredItems().isEmpty()) {
            for(String requiredItem : floor.getRequirements().getRequiredItems()) {
                boolean hasItem = Arrays.stream(player.getInventory().getContents()).anyMatch(itemStack -> itemStack != null && Objects.requireNonNull(itemStack.getItemMeta()).getDisplayName().equals(requiredItem));
                if(hasItem) {
                    lore.add("&a✔ Possess " + requiredItem);
                } else {
                    lore.add("&c✘ Possess " + requiredItem);
                }
            }
        }
        if(floor.getRequirements().getForbiddenItems() != null && !floor.getRequirements().getForbiddenItems().isEmpty()) {
            for(String forbiddenItem : floor.getRequirements().getForbiddenItems()) {
                boolean hasItem = Arrays.stream(player.getInventory().getContents()).anyMatch(itemStack -> itemStack != null && Objects.requireNonNull(itemStack.getItemMeta()).getDisplayName().equals(forbiddenItem));
                if(hasItem) {
                    lore.add("&c✘ Do not possess " + forbiddenItem);
                } else {
                    lore.add("&a✔ Do not possess " + forbiddenItem);
                }
            }
        }
        lore.add("");
        lore.add("&eClick to enter the floor.");
        return lore;
    }
}
