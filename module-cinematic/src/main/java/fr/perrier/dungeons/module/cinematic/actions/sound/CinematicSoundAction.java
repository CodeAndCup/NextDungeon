package fr.perrier.dungeons.module.cinematic.actions.sound;

import fr.perrier.dungeons.module.cinematic.action.SimpleCinematicAction;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Action cinématique jouant un son au joueur au début du segment.
 */
public class CinematicSoundAction extends SimpleCinematicAction<SoundSegment> {

    private List<SoundSegment> segments = new ArrayList<>();

    @Override
    protected void onSegmentStart(Player player, SoundSegment segment) throws Exception {
        try {
            Sound sound = Sound.valueOf(segment.getSoundType());
            player.playSound(player.getLocation(), sound, segment.getVolume(), segment.getPitch());
        } catch (IllegalArgumentException e) {
            // Sound type invalide — essayer comme string (namespace:key)
            player.playSound(player.getLocation(), segment.getSoundType(), segment.getVolume(), segment.getPitch());
        }
    }

    @Override
    public List<SoundSegment> getCinematicSegments() {
        return segments;
    }
}
