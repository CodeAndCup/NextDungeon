package fr.perrier.dungeons.spigot.parties;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.parties.impl.alessio.AlessioDPPartyProvider;
import fr.perrier.dungeons.spigot.parties.impl.internal.InternalPartyProvider;
import fr.perrier.dungeons.spigot.utils.LoggerUtil;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Central service for managing parties.
 * Automatically selects the best available party provider.
 * Follows the Open/Closed principle - extend by adding new providers.
 */
public class PartyService {

    /**
     * -- GETTER --
     *  Gets the singleton instance.
     *
     */
    @Getter
    private static PartyService instance;
    /**
     * -- GETTER --
     *  Gets the active party provider.
     *
     * @return the active provider
     */
    @Getter
    private final IPartyProvider activeProvider;
    private final List<IPartyProvider> providers;
    private final LoggerUtil logger;

    public PartyService() {
        this.logger = Main.getLoggerUtil();
        this.providers = new ArrayList<>();

        // Register providers in order of preference
        registerProvider(new AlessioDPPartyProvider());
        registerProvider(new InternalPartyProvider());

        // Select provider based on config
        this.activeProvider = selectProvider();

        logger.info("Party system initialized with provider: " + activeProvider.getName());
        instance = this;
    }

    /**
     * Registers a party provider.
     *
     * @param provider the provider to register
     */
    public void registerProvider(IPartyProvider provider) {
        providers.add(provider);
        logger.info("Registered party provider: " + provider.getName() + " (available: " + provider.isAvailable() + ")");
    }

    /**
     * Selects the provider based on the configuration.
     * If "AUTO" is selected, returns the first available provider.
     * Otherwise, attempts to find the requested provider by name.
     *
     * @return the selected provider
     */
    private IPartyProvider selectProvider() {
        String configuredType = Objects.requireNonNull(Main.getInstance().getConfig().getString("PartyProvider.type"));
        logger.info("Configured party provider type: " + configuredType);

        // AUTO mode: select first available provider
        if (configuredType.equalsIgnoreCase("AUTO")) {
            for (IPartyProvider provider : providers) {
                if (provider.isAvailable()) {
                    logger.info("AUTO mode: Selected provider " + provider.getName());
                    return provider;
                }
            }
        } else {
            // Try to find the requested provider by name
            for (IPartyProvider provider : providers) {
                if (provider.getName().equalsIgnoreCase(configuredType)) {
                    if (provider.isAvailable()) {
                        return provider;
                    } else {
                        logger.warning("Configured provider '" + configuredType + "' is not available. Falling back to AUTO selection.");
                        break;
                    }
                }
            }
            logger.warning("Configured provider '" + configuredType + "' not found. Falling back to AUTO selection.");

            // Fallback to first available
            for (IPartyProvider provider : providers) {
                if (provider.isAvailable()) {
                    return provider;
                }
            }
        }

        // Ultimate fallback to internal provider (always available)
        return providers.stream()
                .filter(p -> p.getName().equals("Internal"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No party provider available"));
    }

    /**
     * Gets the name of the active provider.
     *
     * @return the provider name
     */
    public String getActiveProviderName() {
        return activeProvider.getName();
    }

    // ==================== Delegate methods ====================

    /**
     * Creates a new party with the given leader.
     *
     * @param leader the party leader
     * @param name the party name
     * @return the created party, or empty if creation failed
     */
    public Optional<IParty> createParty(Player leader, String name) {
        return activeProvider.createParty(leader, name);
    }

    /**
     * Gets a party by its ID.
     *
     * @param partyId the party's unique identifier
     * @return the party, or empty if not found
     */
    public Optional<IParty> getParty(UUID partyId) {
        return activeProvider.getParty(partyId);
    }

    /**
     * Gets a party by its leader.
     *
     * @param leader the party leader
     * @return the party led by the player, or empty if not found
     */
    public Optional<IParty> getPartyByLeader(Player leader) {
        return activeProvider.getPartyByLeader(leader);
    }

    /**
     * Gets a party by its leader's UUID.
     *
     * @param leaderId the leader's UUID
     * @return the party, or empty if not found
     */
    public Optional<IParty> getPartyByLeader(UUID leaderId) {
        return activeProvider.getPartyByLeader(leaderId);
    }

    /**
     * Gets the party that a player is a member of.
     *
     * @param player the player
     * @return the player's party, or empty if not in a party
     */
    public Optional<IParty> getPartyOf(Player player) {
        return activeProvider.getPartyOf(player);
    }

    /**
     * Gets the party that a UUID is a member of.
     *
     * @param memberId the member's UUID
     * @return the party, or empty if not in a party
     */
    public Optional<IParty> getPartyOf(UUID memberId) {
        return activeProvider.getPartyOf(memberId);
    }

    /**
     * Gets all online parties.
     *
     * @return collection of online parties
     */
    public Collection<IParty> getOnlineParties() {
        return activeProvider.getOnlineParties();
    }

    /**
     * Finds parties whose description contains the given substring.
     *
     * @param partOfDesc substring to search for
     * @return collection of matching parties
     */
    public Collection<IParty> findPartiesByDescription(String partOfDesc) {
        return activeProvider.findPartiesByDescription(partOfDesc);
    }

    /**
     * Checks if a player is in any party.
     *
     * @param player the player to check
     * @return true if the player is in a party
     */
    public boolean isInParty(Player player) {
        return activeProvider.isInParty(player);
    }

    /**
     * Checks if a player is the leader of their party.
     *
     * @param player the player to check
     * @return true if the player is a party leader
     */
    public boolean isPartyLeader(Player player) {
        return activeProvider.isPartyLeader(player);
    }

    /**
     * Gets or creates a party member representation.
     *
     * @param player the player
     * @return the party member
     */
    public Optional<IPartyMember> getPartyMember(Player player) {
        return activeProvider.getPartyMember(player);
    }

    /**
     * Gets or creates a party member representation.
     *
     * @param memberId the member's UUID
     * @return the party member
     */
    public Optional<IPartyMember> getPartyMember(UUID memberId) {
        return activeProvider.getPartyMember(memberId);
    }

    /**
     * Shuts down the party service and cleans up resources.
     */
    public void shutdown() {
        if (activeProvider instanceof InternalPartyProvider internalProvider) {
            internalProvider.clearAll();
        }
        logger.info("Party service shut down");
    }
}

