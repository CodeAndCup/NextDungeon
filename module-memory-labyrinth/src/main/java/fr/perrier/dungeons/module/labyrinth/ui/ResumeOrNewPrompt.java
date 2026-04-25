package fr.perrier.dungeons.module.labyrinth.ui;

import fr.perrier.dungeons.module.labyrinth.lifecycle.LabyrinthResumePromptListener;
import fr.perrier.dungeons.module.labyrinth.model.LabyrinthSave;
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

        ComponentBuilder builder = new ComponentBuilder()
                .append("§6▶ §eMemory Labyrinth — §fSave détectée")
                .append("\n", ComponentBuilder.FormatRetention.NONE)
                .append("§7Salle atteinte : §f" + save.getLastBossClearedRoom())
                .append(" §8| §7Palier : §f" + save.getDifficultyTier())
                .append("\n", ComponentBuilder.FormatRetention.NONE);

        TextComponent resume = new TextComponent("§a§l[Reprendre]");
        resume.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                COMMAND_PREFIX + " " + CHOICE_RESUME));
        resume.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text("§eReprendre la save (salle " + save.getLastBossClearedRoom() + ")")));

        TextComponent fresh = new TextComponent("§c§l[Nouvelle partie]");
        fresh.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                COMMAND_PREFIX + " " + CHOICE_NEW));
        fresh.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text("§eEffacer la save et démarrer un nouveau run")));

        builder.append(resume).append("  ").append(fresh);
        leader.spigot().sendMessage(builder.create());
    }

    public static void notifyTeam(UUID[] memberIds, String message) {
        if (memberIds == null) return;
        for (UUID id : memberIds) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) p.sendMessage("§7" + message);
        }
    }
}
