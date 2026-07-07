package fr.perrier.dungeons.spigot.commands;

import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import fr.perrier.cupcodeapi.commands.annotations.Command;
import fr.perrier.cupcodeapi.commands.annotations.Param;
import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.dungeons.common.model.dungeon.FloorData;
import fr.perrier.dungeons.common.model.dungeon.config.FloorInstanceData;
import fr.perrier.dungeons.common.module.ModuleActionHandler;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.menu.dungeon.DungeonGateMenu;
import fr.perrier.dungeons.spigot.model.Dungeon;
import fr.perrier.dungeons.spigot.model.Floor;
import fr.perrier.dungeons.spigot.parties.impl.DungeonPartyImpl;
import fr.perrier.dungeons.spigot.utils.LoggerUtil;
import io.lumine.mythic.lib.api.item.NBTItem;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class DebugCommands {

    private DebugCommands() {
        // Private constructor to prevent from instantiation
    }

    @Command(names = {"dungeon debug help", "dungeons debug help", "nextdungeon debug help", "nextdungeons debug help", "nd debug help"}, permission = "nextdungeon.debug")
    public static void debugDungeonCommand(Player player) {
        player.sendMessage(ChatUtil.getBar());
        player.sendMessage(ChatUtil.translate("<gradient:#8B0000:bold>NextDungeon</gradient:#D10000> &8| &fDebug Commands"));
        player.sendMessage("");
        player.sendMessage(ChatUtil.translate("&#D10000/dungeon debug help &8- &fGet the list of available commands"));
        player.sendMessage(ChatUtil.translate("&#D10000/dungeon debug list dungeons &8- &fList all dungeons"));
        player.sendMessage(ChatUtil.translate("&#D10000/dungeon debug list floors &8- &fList all floors"));
        player.sendMessage(ChatUtil.translate("&#D10000/dungeon debug list instances &8- &fList all instances"));
        player.sendMessage(ChatUtil.translate("&#D10000/dungeon debug openmenu &8- &fOpen the dungeon example menu"));
        player.sendMessage("");
        player.sendMessage(ChatUtil.getBar());
    }

    @Command(names = {"dungeon debug log toggle", "dungeons debug log toggle", "nextdungeon debug log toggle", "nextdungeons debug log toggle", "nd debug log toggle"})
    public static void debugDungeonToggleCommand(Player player) {
        boolean newState = !Main.getLoggerUtil().isDebugEnabled();
        Main.getLoggerUtil().setDebugEnabled(newState);
        player.sendMessage(ChatUtil.translate("&#D10000Debug log is now " + (newState ? "enabled" : "disabled") + "."));
    }

    @Command(names = {"dungeon debug log broadcaster", "dungeons debug log broadcaster", "nextdungeon debug log broadcaster", "nextdungeons debug log broadcaster", "nd debug log broadcaster"}, permission = "nextdungeon.debug")
    public static void debugDungeonSetLogBroadcastCommand(Player player, @Param(name = "type") String type) {
        LoggerUtil.LogBroadcastType logBroadcastType;
        try {
            logBroadcastType = LoggerUtil.LogBroadcastType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatUtil.translate("&#FF0000Invalid log broadcast type. Valid types are: CONSOLE, IN_GAME, BOTH."));
            return;
        }
        Main.getLoggerUtil().setLogBroadcastType(logBroadcastType);
        player.sendMessage(ChatUtil.translate("&#D10000Log broadcast type set to " + logBroadcastType.name() + "."));
    }

    @Command(names = {"dungeon debug list instances", "dungeons debug list instances", "nextdungeon debug list instances", "nextdungeons debug list instances", "nd debug list instances"}, permission = "nextdungeon.debug")
    public static void debugDungeonListInstancesCommand(Player player) {
        player.sendMessage(ChatUtil.getBar());
        player.sendMessage(ChatUtil.translate("&6Instances:"));
        for (FloorInstanceData instanceData : Main.getInstance().getDungeonService().getInstancesMap().values()) {
            TextComponent instanceComponent = new TextComponent(ChatUtil.translate("  &8- &e" + instanceData.getInstanceName() + " &8(&7&o" + instanceData.getInstanceId() + "&8)"));
            HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(instanceData.toString()).create());
            instanceComponent.setHoverEvent(hoverEvent);
            player.spigot().sendMessage(instanceComponent);
        }
        player.sendMessage(ChatUtil.getBar());
    }

    @Command(names = {"dungeon debug list floors", "dungeons debug list floors", "nextdungeon debug list floors", "nextdungeons debug list floors", "nd debug list floors"}, permission = "nextdungeon.debug")
    public static void debugDungeonListFloorsCommand(Player player) {
        player.sendMessage(ChatUtil.getBar());
        player.sendMessage(ChatUtil.translate("&6Floors:"));
        for (FloorData floorData : Main.getInstance().getDungeonService().getFloorsMap().values()) {
            TextComponent floorComponent = new TextComponent(ChatUtil.translate("  &8- &e" + floorData.getName() + " &8(&7&o" + floorData.getId() + "&8)"));
            HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(floorData.toString()).create());
            floorComponent.setHoverEvent(hoverEvent);
            player.spigot().sendMessage(floorComponent);
        }
        player.sendMessage(ChatUtil.getBar());
    }

    @Command(names = {"dungeon debug list dungeons", "dungeons debug list dungeons", "nextdungeon debug list dungeons", "nextdungeons debug list dungeons", "nd debug list dungeons"}, permission = "nextdungeon.debug")
    public static void debugDungeonListDungeonsCommand(Player player) {
        player.sendMessage(ChatUtil.getBar());
        player.sendMessage(ChatUtil.translate("&6Dungeons:"));
        for (Dungeon dungeon : Dungeon.getDungeons()) {
            TextComponent dungeonComponent = new TextComponent(ChatUtil.translate("  &8- &e" + dungeon.getName() + " &8(&7&o" + dungeon.getId() + "&8)"));
            HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(dungeon.toString()).create());
            dungeonComponent.setHoverEvent(hoverEvent);
            player.spigot().sendMessage(dungeonComponent);
        }
        player.sendMessage(ChatUtil.getBar());
    }

    @Command(names = {"dungeon debug floor", "dungeons debug floor", "nextdungeon debug floor", "nextdungeons debug floor", "nd debug floor"}, permission = "nextdungeon.debug")
    public static void debugDungeonFloorCommand(Player player,  @Param(name = "Floor ID", tabCompleteFlags = {"floors"}) FloorData floorData) {
        Floor floor = Floor.getFloor(floorData.getId());
        if (floor == null) {
            player.sendMessage(ChatUtil.translate("&#FF0000Floor with ID '" + floorData.getId() + "' not found."));
            return;
        }
        player.sendMessage(ChatUtil.getBar());
        player.sendMessage(ChatUtil.translate("&6Floor Info:"));
        player.sendMessage(ChatUtil.translate("  &8- &eID: &f" + floor.getId()));
        player.sendMessage(ChatUtil.translate("  &8- &eName: &f" + floor.getName()));
        player.sendMessage(ChatUtil.translate("  &8- &eDescription: &f" + floor.getDescription()));
        player.sendMessage(ChatUtil.translate("  &8- &eNumber of Steps: &f" + (floor.getSteps() != null ? floor.getSteps().size() : 0)));
        player.sendMessage(ChatUtil.translate("  &8- &eNumber of Triggers: &f" + (floor.getTriggers() != null ? floor.getTriggers().size() : 0)));
        player.sendMessage(ChatUtil.getBar());
    }

    @Command(names = {"dungeon debug trigger", "dungeons debug trigger", "nextdungeon debug trigger", "nextdungeons debug trigger", "nd debug trigger"}, permission = "nextdungeon.debug")
    public static void debugDungeonTriggerCommand(Player player, @Param(name = "Floor ID", tabCompleteFlags = {"floors"}) FloorData floorData) {
        Floor floor = Floor.getFloor(floorData.getId());
        if (floor == null) {
            player.sendMessage(ChatUtil.translate("&#FF0000Floor with ID '" + floorData.getId() + "' not found."));
            return;
        }
        player.sendMessage(ChatUtil.getBar());
        player.sendMessage(ChatUtil.translate("&6Triggers for Floor: &e" + floor.getName()));
        if (floor.getTriggers() == null || floor.getTriggers().isEmpty()) {
            player.sendMessage(ChatUtil.translate("  &cNo triggers found for this floor."));
        } else {
            floor.getTriggers().forEach(trigger -> player.sendMessage(ChatUtil.translate("  &b" + trigger.toString())));
        }
        player.sendMessage(ChatUtil.getBar());
    }

    @Command(names = {"dungeon debug party list", "dungeons debug party list", "nextdungeon debug party list", "nextdungeons debug party list", "nd debug party list"}, permission = "nextdungeon.debug")
    public static void debugPartyListCommand(Player player) {
        player.sendMessage(ChatUtil.getBar());
        player.sendMessage(ChatUtil.translate("&6Parties:"));
        DungeonPartyImpl.getDungeonParties().forEach((leaderId, party) -> {
            TextComponent partyComponent = new TextComponent(ChatUtil.translate("  &8- &e" + leaderId + " &8(&7&o" + party.getDungeonId() + "&8)"));
            HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(party.toString()).create());
            partyComponent.setHoverEvent(hoverEvent);
            player.spigot().sendMessage(partyComponent);
        });
        player.sendMessage(ChatUtil.getBar());
    }

    @Command(names = {"dungeon debug party clean", "dungeons debug party clean", "nextdungeon debug party clean", "nextdungeons debug party clean", "nd debug party clean"}, permission = "nextdungeon.debug")
    public static void debugPartyCleanCommand(Player player) {
        DungeonPartyImpl.getDungeonParties().values().stream()
                .filter(DungeonPartyImpl.class::isInstance)
                .map(DungeonPartyImpl.class::cast)
                .forEach(DungeonPartyImpl::disband);
        player.sendMessage(ChatUtil.translate("&#D10000All local parties have been disbanded."));
    }

    /**
     * Compares the held item against a floor's configured item requirements, printing both sides
     * of the match (the item's MMOItems id vs each stored requiredItems/forbiddenItems entry) so a
     * failing ✘ in the gate menu can be diagnosed as either a data mismatch (the stored requirement
     * differs from the id) or a stale-deploy issue (this server runs old matching code).
     */
    @Command(names = {"dungeon debug itemreq", "dungeons debug itemreq", "nextdungeon debug itemreq", "nextdungeons debug itemreq", "nd debug itemreq"}, permission = "nextdungeon.debug")
    public static void debugItemReqCommand(Player player, @Param(name = "Floor ID", tabCompleteFlags = {"floors"}) FloorData floorData) {
        Floor floor = Floor.getFloor(floorData.getId());
        if (floor == null) {
            player.sendMessage(ChatUtil.translate("&#FF0000Floor with ID '" + floorData.getId() + "' not found."));
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            player.sendMessage(ChatUtil.translate("&#FF0000Hold the item you want to test in your main hand."));
            return;
        }
        NBTItem nbtItem = NBTItem.get(item);
        String heldId = nbtItem.hasType() ? nbtItem.getString("MMOITEMS_ITEM_ID") : "<not an MMOItems item>";

        player.sendMessage(ChatUtil.getBar());
        player.sendMessage(ChatUtil.translate("&6Item requirement check for floor &e" + floor.getName()));
        player.sendMessage(ChatUtil.translate("  &8- &eHeld item MMOITEMS_ITEM_ID: &f'" + heldId + "'"));

        java.util.List<String> required = floor.getRequirements().getRequiredItems();
        player.sendMessage(ChatUtil.translate("  &eRequired items (&f" + (required == null ? 0 : required.size()) + "&e):"));
        if (required != null) {
            for (String req : required) {
                boolean match = floor.playerHasItemMatching(player, req);
                player.sendMessage(ChatUtil.translate("    " + (match ? "&#00FF00✔" : "&#FF0000✘") + " &7'" + req + "'"));
            }
        }

        java.util.List<String> forbidden = floor.getRequirements().getForbiddenItems();
        player.sendMessage(ChatUtil.translate("  &eForbidden items (&f" + (forbidden == null ? 0 : forbidden.size()) + "&e):"));
        if (forbidden != null) {
            for (String forb : forbidden) {
                boolean match = floor.playerHasItemMatching(player, forb);
                player.sendMessage(ChatUtil.translate("    " + (match ? "&#FF0000✘ owns" : "&#00FF00✔ clear") + " &7'" + forb + "'"));
            }
        }
        player.sendMessage(ChatUtil.getBar());
    }
}
