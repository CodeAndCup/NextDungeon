package fr.perrier.dungeons.spigot.utils;

import com.alessiodp.parties.api.interfaces.Party;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.instance.InstanceInfo;
import fr.perrier.dungeons.spigot.instance.InstanceProvider;
import fr.perrier.dungeons.spigot.model.FloorInstance;
import fr.perrier.dungeons.spigot.model.Floor;
import lombok.NonNull;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Utilitaire pour gérer les serveurs et instances de donjons.
 * Cette classe agit maintenant comme une façade (Facade Pattern)
 * pour l'InstanceProvider abstrait.
 */
public class ServerUtil {

    /**
     * Récupère le provider d'instances actuel.
     * @return le provider configuré
     */
    private static InstanceProvider getProvider() {
        return Main.getInstance().getInstanceProvider();
    }

    /**
     * Create a cloud service instance for the given floor instance and start it asynchronously.
     *
     * @param floor the floor to create the service for
     * @return the unique id of the created service or null if the creation failed
     */
    public static UUID makeFloorInstance(Floor floor, boolean editMode) {
        try {
            // Utilisation synchrone pour compatibilité avec le code existant
            return getProvider().createInstance(floor, editMode).get();
        } catch (Exception e) {
            Main.getLoggerUtil().severe("Erreur lors de la création de l'instance: " + e.getMessage());
            return null;
        }
    }

    /**
     * Create a cloud service instance asynchronously.
     *
     * @param floor the floor to create the service for
     * @param editMode whether to create in edit mode
     * @return CompletableFuture with the instance UUID
     */
    public static CompletableFuture<UUID> makeFloorInstanceAsync(Floor floor, boolean editMode) {
        return getProvider().createInstance(floor, editMode);
    }

    /**
     * Checks if the current server is a dungeon instance
     * @return true if this is a dungeon instance server
     */
    public static boolean isInstanceServer() {
        return getProvider().isInstanceServer();
    }

    /**
     * Checks if the current server is in edit mode
     * @return true if this server is in edit mode
     */
    public static boolean isInEditMode() {
        return getProvider().isEditMode();
    }

    /**
     * Gets the instance information for this server if it's an instance
     * @return InstanceInfo containing the instance details, or null if not an instance
     */
    public static InstanceInfo getInstanceInfo() {
        return getProvider().getCurrentInstanceInfo();
    }

    /**
     * Retrieves the instance information from the given instance ID.
     * If the instance does not exist, it will return null.
     * @param instanceId the unique ID of the instance to retrieve
     * @return InstanceInfo containing the instance details, or null if not found
     */
    public static InstanceInfo getInstanceInfo(UUID instanceId) {
        return getProvider().getInstanceInfo(instanceId);
    }

    /**
     * Check if a template exists for the given floor in the local storage.
     * @param floor the floor to check
     * @return true if the template exists, false otherwise
     */
    public static boolean isFloorTemplateExists(@NonNull Floor floor) {
        return getProvider().templateExists(floor);
    }

    /**
     * Create a template for a floor in the local storage.
     * The template is based on the global template and is copied to the local storage.
     * The template is named after the floor ID and is given a priority of 0.
     * The template is also added to the service task provider.
     *
     * @param floor the floor to create the template for
     */
    public static CompletableFuture<Boolean> createFloorTemplate(@NonNull Floor floor) {
        return getProvider().createTemplate(floor);
    }

    /**
     * Delete the template (ServiceTask + template files) associated with a floor.
     *
     * @param floorId the ID of the floor whose template should be deleted
     * @return CompletableFuture that completes with true if deletion succeeds
     */
    public static CompletableFuture<Boolean> deleteFloorTemplate(@NonNull String floorId) {
        return getProvider().deleteTemplate(floorId);
    }

    /**
     * Connects a party to a cloud service with the given name.
     * @param party The party to connect to the cloud service.
     * @param server The name of the cloud service to connect to.
     */
    public static void sendToServer(Party party, String server) {
        // Cette méthode dépend de CloudNet spécifiquement
        // Pour une abstraction complète, il faudrait créer une interface PartyProvider
        // Pour l'instant, on garde la compatibilité CloudNet
        Main.getLoggerUtil().warning("sendToServer(Party, String) n'est supporté qu'avec CloudNet");
    }

    /**
     * Connects a party to a cloud service with the given name.
     * @param party The party to connect to the cloud service.
     * @param serviceId The UUID of the cloud service to connect to.
     */
    public static void sendToServer(Party party, UUID serviceId) {
        // Pour les providers non-CloudNet, on téléporte chaque membre individuellement
        for (UUID uuid : party.getMembers()) {
            Player player = org.bukkit.Bukkit.getPlayer(uuid);
            if (player != null) {
                sendToServer(player, serviceId);
            }
        }
    }

    /**
     * Connects a player to a cloud service with the given name.
     * @param player The player to connect to the cloud service.
     * @param server The name of the cloud service to connect to.
     */
    public static void sendToServer(Player player, String server) {
        Main.getLoggerUtil().warning("sendToServer(Player, String) n'est supporté qu'avec CloudNet");
    }

    /**
     * Connects a player to a cloud service using the specified service ID.
     *
     * @param player The player to connect to the cloud service.
     * @param serviceId The UUID of the cloud service to connect to.
     */
    public static void sendToServer(Player player, UUID serviceId) {
        getProvider().sendPlayerToInstance(player, serviceId);
    }

    /**
     * Retrieves a ServiceInfoSnapshot for a cloud service with the specified port.
     * Note: Cette méthode est spécifique à CloudNet et deprecated.
     *
     * @param port The port of the cloud service to retrieve.
     * @return The ServiceInfoSnapshot of the cloud service with the given port,
     *         or null if no such service is found.
     * @deprecated Utiliser getInstanceInfo() à la place
     */
    @Deprecated
    public static Object getFactory(int port) {
        Main.getLoggerUtil().warning("getFactory() est deprecated et spécifique à CloudNet");
        return null;
    }
}
