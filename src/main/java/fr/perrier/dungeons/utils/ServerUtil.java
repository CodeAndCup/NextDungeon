package fr.perrier.dungeons.utils;

import eu.cloudnetservice.driver.DriverEnvironment;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.module.ModuleWrapper;
import eu.cloudnetservice.driver.provider.CloudServiceFactory;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.service.*;
import eu.cloudnetservice.driver.template.TemplateStorage;
import eu.cloudnetservice.driver.template.TemplateStorageProvider;
import eu.cloudnetservice.modules.bridge.player.CloudPlayer;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import eu.cloudnetservice.modules.bridge.player.PlayerProvider;
import eu.cloudnetservice.modules.bridge.player.executor.PlayerExecutor;
import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.manager.FloorInstance;
import fr.perrier.dungeons.model.Floor;
import lombok.NonNull;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ServerUtil {


    public static UUID makeFloorInstance(FloorInstance floorInstance) {
        Floor floor = floorInstance.getFloor();
        String templateName = floor.getId();

        CloudServiceFactory cloudService = InjectionLayer.boot().instance(CloudServiceFactory.class);
        ServiceTask serviceTask = InjectionLayer.boot().instance(ServiceTaskProvider.class).serviceTask(templateName);
        if(serviceTask == null) {
            System.out.println("Impossible to create service task for " + templateName);
            return null;
        }
        ServiceConfiguration config = ServiceConfiguration.builder(serviceTask).build();
        ServiceCreateResult service = cloudService.createCloudService(config);
        if(service.state() != ServiceCreateResult.State.CREATED) {
            System.out.println("Impossible to create service for " + templateName);
            return null;
        }
        service.serviceInfo().provider().startAsync();
        System.out.println("Started service for " + templateName);

        return service.serviceInfo().serviceId().uniqueId();
    }

    public static void saveFloorWorldTemplate(Floor floor) {
        /*ServiceInfoSnapshot serviceInfo = Wrapper.getInstance().getCurrentServiceInfoSnapshot();

        // Find the template the server is based on
        ServiceTemplate template = Arrays.stream(serviceInfo.getConfiguration().getTemplates()).findFirst()
                .orElseThrow(() -> new IllegalStateException("No template found for this service!"));

        // Get the storage (usually "local", but check your config)
        TemplateStorage storage = CloudNetDriver.getInstance().getTemplateStorage(template.getStorage());

        // Build the path to the world directory in the current server's directory
        Path worldDir = Paths.get(serviceInfo., worldDirName);

        // Deploy the world folder to the template
        storage.deployDirectory(template, worldDir);*/
    }

    public static boolean isFloorTemplateExists(@NonNull Floor floor) {
        ServiceTaskProvider serviceTaskProvider = InjectionLayer.boot().instance(ServiceTaskProvider.class);
        return serviceTaskProvider.serviceTask(floor.getId()) != null;
    }

    /**
     * Create a template for a floor in the local storage.
     * The template is based on the global template and is copied to the local storage.
     * The template is named after the floor ID and is given a priority of 0.
     * The template is also added to the service task provider.
     *
     * @param floor the floor to create the template for
     */
    public static void createFloorTemplate(@NonNull Floor floor) {
        //Copy the global template to the floor template

        long startTime = System.currentTimeMillis();

        ServiceTemplate sourceTemplate = new ServiceTemplate.Builder()
                .prefix("Global")
                .name("Global-Dungeon")
                .storage("local")
                .priority(0)
                .alwaysCopyToStaticServices(false)
                .build();

        ServiceTemplate targetTemplate = new ServiceTemplate.Builder()
                .prefix(floor.getId())
                .name("default")
                .storage("local")
                .priority(0)
                .alwaysCopyToStaticServices(false)
                .build();

        TemplateStorage sourceTemplateStorage = sourceTemplate.storage();
        TemplateStorage targetTemplateStorage = targetTemplate.storage();

        targetTemplateStorage.delete(targetTemplate);
        targetTemplateStorage.create(targetTemplate);

        try {
            ZipInputStream zipInputStream = sourceTemplateStorage.openZipInputStream(sourceTemplate);
            if(zipInputStream == null) {
                Main.getInstance().getLogger().severe("Unable to get zip input stream for template " + sourceTemplate.name() + " in storage " + sourceTemplate.storage());
                return;
            }

            try {

                var localStoragePath = Path.of("../../../local/templates/");
                var templatePath = localStoragePath.resolve(targetTemplate.prefix()).resolve(targetTemplate.name());

                if(Main.isDebug())
                    Main.getInstance().getLogger().info("Template path: " + templatePath.toAbsolutePath());
                Files.createDirectories(templatePath);

                ZipEntry entryDeploy;
                while ((entryDeploy = zipInputStream.getNextEntry()) != null) {
                    var file = templatePath.resolve(entryDeploy.getName());
                    if(Main.isDebug()) {
                        Main.getInstance().getLogger().info("Find " + entryDeploy.getName() + " will be copied to (" + file + ")");
                        Main.getInstance().getLogger().info("Entry is directory: " + entryDeploy.isDirectory());
                    }
                    if (entryDeploy.isDirectory()) {
                        if (file != null && Files.notExists(file)) {
                            try {
                                Files.createDirectories(file);
                            } catch (IOException e) {
                                Main.getInstance().getLogger().severe("Unable to create directory " + file);
                            }
                        }
                    } else {
                        try {
                            if(Main.isDebug()) {
                                Main.getInstance().getLogger().info("File: " + file);
                                Main.getInstance().getLogger().info("Parent: " + file.getParent());
                            }
                            Files.createDirectories(file.getParent());
                            try (OutputStream out = Files.newOutputStream(file)) {
                                if (zipInputStream != null && out != null) {
                                    if(Main.isDebug())
                                        Main.getInstance().getLogger().info("Copying " + entryDeploy.getName() + " to " + file);
                                    long transferred = zipInputStream.transferTo(out);
                                    if(Main.isDebug())
                                        Main.getInstance().getLogger().info("Copied " + transferred + " bytes");
                                }
                            }
                        } catch (IOException e) {
                            Main.getInstance().getLogger().severe("Unable to copy file " + entryDeploy.getName());
                        }
                    }
                    zipInputStream.closeEntry();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            Main.getInstance().getLogger().info("Creating template " + targetTemplate.name() + " in storage " + targetTemplate.storage() + " took " + (System.currentTimeMillis() - startTime) + " ms");
            zipInputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //Create the task
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
                                .maxHeapMemorySize(4096)
                                .jvmOptions(Collections.emptyList())
                                .processParameters(Collections.emptyList())
                                .environmentVariables(Collections.emptyMap())
                )
                .startPort(44955)
                .minServiceCount(0)
                .templates(
                        Collections.singletonList(
                                new ServiceTemplate.Builder()
                                        .prefix(floor.getId())
                                        .name("default")
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
    }

    /**
     * Connects a player to a cloud service with the given name.
     * @param player The player to connect to the cloud service.
     * @param server The name of the cloud service to connect to.
     */
    public static void sendToServer(Player player, String server) {
        /*
        [09.08 15:33:40.175] INFO : [Lobby-1] [15:33:40 WARN]: Caused by: java.lang.IllegalArgumentException: Cannot construct abstract type eu.cloudnetservice.modules.bridge.player.PlayerManager
[09.08 15:33:40.175] INFO : [Lobby-1] [15:33:40 WARN]:  at dev.derklaro.aerogel.internal.binding.builder.ConcreteBindingBuilderImpl.toConstructingClass(ConcreteBindingBuilderImpl.java:258)
[09.08 15:33:40.175] INFO : [Lobby-1] [15:33:40 WARN]:  at dev.derklaro.aerogel.internal.injector.JitBindingFactory.createJitBinding(JitBindingFactory.java:113)
[09.08 15:33:40.175] INFO : [Lobby-1] [15:33:40 WARN]:  at dev.derklaro.aerogel.internal.injector.InjectorImpl.lambda$binding$0(InjectorImpl.java:204)
[09.08 15:33:40.176] INFO : [Lobby-1] [15:33:40 WARN]:  at java.base/java.util.Optional.orElseGet(Optional.java:364)
[09.08 15:33:40.176] INFO : [Lobby-1] [15:33:40 WARN]:  at dev.derklaro.aerogel.internal.injector.InjectorImpl.binding(InjectorImpl.java:202)
[09.08 15:33:40.176] INFO : [Lobby-1] [15:33:40 WARN]:  at dev.derklaro.aerogel.internal.injector.InjectorImpl.createInjectionRequest(InjectorImpl.java:195)
[09.08 15:33:40.176] INFO : [Lobby-1] [15:33:40 WARN]:  at dev.derklaro.aerogel.internal.injector.InjectorImpl.instance(InjectorImpl.java:183)
[09.08 15:33:40.176] INFO : [Lobby-1] [15:33:40 WARN]:  at dev.derklaro.aerogel.internal.injector.InjectorImpl.instance(InjectorImpl.java:166)
[09.08 15:33:40.176] INFO : [Lobby-1] [15:33:40 WARN]:  at eu.cloudnetservice.driver.inject.DefaultInjectionLayer.instance(DefaultInjectionLayer.java:61)
[09.08 15:33:40.176] INFO : [Lobby-1] [15:33:40 WARN]:  at eu.cloudnetservice.driver.inject.UncloseableInjectionLayer.instance(UncloseableInjectionLayer.java:61)
[09.08 15:33:40.176] INFO : [Lobby-1] [15:33:40 WARN]:  at dungeons-1.0-SNAPSHOT.jar//fr.perrier.dungeons.utils.ServerUtil.sendToServer(ServerUtil.java:228)
[09.08 15:33:40.176] INFO : [Lobby-1] [15:33:40 WARN]:  at dungeons-1.0-SNAPSHOT.jar//fr.perrier.dungeons.commands.AdminCommands.adminDungeonPlayCommand(AdminCommands.java:46)
[09.08 15:33:40.176] INFO : [Lobby-1] [15:33:40 WARN]:  at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:104)
[09.08 15:33:40.176] INFO : [Lobby-1] [15:33:40 WARN]:  ... 31 more
         */
        PlayerManager playerManager = InjectionLayer.boot().instance(PlayerManager.class);
        playerManager.playerExecutor(player.getUniqueId()).connect(server);
    }

    /**
     * Retrieves a ServiceInfoSnapshot for a cloud service with the specified port.
     *
     * @param port The port of the cloud service to retrieve.
     * @return The ServiceInfoSnapshot of the cloud service with the given port,
     *         or null if no such service is found.
     */
    public static ServiceInfoSnapshot getFactory(int port) {
        CloudServiceProvider cloudServiceProvider = InjectionLayer.ext().instance(CloudServiceProvider.class);
        return cloudServiceProvider.runningServices().stream()
                .filter(service -> service.address().port() == port)
                .findFirst().orElse(null);
    }
}
