package fr.perrier.dungeons.module.labyrinth.generator;

import fr.perrier.dungeons.module.labyrinth.model.RewardIcon;
import fr.perrier.dungeons.module.labyrinth.model.RoomTemplate;
import fr.perrier.dungeons.module.labyrinth.model.RoomType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class IconRollerTest {

    @Test
    void lobbyAlwaysReturnsNone() {
        IconRoller roller = new IconRoller(42L);
        RoomTemplate room = room(RoomType.LOBBY, null);
        assertThat(roller.rollFor(room)).isEqualTo(RewardIcon.NONE);
    }

    @Test
    void bossUsesFixedIcon() {
        IconRoller roller = new IconRoller(42L);
        RoomTemplate room = room(RoomType.BOSS, RewardIcon.GOLD);
        assertThat(roller.rollFor(room)).isEqualTo(RewardIcon.GOLD);
    }

    @Test
    void bossWithoutFixedIconFallsBackToNone() {
        IconRoller roller = new IconRoller(42L);
        RoomTemplate room = room(RoomType.BOSS, null);
        assertThat(roller.rollFor(room)).isEqualTo(RewardIcon.NONE);
    }

    @Test
    void combatPicksFromRollableSet() {
        IconRoller roller = new IconRoller(42L);
        RoomTemplate room = room(RoomType.COMBAT, null);
        RewardIcon picked = roller.rollFor(room);
        assertThat(IconRoller.ROLLABLE_V1).contains(picked);
    }

    @Test
    void combatHonoursFixedIconOverride() {
        IconRoller roller = new IconRoller(new Random(42L), List.of(RewardIcon.GOLD));
        RoomTemplate room = room(RoomType.COMBAT, RewardIcon.BLESSING);
        assertThat(roller.rollFor(room)).isEqualTo(RewardIcon.BLESSING);
    }

    @Test
    void seedingMakesRollsDeterministic() {
        IconRoller a = new IconRoller(123L);
        IconRoller b = new IconRoller(123L);
        RoomTemplate room = room(RoomType.COMBAT, null);
        for (int i = 0; i < 32; i++) {
            assertThat(a.rollFor(room)).isEqualTo(b.rollFor(room));
        }
    }

    private static IconRoller withSeed(long seed) {
        return new IconRoller(seed);
    }

    private static RoomTemplate room(RoomType type, RewardIcon fixedIcon) {
        RoomTemplate r = new RoomTemplate();
        r.setId("test_" + type);
        r.setType(type);
        r.setFixedIcon(fixedIcon);
        return r;
    }
}
