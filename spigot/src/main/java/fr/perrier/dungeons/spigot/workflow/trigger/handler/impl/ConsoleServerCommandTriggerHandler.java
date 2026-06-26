package fr.perrier.dungeons.spigot.workflow.trigger.handler.impl;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.workflow.trigger.Trigger;
import fr.perrier.dungeons.spigot.workflow.trigger.handler.TriggerEventHandler;
import fr.perrier.dungeons.spigot.workflow.trigger.impl.ConsoleCommandTrigger;
import org.bukkit.entity.Player;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler pour les commandes exécutées via la console serveur (ConsoleCommandSender),
 * un BlockCommandSender, ou tout autre sender non-joueur — typiquement MythicMobs
 * `command{c=...}` qui dispatche via Bukkit.dispatchCommand(getConsoleSender(), ...).
 */
public class ConsoleServerCommandTriggerHandler implements TriggerEventHandler<ServerCommandEvent> {

    @Override
    public Class<ServerCommandEvent> getEventType() {
        return ServerCommandEvent.class;
    }

    @Override
    public List<String> getSupportedTriggerTypes() {
        return Collections.singletonList("console_command_trigger");
    }

    @Override
    public void handleEvent(ServerCommandEvent event, List<Trigger> triggers) {
        String fullCommand = event.getCommand();
        if (fullCommand == null || fullCommand.isEmpty()) {
            return;
        }
        if (fullCommand.startsWith("/")) {
            fullCommand = fullCommand.substring(1);
        }

        boolean cancelled = false;

        for (Trigger trigger : triggers) {
            if (!(trigger instanceof ConsoleCommandTrigger consoleTrigger)) continue;
            if (!consoleTrigger.matchesCommand(fullCommand)) continue;

            Map<String, Object> data = extractEventData(event);
            data.put("matched_command", fullCommand);

            int spaceIdx = fullCommand.indexOf(' ');
            if (spaceIdx >= 0) {
                data.put("command_name", fullCommand.substring(0, spaceIdx));
                data.put("command_args", fullCommand.substring(spaceIdx + 1));
            } else {
                data.put("command_name", fullCommand);
                data.put("command_args", "");
            }

            // Mark before scheduling so the dynamic Bukkit command path (same tick,
            // via CommandMap.dispatch after this event) deduplicates and does not fire again.
            Main.getInstance().getTriggersRegistry().markTriggerFiredFromEvent(consoleTrigger.getTriggerId());
            Main.getInstance().getServer().getScheduler().runTask(Main.getInstance(),
                    () -> consoleTrigger.execute(null, null, data));

            if (consoleTrigger.shouldCancelCommand()) {
                cancelled = true;
                if (Main.getLoggerUtil().isDebugEnabled()) {
                    Main.getLoggerUtil().info("Server command cancelled by ConsoleCommandTrigger: "
                            + consoleTrigger.getName());
                }
            }
        }

        if (cancelled) {
            event.setCancelled(true);
        }
    }

    @Override
    public Map<String, Object> extractEventData(ServerCommandEvent event) {
        Map<String, Object> data = new HashMap<>();
        data.put("command", event.getCommand());
        data.put("sender", event.getSender().getName());
        return data;
    }

    @Override
    public Player getPlayerFromEvent(ServerCommandEvent event) {
        return null;
    }
}
