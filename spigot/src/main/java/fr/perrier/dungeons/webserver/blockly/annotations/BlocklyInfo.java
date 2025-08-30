package fr.perrier.dungeons.webserver.blockly.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface BlocklyInfo {
    /**
     * Nom du bloc (auto-généré si vide)
     */
    String name() default "";

    /**
     * Couleur du bloc
     */
    String color();

    /**
     * Texte affiché sur le bloc
     */
    String displayText();

    /**
     * Tooltip du bloc
     */
    String tooltip() default "";

    /**
     * Catégorie dans la toolbox
     */
    String category() default "";
}
