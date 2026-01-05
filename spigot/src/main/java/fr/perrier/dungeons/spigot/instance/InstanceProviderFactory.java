package fr.perrier.dungeons.spigot.instance;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.instance.impl.CloudNetProvider;
import org.bukkit.Bukkit;

/**
 * Factory for creating instance providers based on configuration.
 * This class checks for available providers and initializes the appropriate one.
 */
public class InstanceProviderFactory {

    /**
     * Creates an instance provider based on the configuration.
     * @return the created InstanceProvider
     */
    public static InstanceProvider createProvider() {
        String providerType = Main.getInstance().getConfig().getString("InstanceProvider.type", "CLOUDNET");

        Main.getInstance().getLogger().info("Provider initialization: " + providerType);

        switch (providerType.toUpperCase()) {
            case "CLOUDNET":
                return createCloudNetProvider();

            default:
                Main.getInstance().getLogger().warning("&eProvider type unknown: " + providerType + ".");
                throw new IllegalStateException("Provider type unknown: " + providerType);
        }
    }

    /**
     * Creates a CloudNet instance provider.
     * @return the CloudNet InstanceProvider
     */
    private static InstanceProvider createCloudNetProvider() {
        try {
            Class.forName("eu.cloudnetservice.driver.inject.InjectionLayer");
            Main.getInstance().getLogger().info("CloudNet detected, using CloudNetProvider");
            return new CloudNetProvider();
        } catch (ClassNotFoundException e) {
            Main.getInstance().getLogger().severe("&#FF0000CloudNet is not available on this server!");
            throw new IllegalStateException("CloudNet provider required but not available", e);
        }
    }

    /**
     * Auto-detects and creates an instance provider based on available libraries.
     * @return the detected InstanceProvider
     */
    public static InstanceProvider autoDetect() {

        try {
            Class.forName("eu.cloudnetservice.driver.inject.InjectionLayer");
            Main.getInstance().getLogger().info("Auto-detection: CloudNet found");
            return new CloudNetProvider();
        } catch (ClassNotFoundException e) {
            // CloudNet non disponible
        }

        Main.getInstance().getLogger().info("Auto-detection: No provider available.");
        throw new IllegalStateException("No instance providers available");
    }
}



