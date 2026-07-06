package fr.perrier.dungeons.module.labyrinth.model;

import fr.perrier.dungeons.common.model.labyrinth.LabyrinthRoom;
import fr.perrier.dungeons.common.model.labyrinth.RewardIcon;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Pair of room candidates proposed at the end of a combat room (CDC §6.2).
 *
 * <p>The icon for each side is rolled when the choice is built (or fixed if
 * the side leads to a boss room). When the player traverses one of the two
 * doors, {@code currentRoomIndex++} and {@code iconCounts[chosenIcon]++} on
 * the run.</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class DoorChoice {

    private LabyrinthRoom left;
    private RewardIcon iconLeft;

    private LabyrinthRoom right;
    private RewardIcon iconRight;

    /**
     * True when both candidates point to the same boss room ; in that case
     * a single door is shown and {@link #left} == {@link #right}.
     */
    private boolean bossSingleDoor;

    public DoorChoice(LabyrinthRoom left, RewardIcon iconLeft,
                      LabyrinthRoom right, RewardIcon iconRight,
                      boolean bossSingleDoor) {
        this.left = left;
        this.iconLeft = iconLeft;
        this.right = right;
        this.iconRight = iconRight;
        this.bossSingleDoor = bossSingleDoor;
    }
}
