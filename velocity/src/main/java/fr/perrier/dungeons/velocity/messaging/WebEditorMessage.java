package fr.perrier.dungeons.velocity.messaging;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * Messages Redis pour la communication proxy <-> Spigot pour l'éditeur web
 */
@Data
public class WebEditorMessage<T> {
    private final String requestId;
    private final String spigotServer;
    private final WebEditorMessageType type;
    private final T data;
    private final Instant timestamp;

    public enum WebEditorMessageType {
        // Proxy -> Spigot
        LOAD_TRIGGERS_REQUEST,
        SAVE_TRIGGERS_REQUEST,
        GET_TRIGGER_TYPES_REQUEST,
        GENERATE_BLOCKLY_JS_REQUEST,
        GET_FLOOR_INFO_REQUEST,
        START_EDITOR_SESSION_REQUEST,
        STOP_EDITOR_SESSION_REQUEST,
        
        // Spigot -> Proxy
        LOAD_TRIGGERS_RESPONSE,
        SAVE_TRIGGERS_RESPONSE,
        GET_TRIGGER_TYPES_RESPONSE,
        GENERATE_BLOCKLY_JS_RESPONSE,
        GET_FLOOR_INFO_RESPONSE,
        START_EDITOR_SESSION_RESPONSE,
        STOP_EDITOR_SESSION_RESPONSE,
        
        // Erreurs
        ERROR_RESPONSE
    }

    /**
     * Données pour la demande de démarrage d'une session d'édition
     */
    @Data
    public static class StartEditorSessionData {
        private final String dungeonName;
        private final String floorId;
        private final UUID editorUuid;
        private final String editorName;
    }

    /**
     * Données pour la réponse de démarrage d'une session d'édition
     */
    @Data
    public static class StartEditorSessionResponse {
        private final boolean success;
        private final String sessionId;
        private final String message;
    }

    /**
     * Données pour la demande de chargement des triggers
     */
    @Data
    public static class LoadTriggersData {
        private final String dungeonName;
        private final String floorId;
    }

    /**
     * Données pour la sauvegarde des triggers
     */
    @Data
    public static class SaveTriggersData {
        private final String dungeonName;
        private final String floorId;
        private final String triggersJson;
        private final UUID editorUuid;
    }

    /**
     * Données pour la génération du JavaScript Blockly
     */
    @Data
    public static class GenerateBlocklyJsData {
        private final UUID editorUuid;
    }

    /**
     * Données pour les informations du floor
     */
    @Data
    public static class FloorInfoData {
        private final String dungeonName;
        private final String floorId;
        private final String editorName;
    }

    /**
     * Réponse d'erreur
     */
    @Data
    public static class ErrorResponse {
        private final String error;
        private final String details;
    }

    /**
     * Crée un nouveau message de requête
     */
    public static <T> WebEditorMessage<T> createRequest(String spigotServer, WebEditorMessageType type, T data) {
        return new WebEditorMessage<>(
                UUID.randomUUID().toString(),
                spigotServer,
                type,
                data,
                Instant.now()
        );
    }

    /**
     * Crée un message de réponse avec le même requestId
     */
    public static <T> WebEditorMessage<T> createResponse(String requestId, String spigotServer, WebEditorMessageType type, T data) {
        return new WebEditorMessage<>(
                requestId,
                spigotServer,
                type,
                data,
                Instant.now()
        );
    }
}