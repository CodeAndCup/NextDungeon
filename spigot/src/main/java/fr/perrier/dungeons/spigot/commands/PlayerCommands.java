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
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PlayerCommands {

    @Command(names = {"dungeon","dungeons","nextdungeon","nextdungeons","nd"})
    public static void onDungeonCommand(CommandSender sender) {
        sender.sendMessage(ChatUtil.getBar());
        sender.sendMessage(ChatUtil.translate("<gradient:#8B0000:bold>NextDungeon</gradient:#D10000> &8| &fPlayer Commands"));
        sender.sendMessage("");
        sender.sendMessage(ChatUtil.translate("&#D10000/nd &8» &fShow this help"));
        sender.sendMessage(ChatUtil.translate("&#D10000/nd status &8» &fCheck your position in the queues"));
        sender.sendMessage(ChatUtil.translate("&#D10000/nd leave &8<&7floor_id&8> &8» &fLeave a dungeon queue"));
        sender.sendMessage(ChatUtil.translate("&#D10000/nd party &8» &fManage your party &7(see &#D10000/nd party help&7)"));
        sender.sendMessage(ChatUtil.translate("&#D10000/nd plugin &8» &fPlugin information"));
        sender.sendMessage("");
        sender.sendMessage(ChatUtil.translate("&8Aliases: &7/dungeon&8, &7/dungeons&8, &7/nextdungeon"));
        sender.sendMessage(ChatUtil.getBar());
    }

    @Command(names = {"dungeon help","dungeons help","nextdungeon help","nextdungeons help","nd help"})
    public static  void onDungeonHelpCommand(CommandSender sender) {
        onDungeonCommand(sender);
    }

    @Command(names = {"dungeon plugin", "dungeons plugin", "nextdungeon plugin", "nextdungeons plugin", "nd plugin"})
    public static void onDungeonPluginCommand(CommandSender sender) {
        sender.sendMessage(ChatUtil.getBar());
        sender.sendMessage(ChatUtil.translate("<gradient:#8B0000:bold>NextDungeon</gradient:#D10000> &8| &fPlugin Information"));
        sender.sendMessage("");
        sender.sendMessage(ChatUtil.translate("&#D10000Author: &7" + Main.getInstance().getDescription().getAuthors()));
        sender.sendMessage(ChatUtil.translate("&#D10000Version: &7" + Main.getInstance().getDescription().getVersion()));
        sender.sendMessage(ChatUtil.translate("&#D10000GitHub: &7https://github.com/CodeAndCup/NextDungeon"));
        sender.sendMessage(ChatUtil.translate("&#D10000Website: &7" + Main.getInstance().getDescription().getWebsite()));
        sender.sendMessage("");
        sender.sendMessage(ChatUtil.getBar());
    }

    @Command(names = {"dungeon leave", "dungeons leave", "nextdungeon leave", "nextdungeons leave", "nd leave"})
    public static void onDungeonLeaveCommand(CommandSender sender, @Param(name = "Floor ID", tabCompleteFlags = {"floors"}) FloorData floorData) {
        if(isSenderNotAPlayer(sender)) return;
        Player player = (Player) sender;

        if (Main.getInstance().getQueueManager() == null) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 20.0F, 1.0F);
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Queue system is not available on this server"));
            return;
        }

        if (floorData == null) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 20.0F, 1.0F);
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000FloorData not found."));
            return;
        }

        if (Main.getInstance().getQueueManager().removePlayerFromQueue(player, floorData.getId())) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "Removed from queue for floor: " + floorData.getId()));
        } else {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 20.0F, 1.0F);
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000You are not in the queue for this floor"));
        }
    }

    @Command(names = {"dungeon status", "dungeons status", "nextdungeon status", "nextdungeons status", "nd status"})
    public static void onDungeonStatusCommand(CommandSender sender) {
        if(isSenderNotAPlayer(sender)) return;
        Player player = (Player) sender;

        if (Main.getInstance().getQueueManager() == null) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 20.0F, 1.0F);
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

    private static boolean isSenderNotAPlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000This command can only be used by players"));
            return true;
        }
        return false;
    }
}
