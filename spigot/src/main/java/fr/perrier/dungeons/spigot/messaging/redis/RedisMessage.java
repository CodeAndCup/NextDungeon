package fr.perrier.dungeons.spigot.messaging.redis;

import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data
public class RedisMessage<T> {
    private final String channel;
    private final String sender;
    private final UUID messageId;
    private final Instant timestamp;
    private final MessageType type;
    private final T data;

    public enum MessageType {
        FLOOR_UPDATE,
        INSTANCE_UPDATE,
        INSTANCE_REMOVE,
        PLAYER_JOIN_DUNGEON,
        PLAYER_LEAVE_DUNGEON,
        DUNGEON_STATE_UPDATE,
        // Web Editor messages
        WEB_EDITOR_REQUEST,
        WEB_EDITOR_RESPONSE
    }

    /**
     * Creates a new Redis message.
     *
     * @param channel the channel to which the message should be sent
     * @param sender the name of the sender of the message
     * @param type the type of the message
     * @param data the content of the message
     * @return the created message
     */
    public static <T> RedisMessage<T> create(String channel, String sender, MessageType type, T data) {
        return new RedisMessage<>(
                channel,
                sender,
                UUID.randomUUID(),
                Instant.now(),
                type,
                data
        );
    }
}
