package fr.perrier.dungeons.spigot.workflow.action.impl;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.block.BlockTypes;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.webeditor.blockly.BlocklyAction;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyInfo;
import fr.perrier.dungeons.spigot.workflow.action.Action;
import fr.perrier.dungeons.spigot.workflow.blocks.LocationBlock;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Objects;

/**
 * Action WorldEdit //cut : supprime (remplace par air) tous les blocs dans la région pos1→pos2.
 */
@Setter
@Getter
@BlocklyInfo(
        name = "worldedit_cut_action",
        color = "#F44336",
        displayText = "✂️ WE Cut Région (pos1/pos2)",
        tooltip = "Supprime (remplace par air) tous les blocs entre pos1 et pos2, comme //cut dans WorldEdit.",
        category = "Actions"
)
public class WorldEditCutAction extends Action implements BlocklyAction {
    private static final long serialVersionUID = 1L;

    @BlocklyField(type = BlocklyField.FieldType.LOCATION_INPUT, label = "Position 1:", defaultValue = "", order = 1)
    private LocationBlock pos1;

    @BlocklyField(type = BlocklyField.FieldType.LOCATION_INPUT, label = "Position 2:", defaultValue = "", order = 2)
    private LocationBlock pos2;

    public WorldEditCutAction() {
        super("WorldEdit Cut", "worldedit_cut_action");
        this.pos1 = new LocationBlock();
        this.pos2 = new LocationBlock();
    }

    public WorldEditCutAction(LocationBlock pos1, LocationBlock pos2) {
        super("WorldEdit Cut", "worldedit_cut_action");
        this.pos1 = pos1;
        this.pos2 = pos2;
    }

    @Override
    public boolean execute(Player triggerPlayer, Location triggerLocation, Map<String, Object> data) {
        if (pos1 == null || pos2 == null) {
            Main.getLoggerUtil().warning("[WorldEditCutAction] pos1 ou pos2 est null.");
            return false;
        }

        World bukkitWorld = resolveWorld(triggerLocation);
        if (bukkitWorld == null) {
            Main.getLoggerUtil().warning("[WorldEditCutAction] Monde introuvable.");
            return false;
        }

        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(bukkitWorld);
        BlockVector3 min = BlockVector3.at(
                Math.min(pos1.getX(), pos2.getX()),
                Math.min(pos1.getY(), pos2.getY()),
                Math.min(pos1.getZ(), pos2.getZ()));
        BlockVector3 max = BlockVector3.at(
                Math.max(pos1.getX(), pos2.getX()),
                Math.max(pos1.getY(), pos2.getY()),
                Math.max(pos1.getZ(), pos2.getZ()));
        CuboidRegion region = new CuboidRegion(weWorld, min, max);

        try (EditSession editSession = com.sk89q.worldedit.WorldEdit.getInstance().newEditSession(weWorld)) {
            for (BlockVector3 pos : region) {
                editSession.setBlock(pos, Objects.requireNonNull(BlockTypes.AIR).getDefaultState());
            }
        } catch (Exception e) {
            Main.getLoggerUtil().severe("[WorldEditCutAction] Erreur: " + e.getMessage());
            e.printStackTrace(System.err);
            return false;
        }
        return true;
    }

    private World resolveWorld(Location triggerLocation) {
        if (pos1 != null && pos1.isHasWorld() && pos1.getWorldName() != null) {
            return Bukkit.getWorld(pos1.getWorldName());
        }
        if (pos2 != null && pos2.isHasWorld() && pos2.getWorldName() != null) {
            return Bukkit.getWorld(pos2.getWorldName());
        }
        if (triggerLocation != null && triggerLocation.getWorld() != null) {
            return triggerLocation.getWorld();
        }
        return Bukkit.getWorld("world");
    }
}

