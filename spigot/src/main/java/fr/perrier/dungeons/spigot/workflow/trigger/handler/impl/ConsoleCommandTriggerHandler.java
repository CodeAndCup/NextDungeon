package fr.perrier.dungeons.spigot.workflow.trigger.handler.impl;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.workflow.trigger.Trigger;
import fr.perrier.dungeons.spigot.workflow.trigger.handler.TriggerEventHandler;
import fr.perrier.dungeons.spigot.workflow.trigger.impl.ConsoleCommandTrigger;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handler pour les commandes exécutées depuis la console du serveur.
 */
public class ConsoleCommandTriggerHandler implements TriggerEventHandler<PlayerCommandPreprocessEvent> {

    @Override
    public Class<PlayerCommandPreprocessEvent> getEventType() {
        return PlayerCommandPreprocessEvent.class;
    }

    @Override
    public List<String> getSupportedTriggerTypes() {
        return Collections.singletonList("console_command_trigger");
    }

    @Override
    public void handleEvent(PlayerCommandPreprocessEvent event, List<Trigger> triggers) {
        // Ne déclencher que pour les commandes lancées depuis la console (pas /execute as ...)
        if (!event.getPlayer().isOp()) {
            return;
        }

        String fullCommand = event.getMessage();
        if (fullCommand.isEmpty()) {
            return;
        }
        // ServerCommandEvent.getCommand() ne contient pas le '/' de tête, mais on le retire au cas où.
        if (fullCommand.startsWith("/")) {
            fullCommand = fullCommand.substring(1);
        }

        boolean cancelled = false;

        for (Trigger trigger : triggers) {
            if (!(trigger instanceof ConsoleCommandTrigger consoleTrigger)) continue;
            if (!consoleTrigger.matchesCommand(fullCommand)) continue;

            Map<String, Object> data = extractEventData(event);
            data.put("matched_command", fullCommand);

            // Extraction simple des arguments (tout ce qui suit le premier espace)
            int spaceIdx = fullCommand.indexOf(' ');
            if (spaceIdx >= 0) {
                data.put("command_name", fullCommand.substring(0, spaceIdx));
                data.put("command_args", fullCommand.substring(spaceIdx + 1));
            } else {
                data.put("command_name", fullCommand);
                data.put("command_args", "");
            }

            // Mark before scheduling so the dynamic Bukkit command path (same tick,
            // via CommandMap.dispatch after this event if not cancelled) deduplicates.
            Main.getInstance().getTriggersRegistry().markTriggerFiredFromEvent(consoleTrigger.getTriggerId());
            // Exécuter sur le main thread (ServerCommandEvent peut être appelé depuis n'importe quel thread)
            Main.getInstance().getServer().getScheduler().runTask(Main.getInstance(),
                    () -> consoleTrigger.execute(null, null, data));

            if (consoleTrigger.shouldCancelCommand()) {
                cancelled = true;
                if (Main.getLoggerUtil().isDebugEnabled()) {
                    Main.getLoggerUtil().info("Console command cancelled by ConsoleCommandTrigger: "
                            + consoleTrigger.getName());
                }
            }
        }

        if (cancelled) {
            // Annule proprement l'event : empêche Bukkit de résoudre la commande
            // sans provoquer de "Unknown command" (ServerCommandEvent est Cancellable depuis 1.21).
            event.setCancelled(true);
        }
    }

    @Override
    public Map<String, Object> extractEventData(PlayerCommandPreprocessEvent event) {
        Map<String, Object> data = new HashMap<>();
        data.put("command", event.getMessage());
        data.put("sender", event.getPlayer().getName());
        return data;
    }

    @Override
    public Player getPlayerFromEvent(PlayerCommandPreprocessEvent event) {
        return null;
    }
}
