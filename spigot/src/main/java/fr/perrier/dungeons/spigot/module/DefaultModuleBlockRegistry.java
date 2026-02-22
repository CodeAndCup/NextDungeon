package fr.perrier.dungeons.spigot.module;

import fr.perrier.dungeons.common.module.ModuleBlockDescriptor;
import fr.perrier.dungeons.common.module.ModuleBlockRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of the ModuleBlockRegistry.
 * Stores block descriptors registered by dynamic modules and
 * exposes them to the Blockly generator and workflow engine.
 */
public class DefaultModuleBlockRegistry implements ModuleBlockRegistry {

    private final Map<String, ModuleBlockDescriptor> blocks = new ConcurrentHashMap<>();

    @Override
    public void registerBlock(ModuleBlockDescriptor descriptor) {
        if (descriptor == null || descriptor.getId() == null) {
            throw new IllegalArgumentException("Block descriptor and its ID must not be null");
        }
        blocks.put(descriptor.getId(), descriptor);
        // Also register under the Blockly-normalized name (dots → underscores)
        // so lookups from Blockly-generated types like "cinematic_add_camera_waypoint" work
        String blocklyName = descriptor.getId().replace('.', '_');
        if (!blocklyName.equals(descriptor.getId())) {
            blocks.put(blocklyName, descriptor);
        }
    }

    @Override
    public List<ModuleBlockDescriptor> getAllBlocks() {
        return new ArrayList<>(blocks.values());
    }

    @Override
    public List<ModuleBlockDescriptor> getBlocksByModule(String moduleId) {
        return blocks.values().stream()
                .filter(b -> moduleId.equals(b.getModuleId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ModuleBlockDescriptor> getBlocksByType(ModuleBlockDescriptor.BlockType type) {
        return blocks.values().stream()
                .filter(b -> type.equals(b.getType()))
                .collect(Collectors.toList());
    }

    @Override
    public ModuleBlockDescriptor getBlock(String blockId) {
        return blocks.get(blockId);
    }

    /**
     * Remove all blocks registered by a given module.
     *
     * @param moduleId the module identifier
     */
    public void unregisterModule(String moduleId) {
        blocks.entrySet().removeIf(e -> moduleId.equals(e.getValue().getModuleId()));
    }

    /**
     * @return number of registered blocks
     */
    public int size() {
        return blocks.size();
    }
}
