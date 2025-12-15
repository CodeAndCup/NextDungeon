package fr.perrier.dungeons.spigot.instance.impl;

import com.infernalsuite.asp.api.AdvancedSlimePaperAPI;
import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.api.world.properties.SlimeProperties;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import com.infernalsuite.asp.loaders.file.FileLoader;
import com.infernalsuite.asp.loaders.mongo.MongoLoader;
import com.infernalsuite.asp.loaders.mysql.MysqlLoader;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.instance.InstanceInfo;
import fr.perrier.dungeons.spigot.instance.InstanceProvider;
import fr.perrier.dungeons.spigot.instance.ProviderType;
import fr.perrier.dungeons.spigot.model.Floor;
import fr.perrier.dungeons.spigot.model.FloorInstance;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.File;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Implémentation du provider utilisant Advanced Slime World Manager (ASP).
 * Utilise des mondes "slime" optimisés pour des instances légères et rapides.
 * Supporte plusieurs types de loaders : FILE, MYSQL, MONGODB
 */
public class ASPProvider implements InstanceProvider {

    private AdvancedSlimePaperAPI aspPlugin;
    private SlimeLoader slimeLoader;
    private String loaderType;

    // Stockage des métadonnées des instances
    private final Map<UUID, InstanceInfo> instanceMetadata = new HashMap<>();
    private InstanceInfo currentInstanceInfo = null;

    @Override
    public CompletableFuture<Boolean> initialize() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        try {
            aspPlugin = AdvancedSlimePaperAPI.instance();

            if (aspPlugin == null) {
                Main.getInstance().getLogger().severe("AdvancedSlimePaper n'est pas chargé !");
                future.complete(false);
                return future;
            }

            // Récupérer le type de loader depuis la configuration
            loaderType = Main.getInstance().getConfig().getString("InstanceProvider.ASP.loaderType", "FILE").toUpperCase();

            // Créer le loader approprié
            slimeLoader = switch (loaderType) {
                case "MYSQL" -> new MysqlLoader(
                        Main.getInstance().getConfig().getString("InstanceProvider.ASP.mysql.url", "jdbc:mysql://localhost:3306/slimeworlds"),
                        Main.getInstance().getConfig().getString("InstanceProvider.ASP.mysql.host", "localhost"),
                        Main.getInstance().getConfig().getInt("InstanceProvider.ASP.mysql.port", 3306),
                        Main.getInstance().getConfig().getString("InstanceProvider.ASP.mysql.database", "slimeworlds"),
                        Main.getInstance().getConfig().getBoolean("InstanceProvider.ASP.mysql.useSSL", false),
                        Main.getInstance().getConfig().getString("InstanceProvider.ASP.mysql.username", "root"),
                        Main.getInstance().getConfig().getString("InstanceProvider.ASP.mysql.password", "")
                );
                case "MONGODB" -> new MongoLoader(
                        Main.getInstance().getConfig().getString("InstanceProvider.ASP.mongodb.database", "slimeworlds"),
                        Main.getInstance().getConfig().getString("InstanceProvider.ASP.mongodb.collection", "worlds"),
                        Main.getInstance().getConfig().getString("InstanceProvider.ASP.mongodb.username", ""),
                        Main.getInstance().getConfig().getString("InstanceProvider.ASP.mongodb.password", ""),
                        Main.getInstance().getConfig().getString("InstanceProvider.ASP.mongodb.authSource", ""),
                        Main.getInstance().getConfig().getString("InstanceProvider.ASP.mongodb.host", "localhost"),
                        Main.getInstance().getConfig().getInt("InstanceProvider.ASP.mongodb.port", 27017),
                        Main.getInstance().getConfig().getString("InstanceProvider.ASP.mongodb.uri", "")
                );
                case "FILE" -> {
                    File worldsDir = new File(Main.getInstance().getDataFolder(), "slimeworlds");
                    if (!worldsDir.exists()) {
                        worldsDir.mkdirs();
                    }
                    yield new FileLoader(worldsDir);
                }
                default -> null;
            };

            if (slimeLoader == null) {
                Main.getInstance().getLogger().severe("Impossible de créer le SlimeLoader de type: " + loaderType);
                future.complete(false);
                return future;
            }

            Main.getInstance().getLogger().info("ASP provider initialisé avec succès (Loader: " + loaderType + ")");
            future.complete(true);
        } catch (Exception e) {
            Main.getInstance().getLogger().severe("Erreur lors de l'initialisation de ASP: " + e.getMessage());
            e.printStackTrace();
            future.complete(false);
        }

