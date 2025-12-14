package fr.perrier.dungeons.spigot.instance;

import fr.perrier.dungeons.spigot.model.Floor;
import fr.perrier.dungeons.spigot.model.FloorInstance;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Abstraction principale pour gérer les instances de donjons.
 * Cette interface suit le principe d'inversion de dépendance (DIP) de SOLID.
 *
 * Les implémentations peuvent utiliser différents systèmes :
 * - CloudNet (services cloud)
 * - ASP (Advanced Slime World Manager)
 * - Vanilla (mondes Minecraft optimisés)
 */
public interface InstanceProvider {

    /**
     * Initialise le provider et vérifie que toutes les dépendances sont disponibles.
     *
     * @return CompletableFuture qui se termine avec true si l'initialisation réussit
     */
    CompletableFuture<Boolean> initialize();

    /**
     * Crée une nouvelle instance pour un étage de donjon.
     *
     * @param floor l'étage pour lequel créer l'instance
     * @param editMode true si l'instance doit être créée en mode édition
     * @return CompletableFuture contenant l'UUID de l'instance créée, ou null si échec
     */
    CompletableFuture<UUID> createInstance(Floor floor, boolean editMode);

    /**
     * Supprime une instance de donjon.
     *
     * @param instanceId l'UUID de l'instance à supprimer
     * @return CompletableFuture qui se termine avec true si la suppression réussit
     */
    CompletableFuture<Boolean> deleteInstance(UUID instanceId);

    /**
     * Vérifie si le serveur actuel est une instance de donjon.
     *
     * @return true si c'est une instance
     */
    boolean isInstanceServer();

    /**
     * Vérifie si le serveur actuel est en mode édition.
     *
     * @return true si en mode édition
     */
    boolean isEditMode();

    /**
     * Récupère les informations de l'instance actuelle.
     *
     * @return les informations de l'instance ou null
     */
    InstanceInfo getCurrentInstanceInfo();

    /**
     * Récupère les informations d'une instance par son ID.
     *
     * @param instanceId l'UUID de l'instance
     * @return les informations de l'instance ou null
     */
    InstanceInfo getInstanceInfo(UUID instanceId);

    /**
     * Vérifie si un template/monde existe pour un étage donné.
     *
     * @param floor l'étage à vérifier
     * @return true si le template existe
     */
    boolean templateExists(Floor floor);

    /**
     * Crée un template/monde pour un étage.
     *
     * @param floor l'étage pour lequel créer le template
     * @return CompletableFuture qui se termine avec true si la création réussit
     */
    CompletableFuture<Boolean> createTemplate(Floor floor);

    /**
     * Téléporte un joueur vers une instance.
     *
     * @param player le joueur à téléporter
     * @param instanceId l'UUID de l'instance de destination
     * @return CompletableFuture qui se termine avec true si la téléportation réussit
     */
    CompletableFuture<Boolean> sendPlayerToInstance(Player player, UUID instanceId);

    /**
     * Récupère le type de provider.
     *
     * @return le type de provider
     */
    ProviderType getType();

    /**
     * Sauvegarde le monde d'édition actuel dans le template.
     * Cette méthode copie le monde actuel vers le template du floor,
     * en adaptant la logique selon le provider (CloudNet, ASP, Vanilla).
     *
     * @param floor le floor dont il faut sauvegarder le template
     * @return CompletableFuture qui se termine avec true si la sauvegarde réussit
     */
    CompletableFuture<Boolean> saveEditWorldToTemplate(Floor floor);

    /**
     * Arrête proprement le provider et libère les ressources.
     */
    void shutdown();
}
