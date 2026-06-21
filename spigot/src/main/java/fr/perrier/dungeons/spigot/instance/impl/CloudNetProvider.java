package fr.perrier.dungeons.spigot.instance.impl;

import eu.cloudnetservice.driver.document.property.DocProperty;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.provider.CloudServiceFactory;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.service.*;
import eu.cloudnetservice.driver.template.TemplateStorage;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.wrapper.holder.ServiceInfoHolder;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.instance.InstanceInfo;
import fr.perrier.dungeons.spigot.instance.InstanceProvider;
import fr.perrier.dungeons.spigot.instance.ProviderType;
import fr.perrier.dungeons.spigot.model.Floor;
import fr.perrier.dungeons.spigot.model.FloorInstance;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Implémentation du provider utilisant CloudNet pour gérer les instances.
 * Cette classe encapsule toute la logique spécifique à CloudNet.
 */
public class CloudNetProvider implements InstanceProvider {

    private static final String TEMPLATE_PREFIX = "Dungeon";

    @Override
    public CompletableFuture<Boolean> initialize() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        try {
            // Vérifier que CloudNet est disponible
            InjectionLayer.boot();
            Main.getLoggerUtil().info("CloudNet provider initialized successfully.");
            future.complete(true);
        } catch (Exception e) {
            Main.getLoggerUtil().severe("An error occurred during the initialization phase: " + e.getMessage());
            future.complete(false);
        }

