package fr.perrier.dungeons.spigot.module;

import fr.perrier.dungeons.common.module.ModuleActionHandler;
import fr.perrier.dungeons.common.module.ModuleBlockRegistry;
import fr.perrier.dungeons.common.module.NextDungeonModule;
import fr.perrier.dungeons.spigot.Main;
import lombok.Getter;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

/**
 * Loads dynamic modules from JAR files in the /modules/ directory.
 * Each module gets its own ClassLoader for isolation.
 * Modules register their blocks (triggers/actions/conditions) into
 * the shared {@link ModuleBlockRegistry}.
 */
public class ModuleLoader {

    private final Logger logger;
    private final File modulesDirectory;
    @Getter
    private final DefaultModuleBlockRegistry blockRegistry;
    private final Map<String, NextDungeonModule> loadedModules = new ConcurrentHashMap<>();
    private final Map<String, URLClassLoader> moduleClassLoaders = new ConcurrentHashMap<>();
    private final Map<String, ModuleActionHandler> actionHandlers = new ConcurrentHashMap<>();

    public ModuleLoader(File modulesDirectory) {
        this.logger = Main.getInstance().getLogger();
        this.modulesDirectory = modulesDirectory;
        this.blockRegistry = new DefaultModuleBlockRegistry();
    }

    /**
     * Scans the modules directory and loads all JAR modules.
     */
    public void loadAll() {
        if (!modulesDirectory.exists()) {
            if(modulesDirectory.mkdirs()) {
                logger.info("[ModuleLoader] Created modules directory: " + modulesDirectory.getAbsolutePath());
            } else {
                logger.severe("[ModuleLoader] Failed to create modules directory: " + modulesDirectory.getAbsolutePath());
            }
            return;
        }

        File[] jarFiles = modulesDirectory.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles == null || jarFiles.length == 0) {
            logger.info("[ModuleLoader] No modules found in " + modulesDirectory.getAbsolutePath());
            return;
        }

        for (File jarFile : jarFiles) {
            try {
                loadModule(jarFile);
            } catch (Exception e) {
                logger.severe("[ModuleLoader] Failed to load module from " + jarFile.getName() + ": " + e.getMessage());
                e.printStackTrace(System.err);
            }
        }

