package fr.perrier.dungeons.webserver.blockly.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BlocklyField {

    enum FieldType {
        TEXT_INPUT,
        NUMBER_INPUT,
        DROPDOWN,
        BOOLEAN_INPUT,
        COLOR_INPUT,
        CHECKBOX
    }

    /**
     * Type de champ
     */
    FieldType type();

    /**
     * Label affiché avant le champ
     */
    String label() default "";

    /**
     * Valeur par défaut
     */
    String defaultValue() default "";

    /**
     * Options pour les dropdowns (format: "option1,option2,option3")
     */
    String options() default "";

    /**
     * Valeur minimale pour les nombres
     */
    double min() default Double.MIN_VALUE;

    /**
     * Valeur maximale pour les nombres
     */
    double max() default Double.MAX_VALUE;

    /**
     * Ordre d'affichage du champ
     */
    int order() default 0;
}
