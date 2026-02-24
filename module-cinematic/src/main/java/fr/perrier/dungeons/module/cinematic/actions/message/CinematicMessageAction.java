package fr.perrier.dungeons.module.cinematic.actions.message;

import fr.perrier.dungeons.module.cinematic.action.SimpleCinematicAction;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Action cinématique envoyant un message au joueur (chat ou action bar).
 */
public class CinematicMessageAction extends SimpleCinematicAction<MessageSegment> {

    private List<MessageSegment> segments = new ArrayList<>();

    @Override
    protected void onSegmentStart(Player player, MessageSegment segment) throws Exception {
        if ("ACTION_BAR".equals(segment.getDisplayType())) {
            player.spigot().sendMessage(
                    ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText(segment.getMessage())
            );
        } else {
            player.sendMessage(segment.getMessage());
        }
    }

    @Override
    public List<MessageSegment> getCinematicSegments() {
        return segments;
    }
}
