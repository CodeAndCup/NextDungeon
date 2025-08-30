package fr.perrier.dungeons.spigot.workflow.action.impl;

import fr.perrier.dungeons.spigot.webserver.blockly.BlocklyAction;
import fr.perrier.dungeons.spigot.webserver.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.spigot.webserver.blockly.annotations.BlocklyInfo;
import fr.perrier.dungeons.spigot.workflow.action.Action;
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

    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "X:",
            defaultValue = "0", order = 2)
    private float x;
    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Y:",
            defaultValue = "0", order = 3)
    private float y;
    @BlocklyField(type = BlocklyField.FieldType.NUMBER_INPUT, label = "Z:",
            defaultValue = "0", order = 4)
    private float z;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Monde:",
            defaultValue = "world", order = 5)
    private String worldName;

    public SummonMobAction(String mobType, float x, float y, float z, String worldName) {
        super("SummonMob", "summon_mob_action");
        this.mobType = mobType;
        this.x = x;
        this.y = y;
        this.z = z;
        this.worldName = worldName;
    }

    @Override
    public boolean execute(Player triggerPlayer, Location location, Map<String, Object> data) {

        Location spawnLocation = new Location(
                Bukkit.getWorld(worldName),
                x, y, z
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
