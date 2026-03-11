package fr.perrier.dungeons.common.module;

/**
 * Interface that every dynamic NextDungeon module must implement.
 * Modules are loaded from JAR files in the /modules/ directory at startup.
 */
public interface NextDungeonModule {

    /**
     * Called when the module is loaded and enabled.
     * The module should register its blocks (triggers, actions, conditions)
     * via the provided context.
     *
     * @param ctx the server context providing access to registration APIs
     */
    void onEnable(ModuleContext ctx);

    /**
     * Called when the module is being disabled/unloaded.
     * The module should clean up any resources.
     */
    void onDisable();

    /**
     * @return unique identifier for this module (e.g. "cinematic")
     */
    String getId();

    /**
     * @return human-readable name for this module
     */
    String getName();

    /**
     * @return version string (e.g. "1.0.0")
     */
    String getVersion();
}
