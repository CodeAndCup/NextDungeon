package fr.perrier.dungeons.webserver.blockly;

import fr.perrier.dungeons.webserver.blockly.annotations.BlocklyInfo;
import org.reflections.Reflections;

import java.util.*;

public class BlocklyJavaScriptGenerator {

    private final Map<String, List<Class<? extends BlocklyTrigger>>> triggersByCategory = new HashMap<>();
    private final Map<String, List<Class<? extends BlocklyAction>>> actionsByCategory = new HashMap<>();

    public BlocklyJavaScriptGenerator() {
        scanForComponents();
    }

    private void scanForComponents() {
        Reflections reflections = new Reflections("fr.perrier.dungeons");

        // Scanner les triggers
        Set<Class<? extends BlocklyTrigger>> triggerClasses = reflections.getSubTypesOf(BlocklyTrigger.class);
        for (Class<? extends BlocklyTrigger> clazz : triggerClasses) {
            if (clazz.isAnnotationPresent(BlocklyInfo.class)) {
                BlocklyInfo info = clazz.getAnnotation(BlocklyInfo.class);
                String category = info.category().isEmpty() ? "Triggers" : info.category();
                triggersByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(clazz);
            }
        }

        // Scanner les actions
        Set<Class<? extends BlocklyAction>> actionClasses = reflections.getSubTypesOf(BlocklyAction.class);
        for (Class<? extends BlocklyAction> clazz : actionClasses) {
            if (clazz.isAnnotationPresent(BlocklyInfo.class)) {
                BlocklyInfo info = clazz.getAnnotation(BlocklyInfo.class);
                String category = info.category().isEmpty() ? "Actions" : info.category();
                actionsByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(clazz);
            }
        }
    }

    public String generateJavaScript() {
        StringBuilder js = new StringBuilder();

        js.append("// Auto-généré par BlocklyJavaScriptGenerator\n");
        js.append("console.log('🔧 Chargement automatique des blocs Blockly...');\n\n");

        // Générer les blocs triggers
        generateTriggerBlocks(js);

        // Générer les blocs actions
        generateActionBlocks(js);

        // Générer les blocs utilitaires
        generateUtilityBlocks(js);

        // Générer la toolbox
        generateToolbox(js);

        // Générer les fonctions utilitaires
        generateUtilityFunctions(js);

        js.append("console.log('✅ Tous les blocs ont été générés automatiquement!');\n");

        return js.toString();
    }

    private void generateTriggerBlocks(StringBuilder js) {
        js.append("// ===== BLOCS TRIGGERS (AUTO-GÉNÉRÉS) =====\n");

        for (Map.Entry<String, List<Class<? extends BlocklyTrigger>>> entry : triggersByCategory.entrySet()) {
            js.append("// Catégorie: ").append(entry.getKey()).append("\n");

            for (Class<? extends BlocklyTrigger> triggerClass : entry.getValue()) {
                generateTriggerBlock(js, triggerClass);
            }
        }
        js.append("\n");
    }

    private void generateTriggerBlock(StringBuilder js, Class<? extends BlocklyTrigger> triggerClass) {
        BlocklyInfo info = triggerClass.getAnnotation(BlocklyInfo.class);
        String blockName = info.name().isEmpty() ?
                triggerClass.getSimpleName().toLowerCase().replace("trigger", "_trigger") :
                info.name();

        List<BlocklyFieldExtractor.BlocklyFieldInfo> fields = BlocklyFieldExtractor.extractFields(triggerClass);

        js.append("Blockly.Blocks['").append(blockName).append("'] = {\n");
        js.append("    init: function() {\n");

        // Titre du bloc
        js.append("        this.appendDummyInput()\n");
        js.append("            .appendField(\"").append(escapeJavaScript(info.displayText())).append("\");\n");

        // Générer les champs
        for (BlocklyFieldExtractor.BlocklyFieldInfo field : fields) {
            generateField(js, field);
        }

        // Input pour les actions si supporté
        try {
            BlocklyTrigger instance = triggerClass.getDeclaredConstructor().newInstance();
            if (instance.hasActions()) {
                js.append("        this.appendStatementInput(\"ACTIONS\")\n");
                js.append("            .setCheck(\"Action\")\n");
                js.append("            .appendField(\"Faire:\");\n");
            }
        } catch (Exception e) {
            // Fallback : ajouter les actions par défaut
            js.append("        this.appendStatementInput(\"ACTIONS\")\n");
            js.append("            .setCheck(\"Action\")\n");
            js.append("            .appendField(\"Faire:\");\n");
        }

        js.append("        this.setColour('").append(info.color()).append("');\n");
        if (!info.tooltip().isEmpty()) {
            js.append("        this.setTooltip(\"").append(escapeJavaScript(info.tooltip())).append("\");\n");
        }

        js.append("    }\n");
        js.append("};\n\n");
    }

