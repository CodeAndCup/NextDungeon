package fr.perrier.dungeons.spigot.commands;

import fr.perrier.cupcodeapi.commands.annotations.Command;
import fr.perrier.cupcodeapi.commands.annotations.Param;
import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.parties.IParty;
import fr.perrier.dungeons.spigot.parties.IPartyMember;
import fr.perrier.dungeons.spigot.parties.PartyService;
import fr.perrier.dungeons.spigot.parties.impl.internal.InternalPartyProvider;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Player-facing commands to manage the <b>internal</b> ("NextDungeon") party
 * system : create / invite / accept / leave / kick / promote / disband / info.
 *
 * <p>These commands are only meaningful when the active {@link PartyService}
 * provider is the {@link InternalPartyProvider}. When an external party plugin
 * (e.g. AlessioDPParties) is the active provider, that plugin owns party
 * management and the commands politely defer to it ; see
 * {@link #requireInternal(Player)}.</p>
 *
 * <p>The internal provider has no built-in invitation flow, so a lightweight
 * pending-invite table lives here. Invites expire after {@link #INVITE_TTL_MS}.</p>
 */
public class PartyCommands {

    /** How long (ms) a pending invite stays valid. */
    private static final long INVITE_TTL_MS = 60_000L;

    /** Sentinel meaning "no explicit name given — derive one from the leader". */
    private static final String NAME_SENTINEL = "@self";

    /** invitee UUID -> pending invite (party id + expiry). */
    private static final Map<UUID, Invite> PENDING_INVITES = new ConcurrentHashMap<>();

    private record Invite(UUID partyId, String partyName, UUID inviterId, long expiresAtMs) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAtMs;
        }
    }

    @Command(names = {"dungeon party","dungeons party","nextdungeon party","nextdungeons party","nd party"})
    public static void onParty(CommandSender sender) {
        if (notAPlayer(sender)) return;
        Player player = (Player) sender;
        if (requireInternal(player) == null) return;

        sender.sendMessage(ChatUtil.getBar());
        sender.sendMessage(ChatUtil.translate("<gradient:#8B0000:bold>NextDungeon</gradient:#D10000> &8| &fParty Commands"));
        sender.sendMessage("");
        sender.sendMessage(ChatUtil.translate("&#D10000/party create [name] &8- &fCreate a party"));
        sender.sendMessage(ChatUtil.translate("&#D10000/party invite <player> &8- &fInvite a player"));
        sender.sendMessage(ChatUtil.translate("&#D10000/party accept &8- &fAccept a pending invite"));
        sender.sendMessage(ChatUtil.translate("&#D10000/party deny &8- &fDeny a pending invite"));
        sender.sendMessage(ChatUtil.translate("&#D10000/party leave &8- &fLeave your party"));
        sender.sendMessage(ChatUtil.translate("&#D10000/party kick <player> &8- &fKick a member &7(leader)"));
        sender.sendMessage(ChatUtil.translate("&#D10000/party promote <player> &8- &fTransfer leadership &7(leader)"));
        sender.sendMessage(ChatUtil.translate("&#D10000/party disband &8- &fDisband your party &7(leader)"));
        sender.sendMessage(ChatUtil.translate("&#D10000/party info &8- &fShow your party"));
        sender.sendMessage("");
        sender.sendMessage(ChatUtil.getBar());
    }

    @Command(names = {"dungeon party help","dungeons party help","nextdungeon party help","nextdungeons party help","nd party help"})
    public static void onPartyHelp(CommandSender sender) {
        onParty(sender);
    }

    @Command(names = {"dungeon party create","dungeons party create","nextdungeon party create","nextdungeons party create","nd party create"})
    public static void onCreate(CommandSender sender,
                                @Param(name = "name", wildcard = true, baseValue = NAME_SENTINEL) String name) {
        if (notAPlayer(sender)) return;
        Player player = (Player) sender;
        PartyService service = requireInternal(player);
        if (service == null) return;

        if (service.isInParty(player)) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 20.0F, 1.0F);
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000You are already in a party. Leave it first."));
            return;
        }

        String partyName = NAME_SENTINEL.equals(name.trim()) ? player.getName() + "'s Party" : name.trim();
        Optional<IParty> created = service.createParty(player, partyName);
        if (created.isEmpty()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 20.0F, 1.0F);
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Could not create the party."));
            return;
        }
        player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&fParty &e" + partyName + " &fcreated. Use &e/party invite <player> &fto add members."));
    }

    @Command(names = {"dungeon party invite","dungeons party invite","nextdungeon party invite","nextdungeons party invite","nd party invite"})
    public static void onInvite(CommandSender sender, @Param(name = "Player") String targetName) {
        if (notAPlayer(sender)) return;
        Player player = (Player) sender;
        PartyService service = requireInternal(player);
        if (service == null) return;

        Optional<IParty> partyOpt = service.getPartyOf(player);
        if (partyOpt.isEmpty()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 20.0F, 1.0F);
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000You are not in a party. Use &e/party create&#FF0000 first."));
            return;
        }
        IParty party = partyOpt.get();
        if (!party.getLeaderId().equals(player.getUniqueId())) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Only the party leader can invite players."));
            return;
        }

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null || !target.isOnline()) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Player &e" + targetName + " &#FF0000is not online."));
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000You cannot invite yourself."));
            return;
        }
        if (party.isMember(target)) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&e" + target.getName() + " &#FF0000is already in your party."));
            return;
        }
        if (service.isInParty(target)) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&e" + target.getName() + " &#FF0000is already in another party."));
            return;
        }

        PENDING_INVITES.put(target.getUniqueId(),
                new Invite(party.getPartyId(), party.getName(), player.getUniqueId(),
                        System.currentTimeMillis() + INVITE_TTL_MS));

        player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&fInvited &e" + target.getName() + " &fto the party."));
        target.sendMessage(ChatUtil.translate(Main.getPrefix() + "&e" + player.getName()
                + " &finvited you to &e" + party.getName() + "&f. Use &a/party accept &for &c/party deny&f (60s)."));
    }

    @Command(names = {"dungeon party accept","dungeons party accept","nextdungeon party accept","nextdungeons party accept","nd party accept"})
    public static void onAccept(CommandSender sender) {
        if (notAPlayer(sender)) return;
        Player player = (Player) sender;
        PartyService service = requireInternal(player);
        if (service == null) return;

        Invite invite = PENDING_INVITES.remove(player.getUniqueId());
        if (invite == null || invite.isExpired()) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000You have no pending party invite."));
            return;
        }
        if (service.isInParty(player)) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000You are already in a party. Leave it first."));
            return;
        }

        Optional<IParty> partyOpt = service.getParty(invite.partyId());
        if (partyOpt.isEmpty() || !partyOpt.get().exists()) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000That party no longer exists."));
            return;
        }
        IParty party = partyOpt.get();

        if (!party.addMember(player)) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Could not join the party."));
            return;
        }
        // The internal party's addMember does not update the provider's
        // member->party index, so register it here.
        InternalPartyProvider.getInstance().registerMember(player.getUniqueId(), party.getPartyId());

        player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&fYou joined &e" + party.getName() + "&f."));
        broadcast(party, "&e" + player.getName() + " &fjoined the party.", player.getUniqueId());
    }

    @Command(names = {"dungeon party deny","dungeons party deny","nextdungeon party deny","nextdungeons party deny","nd party deny"})
    public static void onDeny(CommandSender sender) {
        if (notAPlayer(sender)) return;
        Player player = (Player) sender;
        if (requireInternal(player) == null) return;

        Invite invite = PENDING_INVITES.remove(player.getUniqueId());
        if (invite == null || invite.isExpired()) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000You have no pending party invite."));
            return;
        }
        player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&fInvite to &e" + invite.partyName() + " &fdeclined."));
        Player inviter = Bukkit.getPlayer(invite.inviterId());
        if (inviter != null) {
            inviter.sendMessage(ChatUtil.translate(Main.getPrefix() + "&e" + player.getName() + " &fdeclined your party invite."));
        }
    }

    @Command(names = {"dungeon party leave","dungeons party leave","nextdungeon party leave","nextdungeons party leave","nd party leave"})
    public static void onLeave(CommandSender sender) {
        if (notAPlayer(sender)) return;
        Player player = (Player) sender;
        PartyService service = requireInternal(player);
        if (service == null) return;

        Optional<IParty> partyOpt = service.getPartyOf(player);
        if (partyOpt.isEmpty()) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000You are not in a party."));
            return;
        }
        IParty party = partyOpt.get();
        InternalPartyProvider provider = InternalPartyProvider.getInstance();

        boolean isLeader = party.getLeaderId().equals(player.getUniqueId());
        if (isLeader) {
            // Hand leadership to another member if any, otherwise disband.
            Optional<UUID> heir = party.getMemberIds().stream()
                    .filter(id -> !id.equals(player.getUniqueId()))
                    .findFirst();
            if (heir.isEmpty()) {
                party.disband(); // clears the provider index for us
                player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&fYou left and the party was disbanded."));
                return;
            }
            party.setLeader(heir.get());
            party.removeMember(player);
            provider.unregisterMember(player.getUniqueId());
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&fYou left the party."));
            broadcast(party, "&e" + player.getName() + " &fleft. &e"
                    + nameOf(heir.get()) + " &fis the new leader.", null);
            return;
        }

        party.removeMember(player);
        provider.unregisterMember(player.getUniqueId());
        player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&fYou left the party."));
        broadcast(party, "&e" + player.getName() + " &fleft the party.", null);
    }

    @Command(names = {"dungeon party kick","dungeons party kick","nextdungeon party kick","nextdungeons party kick","nd party kick"})
    public static void onKick(CommandSender sender, @Param(name = "Player") String targetName) {
        if (notAPlayer(sender)) return;
        Player player = (Player) sender;
        PartyService service = requireInternal(player);
        if (service == null) return;

        Optional<IParty> partyOpt = service.getPartyOf(player);
        if (partyOpt.isEmpty()) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000You are not in a party."));
            return;
        }
        IParty party = partyOpt.get();
        if (!party.getLeaderId().equals(player.getUniqueId())) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Only the party leader can kick members."));
            return;
        }

        UUID targetId = resolveMember(party, targetName);
        if (targetId == null) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&e" + targetName + " &#FF0000is not in your party."));
            return;
        }
        if (targetId.equals(player.getUniqueId())) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000You cannot kick yourself. Use &e/party disband &#FF0000or &e/party leave&#FF0000."));
            return;
        }

        party.removeMember(targetId);
        InternalPartyProvider.getInstance().unregisterMember(targetId);
        player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&fKicked &e" + nameOf(targetId) + " &ffrom the party."));
        Player kicked = Bukkit.getPlayer(targetId);
        if (kicked != null) {
            kicked.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000You were kicked from the party."));
        }
        broadcast(party, "&e" + nameOf(targetId) + " &fwas kicked from the party.", null);
    }

    @Command(names = {"dungeon party promote","dungeons party promote","nextdungeon party promote","nextdungeons party promote","nd party promote"})
    public static void onPromote(CommandSender sender, @Param(name = "Player") String targetName) {
        if (notAPlayer(sender)) return;
        Player player = (Player) sender;
        PartyService service = requireInternal(player);
        if (service == null) return;

        Optional<IParty> partyOpt = service.getPartyOf(player);
        if (partyOpt.isEmpty()) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000You are not in a party."));
            return;
        }
        IParty party = partyOpt.get();
        if (!party.getLeaderId().equals(player.getUniqueId())) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Only the party leader can transfer leadership."));
            return;
        }

        UUID targetId = resolveMember(party, targetName);
        if (targetId == null || targetId.equals(player.getUniqueId())) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&e" + targetName + " &#FF0000is not a member of your party."));
            return;
        }

        party.setLeader(targetId);
        player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&e" + nameOf(targetId) + " &fis now the party leader."));
        broadcast(party, "&e" + nameOf(targetId) + " &fis now the party leader.", player.getUniqueId());
        Player promoted = Bukkit.getPlayer(targetId);
        if (promoted != null) {
            promoted.sendMessage(ChatUtil.translate(Main.getPrefix() + "&fYou are now the leader of the party."));
        }
    }

    @Command(names = {"dungeon party disband","dungeons party disband","nextdungeon party disband","nextdungeons party disband","nd party disband"})
    public static void onDisband(CommandSender sender) {
        if (notAPlayer(sender)) return;
        Player player = (Player) sender;
        PartyService service = requireInternal(player);
        if (service == null) return;

        Optional<IParty> partyOpt = service.getPartyOf(player);
        if (partyOpt.isEmpty()) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000You are not in a party."));
            return;
        }
        IParty party = partyOpt.get();
        if (!party.getLeaderId().equals(player.getUniqueId())) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Only the party leader can disband the party."));
            return;
        }

        broadcast(party, "&#FF0000The party has been disbanded.", null);
        party.disband();
        player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&fParty disbanded."));
    }

    @Command(names = {"dungeon party info","dungeons party info","nextdungeon party info","nextdungeons party info","nd party info",
            "dungeon party list","dungeons party list","nextdungeon party list","nextdungeons party list","nd party list"})
    public static void onInfo(CommandSender sender) {
        if (notAPlayer(sender)) return;
        Player player = (Player) sender;
        PartyService service = requireInternal(player);
        if (service == null) return;

        Optional<IParty> partyOpt = service.getPartyOf(player);
        if (partyOpt.isEmpty()) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000You are not in a party."));
            return;
        }
        IParty party = partyOpt.get();

        sender.sendMessage(ChatUtil.getBar());
        sender.sendMessage(ChatUtil.translate("<gradient:#8B0000:bold>NextDungeon</gradient:#D10000> &8| &f" + party.getName()));
        sender.sendMessage("");
        sender.sendMessage(ChatUtil.translate("&#D10000Members &7(" + party.getSize() + ") &8:"));
        for (IPartyMember member : party.getMembers()) {
            boolean leader = member.getUniqueId().equals(party.getLeaderId());
            String status = member.isOnline() ? "&a●" : "&7●";
            sender.sendMessage(ChatUtil.translate("  &8• " + status + " &f" + member.getName()
                    + (leader ? " &6(Leader)" : "")));
        }
        sender.sendMessage("");
        sender.sendMessage(ChatUtil.getBar());
    }

    /**
     * Returns the {@link PartyService} if the internal provider is active,
     * otherwise messages the player (external plugin owns parties) and returns
     * {@code null}.
     */
    private static PartyService requireInternal(Player player) {
        PartyService service = PartyService.getInstance();
        if (service == null || !(service.getActiveProvider() instanceof InternalPartyProvider)) {
            String provider = service != null ? service.getActiveProviderName() : "unknown";
            player.sendMessage(ChatUtil.translate(Main.getPrefix()
                    + "&#FF0000Party management is handled by the &e" + provider
                    + " &#FF0000plugin — use its own commands."));
            return null;
        }
        return service;
    }

    /** Resolves a member of {@code party} from a (case-insensitive) name. */
    private static UUID resolveMember(IParty party, String name) {
        for (IPartyMember member : party.getMembers()) {
            if (member.getName() != null && member.getName().equalsIgnoreCase(name)) {
                return member.getUniqueId();
            }
        }
        return null;
    }

    private static String nameOf(UUID id) {
        Player online = Bukkit.getPlayer(id);
        if (online != null) return online.getName();
        String name = Bukkit.getOfflinePlayer(id).getName();
        return name != null ? name : id.toString();
    }

    /** Sends {@code message} to every online member except {@code exclude} (nullable). */
    private static void broadcast(IParty party, String message, UUID exclude) {
        for (IPartyMember member : party.getOnlineMembers()) {
            if (exclude != null && member.getUniqueId().equals(exclude)) continue;
            Player p = member.getPlayer();
            if (p != null) {
                p.sendMessage(ChatUtil.translate(Main.getPrefix() + message));
            }
        }
    }

    private static boolean notAPlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000This command can only be used by players"));
            return true;
        }
        return false;
    }
}
