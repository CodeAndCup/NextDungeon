package fr.perrier.dungeons.utils;

import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.provider.CloudServiceFactory;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.service.*;
import eu.cloudnetservice.driver.template.TemplateStorage;
import eu.cloudnetservice.driver.template.TemplateStorageProvider;
import fr.perrier.dungeons.Main;
import fr.perrier.dungeons.manager.FloorInstance;
import fr.perrier.dungeons.model.Floor;
import lombok.NonNull;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;
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

    public static void createFloorTemplate(@NonNull Floor floor) {
        //Copy the global template to the floor template
        //TODO: Find a way to make copy work cause actualy that just copy default folder not files/folders inside. (Waiting answer from CloudNet Support)

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
            targetTemplateStorage.deploy(targetTemplate,zipInputStream);
            Main.getInstance().getLogger().info("Deployed template " + sourceTemplate.name() + " to " + targetTemplate.name() + " in storage " + targetTemplate.storage());
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
}
