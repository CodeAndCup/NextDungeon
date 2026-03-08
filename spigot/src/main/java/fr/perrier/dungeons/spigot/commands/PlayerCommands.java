package fr.perrier.dungeons.spigot.commands;

import fr.perrier.cupcodeapi.commands.annotations.Command;
import fr.perrier.cupcodeapi.commands.annotations.Param;
import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.dungeons.common.model.dungeon.FloorData;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.model.Dungeon;
import fr.perrier.dungeons.spigot.model.Floor;
import fr.perrier.dungeons.spigot.parties.impl.DungeonPartyImpl;
import fr.perrier.dungeons.spigot.queue.QueuePosition;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PlayerCommands {

    @Command(names = {"dungeon","dungeons","nextdungeon","nextdungeons","nd"})
    public static void onDungeonCommand(CommandSender sender) {
        sender.sendMessage(ChatUtil.getBar());
        sender.sendMessage(ChatUtil.translate("<gradient:#8B0000:bold>NextDungeon</gradient:#D10000> &8| &fPlayer Commands"));
        sender.sendMessage("");
        sender.sendMessage(ChatUtil.translate("&#D10000/dungeon &8- &fGet the list of available commands"));
        sender.sendMessage(ChatUtil.translate("&#D10000/dungeon join <floor_id> &8- &fJoin a dungeon queue"));
        sender.sendMessage(ChatUtil.translate("&#D10000/dungeon leave <floor_id> &8- &fLeave a dungeon queue"));
        sender.sendMessage(ChatUtil.translate("&#D10000/dungeon status &8- &fCheck your queue status"));
        sender.sendMessage(ChatUtil.translate("&#D10000/dungeon list &8- &fList available dungeons"));
        sender.sendMessage("");
        sender.sendMessage(ChatUtil.getBar());
    }

    @Command(names = {"dungeon help","dungeons help","nextdungeon help","nextdungeons help","nd help"})
    public static  void onDungeonHelpCommand(CommandSender sender) {
        onDungeonCommand(sender);
    }

    @Command(names = {"dungeon join", "dungeons join", "nd join"})
    public static void onDungeonJoinCommand(CommandSender sender, @Param(name = "Floor ID", tabCompleteFlags = {"floors"}) FloorData floorData) {
        if(isSenderNotAPlayer(sender)) return;
        Player player = (Player) sender;

        if (Main.getInstance().getQueueManager() == null) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Queue system is not available on this server"));
            return;
        }

        if (floorData == null) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000FloorData not found."));
            return;
        }

        Floor floor = Floor.getFloor(floorData.getId());
        if (floor == null) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Floor not found: " + floorData.getId()));
            return;
        }

        // Create a temporary party for the player if they don't have one, so they can join the queue as a solo player.
        if(DungeonPartyImpl.getDungeonPartyOf(player) == null) {
            new DungeonPartyImpl.Builder()
                    .setDungeonId(floorData.getDungeonId())
                    .setFloorId(floorData.getId())
                    .setMinLevel(0)
                    .setDescription("")
                    .setLeader(player)
                    .build();
        }

        Main.getInstance().getQueueManager().requestInstance(player, floor);
    }

    @Command(names = {"dungeon leave", "dungeons leave", "nd leave"})
    public static void onDungeonLeaveCommand(CommandSender sender, @Param(name = "Floor ID", tabCompleteFlags = {"floors"}) FloorData floorData) {
        if(isSenderNotAPlayer(sender)) return;
        Player player = (Player) sender;

        if (Main.getInstance().getQueueManager() == null) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Queue system is not available on this server"));
            return;
        }

        if (floorData == null) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000FloorData not found."));
            return;
        }

        if (Main.getInstance().getQueueManager().removePlayerFromQueue(player, floorData.getId())) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "Removed from queue for floor: " + floorData.getId()));
        } else {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000You are not in the queue for this floor"));
        }
    }

    @Command(names = {"dungeon status", "dungeons status", "nd status"})
    public static void onDungeonStatusCommand(CommandSender sender) {
        if(isSenderNotAPlayer(sender)) return;
        Player player = (Player) sender;

        if (Main.getInstance().getQueueManager() == null) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Queue system is not available on this server"));
            return;
        }

        sender.sendMessage(ChatUtil.getBar());
        sender.sendMessage(ChatUtil.translate("<gradient:#8B0000:bold>NextDungeon</gradient:#D10000> &8| &fQueue Status"));
        sender.sendMessage("");

        boolean foundInQueue = false;
        for (String floorId : Main.getInstance().getDungeonQueueService().getActiveQueueFloors()) {
            QueuePosition position = Main.getInstance().getQueueManager().getPlayerPosition(player.getUniqueId(), floorId);
            if (position != null) {
                Floor floor = Floor.getFloor(floorId);
                String floorName = floor != null ? floor.getName() : floorId;
                sender.sendMessage(ChatUtil.translate(String.format(
                    "&#D10000%s &8- &fPosition: &e%d/%d",
                    floorName,
                    position.getPosition(),
                    position.getTotalInQueue()
                )));
                foundInQueue = true;
            }
        }

        if (!foundInQueue) {
            sender.sendMessage(ChatUtil.translate("&7You are not in any queue"));
        }

        sender.sendMessage("");
        sender.sendMessage(ChatUtil.getBar());
    }

    @Command(names = {"dungeon list", "dungeons list", "nd list"})
    public static void onDungeonListCommand(CommandSender sender) {
        sender.sendMessage(ChatUtil.getBar());
        sender.sendMessage(ChatUtil.translate("<gradient:#8B0000:bold>NextDungeon</gradient:#D10000> &8| &fAvailable Dungeons"));
        sender.sendMessage("");

        var queueService = Main.getInstance().getDungeonQueueService();
        
        for (Dungeon dungeon : Dungeon.getDungeons()) {
            sender.sendMessage(ChatUtil.translate("&#D10000" + dungeon.getName() + " &7(" + dungeon.getId() + ")"));
            for (Floor floor : dungeon.getFloors()) {
                int queueSize = queueService != null ? queueService.getQueueSize(floor.getId()) : 0;
                int activeInstances = queueService != null ? queueService.getActiveInstanceCount(floor.getId()) : 0;
                int maxInstances = floor.getRules() != null ? floor.getRules().getMaxInstance() : 0;
                
                sender.sendMessage(ChatUtil.translate(String.format(
                    "  &8• &f%s &7(ID: %s) - &eQueue: %d &7| &eInstances: %d/%s",
                    floor.getName(),
                    floor.getId(),
                    queueSize,
                    activeInstances,
                    maxInstances > 0 ? String.valueOf(maxInstances) : "∞"
                )));
            }
        }

        sender.sendMessage("");
        sender.sendMessage(ChatUtil.getBar());
    }

    private static boolean isSenderNotAPlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000This command can only be used by players"));
            return true;
        }
        return false;
    }
}
