package fr.perrier.dungeons.spigot.instance;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.instance.impl.CloudNetProvider;
import org.bukkit.Bukkit;

/**
 * Factory pour créer le provider d'instances approprié.
 * Utilise le pattern Factory et respecte le principe Open/Closed de SOLID.
 */
public class InstanceProviderFactory {

    /**
     * Crée et retourne le provider approprié selon la configuration.
     *
     * @return le provider d'instances configuré
     * @throws IllegalStateException si aucun provider compatible n'est trouvé
     */
    public static InstanceProvider createProvider() {
        String providerType = Main.getInstance().getConfig().getString("InstanceProvider.type", "CLOUDNET");

        Main.getInstance().getLogger().info("Initialisation du provider: " + providerType);

        switch (providerType.toUpperCase()) {
            case "CLOUDNET":
                return createCloudNetProvider();

            default:
                Main.getInstance().getLogger().warning("Type de provider inconnu: " + providerType + ".");
                throw new IllegalStateException("Type de provider inconnu: " + providerType);
        }
    }

    /**
     * Crée un provider CloudNet si disponible.
     */
    private static InstanceProvider createCloudNetProvider() {
        // Vérifier si CloudNet est présent
        try {
            Class.forName("eu.cloudnetservice.driver.inject.InjectionLayer");
            Main.getInstance().getLogger().info("CloudNet détecté, utilisation du CloudNetProvider");
            return new CloudNetProvider();
        } catch (ClassNotFoundException e) {
            Main.getInstance().getLogger().severe("CloudNet n'est pas disponible sur ce serveur !");
            throw new IllegalStateException("CloudNet provider requis mais non disponible", e);
        }
    }


    /**
     * Détecte automatiquement le meilleur provider disponible.
     *
     * @return le provider le plus approprié
     */
    public static InstanceProvider autoDetect() {

        try {
            Class.forName("eu.cloudnetservice.driver.inject.InjectionLayer");
            Main.getInstance().getLogger().info("Auto-détection: CloudNet trouvé");
            return new CloudNetProvider();
        } catch (ClassNotFoundException e) {
            // CloudNet non disponible
        }

        Main.getInstance().getLogger().info("Auto-détection: Aucun provider disponible.");
        throw new IllegalStateException("Aucun provider d'instance disponible");
    }
}



