package fr.perrier.dungeons.module.labyrinth.ui;

import fr.perrier.dungeons.module.labyrinth.lifecycle.BossEncounterHandler;
import fr.perrier.dungeons.spigot.model.FloorInstance;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;

/**
 * Builds and broadcasts the post-boss revive chat prompt.
 *
 * <p>Each dead player name is rendered as a clickable component that runs
 * {@code /labyrinth_revive &lt;name&gt;} on click. The
 * {@link fr.perrier.dungeons.module.labyrinth.lifecycle.LabyrinthReviveListener}
 * intercepts that command via {@code PlayerCommandPreprocessEvent}.</p>
 *
 * <p>Sent only to alive players of the instance — dead players cannot
 * revive and would just see a useless prompt.</p>
 */
public final class RevivePromptComponent {

    public static final String COMMAND_PREFIX = "/" + BossEncounterHandler.REVIVE_COMMAND + " ";

    private RevivePromptComponent() {}

    public static void send(FloorInstance instance, Set<UUID> deadPlayers) {
        if (instance == null || deadPlayers == null || deadPlayers.isEmpty()) return;

        ComponentBuilder builder = new ComponentBuilder()
                .append("§6☠ §eUn revive est disponible — choisis un coéquipier : ")
                .append("\n");
        boolean first = true;
        for (UUID deadId : deadPlayers) {
            String name = nameOf(deadId);
            if (name == null) continue;
            if (!first) builder.append(" §7|§r ").reset();
            TextComponent tc = new TextComponent("§a§l[" + name + "]");
            tc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                    COMMAND_PREFIX + name));
            tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new Text("§eRessusciter " + name)));
            builder.append(tc);
            first = false;
        }
        TextComponent message = new TextComponent(builder.create());

        for (UUID pid : instance.getPlayers()) {
            if (deadPlayers.contains(pid)) continue;
            Player p = Bukkit.getPlayer(pid);
            if (p != null && p.isOnline()) p.spigot().sendMessage(message);
        }
    }

    public static void sendCancellation(FloorInstance instance, String reason) {
        if (instance == null) return;
        String text = "§7" + reason;
        for (UUID pid : instance.getPlayers()) {
            Player p = Bukkit.getPlayer(pid);
            if (p != null && p.isOnline()) p.sendMessage(text);
        }
    }

    private static String nameOf(UUID id) {
        Player online = Bukkit.getPlayer(id);
        if (online != null) return online.getName();
        // Fallback to offline player name (cached). Returns null only for
        // a brand-new UUID the server has never seen.
        return Bukkit.getOfflinePlayer(id).getName();
    }
}
