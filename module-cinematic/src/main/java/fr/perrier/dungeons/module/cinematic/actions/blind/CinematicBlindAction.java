package fr.perrier.dungeons.module.cinematic.actions.blind;

import fr.perrier.dungeons.module.cinematic.action.SimpleCinematicAction;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

/**
 * Action cinématique rendant l'écran noir (aveuglement)
 * pendant la durée du segment.
 */
public class CinematicBlindAction extends SimpleCinematicAction<BlindSegment> {

    private List<BlindSegment> segments = new ArrayList<>();

    @Override
    protected void onSegmentStart(Player player, BlindSegment segment) throws Exception {
        // Duration in frames = endFrame - startFrame; each frame is 1 tick (50ms)
        int durationTicks = segment.getEndFrame() - segment.getStartFrame();
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.BLINDNESS,
                Math.max(1, durationTicks),
                255,
                false,
                false,
                false
        ));
    }

    @Override
    protected void onSegmentStop(Player player, BlindSegment segment) throws Exception {
        player.removePotionEffect(PotionEffectType.BLINDNESS);
    }

    @Override
    public List<BlindSegment> getCinematicSegments() {
        return segments;
    }
}
