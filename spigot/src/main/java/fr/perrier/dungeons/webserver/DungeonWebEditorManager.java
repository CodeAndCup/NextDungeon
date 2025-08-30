package fr.perrier.dungeons.webserver;

import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.utils.ServerUtil;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DungeonWebEditorManager {

    private final Map<UUID, WebEditorServer> activeServers;

    public DungeonWebEditorManager() {
        this.activeServers = new HashMap<>();
    }

    /**
     * Méthode appelée par la commande /dungeon admin webeditor
     */
    public boolean startWebEditor(Player player, String dungeonName, String floorId) {
        UUID playerId = player.getUniqueId();

        if (activeServers.containsKey(playerId)) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&cYou already have an active web editor. Please stop it before starting a new one."));
            return false;
        }

        if(!ServerUtil.isInEditMode()) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&cYou can only start the web editor on an editing server."));
            return false;
        }

        try {
            WebEditorServer server = new WebEditorServer(player);
            if (server.startServer(dungeonName, floorId)) {
                activeServers.put(playerId, server);
                return true;
            } else {
                player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&cAn error occurred while starting the web server. Check the server console for details."));
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
        WebEditorServer server = activeServers.remove(playerId);

        if (server != null) {
            server.stopServer();
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
        return activeServers.containsKey(player.getUniqueId());
    }

    /**
     * Ferme tous les éditeurs web (appelé lors de l'arrêt du plugin)
     */
    public void shutdownAllEditors() {
        for (WebEditorServer server : activeServers.values()) {
            server.stopServer();
        }
        activeServers.clear();
        Main.getInstance().getLogger().info("All web editors have been shut down.");
    }

    /**
     * Retourne le nombre d'éditeurs actifs
     */
    public int getActiveEditorsCount() {
        return activeServers.size();
    }
}
