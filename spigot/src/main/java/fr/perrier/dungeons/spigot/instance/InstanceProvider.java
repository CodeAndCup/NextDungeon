package fr.perrier.dungeons.spigot.instance;

import fr.perrier.dungeons.spigot.model.Floor;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
/**
 * Main abstraction for managing dungeon instances.
 * This interface follows the Dependency Inversion Principle (DIP) from SOLID.
 * Implementations can use different systems:
 * - CloudNet (cloud services)
 * - ASP (Advanced Slime World Manager)
 * - Vanilla (optimized Minecraft worlds)
 */
public interface InstanceProvider {

    /**
     * Initializes the provider and checks that all dependencies are available.
     *
     * @return CompletableFuture that completes with true if initialization succeeds
     */
    CompletableFuture<Boolean> initialize();

    /**
     * Creates a new instance for a dungeon floor.
     *
     * @param floor the floor for which to create the instance
     * @param editMode true if the instance should be created in edit mode
     * @return CompletableFuture containing the UUID of the created instance, or null if failed
     */
    CompletableFuture<UUID> createInstance(Floor floor, boolean editMode);

    /**
     * Deletes a dungeon instance.
     *
     * @param instanceId the UUID of the instance to delete
     * @return CompletableFuture that completes with true if deletion succeeds
     */
    CompletableFuture<Boolean> deleteInstance(UUID instanceId);

    /**
     * Checks if the current server is a dungeon instance.
     *
     * @return true if it is an instance
     */
    boolean isInstanceServer();

    /**
     * Checks if the current server is in edit mode.
     *
     * @return true if in edit mode
     */
    boolean isEditMode();

    /**
     * Retrieves the information of the current instance.
     *
     * @return the instance information or null
     */
    InstanceInfo getCurrentInstanceInfo();

    /**
     * Retrieves the information of an instance by its ID.
     *
     * @param instanceId the UUID of the instance
     * @return the instance information or null
     */
    InstanceInfo getInstanceInfo(UUID instanceId);

    /**
     * Checks if a template/world exists for a given floor.
     *
     * @param floor the floor to check
     * @return true if the template exists
     */
    boolean templateExists(Floor floor);

    /**
     * Creates a template/world for a floor.
     *
     * @param floor the floor for which to create the template
     * @return CompletableFuture that completes with true if creation succeeds
     */
    CompletableFuture<Boolean> createTemplate(Floor floor);

    /**
     * Deletes the template/world associated with a floor.
     * For CloudNet: removes the ServiceTask and the template directory.
     *
     * @param floorId the ID of the floor whose template should be deleted
     * @return CompletableFuture that completes with true if deletion succeeds
     */
    CompletableFuture<Boolean> deleteTemplate(String floorId);

    /**
     * Teleports a player to an instance.
     *
     * @param player the player to teleport
     * @param instanceId the UUID of the destination instance
     * @return CompletableFuture that completes with true if teleportation succeeds
     */
    CompletableFuture<Boolean> sendPlayerToInstance(Player player, UUID instanceId);

    /**
     * Returns the cloud service UUID of the current server, regardless of whether it is
     * a dungeon instance or a lobby. Used to record a player's origin server before they
     * are transferred to a dungeon, so they can be sent back on dungeon completion.
     *
     * @return the UUID of the current cloud service, or null if it cannot be resolved
     */
    UUID getCurrentServiceUniqueId();

    /**
     * Gets the provider type.
     *
     * @return the provider type
     */
    ProviderType getType();

    /**
     * Saves the current edit world to the template.
     * This method copies the current world to the floor's template,
     * adapting the logic according to the provider (CloudNet, ASP, Vanilla).
     *
     * @param floor the floor whose template should be saved
     * @return CompletableFuture that completes with true if saving succeeds
     */
    CompletableFuture<Boolean> saveEditWorldToTemplate(Floor floor);

    /**
     * Properly shuts down the provider and releases resources.
     */
    void shutdown();
}
