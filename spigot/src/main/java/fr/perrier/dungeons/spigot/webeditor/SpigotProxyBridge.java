package fr.perrier.dungeons.spigot.webeditor;

import fr.perrier.dungeons.spigot.Main;

/**
 * Bridge de communication avec le proxy via Redis (remplace l'ancien serveur HTTP)
 * Plus de conflit de port - communication uniquement via Redis pub/sub
 */
public class SpigotProxyBridge {

    public SpigotProxyBridge() {
        // Plus besoin d'initialiser le messageHandler car maintenant traité par WebEditorRequestSubscriber
    }

    /**
     * Démarre le pont de communication avec le proxy
     * Plus besoin de serveur HTTP - utilise uniquement Redis
     */
    public boolean startBridge() {
        try {
            Main.getInstance().getLogger().info("🌉 Pont de communication Redis activé (plus de serveur HTTP)");
            return true;
        } catch (Exception e) {
            Main.getInstance().getLogger().severe("Erreur démarrage pont Redis: " + e.getMessage());
            return false;
        }
    }

    /**
     * Arrête le pont de communication
     * Plus rien à arrêter côté HTTP
     */
    public void stopBridge() {
        Main.getInstance().getLogger().info("🛑 Pont de communication Redis désactivé");
    }
}