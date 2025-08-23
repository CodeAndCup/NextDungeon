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
            String floorId = dungeonId + "_" + floorSection.getString("id");
            String floorName = floorSection.getString("name");
            String floorDescription = floorSection.getString("description");
            Floor floor = new Floor(floorId, floorName, floorDescription);

            // World config
            ConfigurationSection worldSec = floorSection.getConfigurationSection("world");
            if (worldSec != null) {
                String worldFolderName = dungeonId + "_" + floorId;
                Position worldSpawn = readPosition(worldSec.getConfigurationSection("spawn"));
                Difficulty worldDifficulty = Difficulty.valueOf(Objects.requireNonNull(worldSec.getString("difficulty")).toUpperCase());
                floor.setWorldConfig(new WorldConfig(worldFolderName, worldDifficulty.name(), worldSpawn));
            } else {
                throw new RuntimeException("Floor " + floorName + " has no world config");
            }

            // Requirements
            ConfigurationSection reqSec = floorSection.getConfigurationSection("requirements");
            if (reqSec != null) {
                Requirements requirements = new Requirements();
                requirements.setRetryCooldown(TimeUtil.getDuration(Objects.requireNonNull(reqSec.getString("retry_cooldown"))));
                requirements.setRequiredFloorsId(reqSec.getStringList("required_floor"));
                requirements.setRequiredItems(reqSec.getStringList("required_items"));
                requirements.setForbiddenItems(reqSec.getStringList("forbidden_items"));
                requirements.setMinLevel(reqSec.getInt("minimum_level"));

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
            floor.updateMap();
            floor.generateTemplate();

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
            String key = entry.getKey().toString();
            Object value = entry.getValue();

            if (value instanceof Map<?, ?> nestedMap) {
                section.set(key, mapToSection(nestedMap));
            } else {
                section.set(key, value);
            }
        }
        return section;
    }

    public static void loadAllDungeons() {
        File file = new File(Main.getInstance().getDataFolder() + "/dungeons/");
        if(!file.isDirectory()) throw new RuntimeException("Dungeons folder not found");
        File[] files = file.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && f.getName().endsWith(".yml")) {
                    String name = f.getName().replace(".yml", "");
                    Main.getInstance().getLogger().info("Loading dungeon " + name + "...");
                    try {
                        loadDungeon(name);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
