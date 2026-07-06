package fr.perrier.dungeons.module.labyrinth.loot;

import fr.perrier.dungeons.module.labyrinth.model.DifficultyModifier;
import fr.perrier.dungeons.module.labyrinth.model.LabyrinthRun;
import fr.perrier.dungeons.common.model.labyrinth.LootTable;
import fr.perrier.dungeons.common.model.labyrinth.RewardIcon;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LootCalculatorTest {

    private static final UUID PLAYER = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Test
    void noLootTableYieldsZeroGoldNoItems() {
        LootCalculator calc = new LootCalculator(null);
        LabyrinthRun run = run("easy", 5, 1);
        LootResult result = calc.computeForPlayer(run, PLAYER, null, true);
        assertThat(result.getGoldEarned()).isZero();
        assertThat(result.getItemsRolled()).isEmpty();
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void goldUsesIconCountAndTierLinearly() {
        LootTable table = new LootTable();
        table.setBaseGold(100);
        table.setGoldPerIcon(0.20);
        table.setBaseItemRolls(0);

        LootCalculator calc = new LootCalculator(null);
        LabyrinthRun run = run("easy", 0, 1);
        run.getIconCounts().put(RewardIcon.GOLD, 5);
        // tier 1 → no tier bonus → 100 × (1 + 0.20*5) = 100 × 2 = 200
        long gold = calc.computeForPlayer(run, PLAYER, table, true).getGoldEarned();
        assertThat(gold).isEqualTo(200L);

        run.getCurrentModifier().setTier(3);
        // tier 3 → tier bonus = 1 + 0.10 * 2 = 1.2 → 200 × 1.2 = 240
        gold = calc.computeForPlayer(run, PLAYER, table, true).getGoldEarned();
        assertThat(gold).isEqualTo(240L);
    }

    @Test
    void itemsRespectMinTierGate() {
        LootTable table = new LootTable();
        table.setBaseGold(0);
        table.setBaseItemRolls(50);
        table.setItems(List.of(
                new LootTable.Entry("low_tier_only", 1, 1),
                new LootTable.Entry("legendary", 1, 5)
        ));

        LootCalculator calc = new LootCalculator(null);
        LabyrinthRun run = run("infinite", 0, 1);
        LootResult low = calc.computeForPlayer(run, PLAYER, table, true);
        assertThat(low.getItemsRolled()).hasSize(50);
        assertThat(low.getItemsRolled()).allMatch(id -> id.equals("low_tier_only"));

        run.getCurrentModifier().setTier(5);
        LootResult high = calc.computeForPlayer(run, PLAYER, table, true);
        assertThat(high.getItemsRolled()).hasSize(50);
        // Both ids are eligible at tier 5 — at least one of each should appear.
        assertThat(high.getItemsRolled()).contains("low_tier_only", "legendary");
    }

    @Test
    void rollsAreDeterministicPerPlayerSeed() {
        LootTable table = new LootTable();
        table.setBaseItemRolls(8);
        table.setItems(List.of(
                new LootTable.Entry("a", 1, 1),
                new LootTable.Entry("b", 1, 1),
                new LootTable.Entry("c", 1, 1)
        ));
        LootCalculator calc = new LootCalculator(null);
        LabyrinthRun run = run("easy", 0, 1);
        LootResult first = calc.computeForPlayer(run, PLAYER, table, true);
        LootResult second = calc.computeForPlayer(run, PLAYER, table, true);
        assertThat(first.getItemsRolled()).isEqualTo(second.getItemsRolled());
    }

    @Test
    void differentPlayersRollDifferently() {
        LootTable table = new LootTable();
        table.setBaseItemRolls(8);
        table.setItems(List.of(
                new LootTable.Entry("a", 1, 1),
                new LootTable.Entry("b", 1, 1),
                new LootTable.Entry("c", 1, 1)
        ));
        LootCalculator calc = new LootCalculator(null);
        LabyrinthRun run = run("easy", 0, 1);
        LootResult p1 = calc.computeForPlayer(run, PLAYER, table, true);
        LootResult p2 = calc.computeForPlayer(run,
                UUID.fromString("22222222-2222-2222-2222-222222222222"), table, true);
        // Two seeded RNGs should differ on at least one roll across 8 picks.
        assertThat(p1.getItemsRolled()).isNotEqualTo(p2.getItemsRolled());
    }

    private static LabyrinthRun run(String floorId, long seed, int tier) {
        LabyrinthRun run = new LabyrinthRun();
        run.setFloorId(floorId);
        run.setSeed(seed);
        run.setCurrentModifier(new DifficultyModifier(tier));
        run.setCurrentRoomIndex(10);
        return run;
    }

}
