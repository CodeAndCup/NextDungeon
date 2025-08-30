package fr.perrier.dungeons.bungee.commands;

import fr.perrier.dungeons.bungee.NextDungeonBungee;
import fr.perrier.dungeons.bungee.webeditor.EditorSessionManager;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.UUID;

/**
 * Commandes proxy pour gérer les sessions d'édition web
 */
public class WebEditorProxyCommand extends Command {

    private final EditorSessionManager sessionManager;

    public WebEditorProxyCommand() {
        super("webeditor-proxy", "nextdungeons.proxy.webeditor");
        this.sessionManager = NextDungeonBungee.getInstance().getWebEditorServer().getSessionManager();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer player)) {
            sender.sendMessage("§cCette commande ne peut être exécutée que par un joueur.");
            return;
        }

        if (args.length == 0) {
            sendHelp(player);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> handleCreateSession(player, args);
            case "stop" -> handleStopSession(player, args);
            case "list" -> handleListSessions(player);
            case "info" -> handleSessionInfo(player, args);
            default -> sendHelp(player);
        }
    }

    private void handleCreateSession(ProxiedPlayer player, String[] args) {
        if (args.length < 4) {
            player.sendMessage("§cUsage: /webeditor-proxy create <dungeonName> <floorId> <spigotServer>");
            return;
        }

        String dungeonName = args[1];
        String floorId = args[2];
        String spigotServer = args[3];

        if (sessionManager.hasActiveSession(player.getUniqueId())) {
            player.sendMessage("§cVous avez déjà une session d'édition active.");
            return;
        }

        String sessionId = sessionManager.createSessionFromProxy(
            dungeonName, 
            floorId, 
            player.getUniqueId(), 
            player.getName(), 
            spigotServer
        );

        player.sendMessage("§a✅ Session d'édition créée!");
        player.sendMessage("§7URL: §bhttp://localhost:8080/" + sessionId + "/editor/");
        player.sendMessage("§7Session ID: §e" + sessionId);
    }

    private void handleStopSession(ProxiedPlayer player, String[] args) {
        if (sessionManager.removeSessionByPlayer(player.getUniqueId())) {
            player.sendMessage("§a✅ Session d'édition fermée.");
        } else {
            player.sendMessage("§cAucune session d'édition active.");
        }
    }

    private void handleListSessions(ProxiedPlayer player) {
        int count = sessionManager.getActiveSessionCount();
        player.sendMessage("§6Sessions d'édition actives: §e" + count);
        
        sessionManager.getActiveSessions().values().forEach(session -> {
            player.sendMessage("§7- " + session.getSessionId() + " §8(" + session.getEditorName() + ")");
        });
    }

    private void handleSessionInfo(ProxiedPlayer player, String[] args) {
        EditorSessionManager.EditorSession session = sessionManager.getActiveSessions().values().stream()
            .filter(s -> s.getEditorUuid().equals(player.getUniqueId()))
            .findFirst()
            .orElse(null);

        if (session != null) {
            player.sendMessage("§6Votre session d'édition:");
            player.sendMessage("§7ID: §e" + session.getSessionId());
            player.sendMessage("§7Donjon: §e" + session.getDungeonName());
            player.sendMessage("§7Floor: §e" + session.getFloorId());
            player.sendMessage("§7Serveur: §e" + session.getSpigotServer());
            player.sendMessage("§7URL: §bhttp://localhost:8080/" + session.getSessionId() + "/editor/");
        } else {
            player.sendMessage("§cAucune session d'édition active.");
        }
    }

    private void sendHelp(ProxiedPlayer player) {
        player.sendMessage("§6=== Commandes Éditeur Web Proxy ===");
        player.sendMessage("§7/webeditor-proxy create <dungeon> <floor> <server>");
        player.sendMessage("§7/webeditor-proxy stop");
        player.sendMessage("§7/webeditor-proxy list");
        player.sendMessage("§7/webeditor-proxy info");
    }
}