package fr.perrier.dungeons.webserver.blockly;

import fr.perrier.dungeons.webserver.blockly.annotations.BlocklyField;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Interface pour les triggers Blockly
 */
public interface BlocklyTrigger extends BlocklyComponent {

    /**
     * @return Liste des champs du trigger
     */
    default List<BlocklyFieldExtractor.BlocklyFieldInfo> getFields() {
        return BlocklyFieldExtractor.extractFields(this.getClass());
    }

    /**
     * @return True si ce trigger peut avoir des actions
     */
    default boolean hasActions() {
        return true;
    }
}
