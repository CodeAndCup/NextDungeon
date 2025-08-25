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
            case CHECKBOX:
                js.append("            .appendField(new Blockly.FieldCheckbox(")
                        .append(field.defaultValue().equalsIgnoreCase("true") ? "true" : "false")
                        .append("), \"").append(field.fieldName().toUpperCase()).append("\");\n");
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
        js.append("// ===== FONCTIONS UTILITAIRES AUTO-GÉNÉRÉES =====\n");

        js.append("""
        // Génération des triggers depuis l'espace de travail
        function generateTriggersFromWorkspace() {
            console.log('🔄 Génération des triggers...');
            const triggers = [];
            const blocks = workspace.getTopBlocks();
            
            blocks.forEach(block => {
                console.log('Bloc trouvé:', block.type);
                
        """);

        // Générer dynamiquement les cas pour chaque type de trigger
        for (Map.Entry<String, List<Class<? extends BlocklyTrigger>>> entry : triggersByCategory.entrySet()) {
            for (Class<? extends BlocklyTrigger> triggerClass : entry.getValue()) {
                generateTriggerCase(js, triggerClass);
            }
        }

        js.append("""
            });
            
            console.log('Triggers générés:', triggers);
            return triggers;
        }
        
        // Extraction des actions d'un bloc
        function getActionsFromBlock(block) {
            const actions = [];
            let actionBlock = block.getInputTargetBlock('ACTIONS');
            
            while (actionBlock) {
                console.log('Action trouvée:', actionBlock.type);
                
        """);

        // Générer dynamiquement les cas pour chaque type d'action
        for (Map.Entry<String, List<Class<? extends BlocklyAction>>> entry : actionsByCategory.entrySet()) {
            for (Class<? extends BlocklyAction> actionClass : entry.getValue()) {
                generateActionCase(js, actionClass);
            }
        }

        js.append("""
                actionBlock = actionBlock.getNextBlock();
            }
            
            return actions;
        }
        
        // Fonction pour charger les triggers dans l'espace de travail
        function loadTriggersIntoWorkspace(triggersData) {
            console.log('🔄 Chargement des triggers dans l\\'espace de travail...');
            workspace.clear();
            
            if (!triggersData || !triggersData.triggers) {
                console.log('Aucun trigger à charger');
                return;
            }
            
            triggersData.triggers.forEach((trigger, index) => {
                console.log('Chargement du trigger:', trigger);
                
        """);

        // Générer dynamiquement les cas de chargement pour chaque trigger
        for (Map.Entry<String, List<Class<? extends BlocklyTrigger>>> entry : triggersByCategory.entrySet()) {
            for (Class<? extends BlocklyTrigger> triggerClass : entry.getValue()) {
                generateTriggerLoadingCase(js, triggerClass);
            }
        }

        js.append("""
            });
            
            console.log('✅ Triggers chargés dans l\\'espace de travail');
        }
        
        // Fonction pour charger les actions dans un bloc
        function loadActionsIntoBlock(triggerBlock, actions) {
            if (!actions || actions.length === 0) return;
            
            let previousActionBlock = null;
            const actionsInput = triggerBlock.getInput('ACTIONS');
            
            actions.forEach((action, index) => {
        """);

        // Générer dynamiquement les cas de chargement pour chaque action
        for (Map.Entry<String, List<Class<? extends BlocklyAction>>> entry : actionsByCategory.entrySet()) {
            for (Class<? extends BlocklyAction> actionClass : entry.getValue()) {
                generateActionLoadingCase(js, actionClass);
            }
        }

        js.append("""
            });
        }
        
        console.log('✅ Fonctions utilitaires auto-générées chargées');
        
        """);
    }

    private void generateTriggerCase(StringBuilder js, Class<? extends BlocklyTrigger> triggerClass) {
        BlocklyInfo info = triggerClass.getAnnotation(BlocklyInfo.class);
        String blockName = info.name().isEmpty() ?
                triggerClass.getSimpleName().toLowerCase().replace("trigger", "_trigger") :
                info.name();

        String triggerType = triggerClass.getSimpleName().toLowerCase().replace("trigger", "");

        List<BlocklyFieldExtractor.BlocklyFieldInfo> fields = BlocklyFieldExtractor.extractFields(triggerClass);

        js.append("                if (block.type === '").append(blockName).append("') {\n");
        js.append("                    triggers.push({\n");
        js.append("                        type: '").append(triggerType).append("',\n");
        js.append("                        name: '").append(triggerClass.getSimpleName()).append("_' + Date.now(),\n");

        // Générer les champs dynamiquement
        for (BlocklyFieldExtractor.BlocklyFieldInfo field : fields) {
            js.append("                        ");
            generateFieldExtraction(js, field,"block");
            js.append(",\n");
        }

        // Ajouter les actions si le trigger les supporte
        try {
            BlocklyTrigger instance = triggerClass.getDeclaredConstructor().newInstance();
            if (instance.hasActions()) {
                js.append("                        actions: getActionsFromBlock(block)\n");
            } else {
                js.append("                        actions: []\n");
            }
        } catch (Exception e) {
            js.append("                        actions: getActionsFromBlock(block)\n");
        }

        js.append("                    });\n");
        js.append("                }\n");
    }

    private void generateActionCase(StringBuilder js, Class<? extends BlocklyAction> actionClass) {
        BlocklyInfo info = actionClass.getAnnotation(BlocklyInfo.class);
        String blockName = info.name().isEmpty() ?
                actionClass.getSimpleName().toLowerCase().replace("action", "_action") :
                info.name();

        String actionType = actionClass.getSimpleName().toLowerCase().replace("action", "");

        List<BlocklyFieldExtractor.BlocklyFieldInfo> fields = BlocklyFieldExtractor.extractFields(actionClass);

        js.append("                if (actionBlock.type === '").append(blockName).append("') {\n");
        js.append("                    actions.push({\n");
        js.append("                        type: '").append(actionType).append("'");

        // Générer les champs dynamiquement
        for (BlocklyFieldExtractor.BlocklyFieldInfo field : fields) {
            js.append(",\n                        ");
            generateFieldExtraction(js, field,"actionBlock");
        }

        js.append("\n                    });\n");
        js.append("                }\n");
    }

    private void generateTriggerLoadingCase(StringBuilder js, Class<? extends BlocklyTrigger> triggerClass) {
        BlocklyInfo info = triggerClass.getAnnotation(BlocklyInfo.class);
        String blockName = info.name().isEmpty() ?
                triggerClass.getSimpleName().toLowerCase().replace("trigger", "_trigger") :
                info.name();

        String triggerType = triggerClass.getSimpleName().toLowerCase().replace("trigger", "");

        List<BlocklyFieldExtractor.BlocklyFieldInfo> fields = BlocklyFieldExtractor.extractFields(triggerClass);

        js.append("                if (trigger.type === '").append(triggerType).append("') {\n");
        js.append("                    const block = workspace.newBlock('").append(blockName).append("');\n");

        // Générer le chargement des champs dynamiquement
        for (BlocklyFieldExtractor.BlocklyFieldInfo field : fields) {
            generateFieldLoading(js, field);
        }

        js.append("                    \n");
        js.append("                    // Charger les actions\n");
        js.append("                    loadActionsIntoBlock(block, trigger.actions);\n");
        js.append("                    \n");
        js.append("                    block.initSvg();\n");
        js.append("                    block.render();\n");
        js.append("                    block.moveBy(20 + (index * 300), 20);\n");
        js.append("                }\n");
    }

    private void generateActionLoadingCase(StringBuilder js, Class<? extends BlocklyAction> actionClass) {
        BlocklyInfo info = actionClass.getAnnotation(BlocklyInfo.class);
        String blockName = info.name().isEmpty() ?
                actionClass.getSimpleName().toLowerCase().replace("action", "_action") :
                info.name();

        String actionType = actionClass.getSimpleName().toLowerCase().replace("action", "");

        List<BlocklyFieldExtractor.BlocklyFieldInfo> fields = BlocklyFieldExtractor.extractFields(actionClass);

        js.append("                if (action.type === '").append(actionType).append("') {\n");
        js.append("                    const actionBlock = workspace.newBlock('").append(blockName).append("');\n");

        // Générer le chargement des champs dynamiquement
        for (BlocklyFieldExtractor.BlocklyFieldInfo field : fields) {
            generateFieldLoading(js, field);
        }

        js.append("                    \n");
        js.append("                    actionBlock.initSvg();\n");
        js.append("                    actionBlock.render();\n");
        js.append("                    \n");
        js.append("                    if (index === 0) {\n");
        js.append("                        // Premier bloc d'action, connecter au trigger\n");
        js.append("                        actionsInput.connection.connect(actionBlock.previousConnection);\n");
        js.append("                    } else {\n");
        js.append("                        // Blocs suivants, connecter au bloc précédent\n");
        js.append("                        if (previousActionBlock) {\n");
        js.append("                            previousActionBlock.nextConnection.connect(actionBlock.previousConnection);\n");
        js.append("                        }\n");
        js.append("                    }\n");
        js.append("                    \n");
        js.append("                    previousActionBlock = actionBlock;\n");
        js.append("                }\n");
    }

    private void generateFieldExtraction(StringBuilder js, BlocklyFieldExtractor.BlocklyFieldInfo field, String blockVariable) {
        String fieldName = field.fieldName().toUpperCase();

        switch (field.type()) {
            case TEXT_INPUT:
                js.append(field.fieldName().toLowerCase())
                        .append(": ").append(blockVariable).append(".getFieldValue('").append(fieldName).append("') || '")
                        .append(escapeJavaScript(field.defaultValue())).append("'");
                break;

            case NUMBER_INPUT:
                js.append(field.fieldName().toLowerCase())
                        .append(": parseFloat(").append(blockVariable).append(".getFieldValue('").append(fieldName).append("')) || ")
                        .append(field.defaultValue().isEmpty() ? "0" : field.defaultValue());
                break;

            case DROPDOWN:
                js.append(field.fieldName().toLowerCase())
                        .append(": ").append(blockVariable).append(".getFieldValue('").append(fieldName).append("') || '")
                        .append(field.options().split(",")[0].trim()).append("'");
                break;

            case BOOLEAN_INPUT:
                js.append(field.fieldName().toLowerCase())
                        .append(": (() => {\n");
                js.append("                            const boolBlock = ").append(blockVariable).append(".getInputTargetBlock('").append(fieldName).append("');\n");
                js.append("                            return boolBlock ? boolBlock.type === 'boolean_true' : ")
                        .append(field.defaultValue().equals("true") ? "true" : "false").append(";\n");
                js.append("                        })()");
                break;

            case COLOR_INPUT:
                js.append(field.fieldName().toLowerCase())
                        .append(": ").append(blockVariable).append(".getFieldValue('").append(fieldName).append("') || '")
                        .append(field.defaultValue().isEmpty() ? "#ff0000" : field.defaultValue()).append("'");
                break;
            case CHECKBOX:
                js.append(field.fieldName().toLowerCase())
                        .append(": ").append(blockVariable).append(".getFieldValue('").append(fieldName).append("') === 'true'");
                break;
        }
    }

    private void generateFieldLoading(StringBuilder js, BlocklyFieldExtractor.BlocklyFieldInfo field) {
        String fieldName = field.fieldName().toUpperCase();
        String objectField = field.fieldName().toLowerCase();

        switch (field.type()) {
            case TEXT_INPUT:
            case DROPDOWN:
            case COLOR_INPUT:
                js.append("                    block.setFieldValue((trigger.").append(objectField)
                        .append(" || '").append(escapeJavaScript(field.defaultValue()))
                        .append("').toString(), '").append(fieldName).append("');\n");
                break;

            case NUMBER_INPUT:
                js.append("                    block.setFieldValue((trigger.").append(objectField)
                        .append(" || ").append(field.defaultValue().isEmpty() ? "0" : field.defaultValue())
                        .append(").toString(), '").append(fieldName).append("');\n");
                break;

            case BOOLEAN_INPUT:
                js.append("                    if (trigger.").append(objectField).append(" !== undefined) {\n");
                js.append("                        const boolBlock = workspace.newBlock(trigger.")
                        .append(objectField).append(" ? 'boolean_true' : 'boolean_false');\n");
                js.append("                        boolBlock.initSvg();\n");
                js.append("                        boolBlock.render();\n");
                js.append("                        block.getInput('").append(fieldName)
                        .append("').connection.connect(boolBlock.outputConnection);\n");
                js.append("                    }\n");
                break;
            case CHECKBOX:
                js.append("                    block.setFieldValue(trigger.").append(objectField)
                        .append(" ? 'true' : 'false', '").append(fieldName).append("');\n");
                break;
        }
    }

    private String escapeJavaScript(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}