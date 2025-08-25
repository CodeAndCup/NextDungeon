package fr.perrier.dungeons.webserver;

import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.dungeons.Main;
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

        // Vérifier si le joueur a déjà un éditeur ouvert
        if (activeServers.containsKey(playerId)) {
            player.sendMessage(ChatUtil.translate("&cVous avez déjà un éditeur web ouvert ! Fermez-le d'abord avec /dungeon admin webeditor stop"));
            return false;
        }

        // TODO: Vérifier que le joueur est bien dans un donjon en mode edit
        // if (!isDungeonInEditMode(player, dungeonName)) {
        //     player.sendMessage("§cVous devez être dans un donjon en mode édition !");
        //     return false;
        // }

        try {
            WebEditorServer server = new WebEditorServer(player);
            if (server.startServer(dungeonName, floorId)) {
                activeServers.put(playerId, server);
                return true;
            } else {
                player.sendMessage(ChatUtil.translate("&cErreur lors du démarrage de l'éditeur web !"));
                return false;
            }
        } catch (Exception e) {
            Main.getInstance().getLogger().severe("Erreur lors du démarrage de l'éditeur web: " + e.getMessage());
            player.sendMessage(ChatUtil.translate("&cErreur interne lors du démarrage de l'éditeur web !"));
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
            player.sendMessage(ChatUtil.translate("&a✓ Éditeur web arrêté !"));
            return true;
        } else {
            player.sendMessage(ChatUtil.translate("&cAucun éditeur web n'est actuellement ouvert !"));
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
        Main.getInstance().getLogger().info("Tous les éditeurs web ont été fermés");
    }

    /**
     * Retourne le nombre d'éditeurs actifs
     */
    public int getActiveEditorsCount() {
        return activeServers.size();
    }
}
