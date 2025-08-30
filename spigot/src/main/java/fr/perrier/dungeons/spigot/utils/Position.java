package fr.perrier.dungeons.spigot.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;


@Getter
@Setter
@AllArgsConstructor
public class Position {
    private double x, y, z;

    public Location toLocation() {
        return new Location(Bukkit.getWorld("world"), x, y, z);
    }
}
