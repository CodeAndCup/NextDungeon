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
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Getter
public class Pidgin {

    private final RedissonClient client;
    private final RTopic topic;

    private final Gson gson = new Gson();
    private final HashMap<Class<? extends Packet>, PacketListener> adapters;
    private final HashMap<Class<? extends Packet>, String> types;
    private final HashMap<String, Class<? extends Packet>> cTypes;

    private static final ExecutorService executorService = Executors.newFixedThreadPool(10);

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
        this.topic.addListener(String.class, new MessagingListener());

    }

    public Pidgin(String topicName, Config config) {
        this.client = Redisson.create(config);
        this.topic = this.client.getTopic(topicName + ":packets");

        this.adapters = new HashMap<>();
        this.types = new HashMap<>();
        this.cTypes = new HashMap<>();
        this.topic.addListener(String.class, new MessagingListener());
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
        executorService.submit(() ->
                this.topic.publish(types.get(packet.getClass()) + ";" + gson.toJson(packet))
        );
    }

    /**
     * Shuts down the Pidgin executor service.
     * <p>
     * This method is used to shut down the messaging service.
     * It is called automatically when the plugin is disabled.
     * <p>
     * The method will wait 60 seconds for any active tasks to complete.
     * If any tasks are still active after that time, they will be interrupted.
     */
    public static void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
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
            executorService.submit(() -> {
                try {
                    String id = s.split(";")[0];
                    Packet packet = gson.fromJson(s.split(";")[1], cTypes.get(id));

                    Class<? extends Packet> clazz = null;
                    for (Map.Entry<Class<? extends Packet>, String> entry : types.entrySet()) {
                        Class<? extends Packet> aClass = entry.getKey();
                        String s1 = entry.getValue();
                        if (s1.equalsIgnoreCase(id)) clazz = aClass;
                    }

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
        }
    }
}
