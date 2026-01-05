package fr.perrier.dungeons.common.model.dungeon;

import fr.perrier.dungeons.common.utils.CuboidRegion;
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

    @Override
    public String toString() {
        return "Step{" +
               "id='" + id + '\'' +
               ", name='" + name + '\'' +
               ", region=" + region +
               '}';
    }
}
