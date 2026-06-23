package fr.perrier.dungeons.module.labyrinth.ui;

import fr.perrier.dungeons.module.labyrinth.lifecycle.LabyrinthResumePromptListener;
import fr.perrier.dungeons.module.labyrinth.model.LabyrinthSave;
import static fr.perrier.dungeons.module.labyrinth.ui.LabyrinthMessages.RED;
import static fr.perrier.dungeons.module.labyrinth.ui.LabyrinthMessages.WHITE;
import static fr.perrier.dungeons.module.labyrinth.ui.LabyrinthMessages.DARK;
import static fr.perrier.dungeons.module.labyrinth.ui.LabyrinthMessages.GREEN;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * Lobby prompt sent to the party leader at the start of an Infinite run : the
 * list of resumable saves for the party (by SHA) plus a "New run" button.
 *
 * <p>Each entry and the new-run button are clickable
 * ({@link ClickEvent.Action#RUN_COMMAND}); the
 * {@link LabyrinthResumePromptListener} intercepts the chat command. A save
 * entry runs {@code /<cmd> <saveId>}, the new-run button {@code /<cmd> new}.</p>
 */
public final class ResumeOrNewPrompt {

    public static final String CHOICE_NEW = "new";

    private static final String COMMAND_PREFIX = "/" + LabyrinthResumePromptListener.COMMAND;

    private ResumeOrNewPrompt() {}

    public static void sendToLeader(UUID leaderId, List<LabyrinthSave> saves) {
        if (leaderId == null || saves == null || saves.isEmpty()) return;
        Player leader = Bukkit.getPlayer(leaderId);
        if (leader == null || !leader.isOnline()) return;

        ComponentBuilder builder = new ComponentBuilder();
        builder.append(TextComponent.fromLegacyText(LabyrinthMessages.prefixed("&lResume a run")));
        builder.append("\n", ComponentBuilder.FormatRetention.NONE);
        builder.append(TextComponent.fromLegacyText(LabyrinthMessages.color(
                WHITE + "Pick a run to resume, or start a new one " + DARK + ":")));

        int i = 1;
        for (LabyrinthSave save : saves) {
            if (save == null || save.getId() == null) continue;
            builder.append("\n", ComponentBuilder.FormatRetention.NONE);
            String label = LabyrinthMessages.color(
                    DARK + "[" + GREEN + "#" + i + " " + WHITE + "Room " + save.getLastBossClearedRoom()
                            + DARK + " · " + WHITE + "Tier " + save.getDifficultyTier()
                            + DARK + " · " + WHITE + formatDate(save.getUpdatedAt()) + DARK + "]");
            TextComponent entry = new TextComponent(TextComponent.fromLegacyText(label));
            entry.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                    COMMAND_PREFIX + " " + save.getId()));
            entry.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new Text(LabyrinthMessages.color(WHITE + "Resume at room " + save.getLastBossClearedRoom()
                            + DARK + " (" + WHITE + "tier " + save.getDifficultyTier() + DARK + ")"))));
            builder.append(entry, ComponentBuilder.FormatRetention.NONE);
            i++;
        }

        builder.append("\n", ComponentBuilder.FormatRetention.NONE);
        TextComponent fresh = new TextComponent(TextComponent.fromLegacyText(
                LabyrinthMessages.color(DARK + "[" + RED + "&lNew run" + DARK + "]")));
        fresh.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                COMMAND_PREFIX + " " + CHOICE_NEW));
        fresh.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(LabyrinthMessages.color(WHITE + "Start a fresh run from the lobby"))));
        builder.append(fresh, ComponentBuilder.FormatRetention.NONE);

        leader.spigot().sendMessage(builder.create());
    }

    private static String formatDate(long epochMs) {
        if (epochMs <= 0) return "?";
        return new java.text.SimpleDateFormat("dd/MM HH:mm").format(new java.util.Date(epochMs));
    }

    public static void notifyTeam(UUID[] memberIds, String message) {
        if (memberIds == null) return;
        for (UUID id : memberIds) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) p.sendMessage(LabyrinthMessages.prefixed(WHITE + message));
        }
    }
}
