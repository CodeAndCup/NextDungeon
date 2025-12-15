package fr.perrier.dungeons.common.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CuboidRegion {
    private Position position1;
    private Position position2;

    @Override
    public String toString() {
        return "CuboidRegion{" +
               "position1=" + position1 +
               ", position2=" + position2 +
               '}';
    }
}
