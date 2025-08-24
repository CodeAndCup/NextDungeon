package fr.perrier.dungeons.webserver.blockly;

/**
 * Interface de base pour tous les composants Blockly
 */
public interface BlocklyComponent {
    /**
     * @return Le nom du bloc (utilisé comme type dans Blockly)
     */
    String getBlockName();

    /**
     * @return La couleur du bloc en format hex
     */
    String getColor();

    /**
     * @return Le tooltip/aide du bloc
     */
    String getTooltip();

    /**
     * @return Le texte affiché sur le bloc
     */
    String getDisplayText();
}