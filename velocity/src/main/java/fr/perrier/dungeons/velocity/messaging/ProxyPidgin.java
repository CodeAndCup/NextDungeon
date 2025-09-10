package fr.perrier.dungeons.velocity.messaging;

import com.google.gson.Gson;
import fr.perrier.dungeons.velocity.NextDungeonVelocity;
import fr.perrier.dungeons.velocity.messaging.packets.webeditor.WebEditorRequestPacket;
import fr.perrier.dungeons.velocity.messaging.packets.webeditor.WebEditorResponsePacket;
import fr.perrier.dungeons.velocity.messaging.pidgin.IncomingPacketHandler;
import fr.perrier.dungeons.velocity.messaging.pidgin.Packet;
import fr.perrier.dungeons.velocity.messaging.pidgin.PacketListener;
import fr.perrier.dungeons.velocity.messaging.subscribers.WebEditorResponseSubscriber;
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
public class ProxyPidgin {

    private final RedissonClient client;
    private final RTopic topic;

    private final Gson gson = new Gson();
    private final HashMap<Class<? extends Packet>, PacketListener> adapters;
    private final HashMap<Class<? extends Packet>, String> types;
    private final HashMap<String, Class<? extends Packet>> cTypes;

    private static final ExecutorService executorService = Executors.newFixedThreadPool(10);

    public ProxyPidgin(String topicName, String redisHost, int redisPort, String redisUsername, String redisPassword) {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://" + redisHost + ":" + redisPort)
                .setUsername(redisUsername)
                .setPassword(redisPassword);

        this.client = Redisson.create(config);
        this.topic = this.client.getTopic(topicName);

        this.adapters = new HashMap<>();
        this.types = new HashMap<>();
        this.cTypes = new HashMap<>();
        this.topic.addListener(String.class, new MessagingListener());

        // Enregistrer le subscriber pour les réponses web editor
        this.registerAdapter(WebEditorRequestPacket.class, null);
        this.registerAdapter(WebEditorResponsePacket.class, new WebEditorResponseSubscriber());
    }

    /**
     * Registers a packet adapter.
     */
    public void registerAdapter(Class<? extends Packet> clazz, PacketListener listener) {
        this.adapters.put(clazz, listener);
        String uuid = clazz.getSimpleName();
        this.types.put(clazz, uuid);
        this.cTypes.put(uuid, clazz);
    }

    /**
     * Publishes the given packet to the messaging topic.
     */
    public void sendPacket(Packet packet) {
        executorService.submit(() ->
                this.topic.publish(types.get(packet.getClass()) + ";" + gson.toJson(packet))
        );
    }

    /**
     * Shuts down the executor service.
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
                    if (listener == null) return;

                    for (Method m : listener.getClass().getDeclaredMethods()) {
                        if (m.getDeclaredAnnotation(IncomingPacketHandler.class) != null) {
                            try {
                                m.invoke(listener, packet);
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                NextDungeonVelocity.getInstance().getLogger().error("Erreur invocation packet handler: " + e.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    NextDungeonVelocity.getInstance().getLogger().error("Erreur traitement message Redis: " + e.getMessage());
                }
            });
        }
    }
}