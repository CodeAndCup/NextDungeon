package fr.perrier.dungeons.spigot.instance.impl;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.instance.InstanceInfo;
import fr.perrier.dungeons.spigot.instance.InstanceProvider;
import fr.perrier.dungeons.spigot.instance.ProviderType;
import fr.perrier.dungeons.spigot.model.Floor;
import fr.perrier.dungeons.spigot.model.FloorInstance;
import lombok.NonNull;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Implémentation du provider utilisant le système de mondes vanilla de Minecraft.
 * Optimisé pour gérer plusieurs instances sur le même serveur.
 * Utilise le système de fichiers pour stocker et cloner les mondes.
 */
public class VanillaProvider implements InstanceProvider {

    // Répertoire de base pour les templates
    private static final String TEMPLATES_DIR = "dungeon_templates";

    // Répertoire de base pour les instances actives
    private static final String INSTANCES_DIR = "dungeon_instances";

    // Stockage des métadonnées des instances
    private final Map<UUID, InstanceInfo> instanceMetadata = new HashMap<>();
    private InstanceInfo currentInstanceInfo = null;

    @Override
    public CompletableFuture<Boolean> initialize() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        try {
            // Créer les répertoires nécessaires
            File templatesDir = new File(Main.getInstance().getDataFolder().getParentFile().getParentFile(), TEMPLATES_DIR);
            File instancesDir = new File(Main.getInstance().getDataFolder().getParentFile().getParentFile(), INSTANCES_DIR);

            if (!templatesDir.exists()) {
                templatesDir.mkdirs();
                Main.getInstance().getLogger().info("Répertoire des templates créé: " + templatesDir.getAbsolutePath());
            }

            if (!instancesDir.exists()) {
                instancesDir.mkdirs();
                Main.getInstance().getLogger().info("Répertoire des instances créé: " + instancesDir.getAbsolutePath());
            }

            Main.getInstance().getLogger().info("Vanilla provider initialisé avec succès");
            future.complete(true);
        } catch (Exception e) {
            Main.getInstance().getLogger().severe("Erreur lors de l'initialisation du Vanilla provider: " + e.getMessage());
            future.complete(false);
        }

