package fr.perrier.dungeons.webserver.blockly;

import fr.perrier.dungeons.webserver.blockly.annotations.BlocklyField;

import java.util.List;

/**
 * Interface pour les actions Blockly
 */
public interface BlocklyAction {

    /**
     * @return Liste des champs de l'action
     */
    default List<BlocklyFieldExtractor.BlocklyFieldInfo> getFields() {
        return BlocklyFieldExtractor.extractFields(this.getClass());
    }

    /**
     * @return True si cette action peut être chaînée
     */
    default boolean isChainable() {
        return true;
    }

    /**
     * Indicates if this action requires custom JavaScript block generation
     * @return true if custom generation is needed
     */
    default boolean requiresCustomBlockGeneration() {
        return false;
    }

    /**
     * Generate custom JavaScript block definition
     * @param js StringBuilder to append the JavaScript
     */
    default void generateCustomBlock(StringBuilder js) {
        // Default implementation - do nothing
    }

    /**
     * Generate custom action extraction case
     * @param js StringBuilder to append the JavaScript
     */
    default void generateCustomActionCase(StringBuilder js) {
        // Default implementation - do nothing
    }

    /**
     * Generate custom action loading case
     * @param js StringBuilder to append the JavaScript
     */
    default void generateCustomActionLoadingCase(StringBuilder js) {
        // Default implementation - do nothing
    }
}