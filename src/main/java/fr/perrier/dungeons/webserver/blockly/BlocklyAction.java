package fr.perrier.dungeons.webserver.blockly;

import fr.perrier.dungeons.webserver.blockly.annotations.BlocklyField;

import java.util.List;

/**
 * Interface pour les actions Blockly
 */
public interface BlocklyAction extends BlocklyComponent {

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
}