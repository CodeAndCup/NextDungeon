package fr.perrier.dungeons.spigot.menu.utils;

/**
 * Builds inventory titles that paint a {@code saofractured} GUI background behind a menu.
 *
 * <p>A title is laid out as {@code <pixel offset> + <background glyph> + <legacy-coloured name>}.
 * The background glyphs live in the ItemsAdder {@code saofractured} font: the inventory gabarits
 * {@code gui_base_1..6} sit at {@code U+E400 + (rows - 1)}, so a menu of {@code N} rows uses the
 * glyph at {@code U+E3FF + N} (E400 = 1 row … E405 = 6 rows). The labyrinth gate reuses the
 * dedicated {@code gui_dungeon_difficulty} glyph ({@link #LABYRINTH}).
 *
 * <p>The offset is a leftward nudge that aligns the background image with the inventory's left
 * edge (titles render at an 8&nbsp;px inset). It is rendered with the negative-space font, whose
 * glyphs sit at {@code U+F800 + width} for each power-of-two pixel width (U+F808 = -8&nbsp;px),
 * combined to make any value. Inventory titles are legacy (&amp;) text only — no MiniMessage — so
 * the offset has to be a literal glyph rather than an {@code <offset:N>} tag.
 */
public final class MenuTitle {
    private MenuTitle() {}

    /** Labyrinth gate background — {@code gui_dungeon_difficulty} (U+E422). */
    public static final String LABYRINTH = String.valueOf((char) 0xE422);

    /** Leftward nudge (pixels, negative) applied to every GUI background. */
    private static final int OFFSET_X = -8;

    /** Title for a {@code rows}-high menu: {@code gui_base_<rows>} background + offset + name. */
    public static String ofRows(int rows, String legacyName) {
        return legacyName;
        //return of(baseGlyph(rows), legacyName);
    }

    /** Title built from an explicit background glyph (e.g. {@link #LABYRINTH}). */
    public static String of(String glyph, String legacyName) {
        return legacyName;
        //return negativeSpace(OFFSET_X) + glyph + legacyName;
    }

    /** {@code gui_base_<rows>} glyph: U+E400 = 1 row … U+E405 = 6 rows (clamped to that range). */
    public static String baseGlyph(int rows) {
        int clamped = Math.clamp(rows, 1, 6);
        return String.valueOf((char) (0xE400 + clamped - 1));
    }

    /**
     * Renders a negative horizontal offset using the negative-space font. Each power-of-two pixel
     * width has a glyph at {@code U+F800 + width} (e.g. -8 -> U+F808); arbitrary values are summed
     * from those glyphs. Non-negative input produces no offset, since only negative glyphs exist.
     */
    private static String negativeSpace(int pixels) {
        if (pixels >= 0) return "";
        int remaining = -pixels;
        StringBuilder sb = new StringBuilder();
        for (int width = 128; width >= 1; width >>= 1) {
            while (remaining >= width) {
                sb.append((char) (0xF800 + width));
                remaining -= width;
            }
        }
        return sb.toString();
    }
}
