package fr.perrier.dungeons.utils;

import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.provider.CloudServiceFactory;
import eu.cloudnetservice.driver.provider.ServiceTaskProvider;
import eu.cloudnetservice.driver.service.ServiceConfiguration;
import eu.cloudnetservice.driver.service.ServiceCreateResult;
import eu.cloudnetservice.driver.service.ServiceTask;
import fr.perrier.dungeons.manager.FloorInstance;
import fr.perrier.dungeons.model.Floor;

public class ServerUtil {

    /**
     * Make a new SlimeWorldInstance from a floor.
     *
     * @param floorInstance The floor to make the SlimeWorldInstance from.
     * @return A new SlimeWorldInstance.
     * @throws RuntimeException If unable to load world.
     */
    public static void makeFloorInstance(FloorInstance floorInstance) {
        Floor floor = floorInstance.getFloor();
        String templateName = floor.getId();
        String instanceName = floorInstance.getInstanceName();



        CloudServiceFactory cloudService = InjectionLayer.boot().instance(CloudServiceFactory.class);
        ServiceTask serviceTask = InjectionLayer.boot().instance(ServiceTaskProvider.class).serviceTask("Lobby");
        if(serviceTask == null) {
            System.out.println("Impossible to create service task for " + instanceName);
            return;
        }
        ServiceConfiguration config = ServiceConfiguration.builder(serviceTask).build();
        ServiceCreateResult service = cloudService.createCloudService(config);
        if(service.state() != ServiceCreateResult.State.CREATED) {
            System.out.println("Impossible to create service for " + instanceName);
            return;
        }
        service.serviceInfo().provider().startAsync();
        System.out.println("Started service for " + instanceName);

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

    @Deprecated
    public static void loadAllFloorWorldTemplate() {
        /*File file = new File(Main.getInstance().getDataFolder() + "/../../slime_worlds/");
        File[] files = file.listFiles();
        if(files != null) {
            for(File f : files) {
                if(f.isFile() && f.getName().endsWith(".slime")) {
                    String name = f.getName().replace(".slime","");
                    try {
                        byte[] data = Main.getInstance().getAspLoader().readWorld(name);
                        SlimeWorld slimeWorld = Main.getInstance().getAspAPI().getSerializer().deserializeWorld(
                                name,
                                data,
                                Main.getInstance().getAspLoader(),
                                new SlimePropertyMap(),
                                false
                        );
                        Main.getInstance().getAspAPI().loadWorld(slimeWorld,true);
                    }catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }*/
    }

    public static boolean isFloorWorldTemplateExists(Floor floor) {
        /*File file = new File(Main.getInstance().getDataFolder() + "/../../slime_worlds/" + floor.getId() + ".slime");
        return file.exists();*/
        return true;
    }

    public static void loadFloorWorldTemplate(Floor floor) {
        /*try {
            byte[] data = Main.getInstance().getAspLoader().readWorld(floor.getId());
            SlimeWorld slimeWorld = Main.getInstance().getAspAPI().getSerializer().deserializeWorld(
                    floor.getId(),
                    data,
                    Main.getInstance().getAspLoader(),
                    new SlimePropertyMap(),
                    false
            );
            Main.getInstance().getAspAPI().loadWorld(slimeWorld,true);
        }catch (Exception e) {
            e.printStackTrace();
        }*/
    }
}
