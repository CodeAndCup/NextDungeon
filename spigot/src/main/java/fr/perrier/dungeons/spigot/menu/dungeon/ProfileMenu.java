package fr.perrier.dungeons.spigot.menu.dungeon;

import fr.perrier.cupcodeapi.menuapi.Button;
import fr.perrier.cupcodeapi.menuapi.GlassMenu;
import fr.perrier.cupcodeapi.menuapi.Menu;
import fr.perrier.cupcodeapi.menuapi.buttons.BackButton;
import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.cupcodeapi.utils.ItemBuilder;
import fr.perrier.cupcodeapi.utils.TimeUtil;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.model.Dungeon;
import fr.perrier.dungeons.spigot.model.Floor;
import fr.perrier.dungeons.spigot.model.ProfileData;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

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
        return "&#8B0000&l" + ChatUtil.toSmallCaps("Profile Menu");
    }

    @Override
    public Map<Integer, Button> getAllButtons(Player player) {
        HashMap<Integer, Button> buttons = new HashMap<>();

        List<Integer> slots = floorsSlots.get(dungeon.getFloors().size());

        int index = 0;
        for (Integer slot : slots) {
            buttons.put(slot, new FloorStatsButton(dungeon.getFloors().get(index), player.getUniqueId()));
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

        public FloorStatsButton(Floor floor, UUID uuid) {
            this.floor = floor;
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
                            "&7Best Time: &#90FFFF" + TimeUtil.getDuration(floorStats.getFastestCompletionTime()),
                            "&7Total kills: &#90FFFF" + floorStats.getTotalEnemiesKilled(),
                            "&7Most kills in a run: &#90FFFF" + floorStats.getMostEnemiesKilledInRun(),
                            "&7Total deaths: &#90FFFF" + floorStats.getTotalDeaths(),
                            "&7Most deaths in a run: &#90FFFF" + floorStats.getMostDeathsInRun(),
                            "&7Total runs: &#90FFFF" + floorStats.getTotalRuns(),
                            "&7Total completions: &#90FFFF" + floorStats.getTotalCompletions(),
                            ""
                    )
                    .toItemStack();
        }
    }
}
