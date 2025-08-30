package fr.perrier.dungeons.spigot.model;

import fr.perrier.dungeons.spigot.utils.CuboidRegion;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Step {
    private String id;
    private String name;
    private CuboidRegion region;
}
