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

import java.util.UUID;

/**
 * Lobby prompt sent to the party leader at the start of an Infinite run
 * when a resumable save exists for the current composition.
 *
 * <p>Two clickable choices : {@code Reprendre} (applies the save and
 * advances) and {@code Nouvelle partie} (deletes the save and falls
 * back to the standard lobby flow). Implemented via
 * {@link ClickEvent.Action#RUN_COMMAND} ; the
 * {@link LabyrinthResumePromptListener} intercepts the chat command.</p>
 */
public final class ResumeOrNewPrompt {

    public static final String CHOICE_RESUME = "resume";
    public static final String CHOICE_NEW = "new";

    private static final String COMMAND_PREFIX = "/" + LabyrinthResumePromptListener.COMMAND;

    private ResumeOrNewPrompt() {}

    public static void sendToLeader(UUID leaderId, LabyrinthSave save) {
        if (leaderId == null || save == null) return;
        Player leader = Bukkit.getPlayer(leaderId);
        if (leader == null || !leader.isOnline()) return;

        ComponentBuilder builder = new ComponentBuilder();
        builder.append(TextComponent.fromLegacyText(LabyrinthMessages.prefixed("&lSave detected")));
        builder.append("\n", ComponentBuilder.FormatRetention.NONE);
        builder.append(TextComponent.fromLegacyText(LabyrinthMessages.color(
                WHITE + "Reached room " + DARK + ": " + WHITE + save.getLastBossClearedRoom()
                        + " " + DARK + "| " + WHITE + "Tier " + DARK + ": " + WHITE + save.getDifficultyTier())));
        builder.append("\n", ComponentBuilder.FormatRetention.NONE);

        TextComponent resume = new TextComponent(TextComponent.fromLegacyText(
                LabyrinthMessages.color(DARK + "[" + GREEN + "&lResume" + DARK + "]")));
        resume.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                COMMAND_PREFIX + " " + CHOICE_RESUME));
        resume.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(LabyrinthMessages.color(WHITE + "Resume the save " + DARK + "(" + WHITE + "room "
                        + save.getLastBossClearedRoom() + DARK + ")"))));

        TextComponent fresh = new TextComponent(TextComponent.fromLegacyText(
                LabyrinthMessages.color(DARK + "[" + RED + "&lNew game" + DARK + "]")));
        fresh.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                COMMAND_PREFIX + " " + CHOICE_NEW));
        fresh.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(LabyrinthMessages.color(WHITE + "Delete the save and start a new run"))));

        builder.append(resume).append("  ", ComponentBuilder.FormatRetention.NONE).append(fresh);
        leader.spigot().sendMessage(builder.create());
    }

    public static void notifyTeam(UUID[] memberIds, String message) {
        if (memberIds == null) return;
        for (UUID id : memberIds) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) p.sendMessage(LabyrinthMessages.prefixed(WHITE + message));
        }
    }
}
