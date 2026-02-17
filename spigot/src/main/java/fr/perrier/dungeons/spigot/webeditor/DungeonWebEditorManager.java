package fr.perrier.dungeons.spigot.webeditor;

import fr.perrier.cupcodeapi.utils.ChatUtil;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.utils.ServerUtil;
import lombok.Getter;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DungeonWebEditorManager {

    private final Map<UUID, String> activeEditorSessions; // UUID du joueur -> sessionId du proxy
    /**
     * -- GETTER --
     *  Retourne le handler de messages pour traiter les requêtes du proxy
     */
    @Getter
    private final ProxyEditorMessageHandler messageHandler;
    private final ProxyBridgeService bridgeService;

    public DungeonWebEditorManager() {
        this.activeEditorSessions = new HashMap<>();
        this.messageHandler = new ProxyEditorMessageHandler();
        this.bridgeService = new ProxyBridgeService();
    }

    /**
     * Méthode appelée par la commande /dungeon admin webeditor
     * Maintenant enregistre la session sur le proxy au lieu de démarrer un serveur local
     */
    public void startWebEditor(Player player, String dungeonName, String floorId) {
        UUID playerId = player.getUniqueId();

        if (activeEditorSessions.containsKey(playerId)) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000You already have an active web editor. Please stop it before starting a new one."));
            return;
        }

        if(!ServerUtil.isInEditMode()) {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000You can only start the web editor on an editing server."));
            return;
        }

        try {
            // Au lieu de démarrer un serveur local, enregistrer la session sur le proxy via HTTP
            bridgeService.requestEditorSession(dungeonName, floorId, player.getUniqueId(), player.getName()).thenAccept(sessionId -> {
                if (sessionId != null) {
                    activeEditorSessions.put(playerId, sessionId);

                    // Informer le joueur avec la nouvelle URL
                    player.sendMessage("");
                    player.sendMessage(ChatUtil.getBar());
                    player.sendMessage(ChatUtil.translate("&6🏰 &lÉDITEUR WEB DÉMARRÉ (PROXY)"));
                    player.sendMessage(ChatUtil.translate("&7Donjon: &e" + dungeonName));
                    player.sendMessage(ChatUtil.translate("&7Floor: &e" + floorId));

                    // Récupérer le port depuis la config
                    int port = Main.getInstance().getConfig().getInt("WebEditor.proxy-port", 7734);
                    String url = "http://localhost:" + port + "/" + sessionId + "/editor/";

                    // Créer un message cliquable
                    TextComponent urlMessage = new TextComponent(ChatUtil.translate("&7URL: "));
                    TextComponent urlComponent = new TextComponent(ChatUtil.translate("&b&n" + url));
                    urlComponent.setClickEvent(new ClickEvent(
                            ClickEvent.Action.OPEN_URL, url));
                    urlComponent.setHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new ComponentBuilder("§eCliquez pour ouvrir dans votre navigateur").create()));
                    urlMessage.addExtra(urlComponent);
                    player.spigot().sendMessage(urlMessage);

                    player.sendMessage(ChatUtil.translate("&7Arrêt: &#FF0000/dungeon admin webeditor stop"));
                    player.sendMessage(ChatUtil.getBar());

                } else {
                    player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000Impossible de créer la session sur le proxy. Vérifiez que le proxy est démarré."));
                }
            }).exceptionally(e -> {
                Main.getLoggerUtil().severe("An error occurred while requesting the web editor session: " + e.getMessage());
                player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000An error occurred while starting the web editor. Check the server console for details."));
                return null;
            });
        } catch (Exception e) {
            Main.getLoggerUtil().severe("&#00FF00n error occurred while starting the web editor: " + e.getMessage());
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000An error occurred while starting the web editor. Check the server console for details."));
        }
    }

    /**
     * Arrête l'éditeur web d'un joueur
     */
    public void stopWebEditor(Player player) {
        UUID playerId = player.getUniqueId();
        String sessionId = activeEditorSessions.remove(playerId);

        if (sessionId != null) {
            // Informer le proxy que la session doit être supprimée
            bridgeService.requestSessionStop(sessionId);
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#00FF00✓ Web editor stopped."));
        } else {
            player.sendMessage(ChatUtil.translate(Main.getPrefix() + "&#FF0000No web editor is currently active."));
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
            bridgeService.requestSessionStop(sessionId);
        }
        activeEditorSessions.clear();
        Main.getLoggerUtil().info("All web editor sessions have been closed.");
    }

    /**
     * Retourne le nombre d'éditeurs actifs
     */
    public int getActiveEditorsCount() {
        return activeEditorSessions.size();
    }
}
