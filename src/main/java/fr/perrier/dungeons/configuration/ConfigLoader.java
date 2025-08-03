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
import org.bukkit.configuration.MemoryConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ConfigLoader {

    public static Dungeon loadDungeon(String name) {
        File file = new File(Main.getInstance().getDataFolder(), "dungeons/" + name + ".yml");
        return loadDungeon(file);
    }

    public static Dungeon loadDungeon(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection dungeonSection = config.getConfigurationSection("dungeon");

        if (dungeonSection == null) return null;

        String dungeonId = dungeonSection.getString("id");
        String dungeonName = dungeonSection.getString("name");
        Dungeon dungeon = new Dungeon(dungeonId, dungeonName);

        List<Floor> floors = new ArrayList<>();
        List<Map<?, ?>> floorList = dungeonSection.getMapList("floors");

        for (Map<?, ?> rawFloor : floorList) {
            ConfigurationSection floorSection = mapToSection(rawFloor);

            // Basic floor info
            String floorId = floorSection.getString("id");
            String floorName = floorSection.getString("name");
            Floor floor = new Floor(floorId, floorName);

            // World config
            ConfigurationSection worldSec = floorSection.getConfigurationSection("world");
            if (worldSec != null) {
                String worldFolderName = dungeonId + "_" + floorId;
                Position worldSpawn = readPosition(worldSec.getConfigurationSection("spawn"));
                Difficulty worldDifficulty = Difficulty.valueOf(worldSec.getString("difficulty").toUpperCase());
                floor.setWorldConfig(new WorldConfig(worldFolderName, worldDifficulty.name(), worldSpawn));
            }

            // Requirements
            ConfigurationSection reqSec = floorSection.getConfigurationSection("requirements");
            if (reqSec != null) {
                Requirements requirements = new Requirements();
                requirements.setRetryCooldown(TimeUtil.getDuration(reqSec.getString("retry_cooldown")));
                requirements.setRequiredDungeons(reqSec.getStringList("required_dungeons"));
                requirements.setRequiredItems(reqSec.getStringList("required_items"));
                requirements.setForbiddenItems(reqSec.getStringList("forbidden_items"));

                ConfigurationSection partySec = reqSec.getConfigurationSection("party");
                if (partySec != null) {
                    Requirements.PartyRequirements party = new Requirements.PartyRequirements();
                    party.setMinSize(partySec.getInt("min_size"));
                    party.setMaxSize(partySec.getInt("max_size"));
                    requirements.setPartyRequirements(party);
                }

                floor.setRequirements(requirements);
            }

            // Rules
            ConfigurationSection rulesSec = floorSection.getConfigurationSection("rules");
            if (rulesSec != null) {
                Rules rules = new Rules();
                rules.setDeathBanDuration(TimeUtil.getDuration(rulesSec.getString("death_ban")));
                rules.setGamemode(rulesSec.getString("gamemode"));
                rules.setAllowFlight(rulesSec.getBoolean("allow_flight"));
                floor.setRules(rules);
            }

            // Steps
            List<Step> steps = new ArrayList<>();
            List<Map<?, ?>> stepList = floorSection.getMapList("steps");
            for (Map<?, ?> rawStep : stepList) {
                ConfigurationSection stepSec = mapToSection(rawStep);
                ConfigurationSection regionSec = stepSec.getConfigurationSection("region");
                if (regionSec != null) {
                    Position pos1 = readPosition(regionSec.getConfigurationSection("pos1"));
                    Position pos2 = readPosition(regionSec.getConfigurationSection("pos2"));
                    CuboidRegion region = new CuboidRegion(pos1, pos2);

                    Step step = new Step(
                            stepSec.getString("id"),
                            stepSec.getString("name"),
                            region
                    );
                    steps.add(step);
                }
            }
            floor.setSteps(steps);

            floors.add(floor);
        }

        dungeon.setFloors(floors);
        return dungeon;
    }

    private static Position readPosition(ConfigurationSection section) {
        if (section == null) return new Position(0, 0, 0);
        return new Position(
                section.getDouble("x"),
                section.getDouble("y"),
                section.getDouble("z")
        );
    }

    private static ConfigurationSection mapToSection(Map<?, ?> map) {
        ConfigurationSection section = new MemoryConfiguration();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            section.set(entry.getKey().toString(), entry.getValue());
        }
        return section;
    }
}
