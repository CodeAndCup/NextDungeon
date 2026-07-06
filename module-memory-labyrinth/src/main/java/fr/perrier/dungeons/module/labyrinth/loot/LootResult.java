package fr.perrier.dungeons.module.labyrinth.loot;

import fr.perrier.dungeons.common.model.labyrinth.RewardIcon;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player loot resolution emitted at end-of-run by {@link LootCalculator}.
 *
 * <p>Each player rolls independently (CDC Q5 — « loot individuel,
 * chacun son roll »). The {@code success} flag marks the kind of run
 * end : {@code true} for a finite-floor completion, {@code false} for
 * total wipe / voluntary exit.</p>
 */
@Getter
@Setter
public class LootResult {

    private UUID playerId;
    private long goldEarned;
    private List<String> itemsRolled = new ArrayList<>();
    private Map<RewardIcon, Integer> iconCounts = new EnumMap<>(RewardIcon.class);
    private int tier;
    private boolean success;
    private String floorId;
    private int finalRoomIndex;
}
