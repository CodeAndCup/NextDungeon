package fr.perrier.dungeons.velocity.commands;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import fr.perrier.dungeons.velocity.NextDungeonVelocity;
import fr.perrier.dungeons.velocity.webeditor.EditorSessionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;
import java.util.UUID;

/**
 * Commandes proxy pour gérer les sessions d'édition web
 */
public class WebEditorProxyCommand implements SimpleCommand {

    private final EditorSessionManager sessionManager;

    public WebEditorProxyCommand() {
        this.sessionManager = NextDungeonVelocity.getInstance().getWebEditorServer().getSessionManager();
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            invocation.source().sendMessage(Component.text("Cette commande ne peut être exécutée que par un joueur.", NamedTextColor.RED));
            return;
        }

        String[] args = invocation.arguments();

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

    private void handleCreateSession(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(Component.text("Usage: /webeditor-proxy create <dungeonName> <floorId> <spigotServer>", NamedTextColor.RED));
            return;
        }

        String dungeonName = args[1];
        String floorId = args[2];
        String spigotServer = args[3];

        if (sessionManager.hasActiveSession(player.getUniqueId())) {
            player.sendMessage(Component.text("Vous avez déjà une session d'édition active.", NamedTextColor.RED));
            return;
        }

        String sessionId = sessionManager.createSessionFromProxy(
            dungeonName, 
            floorId, 
            player.getUniqueId(), 
            player.getUsername(), 
            spigotServer
        );

        player.sendMessage(Component.text("✅ Session d'édition créée!", NamedTextColor.GREEN));
        player.sendMessage(Component.text()
            .append(Component.text("URL: ", NamedTextColor.GRAY))
            .append(Component.text("http://localhost:8080/" + sessionId + "/editor/", NamedTextColor.AQUA))
            .build());
        player.sendMessage(Component.text()
            .append(Component.text("Session ID: ", NamedTextColor.GRAY))
            .append(Component.text(sessionId, NamedTextColor.YELLOW))
            .build());
    }

    private void handleStopSession(Player player, String[] args) {
        if (sessionManager.removeSessionByPlayer(player.getUniqueId())) {
            player.sendMessage(Component.text("✅ Session d'édition fermée.", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Aucune session d'édition active.", NamedTextColor.RED));
        }
    }

    private void handleListSessions(Player player) {
        int count = sessionManager.getActiveSessionCount();
        player.sendMessage(Component.text()
            .append(Component.text("Sessions d'édition actives: ", NamedTextColor.GOLD))
            .append(Component.text(String.valueOf(count), NamedTextColor.YELLOW))
            .build());
        
        sessionManager.getActiveSessions().values().forEach(session -> {
            player.sendMessage(Component.text()
                .append(Component.text("- " + session.getSessionId() + " ", NamedTextColor.GRAY))
                .append(Component.text("(" + session.getEditorName() + ")", NamedTextColor.DARK_GRAY))
                .build());
        });
    }

    private void handleSessionInfo(Player player, String[] args) {
        EditorSessionManager.EditorSession session = sessionManager.getActiveSessions().values().stream()
            .filter(s -> s.getEditorUuid().equals(player.getUniqueId()))
            .findFirst()
            .orElse(null);

        if (session != null) {
            player.sendMessage(Component.text("Votre session d'édition:", NamedTextColor.GOLD));
            player.sendMessage(Component.text()
                .append(Component.text("ID: ", NamedTextColor.GRAY))
                .append(Component.text(session.getSessionId(), NamedTextColor.YELLOW))
                .build());
            player.sendMessage(Component.text()
                .append(Component.text("Donjon: ", NamedTextColor.GRAY))
                .append(Component.text(session.getDungeonName(), NamedTextColor.YELLOW))
                .build());
            player.sendMessage(Component.text()
                .append(Component.text("Floor: ", NamedTextColor.GRAY))
                .append(Component.text(session.getFloorId(), NamedTextColor.YELLOW))
                .build());
            player.sendMessage(Component.text()
                .append(Component.text("Serveur: ", NamedTextColor.GRAY))
                .append(Component.text(session.getSpigotServer(), NamedTextColor.YELLOW))
                .build());
            player.sendMessage(Component.text()
                .append(Component.text("URL: ", NamedTextColor.GRAY))
                .append(Component.text("http://localhost:8080/" + session.getSessionId() + "/editor/", NamedTextColor.AQUA))
                .build());
        } else {
            player.sendMessage(Component.text("Aucune session d'édition active.", NamedTextColor.RED));
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.text("=== Commandes Éditeur Web Proxy ===", NamedTextColor.GOLD, TextDecoration.BOLD));
        player.sendMessage(Component.text("/webeditor-proxy create <dungeon> <floor> <server>", NamedTextColor.GRAY));
        player.sendMessage(Component.text("/webeditor-proxy stop", NamedTextColor.GRAY));
        player.sendMessage(Component.text("/webeditor-proxy list", NamedTextColor.GRAY));
        player.sendMessage(Component.text("/webeditor-proxy info", NamedTextColor.GRAY));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("nextdungeons.proxy.webeditor");
    }
}