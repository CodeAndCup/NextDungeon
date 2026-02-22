package fr.perrier.dungeons.common.module;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Describes a single block (trigger, action, or condition) provided by a dynamic module.
 * This descriptor is serialized to JSON and consumed by:
 * - The Blockly web panel (to dynamically build the toolbox)
 * - The workflow execution engine (to map block IDs to module handlers)
 */
@Getter
@Setter
public class ModuleBlockDescriptor implements Serializable {

    public enum BlockType {
        TRIGGER, ACTION, CONDITION
    }

    /** Unique block identifier, e.g. "cinematic.start" */
    private String id;

    /** Block type */
    private BlockType type;

    /** Human-readable label shown in Blockly, e.g. "Start Cinematic" */
    private String label;

    /** Tooltip/description for the block */
    private String description;

    /** Module that provides this block */
    private String moduleId;

    /** Hex color for the Blockly block, e.g. "#9C27B0" */
    private String color;

    /** Category in the Blockly toolbox, e.g. "Cinematic" */
    private String category;

    /** Parameter definitions for this block */
    private List<BlockParameter> parameters = new ArrayList<>();

    /**
     * Describes a single parameter/field on a block.
     */
    @Getter
    @Setter
    public static class BlockParameter implements Serializable {
        /** Parameter name (used as field name in JSON) */
        private String name;

        /** Parameter type: string, number, boolean, dropdown, location */
        private String type;

        /** Human-readable label */
        private String label;

        /** Description/tooltip */
        private String description;

        /** Default value */
        private String defaultValue;

        /** Comma-separated options for dropdown type */
        private String options;

        public BlockParameter() {}

        public BlockParameter(String name, String type, String label, String description, String defaultValue) {
            this.name = name;
            this.type = type;
            this.label = label;
            this.description = description;
            this.defaultValue = defaultValue;
        }
    }

    public ModuleBlockDescriptor() {}

    public ModuleBlockDescriptor(String id, BlockType type, String label, String description, String moduleId) {
        this.id = id;
        this.type = type;
        this.label = label;
        this.description = description;
        this.moduleId = moduleId;
    }
}
