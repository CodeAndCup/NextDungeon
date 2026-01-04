package fr.perrier.dungeons.common.model.dungeon;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Métadonnées simplifiées d'un floor pour le dashboard.
 * Cette classe ne contient que les informations nécessaires à l'affichage
 * dans le dashboard, sans les dépendances spécifiques à Spigot.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FloorMetadata implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String description;

    /**
     * Crée des métadonnées à partir d'un FloorData complet
     */
    public static FloorMetadata from(FloorData floorData) {
        return new FloorMetadata(
            floorData.getId(),
            floorData.getName(),
            floorData.getDescription()
        );
    }
}