        logger.info("[ModuleLoader] Loaded " + loadedModules.size() + " module(s), "
                + blockRegistry.size() + " block(s) registered");
    }

    /**
     * Loads a single module from a JAR file.
     */
    private void loadModule(File jarFile) throws Exception {
        URL jarUrl = jarFile.toURI().toURL();
        URLClassLoader classLoader = new URLClassLoader(
                new URL[]{jarUrl},
                getClass().getClassLoader()
        );

        // Scan JAR for classes implementing NextDungeonModule
        NextDungeonModule module = findModuleClass(jarFile, classLoader);
        if (module == null) {
            classLoader.close();
            logger.warning("[ModuleLoader] No NextDungeonModule implementation found in " + jarFile.getName());
            return;
        }

        String moduleId = module.getId();
        if (loadedModules.containsKey(moduleId)) {
            classLoader.close();
            logger.warning("[ModuleLoader] Module with ID '" + moduleId + "' is already loaded, skipping " + jarFile.getName());
            return;
        }

        // Enable the module with a context
        DefaultModuleContext ctx = new DefaultModuleContext(blockRegistry, this::registerActionHandler);
        module.onEnable(ctx);

        loadedModules.put(moduleId, module);
        moduleClassLoaders.put(moduleId, classLoader);

        logger.info("[ModuleLoader] Loaded module: " + module.getName() + " v" + module.getVersion()
                + " (" + moduleId + ") from " + jarFile.getName());
    }

    /**
     * Loads a module from a specific JAR file in the modules directory by filename.
     * Returns the module ID on success, null on failure.
     *
     * @param jarFileName the filename of the JAR (e.g. "module-cinematic.jar")
     * @return the loaded module ID, or null if loading failed
     */
    public String loadModuleFromFile(String jarFileName) {
        File jarFile = new File(modulesDirectory, jarFileName);
        if (!jarFile.exists()) {
            logger.warning("[ModuleLoader] JAR file not found: " + jarFile.getAbsolutePath());
            return null;
        }
        // Snapshot existing IDs to detect the newly added module
        Set<String> before = new HashSet<>(loadedModules.keySet());
        try {
            loadModule(jarFile);
        } catch (Exception e) {
            logger.severe("[ModuleLoader] Failed to load module from " + jarFileName + ": " + e.getMessage());
            e.printStackTrace(System.err);
            return null;
        }
        // The new module is whichever ID appeared after loading
        for (String id : loadedModules.keySet()) {
            if (!before.contains(id)) {
                return id;
            }
        }
        // Module was already loaded (duplicate ID) — nothing new added
        return null;
    }

    /**
     * Unloads a single module by its ID, disabling it and closing its classloader.
     * Also removes all blocks it registered from the registry.
     *
     * @param moduleId the ID of the module to unload
     * @return true if the module was found and unloaded, false otherwise
     */
    public boolean unloadModule(String moduleId) {
        NextDungeonModule module = loadedModules.remove(moduleId);
        if (module == null) {
            logger.warning("[ModuleLoader] No loaded module with ID: " + moduleId);
            return false;
        }
        try {
            module.onDisable();
        } catch (Exception e) {
            logger.warning("[ModuleLoader] Error disabling module " + moduleId + ": " + e.getMessage());
        }
        // Remove all blocks registered by this module
        blockRegistry.unregisterModule(moduleId);
        // Remove action handlers registered by this module
        // (there is no per-module tracking — remove handlers whose key starts with moduleId + "_")
        actionHandlers.entrySet().removeIf(e -> e.getKey().startsWith(moduleId + "_") || e.getKey().equals(moduleId));
        URLClassLoader classLoader = moduleClassLoaders.remove(moduleId);
        if (classLoader != null) {
            try {
                classLoader.close();
            } catch (Exception e) {
                logger.warning("[ModuleLoader] Error closing classloader for " + moduleId + ": " + e.getMessage());
            }
        }
        logger.info("[ModuleLoader] Module '" + moduleId + "' unloaded");
        return true;
    }

    /**
     * Reloads a module by its ID: finds its JAR, unloads the module, then re-loads it.
     *
     * @param moduleId the ID of the module to reload
     * @return true if reload was successful, false otherwise
     */
    public boolean reloadModule(String moduleId) {
        // Find the JAR URL before unloading
        URLClassLoader oldClassLoader = moduleClassLoaders.get(moduleId);
        File jarFile = null;
        if (oldClassLoader != null && oldClassLoader.getURLs().length > 0) {
            try {
                jarFile = new File(oldClassLoader.getURLs()[0].toURI());
            } catch (Exception e) {
                logger.warning("[ModuleLoader] Could not resolve JAR path for module " + moduleId);
            }
        }
        if (jarFile == null || !jarFile.exists()) {
            logger.warning("[ModuleLoader] Cannot reload module '" + moduleId + "': JAR file not found");
            return false;
        }
        // Unload
        boolean unloaded = unloadModule(moduleId);
        if (!unloaded) return false;
        // Re-load
        try {
            loadModule(jarFile);
            logger.info("[ModuleLoader] Module '" + moduleId + "' reloaded successfully");
            return true;
        } catch (Exception e) {
            logger.severe("[ModuleLoader] Failed to reload module '" + moduleId + "': " + e.getMessage());
            e.printStackTrace(System.err);
            return false;
        }
    }

    /**
     * Finds and instantiates the NextDungeonModule implementation in a JAR.
     */
    private NextDungeonModule findModuleClass(File jarFile, ClassLoader classLoader) throws Exception {
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class") && !entry.getName().contains("$")) {
                    String className = entry.getName()
                            .replace('/', '.')
                            .replace(".class", "");
                    try {
                        Class<?> clazz = classLoader.loadClass(className);
                        if (NextDungeonModule.class.isAssignableFrom(clazz) && !clazz.isInterface()) {
                            return (NextDungeonModule) clazz.getDeclaredConstructor().newInstance();
                        }
                    } catch (ClassNotFoundException | NoClassDefFoundError e) {
                        // Skip classes that can't be loaded (missing dependencies)
                    }
                }
            }
        }
        return null;
    }

    /**
     * Register a handler for a module action block ID.
     *
     * @param blockId the action block ID (e.g. "cinematic_start")
     * @param handler the handler to invoke when this action is executed
     */
    public void registerActionHandler(String blockId, ModuleActionHandler handler) {
        actionHandlers.put(blockId, handler);
    }

    /**
     * Get the action handler for a given block ID.
     *
     * @param blockId the action block ID
     * @return the handler, or null if not registered
     */
    public ModuleActionHandler getActionHandler(String blockId) {
        return actionHandlers.get(blockId);
    }

    /**
     * Unloads all modules and closes classloaders.
     */
    public void unloadAll() {
        for (Map.Entry<String, NextDungeonModule> entry : loadedModules.entrySet()) {
            try {
                entry.getValue().onDisable();
            } catch (Exception e) {
                logger.warning("[ModuleLoader] Error disabling module " + entry.getKey() + ": " + e.getMessage());
            }
        }
        for (Map.Entry<String, URLClassLoader> entry : moduleClassLoaders.entrySet()) {
            try {
                entry.getValue().close();
            } catch (Exception e) {
                logger.warning("[ModuleLoader] Error closing classloader for " + entry.getKey() + ": " + e.getMessage());
            }
        }
        loadedModules.clear();
        moduleClassLoaders.clear();
        actionHandlers.clear();
        logger.info("[ModuleLoader] All modules unloaded");
    }

    /**
     * @return unmodifiable view of loaded modules
     */
    public Map<String, NextDungeonModule> getLoadedModules() {
        return Collections.unmodifiableMap(loadedModules);
    }

    /**
     * @return all registered action handlers
     */
    public Map<String, ModuleActionHandler> getActionHandlers() {
        return Collections.unmodifiableMap(actionHandlers);
    }
}
