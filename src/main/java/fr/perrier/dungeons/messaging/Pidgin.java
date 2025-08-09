package fr.perrier.dungeons.messaging;

import com.google.gson.Gson;
import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.messaging.pidgin.IncomingPacketHandler;
import fr.perrier.dungeons.messaging.pidgin.Packet;
import fr.perrier.dungeons.messaging.pidgin.PacketListener;
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

    public Pidgin(String topic) {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://"
                + Main.getInstance().getConfig().getString("RedisConfiguration.host")
                + ":"
                + Main.getInstance().getConfig().getInt("RedisConfiguration.port"))
                .setPassword(Main.getInstance().getConfig().getString("RedisConfiguration.password"));

        this.client = Redisson.create(config);
        this.topic = this.client.getTopic(topic);

        this.adapters = new HashMap<>();
        this.types = new HashMap<>();
        this.cTypes = new HashMap<>();
        this.topic.addListener(String.class, new MessagingListener());

        //this.registerAdapater(Packet.class, new PacketSubscriber());
    }

    public void registerAdapter(Class<? extends Packet> clazz, PacketListener listener) {
        this.adapters.put(clazz, listener);
        String uuid = clazz.getSimpleName();
        this.types.put(clazz, uuid);
        this.cTypes.put(uuid, clazz);
    }

    public void sendPacket(Packet packet) {
        executorService.submit(() ->
                this.topic.publish(types.get(packet.getClass()) + ";" + gson.toJson(packet))
        );
    }

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

                    for (Method m : listener.getClass().getDeclaredMethods()) {
                        if (m.getDeclaredAnnotation(IncomingPacketHandler.class) != null) {
                            try {
                                m.invoke(listener, packet);
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            });
        }
    }
}