        return future;
    }

    @Override
    public CompletableFuture<UUID> createInstance(Floor floor, boolean editMode) {
        CompletableFuture<UUID> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try {
                String floorId = floor.getId();

                CloudServiceFactory cloudService = InjectionLayer.boot().instance(CloudServiceFactory.class);
                ServiceTaskProvider serviceTaskProvider = InjectionLayer.boot().instance(ServiceTaskProvider.class);
                ServiceTask serviceTask = serviceTaskProvider.serviceTask(floorId);

                if (serviceTask == null) {
                    Main.getLoggerUtil().info("No ServiceTask for " + floorId
                                              + " — generating world template on demand.");
                    boolean created;
                    try {
                        created = Boolean.TRUE.equals(createTemplate(floor).get());
                    } catch (Exception e) {
                        Main.getLoggerUtil().severe("On-demand template generation failed for "
                                                    + floorId + ": " + e.getMessage());
                        future.complete(null);
                        return;
                    }
                    serviceTask = serviceTaskProvider.serviceTask(floorId);
                    if (!created || serviceTask == null) {
                        Main.getLoggerUtil().severe("Cannot create task for " + floorId
                                                    + " (template provisioning failed)");
                        future.complete(null);
                        return;
                    }
                }

                ServiceConfiguration config = ServiceConfiguration.builder(serviceTask)
                        .writeProperty(DocProperty.property("editMode", boolean.class), editMode)
                        .writeProperty(DocProperty.property("isDungeonInstance", boolean.class), true)
                        .writeProperty(DocProperty.property("floorId", String.class), floor.getId())
                        .writeProperty(DocProperty.property("createdAt", String.class), Instant.now().toString())
                        .build();

                ServiceCreateResult service = cloudService.createCloudService(config);

                if (service.state() != ServiceCreateResult.State.CREATED) {
                    Main.getLoggerUtil().severe("Cannot create a service for " + floorId);
                    future.complete(null);
                    return;
                }

                service.serviceInfo().provider().startAsync();
                Main.getLoggerUtil().info("Service started for " + floorId + " (editMode=" + editMode + ")");

                future.complete(service.serviceInfo().serviceId().uniqueId());
            } catch (Exception e) {
                Main.getLoggerUtil().severe("An error occurred during the instance creation: " + e.getMessage());
                future.complete(null);
            }
        });

        return future;
    }

    @Override
    public CompletableFuture<Boolean> deleteInstance(UUID instanceId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try {
                CloudServiceProvider cloudServiceProvider = InjectionLayer.ext().instance(CloudServiceProvider.class);
                ServiceInfoSnapshot service = cloudServiceProvider.service(instanceId);

                if (service != null) {
                    service.provider().stop();
                    Main.getLoggerUtil().info("Instance " + instanceId + " stopped.");
                    future.complete(true);
                } else {
                    Main.getLoggerUtil().warning("Instance " + instanceId + " not found.");
                    future.complete(false);
                }
            } catch (Exception e) {
                Main.getLoggerUtil().severe("Error deleting instance: " + e.getMessage());
                future.complete(false);
            }
        });

        return future;
    }

    @Override
    public boolean isInstanceServer() {
        try {
            ServiceInfoSnapshot currentService = InjectionLayer.ext().instance(ServiceInfoHolder.class).serviceInfo();
            return currentService.readProperty(DocProperty.property("isDungeonInstance", boolean.class).withDefault(false));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isEditMode() {
        try {
            ServiceInfoSnapshot currentService = InjectionLayer.ext().instance(ServiceInfoHolder.class).serviceInfo();
            return currentService.readProperty(DocProperty.property("editMode", boolean.class).withDefault(false));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public InstanceInfo getCurrentInstanceInfo() {
        try {
            ServiceInfoSnapshot currentService = InjectionLayer.ext().instance(ServiceInfoHolder.class).serviceInfo();

            if (!isInstanceServer() && !isEditMode()) {
                return null;
            }

            String instanceId = currentService.serviceId().uniqueId().toString();
            String floorId = currentService.readProperty(DocProperty.property("floorId", String.class));
            String createdAt = currentService.readProperty(DocProperty.property("createdAt", String.class));

            if (instanceId == null || floorId == null) {
                return null;
            }

            return new InstanceInfo(
                    UUID.fromString(instanceId),
                    floorId,
                    createdAt,
                    isEditMode(),
                    true
            );
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public InstanceInfo getInstanceInfo(UUID instanceId) {
        try {
            FloorInstance instance = Main.getInstance().getDungeonService().getInstance(instanceId);
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
            ServiceTaskProvider serviceTaskProvider = InjectionLayer.boot().instance(ServiceTaskProvider.class);
            return serviceTaskProvider.serviceTask(floor.getId()) != null;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public CompletableFuture<Boolean> createTemplate(@NonNull Floor floor) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            Main.getLoggerUtil().info("Creating template for " + floor.getId() + " (CloudNet)...");

            new ServiceTemplate.Builder()
                    .prefix(TEMPLATE_PREFIX)
                    .name(floor.getId())
                    .storage("local")
                    .priority(0)
                    .alwaysCopyToStaticServices(false)
                    .build();

            Main.getLoggerUtil().info("Template created for " + floor.getId() + " (CloudNet). Adding ServiceTask...");

            ServiceTaskProvider serviceTaskProvider = InjectionLayer.boot().instance(ServiceTaskProvider.class);
            ServiceTask serviceTask = new ServiceTask.Builder()
                    .name(floor.getId())
                    .runtime("jvm")
                    .hostAddress(null)
                    .javaCommand("java")
                    .nameSplitter("-")
                    .disableIpRewrite(false)
                    .maintenance(false)
                    .autoDeleteOnStop(true)
                    .staticServices(false)
                    .associatedNodes(Collections.emptyList())
                    .deletedFilesAfterStop(Collections.emptyList())
                    .processConfiguration(
                            new ProcessConfiguration.Builder()
                                    .environment("MINECRAFT_SERVER")
                                    .maxHeapMemorySize(8192)
                                    .jvmOptions(Collections.emptyList())
                                    .processParameters(Collections.emptyList())
                                    .environmentVariables(Collections.emptyMap())
                    )
                    .startPort(31500)
                    .minServiceCount(0)
                    .templates(
                            Collections.singletonList(
                                    new ServiceTemplate.Builder()
                                            .prefix(TEMPLATE_PREFIX)
                                            .name(floor.getId())
                                            .storage("local")
                                            .priority(0)
                                            .alwaysCopyToStaticServices(false)
                                            .build()
                            )
                    )
                    .deployments(Collections.emptyList())
                    .inclusions(Collections.emptyList())
                    .build();

            serviceTaskProvider.addServiceTask(serviceTask);

            Main.getLoggerUtil().info("ServiceTask added for " + floor.getId() + " (CloudNet). Template creation complete.");
            future.complete(true);
        });

        return future;
    }

    @Override
    public CompletableFuture<Boolean> deleteTemplate(String floorId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try {
                ServiceTaskProvider serviceTaskProvider = InjectionLayer.boot().instance(ServiceTaskProvider.class);
                ServiceTask existingTask = serviceTaskProvider.serviceTask(floorId);
                if (existingTask != null) {
                    serviceTaskProvider.removeServiceTask(existingTask);
                    Main.getLoggerUtil().info("ServiceTask supprimée pour : " + floorId);
                } else {
                    Main.getLoggerUtil().warning("Aucune ServiceTask trouvée pour : " + floorId + " (déjà supprimée ?)");
                }

                ServiceTemplate template = new ServiceTemplate.Builder()
                        .prefix(TEMPLATE_PREFIX)
                        .name(floorId)
                        .storage("local")
                        .priority(0)
                        .alwaysCopyToStaticServices(false)
                        .build();

                try (TemplateStorage storage = template.storage()) {
                    if (storage.contains(template)) {
                        storage.delete(template);
                        Main.getLoggerUtil().info("Template CloudNet supprimé pour : " + floorId);
                    } else {
                        Main.getLoggerUtil().warning("Aucun template CloudNet trouvé pour : " + floorId + " (déjà supprimé ?)");
                    }
                }

                future.complete(true);
            } catch (Exception e) {
                Main.getLoggerUtil().severe("Erreur lors de la suppression du template pour " + floorId + " : " + e.getMessage());
                e.printStackTrace(System.err);
                future.complete(false);
            }
        });

        return future;
    }

    @Override
    public CompletableFuture<Boolean> sendPlayerToInstance(Player player, UUID instanceId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try {
                CloudServiceProvider cloudServiceProvider = InjectionLayer.ext().instance(CloudServiceProvider.class);
                ServiceInfoSnapshot service = cloudServiceProvider.service(instanceId);

                if (service == null) {
                    future.complete(false);
                    return;
                }

                String serverName = service.name();
                ServiceRegistry serviceRegistry = InjectionLayer.ext().instance(ServiceRegistry.class);
                PlayerManager playerManager = serviceRegistry.defaultInstance(PlayerManager.class);
                playerManager.playerExecutor(player.getUniqueId()).connect(serverName);

                future.complete(true);
            } catch (Exception e) {
                Main.getLoggerUtil().severe("Error sending player: " + e.getMessage());
                future.complete(false);
            }
        });

        return future;
    }

    @Override
    public UUID getCurrentServiceUniqueId() {
        try {
            ServiceInfoSnapshot currentService = InjectionLayer.ext().instance(ServiceInfoHolder.class).serviceInfo();
            return currentService.serviceId().uniqueId();
        } catch (Exception e) {
            Main.getLoggerUtil().warning("Could not resolve current service UUID: " + e.getMessage());
            return null;
        }
    }

    @Override
    public ProviderType getType() {
        return ProviderType.CLOUDNET;
    }

    @Override
    public CompletableFuture<Boolean> saveEditWorldToTemplate(Floor floor) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            try {
                Main.getLoggerUtil().info("Saving the world of publishing for " + floor.getId() + " (CloudNet)...");

                // Sauvegarder le monde
                Objects.requireNonNull(Bukkit.getWorld("world")).save();

                // Chemins spécifiques à CloudNet
                File worldSource = new File(Main.getInstance().getDataFolder() + "/../../world");
                File templateDest = new File(Main.getInstance().getDataFolder() + "/../../../../../local/templates/" + TEMPLATE_PREFIX + "/" + floor.getId() + "/world");

                // Copier les fichiers du monde vers le template CloudNet
                jodd.io.FileUtil.copyDir(new File(worldSource, "data"), new File(templateDest, "data"));
                jodd.io.FileUtil.copyDir(new File(worldSource, "entities"), new File(templateDest, "entities"));
                jodd.io.FileUtil.copyDir(new File(worldSource, "region"), new File(templateDest, "region"));
                jodd.io.FileUtil.copyFile(new File(worldSource, "uid.dat"), new File(templateDest, "uid.dat"));
                jodd.io.FileUtil.copyFile(new File(worldSource, "level.dat"), new File(templateDest, "level.dat"));

                Main.getLoggerUtil().info("World saved in the CloudNet template for " + floor.getId());
                future.complete(true);
            } catch (Exception e) {
                Main.getLoggerUtil().severe("Error during CloudNet backup: " + e.getMessage());
                e.printStackTrace(System.err);
                future.complete(false);
            }
        });

        return future;
    }

    @Override
    public void shutdown() {
        Main.getLoggerUtil().info("CloudNet provider shutdown");
        // Rien de spécial à faire pour CloudNet
    }
}