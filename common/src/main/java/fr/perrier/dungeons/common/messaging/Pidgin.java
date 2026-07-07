package fr.perrier.dungeons.common.messaging;

import com.google.gson.Gson;
import fr.perrier.dungeons.common.messaging.pidgin.IncomingPacketHandler;
import fr.perrier.dungeons.common.messaging.pidgin.Packet;
import fr.perrier.dungeons.common.messaging.pidgin.PacketListener;
import lombok.Getter;
import org.redisson.Redisson;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.api.listener.MessageListener;
import org.redisson.config.Config;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

@Getter
public class Pidgin {

    private final RedissonClient client;
    private final RTopic topic;

    private final Gson gson = new Gson();
    private final HashMap<Class<? extends Packet>, PacketListener> adapters;
    private final HashMap<Class<? extends Packet>, String> types;
    private final HashMap<String, Class<? extends Packet>> cTypes;

    /**
     * Per-instance worker pool. It used to be {@code static}, which meant that once a plugin
     * disable shut it down it could never be used again — a reload (e.g. via PlugManX) would
     * keep the old, now-terminated pool and every incoming message rejected into it. Making it
     * an instance field lets a freshly created Pidgin start with a live pool.
     */
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    /** Redis listener id, kept so {@link #close()} can unsubscribe on shutdown. */
    private final int listenerId;

    public Pidgin(String topicName, String redisHost, int redisPort, String redisUsername, String redisPassword, int redisDatabase) {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://" + redisHost + ":" + redisPort)
                .setUsername(redisUsername)
                .setPassword(redisPassword)
                .setDatabase(redisDatabase);

        this.client = Redisson.create(config);
        this.topic = this.client.getTopic(topicName + ":packets");

        this.adapters = new HashMap<>();
        this.types = new HashMap<>();
        this.cTypes = new HashMap<>();
        this.listenerId = this.topic.addListener(String.class, new MessagingListener());
    }

    public Pidgin(String topicName, Config config) {
        this.client = Redisson.create(config);
        this.topic = this.client.getTopic(topicName + ":packets");

        this.adapters = new HashMap<>();
        this.types = new HashMap<>();
        this.cTypes = new HashMap<>();
        this.listenerId = this.topic.addListener(String.class, new MessagingListener());
    }

    /**
     * Registers a packet adapter.
     * <p>
     * This method will link the given packet class to the given packet listener.
     * When a packet is received and the packet's class matches the given packet
     * class, the packet listener will be called with the packet as the argument.
     *
     * @param clazz the packet class to register
     * @param listener the packet listener to register
     */
    public void registerAdapter(Class<? extends Packet> clazz, PacketListener listener) {
        this.adapters.put(clazz, listener);
        String uuid = clazz.getSimpleName();
        this.types.put(clazz, uuid);
        this.cTypes.put(uuid, clazz);
    }

    /**
     * Publishes the given packet to the messaging topic.
     * <p>
     * The packet is serialized to JSON and sent to the messaging topic.
     * The packet's class name is prepended to the JSON string as a unique
     * identifier for the packet type.
     * <p>
     * The call is asynchronous, meaning that the method will return immediately
     * and the packet will be sent in a separate thread.
     *
     * @param packet the packet to send
     */
    public void sendPacket(Packet packet) {
        if (executorService.isShutdown()) return;
        try {
            executorService.submit(() ->
                    this.topic.publish(types.get(packet.getClass()) + ";" + gson.toJson(packet))
            );
        } catch (RejectedExecutionException ignored) {
            // Pool was shut down concurrently (plugin disabling) — safe to drop the packet.
        }
    }

    /**
     * Shuts this Pidgin instance down cleanly: stops receiving messages, drains the worker
     * pool, and closes the Redis connection.
     * <p>
     * Called on plugin disable. Unsubscribing the listener and closing the client is what
     * makes a reload safe — otherwise the old instance's listener keeps receiving messages
     * and rejects them into the terminated pool, and each reload leaks a Redis connection.
     */
    public void close() {
        try {
            this.topic.removeListener(listenerId);
        } catch (Exception ignored) {
        }
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        try {
            this.client.shutdown();
        } catch (Exception ignored) {
        }
    }

    private class MessagingListener implements MessageListener<String> {
        /**
         * Handles incoming messages from the Redis topic.
         * <p>
         * This method is triggered when a message is received. It extracts the packet ID
         * and deserializes the packet data. It then identifies the appropriate packet class
         * and listener, and invokes methods annotated with {@link IncomingPacketHandler} on
         * the listener, passing the packet as an argument.
         * </p>
         *
         * @param charSequence the topic name as a character sequence
         * @param s the message received, containing the packet ID and serialized packet data
         */
        @Override
        public void onMessage(CharSequence charSequence, String s) {
            if (executorService.isShutdown()) return;
            try {
                executorService.submit(() -> {
                    try {
                        String[] parts = s.split(";", 2);
                        String id = parts[0];
                        Packet packet = gson.fromJson(parts[1], cTypes.get(id));

                        Class<? extends Packet> clazz = cTypes.get(id);

                        PacketListener listener = adapters.get(clazz);

                        for (Method m : listener.getClass().getDeclaredMethods()) {
                            if (m.getDeclaredAnnotation(IncomingPacketHandler.class) != null) {
                                try {
                                    m.invoke(listener, packet);
                                } catch (IllegalAccessException | InvocationTargetException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    } catch (Exception ignored) {
                    }
                });
            } catch (RejectedExecutionException ignored) {
                // Pool shut down between the guard above and submit — safe to drop.
            }
        }
    }
}
