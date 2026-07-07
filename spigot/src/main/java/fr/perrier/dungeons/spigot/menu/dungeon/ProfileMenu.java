package fr.perrier.dungeons.spigot.menu.dungeon;

import fr.perrier.cupcodeapi.menuapi.Button;
import fr.perrier.cupcodeapi.menuapi.GlassMenu;
import fr.perrier.cupcodeapi.menuapi.Menu;
import fr.perrier.cupcodeapi.menuapi.buttons.BackButton;
import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.cupcodeapi.utils.ItemBuilder;
import fr.perrier.cupcodeapi.utils.TimeUtil;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.menu.utils.MenuTitle;
import fr.perrier.dungeons.spigot.model.Dungeon;
import fr.perrier.dungeons.spigot.model.Floor;
import fr.perrier.dungeons.spigot.model.ProfileData;
import fr.perrier.dungeons.spigot.storage.LeaderboardService;
import fr.perrier.dungeons.spigot.storage.LeaderboardService.Metric;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
public class ProfileMenu extends GlassMenu {
    private final Menu oldMenu;
    private final Dungeon dungeon;

    private final HashMap<Integer, List<Integer>> floorsSlots = new HashMap<>() {
        {
            put(1, List.of(22));
            put(2, List.of(21,23));
            put(3, List.of(20,22,24 ));
            put(4, List.of(20,21,23,24));
            put(5, List.of(20,21,22,23,24));
            put(6, List.of(21,22,23,30,31,32));
            put(7, List.of(20,21,23,24,30,31,32));
            put(8, List.of(20,21,22,23,24,30,31,32));
            put(9, List.of(20,21,22,23,24,29,30,32,33));
            put(10, List.of(20,21,22,23,24,29,30,31,32,33));
        }
    };

    @Override
    public int getGlassColor() {
        return 0;
    }

    @Override
    public String getTitle(Player player) {
        // Back button sits at slot 40 (<=5 floors) or 49 (>5), giving a 5- or 6-row inventory.
        int rows = dungeon.getFloors().size() <= 5 ? 5 : 6;
        return MenuTitle.ofRows(rows, "&#8B0000&l" + ChatUtil.toSmallCaps("Profile Menu"));
    }

    /** Metrics shown with a global ranking suffix, in the order they appear in the lore. */
    private static final Metric[] RANKED_METRICS = {
            Metric.BEST_TIME, Metric.TOTAL_KILLS, Metric.MOST_KILLS_IN_RUN,
            Metric.TOTAL_RUNS, Metric.TOTAL_COMPLETIONS
    };

    @Override
    public Map<Integer, Button> getAllButtons(Player player) {
        HashMap<Integer, Button> buttons = new HashMap<>();

        List<Integer> slots = floorsSlots.get(dungeon.getFloors().size());

        // Fetch every (floor × metric) rank in one pipelined round-trip instead of per-button.
        List<String> floorIds = new ArrayList<>();
        for (Floor f : dungeon.getFloors()) floorIds.add(f.getId());
        Map<String, EnumMap<Metric, LeaderboardService.Rank>> ranks =
                Main.getInstance().getLeaderboardService().getRanks(floorIds, player.getUniqueId(), RANKED_METRICS);

        int index = 0;
        for (Integer slot : slots) {
            Floor floor = dungeon.getFloors().get(index);
            buttons.put(slot, new FloorStatsButton(floor, player.getUniqueId(),
                    ranks.getOrDefault(floor.getId(), new EnumMap<>(Metric.class))));
            index++;
        }

        if(dungeon.getFloors().size() <= 5) {
            buttons.put(40, new BackButton(oldMenu));
        } else {
            buttons.put(49, new BackButton(oldMenu));
        }

        return buttons;
    }

    private static class FloorStatsButton extends Button {
        private final Floor floor;
        private final ProfileData.FloorStats floorStats;
        private final EnumMap<Metric, LeaderboardService.Rank> ranks;

        public FloorStatsButton(Floor floor, UUID uuid, EnumMap<Metric, LeaderboardService.Rank> ranks) {
            this.floor = floor;
            this.ranks = ranks;
            this.floorStats = Main.getInstance().getProfileService().getProfileData(uuid).getFloorStats()
                    .stream()
                    .filter(fs -> fs.getFloorId().equals(floor.getId()))
                    .findFirst()
                    .orElse(new ProfileData.FloorStats(floor.getId()));
        }


        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.OMINOUS_TRIAL_KEY)
                    .setName("&f" + floor.getName())
                    .setLore(
                            "",
                            "&7Best Time: &#90FFFF" + TimeUtil.getDuration(floorStats.getFastestCompletionTime()) + rankSuffix(Metric.BEST_TIME),
                            "&7Total kills: &#90FFFF" + floorStats.getTotalEnemiesKilled() + rankSuffix(Metric.TOTAL_KILLS),
                            "&7Most kills in a run: &#90FFFF" + floorStats.getMostEnemiesKilledInRun() + rankSuffix(Metric.MOST_KILLS_IN_RUN),
                            "&7Total deaths: &#90FFFF" + floorStats.getTotalDeaths(),
                            "&7Most deaths in a run: &#90FFFF" + floorStats.getMostDeathsInRun(),
                            "&7Total runs: &#90FFFF" + floorStats.getTotalRuns() + rankSuffix(Metric.TOTAL_RUNS),
                            "&7Total completions: &#90FFFF" + floorStats.getTotalCompletions() + rankSuffix(Metric.TOTAL_COMPLETIONS),
                            ""
                    )
                    .toItemStack();
        }

        /**
         * Renders the player's standing on a metric as a trailing " (#pos/total)" for the
         * top {@link LeaderboardService#ABSOLUTE_RANK_LIMIT}, or " (Top X%)" beyond it.
         * Returns "" when the player is not ranked (e.g. never completed the floor).
         */
        private String rankSuffix(Metric metric) {
            LeaderboardService.Rank rank = ranks.get(metric);
            if (rank == null || rank.total() <= 0) return "";
            if (rank.position() <= LeaderboardService.ABSOLUTE_RANK_LIMIT) {
                return " &8(&e#" + rank.position() + "&8/&7" + rank.total() + "&8)";
            }
            int percent = (int) Math.max(1, Math.ceil((double) rank.position() / rank.total() * 100.0));
            return " &8(&eTop " + percent + "%&8)";
        }
    }
}
