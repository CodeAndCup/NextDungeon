package fr.perrier.dungeons.module.labyrinth.ui;

import fr.perrier.cupcodeapi.utils.ChatUtil;
import org.bukkit.command.CommandSender;

/**
 * Centralizes the Memory Labyrinth chat palette so every player-facing
 * message shares the NextDungeon look :
 * <ul>
 *   <li>{@link #PREFIX} — "Memory Laby" with the red gradient, mirroring the
 *       core {@code NextDungeon} prefix.</li>
 *   <li>{@link #RED} — accent / highlight color ({@code #D10000}).</li>
 *   <li>{@link #WHITE} — body text.</li>
 *   <li>{@link #DARK} — dark gray for punctuation / separators ({@code ( [ . | »}).</li>
 * </ul>
 *
 * <p>Strings are written with cupcodeapi color tokens and run through
 * {@link ChatUtil#translate(String)} (which resolves {@code <gradient>},
 * {@code &#hex} and legacy {@code &} codes) before being sent.</p>
 */
public final class LabyrinthMessages {

    private LabyrinthMessages() {}

    /** "Memory Laby" with the NextDungeon red gradient + a dark-gray separator. */
    public static final String PREFIX = "<gradient:#8B0000:bold>Memory Laby</gradient:#D10000> &8» &r";

    public static final String RED = "&#D10000";
    public static final String WHITE = "&f";
    public static final String DARK = "&8";
    public static final String GREEN = "&#3FD17A";
    public static final String GOLD = "&#FFD24A";

    /** Translate a raw token string (no prefix) into a colored message. */
    public static String color(String raw) {
        return ChatUtil.translate(raw);
    }

    /** Prefix + translate a token string. */
    public static String prefixed(String raw) {
        return ChatUtil.translate(PREFIX + raw);
    }

    /** Send a prefixed, colored line to a recipient. */
    public static void send(CommandSender to, String raw) {
        if (to != null) to.sendMessage(prefixed(raw));
    }
}
