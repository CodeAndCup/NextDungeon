package fr.perrier.dungeons.spigot.webserver.blockly;

import java.util.List;

/**
 * Interface pour les triggers Blockly
 */
public interface BlocklyTrigger {

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
