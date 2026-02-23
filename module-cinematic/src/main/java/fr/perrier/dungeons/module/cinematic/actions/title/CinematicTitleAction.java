package fr.perrier.dungeons.module.cinematic.actions.title;

import fr.perrier.dungeons.module.cinematic.action.SimpleCinematicAction;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Action cinématique affichant un titre et sous-titre au joueur
 * avec contrôle des durées de fade in/out.
 */
public class CinematicTitleAction extends SimpleCinematicAction<TitleSegment> {

    private List<TitleSegment> segments = new ArrayList<>();

    @Override
    protected void onSegmentStart(Player player, TitleSegment segment) throws Exception {
        int totalDuration = segment.getEndFrame() - segment.getStartFrame();
        int stayDuration = totalDuration - segment.getFadeIn() - segment.getFadeOut();

        // Convert frames to ticks (1 frame = 50ms, 1 tick = 50ms, so 1:1)
        player.sendTitle(
                segment.getTitle(),
                segment.getSubtitle(),
                segment.getFadeIn(),
                Math.max(0, stayDuration),
                segment.getFadeOut()
        );
    }

    @Override
    protected void onSegmentStop(Player player, TitleSegment segment) throws Exception {
        player.resetTitle();
    }

    @Override
    public List<TitleSegment> getCinematicSegments() {
        return segments;
    }
}
