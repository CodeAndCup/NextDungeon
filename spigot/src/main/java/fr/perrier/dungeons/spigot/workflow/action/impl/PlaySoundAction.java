package fr.perrier.dungeons.spigot.workflow.action.impl;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.webeditor.blockly.BlocklyAction;
import fr.perrier.dungeons.spigot.workflow.action.Action;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyInfo;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Action pour jouer un son
 */
@Setter
@Getter
@BlocklyInfo(
        name = "play_sound_action",
        color = "#E91E63",
        displayText = "🔊 Jouer un son",
        tooltip = "Joue un son pour un joueur",
        category = "Actions"
)
public class PlaySoundAction extends Action implements BlocklyAction {
    private static final long serialVersionUID = 1L;

    @BlocklyField(type = BlocklyField.FieldType.DROPDOWN, label = "Son:",
            options = "ENTITY_PLAYER_LEVELUP,BLOCK_NOTE_BLOCK_PLING,ENTITY_EXPERIENCE_ORB_PICKUP,BLOCK_ANVIL_LAND,ENTITY_ENDERMAN_TELEPORT,ENTITY_VILLAGER_YES,ENTITY_VILLAGER_NO,UI_TOAST_CHALLENGE_COMPLETE,ENTITY_ENDER_DRAGON_GROWL,ENTITY_WITHER_SPAWN",
            defaultValue = "ENTITY_PLAYER_LEVELUP", order = 1)
    private String sound;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Volume:",
            defaultValue = "1.0", order = 2)
    private float volume;

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Pitch:",
            defaultValue = "1.0", order = 3)
    private float pitch;

    @BlocklyField(type = BlocklyField.FieldType.DROPDOWN, label = "Jouer pour:",
            options = "player,@all", defaultValue = "player", order = 4)
    private String target;

    public PlaySoundAction() {
        super("Play Sound", "play_sound_action");
        this.sound = "ENTITY_PLAYER_LEVELUP";
        this.volume = 1.0f;
        this.pitch = 1.0f;
        this.target = "player";
    }

    public PlaySoundAction(String sound, float volume, float pitch, String target) {
        super("Play Sound", "play_sound_action");
        this.sound = sound;
        this.volume = volume;
        this.pitch = pitch;
        this.target = target;
    }

    @Override
    public boolean execute(Player triggerPlayer, Location location, Map<String, Object> data) {
        if (sound == null || sound.isEmpty()) {
            Main.getLoggerUtil().warning("Sound is empty in PlaySoundAction");
            return false;
        }

        try {
            Sound soundEnum = Sound.valueOf(sound.toUpperCase());
            Location soundLocation = location != null ? location : triggerPlayer.getLocation();

            if ("@all".equals(target)) {
                // Jouer le son pour tous les joueurs en ligne
                for (Player onlinePlayer : Main.getInstance().getServer().getOnlinePlayers()) {
                    onlinePlayer.playSound(soundLocation, soundEnum, volume, pitch);
                }
            } else {
                // Jouer le son pour le joueur déclencheur
                if (triggerPlayer != null) {
                    triggerPlayer.playSound(soundLocation, soundEnum, volume, pitch);
                }
            }

            if (Main.getLoggerUtil().isDebugEnabled()) {
                Main.getLoggerUtil().info("Played sound " + sound + " for " + target);
            }

            return true;

        } catch (IllegalArgumentException e) {
            Main.getLoggerUtil().warning("Invalid sound: " + sound);
            return false;
        }
    }
}