        return future;
    }

    @Override
    public CompletableFuture<UUID> createInstance(FloorInstance floorInstance, boolean editMode) {
        CompletableFuture<UUID> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try {
                Floor floor = floorInstance.getFloor();
                UUID instanceId = UUID.randomUUID();
                String worldName = generateWorldName(floor.getId(), instanceId);

                // Copier le template vers le répertoire d'instance
                File templateDir = getTemplateDirectory(floor.getId());
                File instanceDir = getInstanceDirectory(worldName);

                if (!templateDir.exists()) {
                    Main.getInstance().getLogger().severe("Template " + floor.getId() + " introuvable !");
                    future.complete(null);
                    return;
                }

                // Copier les fichiers du template
                Main.getInstance().getLogger().info("Copie du template " + floor.getId() + " vers " + worldName + "...");
                FileUtils.copyDirectory(templateDir, instanceDir);

                // Charger le monde sur le thread principal
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    try {
                        WorldCreator creator = new WorldCreator(worldName);
                        World world = creator.createWorld();

                        if (world == null) {
                            Main.getInstance().getLogger().severe("Impossible de créer le monde " + worldName);
                            future.complete(null);
                            return;
                        }

                        // Configurer le monde pour les donjons
                        world.setAutoSave(false); // Pas de sauvegarde automatique pour optimiser
                        world.setKeepSpawnInMemory(true);

                        // Stocker les métadonnées
                        InstanceInfo info = new InstanceInfo(
                                instanceId,
                                floor.getId(),
                                Instant.now().toString(),
                                editMode,
                                true
                        );
                        instanceMetadata.put(instanceId, info);

                        Main.getInstance().getLogger().info("Instance Vanilla créée: " + worldName + " (editMode=" + editMode + ")");
                        future.complete(instanceId);
                    } catch (Exception e) {
                        Main.getInstance().getLogger().severe("Erreur lors du chargement du monde: " + e.getMessage());
                        future.complete(null);
                    }
                });

            } catch (IOException e) {
                Main.getInstance().getLogger().severe("Erreur lors de la copie du template: " + e.getMessage());
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
                }

                // Supprimer les fichiers du monde de manière asynchrone
                Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
                    try {
                        File instanceDir = getInstanceDirectory(worldName);
                        if (instanceDir.exists()) {
                            FileUtils.deleteDirectory(instanceDir);
                            Main.getInstance().getLogger().info("Fichiers de l'instance " + instanceId + " supprimés");
                        }
                    } catch (IOException e) {
                        Main.getInstance().getLogger().warning("Impossible de supprimer les fichiers de l'instance: " + e.getMessage());
                    }
                });

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
        // En mode Vanilla, toutes les instances sont sur le même serveur
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
        File templateDir = getTemplateDirectory(floor.getId());
        return templateDir.exists() && templateDir.isDirectory();
    }

    @Override
    public CompletableFuture<Boolean> createTemplate(@NonNull Floor floor) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try {
                String templateName = floor.getId();
                File templateDir = getTemplateDirectory(templateName);

                // Vérifier si un template global existe
                File globalTemplateDir = getTemplateDirectory("Global-Dungeon");

                if (globalTemplateDir.exists()) {
                    // Copier le template global
                    Main.getInstance().getLogger().info("Copie du template global vers " + templateName + "...");
                    FileUtils.copyDirectory(globalTemplateDir, templateDir);
                    Main.getInstance().getLogger().info("Template Vanilla créé pour " + floor.getId());
                    future.complete(true);
                } else {
                    // Créer un répertoire vide (un monde devra être créé manuellement)
                    templateDir.mkdirs();
                    Main.getInstance().getLogger().warning("Template global introuvable. Répertoire créé: " + templateDir.getAbsolutePath());
                    Main.getInstance().getLogger().warning("Vous devez créer un monde manuellement ou copier des fichiers de monde existants.");
                    future.complete(true);
                }
            } catch (IOException e) {
                Main.getInstance().getLogger().severe("Erreur lors de la création du template: " + e.getMessage());
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
        return ProviderType.VANILLA;
    }

    @Override
    public CompletableFuture<Boolean> saveEditWorldToTemplate(Floor floor) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try {
                Main.getInstance().getLogger().info("Sauvegarde du monde d'édition pour " + floor.getId() + " (Vanilla)...");

                // Sauvegarder le monde en cours
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    World world = Bukkit.getWorld("world");
                    if (world != null) {
                        world.save();
                    }
                });

                // Attendre que la sauvegarde soit terminée
                Thread.sleep(1000);

                // Chemins pour Vanilla
                File worldSource = new File(Main.getInstance().getDataFolder().getParentFile().getParentFile(), "world");
                File templateDest = getTemplateDirectory(floor.getId());

                // Créer le répertoire de destination si nécessaire
                templateDest.mkdirs();

                // Copier les fichiers du monde vers le template
                Main.getInstance().getLogger().info("Copie des fichiers du monde vers " + templateDest.getAbsolutePath());

                if (new File(worldSource, "data").exists()) {
                    FileUtils.copyDirectory(new File(worldSource, "data"), new File(templateDest, "data"));
                }
                if (new File(worldSource, "entities").exists()) {
                    FileUtils.copyDirectory(new File(worldSource, "entities"), new File(templateDest, "entities"));
                }
                if (new File(worldSource, "region").exists()) {
                    FileUtils.copyDirectory(new File(worldSource, "region"), new File(templateDest, "region"));
                }
                if (new File(worldSource, "uid.dat").exists()) {
                    FileUtils.copyFile(new File(worldSource, "uid.dat"), new File(templateDest, "uid.dat"));
                }
                if (new File(worldSource, "level.dat").exists()) {
                    FileUtils.copyFile(new File(worldSource, "level.dat"), new File(templateDest, "level.dat"));
                }

                Main.getInstance().getLogger().info("Monde sauvegardé dans le template Vanilla pour " + floor.getId());
                future.complete(true);
            } catch (Exception e) {
                Main.getInstance().getLogger().severe("Erreur lors de la sauvegarde Vanilla: " + e.getMessage());
                e.printStackTrace();
                future.complete(false);
            }
        });

        return future;
    }

    @Override
    public void shutdown() {
        Main.getInstance().getLogger().info("Arrêt du Vanilla provider");

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
     * Récupère le répertoire d'un template.
     */
    private File getTemplateDirectory(String templateName) {
        File serverDir = Main.getInstance().getDataFolder().getParentFile().getParentFile();
        return new File(serverDir, TEMPLATES_DIR + File.separator + templateName);
    }

    /**
     * Récupère le répertoire d'une instance.
     */
    private File getInstanceDirectory(String worldName) {
        File serverDir = Main.getInstance().getDataFolder().getParentFile().getParentFile();
        return new File(serverDir, worldName);
    }

    /**
     * Définit les informations de l'instance actuelle (pour le mode instance).
     */
    public void setCurrentInstanceInfo(InstanceInfo info) {
        this.currentInstanceInfo = info;
    }
}
