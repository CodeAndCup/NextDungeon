package fr.perrier.dungeons.module.labyrinth.model;

import fr.perrier.dungeons.common.model.labyrinth.RewardIcon;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LabyrinthSaveTest {

    @Test
    void partyHashIsDeterministicAndOrderIndependent() {
        UUID a = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID b = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID c = UUID.fromString("00000000-0000-0000-0000-000000000003");

        String h1 = LabyrinthSave.computePartyHash(Arrays.asList(a, b, c));
        String h2 = LabyrinthSave.computePartyHash(Arrays.asList(c, a, b));
        String h3 = LabyrinthSave.computePartyHash(Arrays.asList(b, c, a));

        assertThat(h1).isEqualTo(h2).isEqualTo(h3);
        assertThat(h1).hasSize(64); // sha-256 hex
    }

    @Test
    void partyHashChangesWhenCompositionChanges() {
        UUID a = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID b = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID c = UUID.fromString("00000000-0000-0000-0000-000000000003");

        String two = LabyrinthSave.computePartyHash(List.of(a, b));
        String three = LabyrinthSave.computePartyHash(List.of(a, b, c));

        assertThat(two).isNotEqualTo(three);
    }

    @Test
    void checksumRoundtripVerifiesTrue() {
        LabyrinthSave save = sample();
        save.recomputeChecksum();
        assertThat(save.verifyChecksum()).isTrue();
    }

    @Test
    void checksumRejectsTampering() {
        LabyrinthSave save = sample();
        save.recomputeChecksum();
        // Mutate a load-bearing field after sign — verify must fail.
        save.setLastBossClearedRoom(save.getLastBossClearedRoom() + 10);
        assertThat(save.verifyChecksum()).isFalse();
    }

    @Test
    void verifyFailsWhenChecksumIsNull() {
        LabyrinthSave save = sample();
        save.setChecksum(null);
        assertThat(save.verifyChecksum()).isFalse();
    }

    private static LabyrinthSave sample() {
        LabyrinthSave save = new LabyrinthSave();
        save.setId("save-1");
        save.setFloorId("infinite");
        save.setPartyHash("partyhash");
        save.setLastBossClearedRoom(30);
        save.setDifficultyTier(3);
        save.setSeed(42L);
        save.getIconCounts().put(RewardIcon.GOLD, 5);
        save.setCreatedAt(1L);
        save.setUpdatedAt(2L);
        return save;
    }
}
