package fr.perrier.dungeons.spigot.workflow.action.impl;

import fr.perrier.dungeons.spigot.webeditor.blockly.BlocklyAction;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyInfo;
import fr.perrier.dungeons.spigot.workflow.action.Action;
import fr.perrier.dungeons.spigot.workflow.blocks.LocationBlock;
import io.lumine.mythic.bukkit.MythicBukkit;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Setter
@Getter
@BlocklyInfo(
        name = "summon_mob_action",
        color = "#2196F3",
        displayText = "\uD83D\uDCA0\u200B Invoquer un monstre",
        tooltip = "Permet d'invoquer un monstre à une position donnée.",
        category = "Actions"
)
public class SummonMobAction extends Action implements BlocklyAction {
    private static final long serialVersionUID = 1L;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Mob Type:",
            defaultValue = "ZOMBIE", order = 1)
    private String mobType;

    @BlocklyField(type = BlocklyField.FieldType.LOCATION_INPUT, label = "Position:",
            defaultValue = "", order = 2)
    private LocationBlock location;

    public SummonMobAction(String mobType, LocationBlock location) {
        super("SummonMob", "summon_mob_action");
        this.mobType = mobType;
        this.location = location;
    }

    @Override
    public boolean execute(Player triggerPlayer, Location location, Map<String, Object> data) {

        Location spawnLocation = new Location(
                Bukkit.getWorld(this.location.getWorldName()),
                this.location.getX(), this.location.getY(), this.location.getZ()
        );

        EntityType entityType = null;
        try {
            entityType = EntityType.valueOf(mobType.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {}

        if(entityType == null) {
            MythicBukkit.inst().getMobManager().spawnMob(mobType, spawnLocation);
        } else {
            Objects.requireNonNull(spawnLocation.getWorld()).spawnEntity(spawnLocation, entityType);
        }

        return true;
    }
}
