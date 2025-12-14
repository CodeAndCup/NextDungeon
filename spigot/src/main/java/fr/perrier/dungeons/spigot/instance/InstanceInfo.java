package fr.perrier.dungeons.spigot.instance;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

/**
 * Classe de données (DTO) contenant les informations d'une instance.
 * Indépendante du provider utilisé.
 */
@Data
@AllArgsConstructor
public class InstanceInfo {
    /**
     * Identifiant unique de l'instance
     */
    private final UUID instanceId;

    /**
     * Identifiant de l'étage (floor)
     */
    private final String floorId;

    /**
     * Date de création de l'instance
     */
    private final String createdAt;

    /**
     * Indique si l'instance est en mode édition
     */
    private boolean editMode;

    /**
     * Indique si l'instance est prête à recevoir des joueurs
     */
    private boolean ready;

    /**
     * Constructeur sans les flags optionnels
     */
    public InstanceInfo(UUID instanceId, String floorId, String createdAt) {
        this(instanceId, floorId, createdAt, false, false);
    }
}