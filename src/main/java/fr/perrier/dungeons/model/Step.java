package fr.perrier.dungeons.model;

import fr.perrier.dungeons.utils.CuboidRegion;
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
