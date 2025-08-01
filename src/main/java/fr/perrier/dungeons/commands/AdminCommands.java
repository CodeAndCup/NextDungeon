package fr.perrier.dungeons.commands;

import fr.perrier.cupcodeapi.commands.annotations.Command;
import fr.perrier.cupcodeapi.commands.annotations.Param;
import fr.perrier.dungeons.configuration.DungeonLoader;
import fr.perrier.dungeons.model.Dungeon;
import org.bukkit.entity.Player;

public class AdminCommands {

    @Command(names = "dungeon admin")
    public static void adminDungeonCommand(Player player) {
        // TODO
    }

    @Command(names = "dungeon admin play")
    public static void adminDungeonPlayCommand(Player player, @Param(name = "Dungeon") String dungeonName, @Param(name = "Floor") String floorName) {
        Dungeon dungeon = DungeonLoader.loadDungeon(dungeonName);
    }
}
