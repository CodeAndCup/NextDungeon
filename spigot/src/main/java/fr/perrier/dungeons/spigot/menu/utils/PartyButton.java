package fr.perrier.dungeons.spigot.menu.utils;

import fr.perrier.cupcodeapi.menuapi.Button;
import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.cupcodeapi.utils.ItemBuilder;
import fr.perrier.dungeons.common.model.party.DungeonPartyData;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.messaging.packets.DungeonPartyJoinRequestPacket;
import fr.perrier.dungeons.spigot.parties.IDungeonParty;
import fr.perrier.dungeons.spigot.parties.IPartyMember;
import fr.perrier.dungeons.spigot.parties.impl.DungeonPartyRegistry;
import fr.perrier.dungeons.spigot.parties.impl.RemoteDungeonParty;
import net.Indyuce.mmocore.api.player.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class PartyButton extends Button {
    private final IDungeonParty dungeonParty;
    private final String leaderName;

    public PartyButton(IDungeonParty dungeonParty) {
        this.dungeonParty = dungeonParty;
        this.leaderName = resolveLeaderName(dungeonParty);
    }

    @Override
    public ItemStack getButtonItem(Player player) {
        String displayName = leaderName != null ? leaderName : "Unknown";

        ItemStack head = new ItemBuilder(Material.PLAYER_HEAD)
                .setName("&f&l" + ChatUtil.toSmallCaps(displayName + "'s Party"))
                .setLore(getLore(dungeonParty))
                .toItemStack();
        applyLeaderSkin(head);
        return head;
    }

    /**
     * Sets the leader's real Mojang skin on the head. Uses the skin URL pushed by the owner
     * server via {@link fr.perrier.dungeons.common.model.party.DungeonPartyData#getLeaderSkinUrl()}
     * when available — Bukkit's {@code setSkullOwner(name)} can't resolve it cross-server because
     * the local server has never downloaded the profile for a leader it has never hosted.
     * Falls back to the name-based lookup, and ultimately to the default skin.
     */
    private void applyLeaderSkin(ItemStack head) {
        if (!(head.getItemMeta() instanceof SkullMeta meta)) return;

        DungeonPartyData data = resolveData(dungeonParty);
        String skinUrl = data != null ? data.getLeaderSkinUrl() : null;

        try {
            UUID leaderId = dungeonParty.getLeaderId();
            PlayerProfile profile = Bukkit.createPlayerProfile(leaderId, leaderName);
            if (skinUrl != null) {
                PlayerTextures textures = profile.getTextures();
                textures.setSkin(URI.create(skinUrl).toURL());
                profile.setTextures(textures);
            }
            meta.setOwnerProfile(profile);
            head.setItemMeta(meta);
        } catch (Exception e) {
            // If anything goes wrong (malformed URL, provider issues), fall back to the local
            // OfflinePlayer so the head at least renders correctly for leaders this server knows.
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(dungeonParty.getLeaderId()));
            head.setItemMeta(meta);
        }
    }

    @Override
    public void clicked(Player player, int slot, ClickType clickType, int hotbarButton) {
        String displayName = leaderName != null ? leaderName : "this";

        if (dungeonParty.getMemberIds().contains(player.getUniqueId())) {
            player.sendMessage(ChatUtil.translate("&#FF0000You are already in this party!"));
            return;
        }

        if (dungeonParty instanceof RemoteDungeonParty remote) {
            // Party is hosted on another server — route the join through Pidgin so the owner
            // server applies the mutation against the live IParty and re-publishes the DTO.
            // We also carry our display info (name + MMOCore class/level) because the owner
            // server can't resolve it otherwise.
            PlayerData ourData = PlayerData.get(player);
            String ourClass = ourData != null && ourData.getProfess() != null ? ourData.getProfess().getName() : null;
            int ourLevel = ourData != null ? ourData.getLevel() : 0;

            Main.getInstance().getMessaging().sendPacket(new DungeonPartyJoinRequestPacket(
                    remote.getLeaderId(),
                    player.getUniqueId(),
                    remote.getData().getOwnerServiceId(),
                    player.getName(),
                    ourClass,
                    ourLevel
            ));
            player.sendMessage(ChatUtil.translate("&#00FF00Join request sent to " + displayName + "'s party!"));
        } else {
            dungeonParty.addMember(player);
            player.sendMessage(ChatUtil.translate("&#00FF00You have been added to " + displayName + "'s party!"));
        }
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
        lore.add("&7Floor: &#90FFFF" + Objects.requireNonNull(Main.getInstance().getDungeonService().getFloor(dungeonParty.getFloorId())).getName());
        lore.add("&7Description: &f" + dungeonParty.getDescription());
        lore.add("&7Min Level: &#90FFFF" + dungeonParty.getMinLevel());
        lore.add("&7Members:");

        // Read member info from the canonical DTO in the registry so cross-server data (names,
        // MMOCore class/level) stays consistent regardless of which lobby is rendering.
        DungeonPartyData data = resolveData(dungeonParty);

        int maxMemberPrinted = 5;
        IPartyMember[] members = dungeonParty.getMembers().toArray(new IPartyMember[0]);
        for(int i = 0; i < maxMemberPrinted; i++) {
            if (members.length <= i) {
                lore.add(" &8Empty");
            } else {
                lore.add(" " + formatMemberLine(members[i].getUniqueId(), data));
            }
        }
        lore.add(" &8&o+ " + (dungeonParty.getSize() <= 5 ? 0 : dungeonParty.getSize() - 5) + " other players..");
        lore.add("");
        lore.add("&#FFC700Click to join");

        return lore;
    }

    /**
     * Returns the party DTO from the registry if possible — covers both local parties (data was
     * just published by publishToRegistry) and remote parties (data is embedded in the wrapper).
     */
    private static DungeonPartyData resolveData(IDungeonParty dungeonParty) {
        if (dungeonParty instanceof RemoteDungeonParty remote) return remote.getData();
        DungeonPartyRegistry registry = DungeonPartyRegistry.getInstance();
        return registry != null ? registry.get(dungeonParty.getLeaderId()) : null;
    }

    /**
     * Builds a single member line. Prefers a live {@code PlayerData} for members online on this
     * server, and falls back to the DTO snapshot (name + class + level) pushed by the owner
     * server for everyone else. Last resort is a truncated UUID to avoid crashing on missing data.
     */
    private static String formatMemberLine(UUID memberId, DungeonPartyData data) {
        Player online = Bukkit.getPlayer(memberId);
        if (online != null) {
            PlayerData playerData = PlayerData.get(online);
            if (playerData != null && playerData.getProfess() != null) {
                return "&b" + online.getName() + "&f: &e" + playerData.getProfess().getName()
                        + " &7[&f" + playerData.getLevel() + "&7]";
            }
            return "&b" + online.getName();
        }

        String name = data != null && data.getMemberNames() != null ? data.getMemberNames().get(memberId) : null;
        String className = data != null && data.getMemberClasses() != null ? data.getMemberClasses().get(memberId) : null;
        Integer level = data != null && data.getMemberLevels() != null ? data.getMemberLevels().get(memberId) : null;

        if (name == null) {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(memberId);
            name = offline.getName();
        }
        if (name == null) {
            return "&b" + memberId.toString().substring(0, 8);
        }
        if (className != null && level != null) {
            return "&b" + name + "&f: &e" + className + " &7[&f" + level + "&7]";
        }
        return "&b" + name;
    }

    private static String resolveLeaderName(IDungeonParty dungeonParty) {
        if (dungeonParty instanceof RemoteDungeonParty remote) {
            DungeonPartyData data = remote.getData();
            if (data.getLeaderName() != null) return data.getLeaderName();
        }

        Player online = Bukkit.getPlayer(dungeonParty.getLeaderId());
        if (online != null) return online.getName();

        OfflinePlayer offline = Bukkit.getOfflinePlayer(dungeonParty.getLeaderId());
        return offline.getName();
    }
}
