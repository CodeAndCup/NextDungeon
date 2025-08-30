package fr.perrier.dungeons.spigot.webserver;

import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.utils.ServerUtil;
import fr.perrier.dungeons.spigot.webeditor.ProxyEditorMessageHandler;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DungeonWebEditorManager {

    private final Map<UUID, String> activeEditorSessions; // UUID du joueur -> sessionId du proxy
    private final ProxyEditorMessageHandler messageHandler;

    public DungeonWebEditorManager() {
        this.activeEditorSessions = new HashMap<>();
        this.messageHandler = new ProxyEditorMessageHandler();
    }

    /**
     * Méthode appelée par la commande /dungeon admin webeditor
     * Maintenant enregistre la session sur le proxy au lieu de démarrer un serveur local
     */
    public boolean startWebEditor(Player player, String dungeonName, String floorId) {
        UUID playerId = player.getUniqueId();

        if (activeEditorSessions.containsKey(playerId)) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&cYou already have an active web editor. Please stop it before starting a new one."));
            return false;
        }

        if(!ServerUtil.isInEditMode()) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&cYou can only start the web editor on an editing server."));
            return false;
        }

        try {
            // Au lieu de démarrer un serveur local, enregistrer la session sur le proxy via Redis
            String sessionId = createProxyEditorSession(player, dungeonName, floorId);
            
            if (sessionId != null) {
                activeEditorSessions.put(playerId, sessionId);
                
                // Informer le joueur avec la nouvelle URL
                player.sendMessage("");
                player.sendMessage(ChatUtil.getBar());
                player.sendMessage(ChatUtil.translate("&6🏰 &lÉDITEUR WEB DÉMARRÉ (PROXY)"));
                player.sendMessage(ChatUtil.translate("&7Donjon: &e" + dungeonName));
                player.sendMessage(ChatUtil.translate("&7Floor: &e" + floorId));
                player.sendMessage(ChatUtil.translate("&7URL: &b&nhttp://localhost:8080/" + sessionId + "/editor/"));
                player.sendMessage(ChatUtil.translate("&7Arrêt: &c/dungeon admin webeditor stop"));
                player.sendMessage(ChatUtil.getBar());
                
                return true;
            } else {
                player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&cAn error occurred while starting the web editor. Check the server console for details."));
                return false;
            }
        } catch (Exception e) {
            Main.getInstance().getLogger().severe("&An error occurred while starting the web editor: " + e.getMessage());
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&cAn error occurred while starting the web editor. Check the server console for details."));
            return false;
        }
    }

    /**
     * Arrête l'éditeur web d'un joueur
     */
    public boolean stopWebEditor(Player player) {
        UUID playerId = player.getUniqueId();
        String sessionId = activeEditorSessions.remove(playerId);

        if (sessionId != null) {
            // Informer le proxy que la session doit être supprimée
            notifyProxySessionStop(sessionId);
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&a✓ Web editor stopped."));
            return true;
        } else {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&cNo web editor is currently active."));
            return false;
        }
    }

    /**
     * Vérifie si un joueur a un éditeur web actif
     */
    public boolean hasActiveEditor(Player player) {
        return activeEditorSessions.containsKey(player.getUniqueId());
    }

    /**
     * Ferme tous les éditeurs web (appelé lors de l'arrêt du plugin)
     */
    public void shutdownAllEditors() {
        for (String sessionId : activeEditorSessions.values()) {
            notifyProxySessionStop(sessionId);
        }
        activeEditorSessions.clear();
        Main.getInstance().getLogger().info("All web editor sessions have been closed.");
    }

    /**
     * Retourne le nombre d'éditeurs actifs
     */
    public int getActiveEditorsCount() {
        return activeEditorSessions.size();
    }

    /**
     * Retourne le handler de messages pour traiter les requêtes du proxy
     */
    public ProxyEditorMessageHandler getMessageHandler() {
        return messageHandler;
    }

    /**
     * Crée une session d'édition sur le proxy via Redis
     */
    private String createProxyEditorSession(Player player, String dungeonName, String floorId) {
        try {
            // Créer directement la session puisque nous n'avons pas encore la communication Redis complète
            // Dans la vraie implémentation, ceci passerait par Redis
            
            String currentServerName = getCurrentServerName();
            String sessionId = floorId + "-" + UUID.randomUUID().toString().substring(0, 8);
            
            Main.getInstance().getLogger().info("📤 Demande création session proxy: " + sessionId);
            Main.getInstance().getLogger().info("📤 Joueur: " + player.getName() + " (" + player.getUniqueId() + ")");
            Main.getInstance().getLogger().info("📤 Serveur: " + currentServerName);
            Main.getInstance().getLogger().info("📤 Floor: " + dungeonName + "/" + floorId);
            
            // TODO: Dans l'implémentation complète, envoyer via Redis:
            // - Message au proxy avec les détails de la session
            // - Attendre confirmation du proxy
            // - Retourner le sessionId fourni par le proxy
            
            return sessionId;
        } catch (Exception e) {
            Main.getInstance().getLogger().severe("Erreur création session proxy: " + e.getMessage());
            return null;
        }
    }

    /**
     * Notifie le proxy qu'une session doit être arrêtée
     */
    private void notifyProxySessionStop(String sessionId) {
        // TODO: Envoyer message Redis au proxy pour supprimer la session
        Main.getInstance().getLogger().info("📤 Arrêt session proxy: " + sessionId);
    }

    /**
     * Récupère le nom du serveur actuel
     */
    private String getCurrentServerName() {
        // TODO: Récupérer le vrai nom du serveur depuis CloudNet ou la configuration
        return "dungeon-edit-server";
    }
}