    private void generateActionBlocks(StringBuilder js) {
        js.append("// ===== BLOCS ACTIONS (AUTO-GÉNÉRÉS) =====\n");

        for (Map.Entry<String, List<Class<? extends BlocklyAction>>> entry : actionsByCategory.entrySet()) {
            js.append("// Catégorie: ").append(entry.getKey()).append("\n");

            for (Class<? extends BlocklyAction> actionClass : entry.getValue()) {
                generateActionBlock(js, actionClass);
            }
        }
        js.append("\n");
    }

    private void generateActionBlock(StringBuilder js, Class<? extends BlocklyAction> actionClass) {
        BlocklyInfo info = actionClass.getAnnotation(BlocklyInfo.class);
        String blockName = info.name().isEmpty() ?
                actionClass.getSimpleName().toLowerCase().replace("action", "_action") :
                info.name();

        List<BlocklyFieldExtractor.BlocklyFieldInfo> fields = BlocklyFieldExtractor.extractFields(actionClass);

        js.append("Blockly.Blocks['").append(blockName).append("'] = {\n");
        js.append("    init: function() {\n");

        // Titre du bloc
        js.append("        this.appendDummyInput()\n");
        js.append("            .appendField(\"").append(escapeJavaScript(info.displayText())).append("\");\n");

        // Générer les champs
        for (BlocklyFieldExtractor.BlocklyFieldInfo field : fields) {
            generateField(js, field);
        }

        // Connexions pour chaînage
        try {
            BlocklyAction instance = actionClass.getDeclaredConstructor().newInstance();
            if (instance.isChainable()) {
                js.append("        this.setPreviousStatement(true, \"Action\");\n");
                js.append("        this.setNextStatement(true, \"Action\");\n");
            }
        } catch (Exception e) {
            // Fallback : chaînable par défaut
            js.append("        this.setPreviousStatement(true, \"Action\");\n");
            js.append("        this.setNextStatement(true, \"Action\");\n");
        }

        js.append("        this.setColour('").append(info.color()).append("');\n");
        if (!info.tooltip().isEmpty()) {
            js.append("        this.setTooltip(\"").append(escapeJavaScript(info.tooltip())).append("\");\n");
        }

        js.append("    }\n");
        js.append("};\n\n");
    }

    private void generateField(StringBuilder js, BlocklyFieldExtractor.BlocklyFieldInfo field) {
        js.append("        this.appendDummyInput()\n");

        if (!field.label().isEmpty()) {
            js.append("            .appendField(\"").append(escapeJavaScript(field.label())).append("\")\n");
        }

        switch (field.type()) {
            case TEXT_INPUT:
                js.append("            .appendField(new Blockly.FieldTextInput(\"")
                        .append(escapeJavaScript(field.defaultValue()))
                        .append("\"), \"").append(field.fieldName().toUpperCase()).append("\");\n");
                break;

            case NUMBER_INPUT:
                js.append("            .appendField(new Blockly.FieldNumber(")
                        .append(field.defaultValue().isEmpty() ? "0" : field.defaultValue());
                if (field.min() != Double.MIN_VALUE) {
                    js.append(", ").append(field.min());
                }
                if (field.max() != Double.MAX_VALUE) {
                    js.append(", ").append(field.max());
                }
                js.append("), \"").append(field.fieldName().toUpperCase()).append("\");\n");
                break;

            case DROPDOWN:
                js.append("            .appendField(new Blockly.FieldDropdown([");
                String[] options = field.options().split(",");
                for (int i = 0; i < options.length; i++) {
                    if (i > 0) js.append(", ");
                    js.append("[\"").append(options[i].trim()).append("\", \"").append(options[i].trim()).append("\"]");
                }
                js.append("]), \"").append(field.fieldName().toUpperCase()).append("\");\n");
                break;

            case BOOLEAN_INPUT:
                js.append("        this.appendValueInput(\"").append(field.fieldName().toUpperCase()).append("\")\n");
                js.append("            .setCheck(\"Boolean\")");
                if (!field.label().isEmpty()) {
                    js.append("\n            .appendField(\"").append(escapeJavaScript(field.label())).append("\")");
                }
                js.append(";\n");
                return; // Pas besoin de fermer avec appendDummyInput

            case COLOR_INPUT:
                js.append("            .appendField(new Blockly.FieldColour(\"")
                        .append(field.defaultValue().isEmpty() ? "#ff0000" : field.defaultValue())
                        .append("\"), \"").append(field.fieldName().toUpperCase()).append("\");\n");
                break;
        }
    }