        return future;
    }

    @Override
    public CompletableFuture<UUID> createInstance(Floor floor, boolean editMode) {
        CompletableFuture<UUID> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try {
                UUID instanceId = UUID.randomUUID();
                String worldName = generateWorldName(floor.getId(), instanceId);

                // Créer les propriétés du monde
                SlimePropertyMap properties = new SlimePropertyMap();
                properties.setValue(SlimeProperties.DIFFICULTY, "normal");
                properties.setValue(SlimeProperties.SPAWN_X, 0);
                properties.setValue(SlimeProperties.SPAWN_Y, 64);
                properties.setValue(SlimeProperties.SPAWN_Z, 0);

                // Charger ou créer le monde slime
                SlimeWorld slimeWorld;
                String templateName = floor.getId();

                if (slimeLoader.worldExists(templateName)) {
                    // Cloner depuis le template
                    slimeWorld = aspPlugin.readWorld(slimeLoader, templateName, true, properties);
                } else {
                    Main.getInstance().getLogger().warning("Template " + templateName + " introuvable, création d'un monde vide");
                    slimeWorld = aspPlugin.createEmptyWorld(worldName, false, properties, slimeLoader);
                }

                // Générer le monde sur le serveur principal
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    try {
                        aspPlugin.loadWorld(slimeWorld,false);

                        // Stocker les métadonnées
                        InstanceInfo info = new InstanceInfo(
                                instanceId,
                                floor.getId(),
                                Instant.now().toString(),
                                editMode,
                                true
                        );
                        instanceMetadata.put(instanceId, info);

                        Main.getInstance().getLogger().info("Instance ASP créée: " + worldName + " (editMode=" + editMode + ")");
                        future.complete(instanceId);
                    } catch (Exception e) {
                        Main.getInstance().getLogger().severe("Erreur lors de la génération du monde: " + e.getMessage());
                        future.complete(null);
                    }
                });

            } catch (Exception e) {
                Main.getInstance().getLogger().severe("Erreur lors de la création de l'instance ASP: " + e.getMessage());
                e.printStackTrace();
                future.complete(null);
            }
        });

        return future;
    }

    @Override
    public CompletableFuture<Boolean> deleteInstance(UUID instanceId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            try {
                InstanceInfo info = instanceMetadata.get(instanceId);
                if (info == null) {
                    future.complete(false);
                    return;
                }

                String worldName = generateWorldName(info.getFloorId(), instanceId);
                World world = Bukkit.getWorld(worldName);

                if (world != null) {
                    // Téléporter les joueurs restants au spawn
                    world.getPlayers().forEach(player ->
                        player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation())
                    );

                    // Décharger le monde
                    Bukkit.unloadWorld(world, false);
                    Main.getInstance().getLogger().info("Instance ASP " + instanceId + " déchargée");
                }

                instanceMetadata.remove(instanceId);
                future.complete(true);
            } catch (Exception e) {
                Main.getInstance().getLogger().severe("Erreur lors de la suppression de l'instance: " + e.getMessage());
                future.complete(false);
            }
        });

        return future;
    }

    @Override
    public boolean isInstanceServer() {
        // En mode ASP, toutes les instances sont sur le même serveur
        // On vérifie si on a des métadonnées d'instance actuelle
        return currentInstanceInfo != null || !instanceMetadata.isEmpty();
    }

    @Override
    public boolean isEditMode() {
        return currentInstanceInfo != null && currentInstanceInfo.isEditMode();
    }

    @Override
    public InstanceInfo getCurrentInstanceInfo() {
        return currentInstanceInfo;
    }

    @Override
    public InstanceInfo getInstanceInfo(UUID instanceId) {
        // Vérifier d'abord en mémoire
        InstanceInfo info = instanceMetadata.get(instanceId);
        if (info != null) {
            return info;
        }

        // Sinon, essayer de récupérer depuis Redis
        try {
            FloorInstance instance = Main.getInstance().getRedisStorageService().getInstance(instanceId);
            if (instance == null) {
                return null;
            }

            return new InstanceInfo(
                    instance.getInstanceId(),
                    instance.getFloorId(),
                    "Unknown",
                    false,
                    instance.isReady()
            );
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean templateExists(@NonNull Floor floor) {
        try {
            return slimeLoader.worldExists(floor.getId());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public CompletableFuture<Boolean> createTemplate(@NonNull Floor floor) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try {
                String templateName = floor.getId();

                // Vérifier si un monde source existe (Global-Dungeon)
                if (slimeLoader.worldExists("Global-Dungeon")) {
                    // Charger le monde source
                    SlimePropertyMap properties = new SlimePropertyMap();
                    SlimeWorld sourceWorld = aspPlugin.readWorld(slimeLoader, "Global-Dungeon", true, properties);
                    SlimeWorld newWorld = sourceWorld.clone(templateName);
                    // Sauvegarder une copie avec le nouveau nom
                    aspPlugin.saveWorld(newWorld);
                    Main.getInstance().getLogger().info("Template ASP créé pour " + floor.getId());
                    future.complete(true);
                } else {
                    // Créer un monde vide comme template
                    SlimePropertyMap properties = new SlimePropertyMap();
                    SlimeWorld emptyWorld = aspPlugin.createEmptyWorld(templateName, false, properties,slimeLoader);
                    aspPlugin.saveWorld(emptyWorld);
                    Main.getInstance().getLogger().info("Template ASP vide créé pour " + floor.getId());
                    future.complete(true);
                }
            } catch (Exception e) {
                Main.getInstance().getLogger().severe("Erreur lors de la création du template ASP: " + e.getMessage());
                e.printStackTrace();
                future.complete(false);
            }
        });

        return future;
    }

    @Override
    public CompletableFuture<Boolean> sendPlayerToInstance(Player player, UUID instanceId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            try {
                InstanceInfo info = instanceMetadata.get(instanceId);
                if (info == null) {
                    future.complete(false);
                    return;
                }

                String worldName = generateWorldName(info.getFloorId(), instanceId);
                World world = Bukkit.getWorld(worldName);

                if (world == null) {
                    Main.getInstance().getLogger().warning("Monde " + worldName + " introuvable");
                    future.complete(false);
                    return;
                }

                player.teleport(world.getSpawnLocation());
                future.complete(true);
            } catch (Exception e) {
                Main.getInstance().getLogger().severe("Erreur lors de la téléportation: " + e.getMessage());
                future.complete(false);
            }
        });

        return future;
    }

    @Override
    public ProviderType getType() {
        return ProviderType.ASP;
    }

    @Override
    public CompletableFuture<Boolean> saveEditWorldToTemplate(Floor floor) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            try {
                Main.getInstance().getLogger().info("Sauvegarde du monde d'édition pour " + floor.getId() + " (ASP)...");

                // Avec ASP, le monde est stocké dans le loader (FILE, MySQL ou MongoDB)
                // Il faut simplement sauvegarder le monde actuel vers le template
                String templateName = floor.getId();

                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    try {
                        World world = Bukkit.getWorld("world");
                        if (world != null) {
                            // Sauvegarder le monde actuel
                            world.save();

                            // Récupérer le SlimeWorld
                            SlimeWorld slimeWorld = aspPlugin.readWorld(slimeLoader,world.getName(),false,new SlimePropertyMap());
                            if (slimeWorld != null) {
                                // Cloner vers le template
                                SlimeWorld templateWorld = slimeWorld.clone(templateName);

                                // Sauvegarder dans le loader
                                aspPlugin.saveWorld(templateWorld);

                                Main.getInstance().getLogger().info("Monde sauvegardé dans le template ASP pour " + floor.getId() + " (Loader: " + loaderType + ")");
                                future.complete(true);
                            } else {
                                Main.getInstance().getLogger().warning("Impossible de récupérer le SlimeWorld pour 'world'");

                                // Fallback : créer un nouveau monde depuis le monde actuel
                                SlimePropertyMap properties = new SlimePropertyMap();
                                SlimeWorld newWorld = aspPlugin.readWorld(slimeLoader, "world", true, properties);
                                SlimeWorld clonedWorld = newWorld.clone(templateName);
                                aspPlugin.saveWorld(clonedWorld);

                                Main.getInstance().getLogger().info("Monde sauvegardé via fallback dans le template ASP pour " + floor.getId());
                                future.complete(true);
                            }
                        } else {
                            Main.getInstance().getLogger().severe("Monde 'world' introuvable");
                            future.complete(false);
                        }
                    } catch (Exception e) {
                        Main.getInstance().getLogger().severe("Erreur lors de la sauvegarde ASP: " + e.getMessage());
                        e.printStackTrace();
                        future.complete(false);
                    }
                });
            } catch (Exception e) {
                Main.getInstance().getLogger().severe("Erreur lors de la sauvegarde ASP: " + e.getMessage());
                e.printStackTrace();
                future.complete(false);
            }
        });

        return future;
    }

    @Override
    public void shutdown() {
        Main.getInstance().getLogger().info("Arrêt du ASP provider");

        // Décharger tous les mondes d'instance
        for (UUID instanceId : instanceMetadata.keySet()) {
            deleteInstance(instanceId);
        }

        instanceMetadata.clear();
    }

    /**
     * Génère un nom de monde unique pour une instance.
     */
    private String generateWorldName(String floorId, UUID instanceId) {
        return "dungeon_" + floorId + "_" + instanceId.toString().substring(0, 8);
    }

    /**
     * Définit les informations de l'instance actuelle (pour le mode instance).
     */
    public void setCurrentInstanceInfo(InstanceInfo info) {
        this.currentInstanceInfo = info;
    }
}
