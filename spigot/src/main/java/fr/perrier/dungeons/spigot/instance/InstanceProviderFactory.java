package fr.perrier.dungeons.spigot.instance;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.instance.impl.ASPProvider;
import fr.perrier.dungeons.spigot.instance.impl.CloudNetProvider;
import fr.perrier.dungeons.spigot.instance.impl.VanillaProvider;
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

            case "ASP":
                return createASPProvider();

            case "VANILLA":
                return createVanillaProvider();

            default:
                Main.getInstance().getLogger().warning("Type de provider inconnu: " + providerType + ". Utilisation de VANILLA par défaut.");
                return createVanillaProvider();
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
     * Crée un provider ASP si disponible.
     */
    private static InstanceProvider createASPProvider() {
        // Vérifier si ASP est présent
        if (Bukkit.getPluginManager().getPlugin("SlimeWorldManager") != null) {
            Main.getInstance().getLogger().info("SlimeWorldManager détecté, utilisation du ASPProvider");
            return new ASPProvider();
        } else {
            Main.getInstance().getLogger().severe("SlimeWorldManager n'est pas disponible sur ce serveur !");
            throw new IllegalStateException("ASP provider requis mais non disponible");
        }
    }

    /**
     * Crée un provider Vanilla (toujours disponible).
     */
    private static InstanceProvider createVanillaProvider() {
        Main.getInstance().getLogger().info("Utilisation du VanillaProvider (système de mondes natif)");
        return new VanillaProvider();
    }

    /**
     * Détecte automatiquement le meilleur provider disponible.
     *
     * @return le provider le plus approprié
     */
    public static InstanceProvider autoDetect() {
        // Ordre de priorité : CloudNet > ASP > Vanilla

        try {
            Class.forName("eu.cloudnetservice.driver.inject.InjectionLayer");
            Main.getInstance().getLogger().info("Auto-détection: CloudNet trouvé");
            return new CloudNetProvider();
        } catch (ClassNotFoundException e) {
            // CloudNet non disponible
        }

        if (Bukkit.getPluginManager().getPlugin("SlimeWorldManager") != null) {
            Main.getInstance().getLogger().info("Auto-détection: ASP trouvé");
            return new ASPProvider();
        }

        Main.getInstance().getLogger().info("Auto-détection: Utilisation de Vanilla");
        return new VanillaProvider();
    }
}