    private void generateUtilityBlocks(StringBuilder js) {
        js.append("// ===== BLOCS UTILITAIRES =====\n");

        // Boolean blocks
        js.append("""
        Blockly.Blocks['boolean_true'] = {
            init: function() {
                this.appendDummyInput()
                    .appendField("✅ Vrai");
                this.setOutput(true, "Boolean");
                this.setColour('#4CAF50');
            }
        };
        
        Blockly.Blocks['boolean_false'] = {
            init: function() {
                this.appendDummyInput()
                    .appendField("❌ Faux");
                this.setOutput(true, "Boolean");
                this.setColour('#F44336');
            }
        };
        
        """);
    }

    private void generateToolbox(StringBuilder js) {
        js.append("// ===== TOOLBOX AUTO-GÉNÉRÉE =====\n");
        js.append("window.DUNGEON_TOOLBOX = {\n");
        js.append("    \"kind\": \"categoryToolbox\",\n");
        js.append("    \"contents\": [\n");

        // Catégories de triggers
        for (Map.Entry<String, List<Class<? extends BlocklyTrigger>>> entry : triggersByCategory.entrySet()) {
            js.append("        {\n");
            js.append("            \"kind\": \"category\",\n");
            js.append("            \"name\": \"🎯 ").append(entry.getKey()).append("\",\n");
            js.append("            \"colour\": \"#FF6B6B\",\n");
            js.append("            \"contents\": [\n");

            for (Class<? extends BlocklyTrigger> triggerClass : entry.getValue()) {
                BlocklyInfo info = triggerClass.getAnnotation(BlocklyInfo.class);
                String blockName = info.name().isEmpty() ?
                        triggerClass.getSimpleName().toLowerCase().replace("trigger", "_trigger") :
                        info.name();
                js.append("                {\"kind\": \"block\", \"type\": \"").append(blockName).append("\"},\n");
            }

            js.append("            ]\n");
            js.append("        },\n");
        }

        // Catégories d'actions
        for (Map.Entry<String, List<Class<? extends BlocklyAction>>> entry : actionsByCategory.entrySet()) {
            js.append("        {\n");
            js.append("            \"kind\": \"category\",\n");
            js.append("            \"name\": \"⚡ ").append(entry.getKey()).append("\",\n");
            js.append("            \"colour\": \"#2196F3\",\n");
            js.append("            \"contents\": [\n");

            for (Class<? extends BlocklyAction> actionClass : entry.getValue()) {
                BlocklyInfo info = actionClass.getAnnotation(BlocklyInfo.class);
                String blockName = info.name().isEmpty() ?
                        actionClass.getSimpleName().toLowerCase().replace("action", "_action") :
                        info.name();
                js.append("                {\"kind\": \"block\", \"type\": \"").append(blockName).append("\"},\n");
            }

            js.append("            ]\n");
            js.append("        },\n");
        }

        // Catégorie utilitaires
        js.append("""
                {
                    "kind": "category",
                    "name": "🔧 Utilitaires",
                    "colour": "#9E9E9E",
                    "contents": [
                        {"kind": "block", "type": "boolean_true"},
                        {"kind": "block", "type": "boolean_false"}
                    ]
                }
            ]
        };
        
        console.log('📦 Toolbox auto-générée:', window.DUNGEON_TOOLBOX);
        
        """);
    }

    private void generateUtilityFunctions(StringBuilder js) {
        // Ici vous pourrez ajouter vos fonctions utilitaires existantes
        js.append("// ===== FONCTIONS UTILITAIRES =====\n");
        // ... votre code existant pour generateTriggersFromWorkspace, etc.
    }

    private String escapeJavaScript(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
