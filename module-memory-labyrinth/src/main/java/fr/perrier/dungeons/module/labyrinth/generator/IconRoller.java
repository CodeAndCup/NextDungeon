package fr.perrier.dungeons.module.labyrinth.generator;

import fr.perrier.dungeons.module.labyrinth.model.RewardIcon;
import fr.perrier.dungeons.module.labyrinth.model.RoomTemplate;
import fr.perrier.dungeons.module.labyrinth.model.RoomType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Rolls the reward icon shown above a candidate room's door at proposal time.
 *
 * <p>Decision tree (CDC §1.6, §4.1, Q5.3 / Q5.5 / Q5.6) :</p>
 * <ul>
 *   <li>{@link RoomType#LOBBY} → forced {@link RewardIcon#NONE}</li>
 *   <li>{@link RoomType#BOSS}  → use {@link RoomTemplate#getFixedIcon()} (admin-defined, fixed)</li>
 *   <li>{@link RoomType#COMBAT}:
 *     <ul>
 *       <li>If {@code fixedIcon} is non-null on the template, honour it (admin override).</li>
 *       <li>Otherwise, uniformly pick from the v1 rollable set (currently only GOLD).</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p>{@link RewardIcon#BLESSING} is reserved but not yet rollable — see
 * CDC §10. When a future version flips it on, just add it to
 * {@link #ROLLABLE_V1} (or wire weights here).</p>
 */
public class IconRoller {

    /** Icons that may be rolled on COMBAT rooms in v1. */
    public static final List<RewardIcon> ROLLABLE_V1 = List.of(RewardIcon.GOLD);

    private final Random random;
    private final List<RewardIcon> rollable;

    public IconRoller() {
        this(new Random(), ROLLABLE_V1);
    }

    public IconRoller(long seed) {
        this(new Random(seed), ROLLABLE_V1);
    }

    public IconRoller(Random random, List<RewardIcon> rollable) {
        this.random = random;
        this.rollable = rollable == null || rollable.isEmpty()
                ? new ArrayList<>(ROLLABLE_V1)
                : new ArrayList<>(rollable);
    }

    /**
     * Roll the icon for a candidate room.
     */
    public RewardIcon rollFor(RoomTemplate room) {
        if (room == null) return RewardIcon.NONE;
        return switch (room.getType()) {
            case LOBBY -> RewardIcon.NONE;
            case BOSS -> room.getFixedIcon() != null ? room.getFixedIcon() : RewardIcon.NONE;
            case COMBAT -> room.getFixedIcon() != null ? room.getFixedIcon() : pickFromRollable();
        };
    }

    private RewardIcon pickFromRollable() {
        if (rollable.isEmpty()) return RewardIcon.NONE;
        return rollable.get(random.nextInt(rollable.size()));
    }
}
