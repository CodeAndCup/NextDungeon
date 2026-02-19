package fr.perrier.dungeons.spigot.workflow.action.impl;

import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.webeditor.blockly.BlocklyAction;
import fr.perrier.dungeons.spigot.workflow.action.Action;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyInfo;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.Location;

import java.util.Map;

/**
 * Action pour additionner ou concaténer deux valeurs
 */
@Setter
@Getter
@BlocklyInfo(
        name = "math_operation_action",
        color = "#9C27B0",
        displayText = "🧮 Opération mathématique",
        tooltip = "Effectue une opération mathématique ou de concaténation entre deux valeurs",
        category = "Actions"
)
public class MathOperationAction extends Action implements BlocklyAction {
    private static final long serialVersionUID = 1L;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Première valeur:",
            defaultValue = "5", order = 1)
    private String firstValue;

    @BlocklyField(type = BlocklyField.FieldType.DROPDOWN, label = "Opération:",
            options = "add,subtract,multiply,divide,concatenate", defaultValue = "add", order = 2)
    private String operation;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Deuxième valeur:",
            defaultValue = "3", order = 3)
    private String secondValue;

    @BlocklyField(type = BlocklyField.FieldType.TEXT_INPUT, label = "Variable résultat:",
            defaultValue = "resultat", order = 4)
    private String resultVariableName;

    @BlocklyField(type = BlocklyField.FieldType.DROPDOWN, label = "Portée résultat:",
            options = "player,global", defaultValue = "player", order = 5)
    private String resultScope;

    public MathOperationAction() {
        super("Math Operation", "math_operation_action");
        this.firstValue = "5";
        this.operation = "add";
        this.secondValue = "3";
        this.resultVariableName = "resultat";
        this.resultScope = "player";
    }

    public MathOperationAction(String firstValue, String operation, String secondValue, String resultVariableName, String resultScope) {
        super("Math Operation", "math_operation_action");
        this.firstValue = firstValue;
        this.operation = operation;
        this.secondValue = secondValue;
        this.resultVariableName = resultVariableName;
        this.resultScope = resultScope;
    }

    @Override
    public boolean execute(Player triggerPlayer, Location location, Map<String, Object> data) {
        if (resultVariableName == null || resultVariableName.trim().isEmpty()) {
            Main.getLoggerUtil().warning("Result variable name is empty in MathOperationAction");
            return false;
        }

        String trimmedResultName = resultVariableName.trim();
        String resultScopeToUse = resultScope != null ? resultScope : "player";
        String operationToUse = operation != null ? operation : "add";

        try {
            // Résoudre les valeurs (peuvent être des variables ou des constantes)
            Object resolvedFirst = resolveValue(firstValue, triggerPlayer, data);
            Object resolvedSecond = resolveValue(secondValue, triggerPlayer, data);

            Object result = performOperation(resolvedFirst, resolvedSecond, operationToUse);

            // Stocker le résultat
            Main.getInstance().getVariableRegistry().setVariable(triggerPlayer, trimmedResultName, result, resultScopeToUse);
            data.put(trimmedResultName, result);

            if (Main.getLoggerUtil().isDebugEnabled()) {
                Main.getLoggerUtil().info("Math operation: " + resolvedFirst + " " + operationToUse + " " + resolvedSecond + " = " + result);
            }

            return true;

        } catch (Exception e) {
            Main.getLoggerUtil().warning("Error in MathOperationAction: " + e.getMessage());
            return false;
        }
    }

    /**
     * Résout une valeur (peut être une variable ou une constante)
     */
    private Object resolveValue(String value, Player player, Map<String, Object> data) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }

        String trimmed = value.trim();

        // Vérifier si c'est une variable (commence par $)
        if (trimmed.startsWith("$")) {
            String variableName = trimmed.substring(1);
            Object variableValue = Main.getInstance().getVariableRegistry().getVariable(player, variableName);
            if (variableValue == null) {
                // Essayer dans les données locales
                variableValue = data.get(variableName);
                if (variableValue == null) {
                    return 0; // Variable non trouvée
                }
            }
            return variableValue;
        }

        // Essayer de parser comme nombre
        try {
            if (trimmed.contains(".")) {
                return Double.parseDouble(trimmed);
            } else {
                return Integer.parseInt(trimmed);
            }
        } catch (NumberFormatException e) {
            // Si ce n'est pas un nombre, retourner comme string
            return trimmed;
        }
    }

    /**
     * Effectue l'opération demandée
     */
    private Object performOperation(Object first, Object second, String operation) {
        return switch (operation) {
            case "add" -> performAddition(first, second);
            case "subtract" -> performSubtraction(first, second);
            case "multiply" -> performMultiplication(first, second);
            case "divide" -> performDivision(first, second);
            case "concatenate" -> performConcatenation(first, second);
            default -> {
                Main.getLoggerUtil().warning("Unknown operation: " + operation);
                yield first;
            }
        };
    }

    private Object performAddition(Object first, Object second) {
        if (isNumeric(first) && isNumeric(second)) {
            double num1 = getNumericValue(first);
            double num2 = getNumericValue(second);
            double result = num1 + num2;
            // Retourner int si pas de décimales
            if (result == (int) result) {
                return (int) result;
            }
            return result;
        }
        // Concaténation si pas numérique
        return first.toString() + second.toString();
    }

    private Object performSubtraction(Object first, Object second) {
        if (isNumeric(first) && isNumeric(second)) {
            double num1 = getNumericValue(first);
            double num2 = getNumericValue(second);
            double result = num1 - num2;
            if (result == (int) result) {
                return (int) result;
            }
            return result;
        }
        return 0;
    }

    private Object performMultiplication(Object first, Object second) {
        if (isNumeric(first) && isNumeric(second)) {
            double num1 = getNumericValue(first);
            double num2 = getNumericValue(second);
            double result = num1 * num2;
            if (result == (int) result) {
                return (int) result;
            }
            return result;
        }
        return 0;
    }

    private Object performDivision(Object first, Object second) {
        if (isNumeric(first) && isNumeric(second)) {
            double num1 = getNumericValue(first);
            double num2 = getNumericValue(second);
            if (num2 == 0) {
                Main.getLoggerUtil().warning("Division by zero in MathOperationAction");
                return 0;
            }
            double result = num1 / num2;
            if (result == (int) result) {
                return (int) result;
            }
            return result;
        }
        return 0;
    }

    private Object performConcatenation(Object first, Object second) {
        return first.toString() + second.toString();
    }

    private boolean isNumeric(Object value) {
        if (value instanceof Number) {
            return true;
        }
        if (value instanceof String str) {
            try {
                Double.parseDouble(str);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }

    private double getNumericValue(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String str) {
            return Double.parseDouble(str);
        }
        return 0;
    }
}
