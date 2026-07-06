package fr.perrier.dungeons.spigot.menu.dungeon;

import fr.perrier.cupcodeapi.menuapi.Button;
import fr.perrier.cupcodeapi.menuapi.GlassMenu;
import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.cupcodeapi.utils.ItemBuilder;
import fr.perrier.dungeons.common.model.dungeon.FloorType;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.model.Dungeon;
import fr.perrier.dungeons.spigot.model.Floor;
import fr.perrier.dungeons.spigot.menu.utils.MenuTitle;
import fr.perrier.dungeons.spigot.model.FloorInstance;
import fr.perrier.dungeons.spigot.model.ProfileData;
import fr.perrier.dungeons.spigot.parties.CrossServerValidationService;
import fr.perrier.dungeons.spigot.parties.impl.DungeonPartyImpl;
import lombok.RequiredArgsConstructor;
import net.Indyuce.mmocore.api.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

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
        String name = "&#8B0000&l" + ChatUtil.toSmallCaps(dungeon.getName());
        if(dungeon.getFloors().getFirst().getFloorType() == FloorType.LABYRINTH) {
            return MenuTitle.of(MenuTitle.LABYRINTH, name);
        }
        // Footer buttons sit at slot 42 (<=5 floors) or 51 (>5), giving a 5- or 6-row inventory.
        int rows = dungeon.getFloors().size() <= 5 ? 5 : 6;
        return MenuTitle.ofRows(rows, name);
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
        /**
         * Party leaders whose dungeon launch is mid-flight — between the click and the instance
         * actually registering in Redis. Instance creation is fully async (party validation +
         * cloud service spin-up), so during that window {@code isPlayerInAnyInstance}
         * still returns {@code false} and every extra click would spawn a separate instance.
         * Acquiring the leader's slot here closes that race.
         */
        private static final Set<UUID> launchingLeaders = ConcurrentHashMap.newKeySet();

        /** Safety net (in ticks) after which the launch lock is released even if no callback fired. */
        private static final long LAUNCH_LOCK_TIMEOUT_TICKS = 200L; // 10 seconds

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
            //TODO: Warning player that don't have create a party can't play solo, they need to create a party builder first, need to be fixed

            // Prevent clicking if any player of that party is already in an instance or being prepared
            if (DungeonPartyImpl.hasLeadParty(player)) {
                UUID leaderId = DungeonPartyImpl.getDungeonPartyOf(player).getLeaderId();
                if (Main.getInstance().getDungeonService().isPlayerInAnyInstance(leaderId)) {
                    player.sendRawMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000An instance for your party is already being prepared or active. Please wait..."));
                    return;
                }
            }

            // Original requirement checks
            if(Objects.requireNonNull(Objects.requireNonNull(getButtonItem(player).getItemMeta()).getLore()).stream().anyMatch(s -> s.contains("✘"))) {
                player.sendRawMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000You do not meet the requirements to enter this floor."));
                return;
            }
            if(DungeonPartyImpl.hasLeadParty(player)) {
                DungeonPartyImpl party = DungeonPartyImpl.getDungeonPartyOf(player);
                if(party.getMembers().size() > floor.getRequirements().getPartyRequirements().getMaxSize()) {
                    player.sendRawMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Your party is too big to enter this floor."));
                    return;
                }
                if (party.getMembers().size() < floor.getRequirements().getPartyRequirements().getMinSize()) {
                    player.sendRawMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Your party is too small to enter this floor."));
                    return;
                }

                // Acquire the launch lock for this party leader. add() returns false when a launch
                // is already in flight, so rapid double-clicks are rejected before any async work.
                UUID leaderId = party.getLeaderId();
                if (!launchingLeaders.add(leaderId)) {
                    player.sendRawMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000A dungeon launch is already in progress for your party. Please wait..."));
                    return;
                }
                // Safety net: never strand the leader behind the lock if a callback is somehow lost.
                Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> launchingLeaders.remove(leaderId), LAUNCH_LOCK_TIMEOUT_TICKS);

                player.sendRawMessage(ChatUtil.translate(Main.getPrefix() + "&fChecking party requirements..."));
                Set<UUID> memberIds = party.getMemberIds();
                validateAllMembersAsync(floor, memberIds).thenAccept(allPassed -> {
                    if (!allPassed) {
                        launchingLeaders.remove(leaderId);
                        Bukkit.getScheduler().runTask(Main.getInstance(), () ->
                                player.sendRawMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000One or more players in your party break the requirements to enter this floor.")));
                        return;
                    }

                    Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                        // Consume the required items now — on the lobby, before any instance loading —
                        // so a member can't drop the item during provisioning to dodge the cost.
                        Map<UUID, List<ItemStack>> consumedItems =
                                CrossServerValidationService.consumeForAllMembers(floor, memberIds);
                        FloorInstance.generateNewInstanceAsync(floor.getId(), memberIds, false, floorInstance -> {
                            if (floorInstance == null) {
                                // Launch aborted after consumption: refund the local members.
                                CrossServerValidationService.refundLocalMembers(consumedItems);
                                launchingLeaders.remove(leaderId);
                                player.sendRawMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Failed to create the dungeon instance. Please try again."));
                                return;
                            }
                            // Pull the party out of the finder as soon as the dungeon starts —
                            // the party itself stays alive so members return together afterwards.
                            party.setListed(false);
                            floorInstance.sendToServer(party);
                            // Keep the lock held: by the time the safety-net timeout releases it the
                            // instance is registered in Redis, so isPlayerInAnyInstance takes over.
                        });
                        player.sendRawMessage(ChatUtil.translate(Main.getPrefix() + "&fPlease wait while the instance is being prepared..."));
                    });
                });
            }
        }

        /**
         * Validates every party member against the floor's requirements, running local checks
         * inline and cross-server ones through {@link CrossServerValidationService}. Returns a
         * future that completes with {@code true} only if every member passes.
         */
        private CompletableFuture<Boolean> validateAllMembersAsync(Floor floor, Set<UUID> memberIds) {
            List<CompletableFuture<Boolean>> futures = new ArrayList<>();
            for (UUID memberId : memberIds) {
                Player local = Bukkit.getPlayer(memberId);
                if (local != null) {
                    futures.add(CompletableFuture.completedFuture(floor.isRequirementsValid(local)));
                } else {
                    CrossServerValidationService service = CrossServerValidationService.getInstance();
                    if (service == null) {
                        futures.add(CompletableFuture.completedFuture(false));
                    } else {
                        futures.add(service.validateRemote(floor.getId(), memberId));
                    }
                }
            }
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> futures.stream().allMatch(CompletableFuture::join));
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
                lore.add("&#00FF00✔ Level: " + floor.getRequirements().getMinLevel());
            } else {
                lore.add("&#FF0000✘ Level: " + floor.getRequirements().getMinLevel());
            }
        }
        if(floor.getRequirements().getRequiredFloorsId() != null && !floor.getRequirements().getRequiredFloorsId().isEmpty()) {
            for(String requiredFloorId : floor.getRequirements().getRequiredFloorsId()) {
                Floor requiredFloor = Floor.getFloor(requiredFloorId);
                String requiredFloorName = requiredFloor != null ? requiredFloor.getName() : requiredFloorId;
                if(profileData.getCompletedFloors().contains(requiredFloorId)) {
                    lore.add("&#00FF00✔ " + requiredFloorName + " Completion.");
                } else {
                    lore.add("&#FF0000✘ " + requiredFloorName + " Completion.");
                }
            }
        }
        if(floor.getRequirements().getRequiredItems() != null && !floor.getRequirements().getRequiredItems().isEmpty()) {
            for(String requiredItem : floor.getRequirements().getRequiredItems()) {
                boolean hasItem = floor.playerHasItemMatching(player, requiredItem);
                if(hasItem) {
                    lore.add("&#00FF00✔ Possess " + requiredItem);
                } else {
                    lore.add("&#FF0000✘ Possess " + requiredItem);
                }
            }
        }
        if(floor.getRequirements().getForbiddenItems() != null && !floor.getRequirements().getForbiddenItems().isEmpty()) {
            for(String forbiddenItem : floor.getRequirements().getForbiddenItems()) {
                boolean hasItem = floor.playerHasItemMatching(player, forbiddenItem);
                if(hasItem) {
                    lore.add("&#FF0000✘ Do not possess " + forbiddenItem);
                } else {
                    lore.add("&#00FF00✔ Do not possess " + forbiddenItem);
                }
            }
        }
        lore.add("");
        lore.add("&eClick to enter the floor.");
        return lore;
    }
}
