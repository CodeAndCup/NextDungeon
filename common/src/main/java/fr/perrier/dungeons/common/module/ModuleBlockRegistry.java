package fr.perrier.dungeons.common.module;

import java.util.List;

/**
 * Registry interface for modules to register their custom blocks
 * (triggers, actions, conditions) that will be exposed to the
 * Blockly web panel and the workflow execution engine.
 */
public interface ModuleBlockRegistry {

    /**
     * Register a block descriptor that will be available in
     * the Blockly web editor and the workflow execution engine.
     *
     * @param descriptor the block descriptor to register
     */
    void registerBlock(ModuleBlockDescriptor descriptor);

    /**
     * @return all registered block descriptors from all modules
     */
    List<ModuleBlockDescriptor> getAllBlocks();

    /**
     * @param moduleId the module identifier
     * @return all block descriptors registered by the specified module
     */
    List<ModuleBlockDescriptor> getBlocksByModule(String moduleId);

    /**
     * @param type the block type (trigger, action, condition)
     * @return all block descriptors of the specified type
     */
    List<ModuleBlockDescriptor> getBlocksByType(ModuleBlockDescriptor.BlockType type);

    /**
     * Retrieve a block descriptor by its unique ID.
     *
     * @param blockId the block ID
     * @return the descriptor, or null if not found
     */
    ModuleBlockDescriptor getBlock(String blockId);
}
