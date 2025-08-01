package fr.perrier.dungeons.configuration;

import fr.perrier.cupcodeapi.utils.TimeUtil;
import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.model.Dungeon;
import fr.perrier.dungeons.model.Floor;
import fr.perrier.dungeons.model.Step;
import fr.perrier.dungeons.utils.CuboidRegion;
import fr.perrier.dungeons.utils.Position;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DungeonLoader {

    public static Dungeon loadDungeon(String name) {
        File file = new File(Main.getInstance().getDataFolder(), "dungeons/" + name + ".yml");
        return loadDungeon(file);
    }

    public static Dungeon loadDungeon(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection dungeonSection = config.getConfigurationSection("dungeon");
        if (dungeonSection == null) return null;

        Dungeon dungeon = new Dungeon(dungeonSection.getString("id"),dungeonSection.getString("name"));

        List<Floor> floors = new ArrayList<>();
        for (ConfigurationSection floorSection : getSections(dungeonSection, "floors")) {
            Floor floor = new Floor(floorSection.getString("id"),floorSection.getString("name"));
            floor.setDifficulty(floorSection.getString("difficulty"));

            // World
            ConfigurationSection worldSec = floorSection.getConfigurationSection("world");
            WorldConfig world = new WorldConfig(worldSec.getString("name"));
            world.setProperties(floorSection.getString("difficulty"),readPosition(worldSec.getConfigurationSection("spawn")));
            floor.setWorldConfig(world);

            // Requirements
            ConfigurationSection reqSec = floorSection.getConfigurationSection("requirements");
            Requirements requirements = new Requirements();
            requirements.setRetryCooldown(TimeUtil.getDuration(reqSec.getString("retry_cooldown")));
            requirements.setRequiredDungeons(reqSec.getStringList("required_dungeons"));
            requirements.setRequiredItems(reqSec.getStringList("required_items"));
            requirements.setForbiddenItems(reqSec.getStringList("forbidden_items"));

            ConfigurationSection partySec = reqSec.getConfigurationSection("party");
            Requirements.PartyRequirements party = new Requirements.PartyRequirements();
            party.setMinSize(partySec.getInt("min_size"));
            party.setMaxSize(partySec.getInt("max_size"));
            requirements.setPartyRequirements(party);
            floor.setRequirements(requirements);

            // Rules
            ConfigurationSection rulesSec = floorSection.getConfigurationSection("rules");
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
                section.getInt("x"),
                section.getInt("y"),
                section.getInt("z")
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