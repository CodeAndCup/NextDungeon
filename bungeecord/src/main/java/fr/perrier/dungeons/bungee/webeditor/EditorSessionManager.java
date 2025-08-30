package fr.perrier.dungeons.bungee.webeditor;

import lombok.Data;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire des sessions d'édition actives sur le proxy
 */
public class EditorSessionManager {

    @Getter
    private final Map<String, EditorSession> activeSessions = new ConcurrentHashMap<>();

    /**
     * Crée une nouvelle session d'édition
     */
    public String createSession(String dungeonName, String floorId, UUID editorUuid, String editorName, String spigotServer) {
        String sessionId = floorId + "-" + UUID.randomUUID().toString().substring(0, 8);
        
        EditorSession session = new EditorSession(
            sessionId,
            dungeonName, 
            floorId,
            editorUuid,
            editorName,
            spigotServer,
            Instant.now()
        );
        
        activeSessions.put(sessionId, session);
        return sessionId;
    }

    /**
     * Récupère une session par son ID
     */
    public EditorSession getSession(String sessionId) {
        return activeSessions.get(sessionId);
    }

    /**
     * Supprime une session
     */
    public boolean removeSession(String sessionId) {
        return activeSessions.remove(sessionId) != null;
    }

    /**
     * Supprime toutes les sessions d'un serveur Spigot
     */
    public void removeSessionsForServer(String spigotServer) {
        activeSessions.entrySet().removeIf(entry -> 
            entry.getValue().getSpigotServer().equals(spigotServer));
    }

    /**
     * Supprime une session par UUID de joueur
     */
    public boolean removeSessionByPlayer(UUID playerUuid) {
        return activeSessions.entrySet().removeIf(entry -> 
            entry.getValue().getEditorUuid().equals(playerUuid));
    }

    /**
     * Vérifie si un joueur a déjà une session active
     */
    public boolean hasActiveSession(UUID playerUuid) {
        return activeSessions.values().stream()
            .anyMatch(session -> session.getEditorUuid().equals(playerUuid));
    }

    /**
     * Nettoie les sessions expirées (plus de 2 heures)
     */
    public void cleanupExpiredSessions() {
        Instant expireTime = Instant.now().minusSeconds(7200); // 2 heures
        activeSessions.entrySet().removeIf(entry -> 
            entry.getValue().getCreatedAt().isBefore(expireTime));
    }

    /**
     * Retourne le nombre de sessions actives
     */
    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    /**
     * Crée une nouvelle session d'édition depuis le proxy
     */
    public String createSessionFromProxy(String dungeonName, String floorId, UUID editorUuid, String editorName, String spigotServer) {
        return createSession(dungeonName, floorId, editorUuid, editorName, spigotServer);
    }

    /**
     * Représente une session d'édition active
     */
    @Data
    public static class EditorSession {
        private final String sessionId;
        private final String dungeonName;
        private final String floorId;
        private final UUID editorUuid;
        private final String editorName;
        private final String spigotServer;
        private final Instant createdAt;
    }
}