package fr.perrier.dungeons.configuration;

import fr.perrier.cupcodeapi.utils.TimeUtil;
import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.model.Dungeon;
import fr.perrier.dungeons.model.Floor;
import fr.perrier.dungeons.model.Step;
import fr.perrier.dungeons.utils.CuboidRegion;
import fr.perrier.dungeons.utils.Position;
import org.bukkit.Difficulty;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DungeonLoader {

    public static Dungeon loadDungeon(String name) {
        File file = new File(Main.getInstance().getDataFolder(), "dungeons/" + name + ".yml");
        return loadDungeon(file);
    }

    public static Dungeon loadDungeon(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection dungeonSection = config.getConfigurationSection("dungeon");
        if (dungeonSection == null) return null;

        // Dungeon parameters
        String dungeonId = dungeonSection.getString("id");
        String dungeonName = dungeonSection.getString("name");

        Dungeon dungeon = new Dungeon(dungeonId,dungeonName);

        List<Floor> floors = new ArrayList<>();
        for (ConfigurationSection floorSection : getSections(dungeonSection, "floors")) {

            // Configuration sections
            ConfigurationSection worldSec = floorSection.getConfigurationSection("world");
            ConfigurationSection reqSec = floorSection.getConfigurationSection("requirements");
            ConfigurationSection partySec = reqSec.getConfigurationSection("party");
            ConfigurationSection rulesSec = floorSection.getConfigurationSection("rules");

            // Floor parameters
            String floorId = floorSection.getString("id");
            String floorName = floorSection.getString("name");

            // World parameters
            String worldFolderName = dungeonId + "_" + floorId;
            Position worldSpawn = readPosition(Objects.requireNonNull(worldSec.getConfigurationSection("spawn")));
            Difficulty worldDifficulty = Difficulty.valueOf(worldSec.getString("difficulty"));

            // Floor
            Floor floor = new Floor(floorId,floorName);

            // World
            WorldConfig world = new WorldConfig(worldFolderName,worldDifficulty.name(),worldSpawn);
            floor.setWorldConfig(world);

            // Requirements
            Requirements requirements = new Requirements();
            requirements.setRetryCooldown(TimeUtil.getDuration(reqSec.getString("retry_cooldown")));
            requirements.setRequiredDungeons(reqSec.getStringList("required_dungeons"));
            requirements.setRequiredItems(reqSec.getStringList("required_items"));
            requirements.setForbiddenItems(reqSec.getStringList("forbidden_items"));

            Requirements.PartyRequirements party = new Requirements.PartyRequirements();
            party.setMinSize(partySec.getInt("min_size"));
            party.setMaxSize(partySec.getInt("max_size"));
            requirements.setPartyRequirements(party);
            floor.setRequirements(requirements);

            // Rules
            Rules rules = new Rules();
            rules.setDeathBanDuration(TimeUtil.getDuration(rulesSec.getString("death_ban")));
            rules.setGamemode(rulesSec.getString("gamemode"));
            rules.setAllowFlight(rulesSec.getBoolean("allow_flight"));
            floor.setRules(rules);

            // Steps
            List<Step> steps = new ArrayList<>();
            for (ConfigurationSection stepSec : getSections(floorSection, "steps")) {
                ConfigurationSection regionSec = stepSec.getConfigurationSection("region");

                CuboidRegion region = new CuboidRegion(
                        readPosition(regionSec.getConfigurationSection("pos1")),
                        readPosition(regionSec.getConfigurationSection("pos2"))
                );

                Step step = new Step(
                        stepSec.getString("id"),
                        stepSec.getString("name"),
                        region
                );

                steps.add(step);
            }
            floor.setSteps(steps);

            floors.add(floor);
        }

        dungeon.setFloors(floors);
        return dungeon;
    }

    private static Position readPosition(ConfigurationSection section) {
        return new Position(
                section.getDouble("x"),
                section.getDouble("y"),
                section.getDouble("z")
        );
    }

    private static List<ConfigurationSection> getSections(ConfigurationSection parent, String path) {
        List<ConfigurationSection> list = new ArrayList<>();
        for (String key : parent.getConfigurationSection(path).getKeys(false)) {
            list.add(parent.getConfigurationSection(path + "." + key));
        }
        return list;
    }
}