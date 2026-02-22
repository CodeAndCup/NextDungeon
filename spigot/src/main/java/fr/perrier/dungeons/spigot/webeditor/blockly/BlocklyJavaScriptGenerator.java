package fr.perrier.dungeons.spigot.webeditor.blockly;

import fr.perrier.dungeons.common.module.ModuleBlockDescriptor;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.module.ModuleLoader;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyInfo;
import org.bukkit.entity.Player;
import org.reflections.Reflections;

import java.util.*;

public class BlocklyJavaScriptGenerator {

    private final Map<String, List<Class<? extends BlocklyTrigger>>> triggersByCategory = new HashMap<>();
    private final Map<String, List<Class<? extends BlocklyAction>>> actionsByCategory = new HashMap<>();

    public BlocklyJavaScriptGenerator() {
        scanForComponents();
    }

    /**
     * Scanne les packages pour trouver les triggers et actions annotés avec @BlocklyInfo
     * et les organise par catégorie
     */
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

    /**
     * Génère le code JavaScript pour définir les blocs Blockly, la toolbox et les fonctions utilitaires
     *
     * @return Le code JavaScript généré
     */
    public String generateJavaScript(Player editor) {
        StringBuilder js = new StringBuilder();

        js.append("""
                console.log(`
                ===== ÉDITEUR DE TRIGGERS DUNGEONS =====
                🎯 Système de génération automatique activé
                👤 Utilisateur:\s""").append(editor.getName()).append("""
                                
                📅 ${new Date().toLocaleString()}
                ==========================================
                `);
                """);

        js.append("// Auto-généré par BlocklyJavaScriptGenerator\n");
        js.append("console.log('🔧 Chargement automatique des blocs Blockly...');\n\n");

        // Générer les blocs triggers
        generateTriggerBlocks(js);

        // Générer les blocs actions
        generateActionBlocks(js);

        // Générer les blocs fonctions
        generateFunctionBlocks(js);

        // Générer les blocs utilitaires
        generateUtilityBlocks(js);

        // Générer les blocs variables
        generateVariableBlocks(js);

        // Générer les blocs dynamiques des modules
        generateModuleBlocks(js);

        // Générer la toolbox
        generateToolbox(js);

        // Générer les fonctions utilitaires
        generateUtilityFunctions(js);

        js.append("console.log('✅ Tous les blocs ont été générés automatiquement!');\n");

        return js.toString();
    }

    /**
     * Génère les blocs pour tous les triggers
     *
     * @param js Le StringBuilder pour accumuler le code JavaScript
     */
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

    /**
     * Génère le code JavaScript pour un bloc de trigger spécifique
     *
     * @param js           Le StringBuilder pour accumuler le code JavaScript
     * @param triggerClass La classe du trigger à générer
     */
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

    /**
     * Génère les blocs pour toutes les actions
     *
     * @param js Le StringBuilder pour accumuler le code JavaScript
     */
    private void generateActionBlocks(StringBuilder js) {
        js.append("// ===== BLOCS ACTIONS (AUTO-GÉNÉRÉS) =====\n");

        for (Map.Entry<String, List<Class<? extends BlocklyAction>>> entry : actionsByCategory.entrySet()) {
            js.append("// Catégorie: ").append(entry.getKey()).append("\n");
            for (Class<? extends BlocklyAction> actionClass : entry.getValue()) {
                try {
                    BlocklyAction instance = actionClass.getDeclaredConstructor().newInstance();
                    if (instance.requiresCustomBlockGeneration()) {
                        instance.generateCustomBlock(js);
                    } else {
                        generateActionBlock(js, actionClass);
                    }
                } catch (Exception e) {
                    generateActionBlock(js, actionClass);
                }
            }
        }
        js.append("\n");
    }

    /**
     * Génère le code JavaScript pour un bloc d'action spécifique
     *
     * @param js          Le StringBuilder pour accumuler le code JavaScript
     * @param actionClass La classe de l'action à générer
     */
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

    /**
     * Génère le code JavaScript pour un champ spécifique d'un bloc Blockly.
     *
     * @param js    Le `StringBuilder` utilisé pour accumuler le code JavaScript généré.
     * @param field Une instance de `BlocklyFieldExtractor.BlocklyFieldInfo` contenant les informations
     *              sur le champ à générer (type, label, valeur par défaut, etc.).
     */
    private void generateField(StringBuilder js, BlocklyFieldExtractor.BlocklyFieldInfo field) {
        // Ajoute une entrée "dummy" pour le champ
        js.append("        this.appendDummyInput()\n");

        // Si le champ a un label, l'ajouter au bloc
        if (!field.label().isEmpty()) {
            js.append("            .appendField(\"").append(escapeJavaScript(field.label())).append("\")\n");
        }

        // Génère le champ en fonction de son type
        switch (field.type()) {
            // Champ de type texte avec une valeur par défaut
            case TEXT_INPUT:
                js.append("            .appendField(new Blockly.FieldTextInput(\"")
                        .append(escapeJavaScript(field.defaultValue()))
                        .append("\"), \"").append(field.fieldName().toUpperCase()).append("\");\n");
                break;

            case NUMBER_INPUT:
                // Champ de type nombre avec des bornes optionnelles
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
                // Champ de type menu déroulant avec des options
                js.append("            .appendField(new Blockly.FieldDropdown([");
                String[] options = field.options().split(",");
                for (int i = 0; i < options.length; i++) {
                    if (i > 0) js.append(", ");
                    js.append("[\"").append(options[i].trim()).append("\", \"").append(options[i].trim()).append("\"]");
                }
                js.append("]), \"").append(field.fieldName().toUpperCase()).append("\");\n");
                break;

            case BOOLEAN_INPUT:
                // Champ de type booléen (vrai/faux) avec une connexion à un autre bloc
                if (!field.label().isEmpty()) {
                    js.append("        this.appendDummyInput()\n");
                    js.append("            .appendField(\"").append(escapeJavaScript(field.label())).append("\");\n");
                }
                js.append("        this.appendValueInput(\"").append(field.fieldName().toUpperCase()).append("\")\n");
                js.append("            .setCheck(\"Boolean\");\n");
                return; // Pas besoin de fermer avec appendDummyInput

            case COLOR_INPUT:
                // Champ de type couleur avec une valeur par défaut
                js.append("            .appendField(new Blockly.FieldColour(\"")
                        .append(field.defaultValue().isEmpty() ? "#ff0000" : field.defaultValue())
                        .append("\"), \"").append(field.fieldName().toUpperCase()).append("\");\n");
                break;
            case CHECKBOX:
                // Champ de type case à cocher avec une valeur par défaut
                js.append("            .appendField(new Blockly.FieldCheckbox(")
                        .append(field.defaultValue().equalsIgnoreCase("true") ? "true" : "false")
                        .append("), \"").append(field.fieldName().toUpperCase()).append("\");\n");
                break;
            case LOCATION_INPUT:
                // Champ de type location avec connexion à un bloc de location
                if (!field.label().isEmpty()) {
                    js.append("        this.appendDummyInput()\n");
                    js.append("            .appendField(\"").append(escapeJavaScript(field.label())).append("\");\n");
                }
                js.append("        this.appendValueInput(\"").append(field.fieldName().toUpperCase()).append("\")\n");
                js.append("            .setCheck(\"Location\");\n");
                return; // Pas besoin de fermer avec appendDummyInput
        }
    }

    private void generateFunctionBlocks(StringBuilder js) {
        js.append("// ===== BLOCS USER FUNCTIONS (AUTO-GÉNÉRÉS) =====\n");

        // Function Definition Block
        js.append("""
                Blockly.Blocks['function_trigger'] = {
                    init: function() {
                        this.appendDummyInput()
                            .appendField("🔧 Fonction")
                            .appendField(new Blockly.FieldTextInput("ma_fonction"), "FUNCTIONNAME");
                        this.appendStatementInput("ACTIONS")
                            .setCheck("Action")
                            .appendField("Faire:");
                        this.setColour('#673AB7');
                        this.setTooltip("Définit une fonction personnalisée réutilisable");
                    }
                };
                        
                Blockly.Blocks['call_function_action'] = {
                    init: function() {
                        this.appendDummyInput()
                            .appendField("📞 Appeler")
                            .appendField(new Blockly.FieldTextInput("ma_fonction"), "FUNCTIONNAME");
                        this.setPreviousStatement(true, "Action");
                        this.setNextStatement(true, "Action");
                        this.setColour('#9C27B0');
                        this.setTooltip("Appelle une fonction définie précédemment");
                    }
                };
                        
                """);
    }

    /**
     * Génère les blocs utilitaires pour Blockly, tels que les blocs booléens "Vrai" et "Faux".
     *
     * @param js Le `StringBuilder` utilisé pour accumuler le code JavaScript généré.
     */
    private void generateUtilityBlocks(StringBuilder js) {
        // Ajoute un commentaire pour indiquer le début des blocs utilitaires
        js.append("// ===== BLOCS UTILITAIRES =====\n");

        // Génère le bloc booléen "Vrai"
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
                
                Blockly.Blocks['text'] = {
                    init: function() {
                        this.appendDummyInput().appendField(new Blockly.FieldTextInput(""), "TEXT");
                        this.setOutput(true, null);
                        this.setColour('#5C6BC0');
                        this.setTooltip('Bloc de texte pour valeurs');
                    }
                };
                
                // Location Blocks
                Blockly.Blocks['location_xyz'] = {
                    init: function() {
                        this.appendDummyInput()
                            .appendField("📍 Position");
                        this.appendDummyInput()
                            .appendField("X:")
                            .appendField(new Blockly.FieldNumber(0), "X");
                        this.appendDummyInput()
                            .appendField("Y:")
                            .appendField(new Blockly.FieldNumber(64), "Y");
                        this.appendDummyInput()
                            .appendField("Z:")
                            .appendField(new Blockly.FieldNumber(0), "Z");
                        this.setOutput(true, "Location");
                        this.setColour('#FF9800');
                        this.setTooltip('Définit une position avec coordonnées X, Y, Z');
                    }
                };
                
                Blockly.Blocks['location_xyz_world'] = {
                    init: function() {
                        this.appendDummyInput()
                            .appendField("🌍 Position + Monde");
                        this.appendDummyInput()
                            .appendField("X:")
                            .appendField(new Blockly.FieldNumber(0), "X");
                        this.appendDummyInput()
                            .appendField("Y:")
                            .appendField(new Blockly.FieldNumber(64), "Y");
                        this.appendDummyInput()
                            .appendField("Z:")
                            .appendField(new Blockly.FieldNumber(0), "Z");
                        this.appendDummyInput()
                            .appendField("Monde:")
                            .appendField(new Blockly.FieldTextInput("world"), "WORLD");
                        this.setOutput(true, "Location");
                        this.setColour('#FF9800');
                        this.setTooltip('Définit une position avec coordonnées et monde');
                    }
                };
                
                Blockly.Blocks['location_full'] = {
                    init: function() {
                        this.appendDummyInput()
                            .appendField("🎯 Position Complète");
                        this.appendDummyInput()
                            .appendField("X:")
                            .appendField(new Blockly.FieldNumber(0), "X");
                        this.appendDummyInput()
                            .appendField("Y:")
                            .appendField(new Blockly.FieldNumber(64), "Y");
                        this.appendDummyInput()
                            .appendField("Z:")
                            .appendField(new Blockly.FieldNumber(0), "Z");
                        this.appendDummyInput()
                            .appendField("Monde:")
                            .appendField(new Blockly.FieldTextInput("world"), "WORLD");
                        this.appendDummyInput()
                            .appendField("Yaw:")
                            .appendField(new Blockly.FieldNumber(0), "YAW");
                        this.appendDummyInput()
                            .appendField("Pitch:")
                            .appendField(new Blockly.FieldNumber(0), "PITCH");
                        this.setOutput(true, "Location");
                        this.setColour('#FF9800');
                        this.setTooltip('Définit une position avec coordonnées, monde et rotation');
                    }
                };
                        
                """);
    }

    /**
     * Génère les blocs pour la gestion des variables dans Blockly.
     * Inclut les blocs pour définir et récupérer des variables avec différentes portées.
     *
     * @param js Le `StringBuilder` utilisé pour accumuler le code JavaScript généré.
     */
    private void generateVariableBlocks(StringBuilder js) {
        js.append("// ===== BLOCS VARIABLES (AUTO-GÉNÉRÉS) =====\n");

        js.append("""
                Blockly.Blocks['get_variable'] = {
                    init: function() {
                        this.appendDummyInput()
                            .appendField("📊 Obtenir variable")
                            .appendField(new Blockly.FieldTextInput("{global.ma_variable}"), "TEXT");
                        this.setOutput(true, null);
                        this.setColour('#5C6BC0');
                        this.setTooltip('Bloc pour obtenir la valeur d\\'une variable (ex: {global.ma_variable}, {player.ma_variable})');
                    }
                };
                """);
    }

    /**
     * Generates Blockly block definitions for all blocks registered by dynamic modules.
     */
    private void generateModuleBlocks(StringBuilder js) {
        ModuleLoader moduleLoader = Main.getInstance().getModuleLoader();
        if (moduleLoader == null) return;

        List<ModuleBlockDescriptor> allBlocks = moduleLoader.getBlockRegistry().getAllBlocks();
        if (allBlocks.isEmpty()) return;

        js.append("// ===== BLOCS MODULES DYNAMIQUES =====\n");

        for (ModuleBlockDescriptor descriptor : allBlocks) {
            String blockName = descriptor.getId().replace('.', '_');
            String color = descriptor.getColor() != null ? descriptor.getColor() : "#9C27B0";

            js.append("Blockly.Blocks['").append(blockName).append("'] = {\n");
            js.append("    init: function() {\n");
            js.append("        this.appendDummyInput()\n");
            js.append("            .appendField(\"").append(escapeJavaScript(descriptor.getLabel())).append("\");\n");

            // Generate fields from parameters
            if (descriptor.getParameters() != null) {
                for (ModuleBlockDescriptor.BlockParameter param : descriptor.getParameters()) {
                    String fieldLabel = param.getLabel() != null ? param.getLabel() : param.getName();
                    String defaultVal = param.getDefaultValue() != null ? param.getDefaultValue() : "";

                    switch (param.getType() != null ? param.getType() : "string") {
                        case "number" -> {
                            js.append("        this.appendDummyInput()\n");
                            js.append("            .appendField(\"").append(escapeJavaScript(fieldLabel)).append("\")\n");
                            js.append("            .appendField(new Blockly.FieldNumber(")
                                    .append(defaultVal.isEmpty() ? "0" : defaultVal).append("), '")
                                    .append(param.getName()).append("');\n");
                        }
                        case "boolean" -> {
                            js.append("        this.appendDummyInput()\n");
                            js.append("            .appendField(\"").append(escapeJavaScript(fieldLabel)).append("\")\n");
                            js.append("            .appendField(new Blockly.FieldCheckbox('")
                                    .append("true".equalsIgnoreCase(defaultVal) ? "TRUE" : "FALSE").append("'), '")
                                    .append(param.getName()).append("');\n");
                        }
                        case "dropdown" -> {
                            js.append("        this.appendDummyInput()\n");
                            js.append("            .appendField(\"").append(escapeJavaScript(fieldLabel)).append("\")\n");
                            js.append("            .appendField(new Blockly.FieldDropdown([");
                            String options = param.getOptions() != null ? param.getOptions() : defaultVal;
                            if (!options.isEmpty()) {
                                String[] opts = options.split(",");
                                for (int i = 0; i < opts.length; i++) {
                                    if (i > 0) js.append(", ");
                                    js.append("[\"").append(escapeJavaScript(opts[i].trim())).append("\", \"")
                                            .append(escapeJavaScript(opts[i].trim())).append("\"]");
                                }
                            }
                            js.append("]), '").append(param.getName()).append("');\n");
                        }
                        default -> { // string
                            js.append("        this.appendDummyInput()\n");
                            js.append("            .appendField(\"").append(escapeJavaScript(fieldLabel)).append("\")\n");
                            js.append("            .appendField(new Blockly.FieldTextInput(\"")
                                    .append(escapeJavaScript(defaultVal)).append("\"), '")
                                    .append(param.getName()).append("');\n");
                        }
                    }
                }
            }

            // Actions can chain, triggers have statement inputs
            if (descriptor.getType() == ModuleBlockDescriptor.BlockType.ACTION) {
                js.append("        this.setPreviousStatement(true, \"Action\");\n");
                js.append("        this.setNextStatement(true, \"Action\");\n");
            } else if (descriptor.getType() == ModuleBlockDescriptor.BlockType.TRIGGER) {
                js.append("        this.appendStatementInput(\"ACTIONS\")\n");
                js.append("            .setCheck(\"Action\")\n");
                js.append("            .appendField(\"Faire:\");\n");
            }

            js.append("        this.setColour('").append(color).append("');\n");
            if (descriptor.getDescription() != null && !descriptor.getDescription().isEmpty()) {
                js.append("        this.setTooltip(\"").append(escapeJavaScript(descriptor.getDescription())).append("\");\n");
            }

            js.append("    }\n");
            js.append("};\n\n");
        }
    }

    /**
     * Generates toolbox categories for dynamic module blocks grouped by category.
     */
    private void generateModuleToolboxCategories(StringBuilder js) {
        ModuleLoader moduleLoader = Main.getInstance().getModuleLoader();
        if (moduleLoader == null) return;

        List<ModuleBlockDescriptor> allBlocks = moduleLoader.getBlockRegistry().getAllBlocks();
        if (allBlocks.isEmpty()) return;

        // Group blocks by category
        Map<String, List<ModuleBlockDescriptor>> byCategory = new LinkedHashMap<>();
        for (ModuleBlockDescriptor block : allBlocks) {
            String category = block.getCategory() != null ? block.getCategory() : block.getModuleId();
            byCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(block);
        }

        for (Map.Entry<String, List<ModuleBlockDescriptor>> entry : byCategory.entrySet()) {
            List<ModuleBlockDescriptor> categoryBlocks = entry.getValue();
            String catColor = categoryBlocks.get(0).getColor() != null ? categoryBlocks.get(0).getColor() : "#9C27B0";
            js.append("        {\n");
            js.append("            \"kind\": \"category\",\n");
            js.append("            \"name\": \"🧩 ").append(escapeJavaScript(entry.getKey())).append("\",\n");
            js.append("            \"colour\": \"").append(catColor).append("\",\n");
            js.append("            \"contents\": [\n");

            for (ModuleBlockDescriptor block : categoryBlocks) {
                String blockName = block.getId().replace('.', '_');
                js.append("                {\"kind\": \"block\", \"type\": \"").append(blockName).append("\"},\n");
            }

            js.append("            ]\n");
            js.append("        },\n");
        }
    }

    /**
     * Generates JavaScript extraction cases for dynamic module trigger blocks.
     * When a module trigger block is in the workspace, this extracts its fields
     * and creates a trigger object with the correct type and actions.
     */
    private void generateModuleTriggerCases(StringBuilder js) {
        ModuleLoader moduleLoader = Main.getInstance().getModuleLoader();
        if (moduleLoader == null) return;

        List<ModuleBlockDescriptor> allBlocks = moduleLoader.getBlockRegistry().getAllBlocks();
        for (ModuleBlockDescriptor descriptor : allBlocks) {
            if (descriptor.getType() != ModuleBlockDescriptor.BlockType.TRIGGER) continue;

            String blockName = descriptor.getId().replace('.', '_');
            js.append("                if (block.type === '").append(blockName).append("') {\n");
            js.append("                    triggers.push({\n");
            js.append("                        type: '").append(blockName).append("',\n");
            js.append("                        name: 'ModuleTrigger_' + uuidv4(),\n");

            if (descriptor.getParameters() != null) {
                for (ModuleBlockDescriptor.BlockParameter param : descriptor.getParameters()) {
                    String fieldName = param.getName();
                    switch (param.getType() != null ? param.getType() : "string") {
                        case "number" ->
                            js.append("                        ").append(fieldName).append(": Number(block.getFieldValue('").append(fieldName).append("')),\n");
                        case "boolean" ->
                            js.append("                        ").append(fieldName).append(": block.getFieldValue('").append(fieldName).append("') === 'TRUE',\n");
                        default ->
                            js.append("                        ").append(fieldName).append(": block.getFieldValue('").append(fieldName).append("'),\n");
                    }
                }
            }

            js.append("                        actions: getActionsFromBlock(block)\n");
            js.append("                    });\n");
            js.append("                }\n");
        }
    }

    /**
     * Generates JavaScript loading cases for dynamic module trigger blocks.
     * When loading saved triggers, this recreates module trigger blocks and
     * sets their field values from saved data.
     */
    private void generateModuleTriggerLoadingCases(StringBuilder js) {
        ModuleLoader moduleLoader = Main.getInstance().getModuleLoader();
        if (moduleLoader == null) return;

        List<ModuleBlockDescriptor> allBlocks = moduleLoader.getBlockRegistry().getAllBlocks();
        for (ModuleBlockDescriptor descriptor : allBlocks) {
            if (descriptor.getType() != ModuleBlockDescriptor.BlockType.TRIGGER) continue;

            String blockName = descriptor.getId().replace('.', '_');
            js.append("                if (trigger.type === '").append(blockName).append("') {\n");
            js.append("                    const triggerBlock = workspace.newBlock('").append(blockName).append("');\n");

            if (descriptor.getParameters() != null) {
                for (ModuleBlockDescriptor.BlockParameter param : descriptor.getParameters()) {
                    String fieldName = param.getName();
                    switch (param.getType() != null ? param.getType() : "string") {
                        case "boolean" ->
                            js.append("                    triggerBlock.setFieldValue(trigger.").append(fieldName)
                                    .append(" ? 'TRUE' : 'FALSE', '").append(fieldName).append("');\n");
                        default ->
                            js.append("                    if (trigger.").append(fieldName).append(" !== undefined) triggerBlock.setFieldValue(String(trigger.")
                                    .append(fieldName).append("), '").append(fieldName).append("');\n");
                    }
                }
            }

            js.append("                    triggerBlock.initSvg();\n");
            js.append("                    triggerBlock.render();\n");
            js.append("                    loadActionsIntoBlock(triggerBlock, trigger.actions);\n");
            js.append("                    triggerBlock.moveBy(20 + (index * 300), 20);\n");
            js.append("                }\n");
        }
    }

    /**
     * Generates JavaScript extraction cases for dynamic module action blocks.
     * When a module action block is encountered in the workspace, this extracts
     * all its field values and creates an action object with the correct type.
     */
    private void generateModuleActionCases(StringBuilder js) {
        ModuleLoader moduleLoader = Main.getInstance().getModuleLoader();
        if (moduleLoader == null) return;

        List<ModuleBlockDescriptor> allBlocks = moduleLoader.getBlockRegistry().getAllBlocks();
        for (ModuleBlockDescriptor descriptor : allBlocks) {
            if (descriptor.getType() != ModuleBlockDescriptor.BlockType.ACTION) continue;

            String blockName = descriptor.getId().replace('.', '_');
            js.append("                if (actionBlock.type === '").append(blockName).append("') {\n");
            js.append("                    actions.push({\n");
            js.append("                        type: '").append(blockName).append("'");

            if (descriptor.getParameters() != null) {
                for (ModuleBlockDescriptor.BlockParameter param : descriptor.getParameters()) {
                    js.append(",\n                        ");
                    String fieldName = param.getName();
                    switch (param.getType() != null ? param.getType() : "string") {
                        case "number" ->
                            js.append(fieldName).append(": Number(actionBlock.getFieldValue('").append(fieldName).append("'))");
                        case "boolean" ->
                            js.append(fieldName).append(": actionBlock.getFieldValue('").append(fieldName).append("') === 'TRUE'");
                        default ->
                            js.append(fieldName).append(": actionBlock.getFieldValue('").append(fieldName).append("')");
                    }
                }
            }

            js.append("\n                    });\n");
            js.append("                }\n");
        }
    }

    /**
     * Generates JavaScript loading cases for dynamic module action blocks.
     * When loading a saved workflow, this recreates module action blocks in the workspace
     * and sets their field values from the saved action data.
     */
    private void generateModuleActionLoadingCases(StringBuilder js) {
        ModuleLoader moduleLoader = Main.getInstance().getModuleLoader();
        if (moduleLoader == null) return;

        List<ModuleBlockDescriptor> allBlocks = moduleLoader.getBlockRegistry().getAllBlocks();
        for (ModuleBlockDescriptor descriptor : allBlocks) {
            if (descriptor.getType() != ModuleBlockDescriptor.BlockType.ACTION) continue;

            String blockName = descriptor.getId().replace('.', '_');
            js.append("                if (action.type === '").append(blockName).append("') {\n");
            js.append("                    actionBlock = workspace.newBlock('").append(blockName).append("');\n");

            if (descriptor.getParameters() != null) {
                for (ModuleBlockDescriptor.BlockParameter param : descriptor.getParameters()) {
                    String fieldName = param.getName();
                    switch (param.getType() != null ? param.getType() : "string") {
                        case "boolean" ->
                            js.append("                    actionBlock.setFieldValue(action.").append(fieldName)
                                    .append(" ? 'TRUE' : 'FALSE', '").append(fieldName).append("');\n");
                        default ->
                            js.append("                    if (action.").append(fieldName).append(" !== undefined) actionBlock.setFieldValue(String(action.")
                                    .append(fieldName).append("), '").append(fieldName).append("');\n");
                    }
                }
            }

            js.append("                }\n");
        }
    }

    /**
     * Génère la toolbox pour Blockly, qui contient les catégories et les blocs associés.
     *
     * @param js Le `StringBuilder` utilisé pour accumuler le code JavaScript généré.
     *           La toolbox est définie comme un objet JSON et inclut des catégories
     *           pour les triggers, les actions et les blocs utilitaires.
     */
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
            if(entry.getKey().equals("Actions")) {
                js.append("        {\n");
                js.append("            \"kind\": \"category\",\n");
                js.append("            \"name\": \"⚡ ").append(entry.getKey()).append("\",\n");
                js.append("            \"colour\": \"#2196F3\",\n");
                js.append("            \"contents\": [\n");
            } else if (entry.getKey().equals("Logic")) {
                js.append("        {\n");
                js.append("            \"kind\": \"category\",\n");
                js.append("            \"name\": \"🔀 ").append(entry.getKey()).append("\",\n");
                js.append("            \"colour\": \"#FF9800\",\n");
                js.append("            \"contents\": [\n");
            } else {
                js.append("        {\n");
                js.append("            \"kind\": \"category\",\n");
                js.append("            \"name\": \"⚙️ ").append(entry.getKey()).append("\",\n");
                js.append("            \"colour\": \"#607D8B\",\n");
                js.append("            \"contents\": [\n");
            }

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

        // Catégorie fonctions
        js.append("""
                        {
                            "kind": "category",
                            "name": "🔧 Functions",
                            "colour": "#673AB7",
                            "contents": [
                                {"kind": "block", "type": "function_trigger"},
                                {"kind": "block", "type": "call_function_action"}
                            ]
                        },
                """);

        // Catégorie variables
        js.append("""
                {
                    "kind": "category",
                    "name": "📊 Variables",
                    "colour": "#FF5722",
                    "contents": [
                        {"kind": "block", "type": "set_variable_action"},
                        {"kind": "block", "type": "get_variable"}
                    ]
                },
                """);

        // Catégorie utilitaires
        js.append("""
                        {
                            "kind": "category",
                            "name": "🔧 Utilitaires",
                            "colour": "#9E9E9E",
                            "contents": [
                                {"kind": "block", "type": "boolean_true"},
                                {"kind": "block", "type": "boolean_false"},
                                {"kind": "block", "type": "text"},
                                {"kind": "label", "text": "Locations"},
                                {"kind": "block", "type": "location_xyz"},
                                {"kind": "block", "type": "location_xyz_world"},
                                {"kind": "block", "type": "location_full"}
                            ]
                        },
                """);

        // Dynamic module categories
        generateModuleToolboxCategories(js);

        js.append("""
                    ]
                };

                console.log('📦 Toolbox auto-générée:', window.DUNGEON_TOOLBOX);

                """);
    }

    /**
     * Génère les fonctions utilitaires pour Blockly, telles que la génération de triggers,
     * l'extraction d'actions, et le chargement de données dans l'espace de travail.
     *
     * @param js Le `StringBuilder` utilisé pour accumuler le code JavaScript généré.
     */
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

        // Generate extraction cases for dynamic module trigger blocks
        generateModuleTriggerCases(js);

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
                try {
                    BlocklyAction instance = actionClass.getDeclaredConstructor().newInstance();
                    if (instance.requiresCustomBlockGeneration()) {
                        instance.generateCustomActionCase(js);
                    } else {
                        generateActionCase(js, actionClass);
                    }
                } catch (Exception e) {
                    generateActionCase(js, actionClass);
                }
            }
        }

        // Generate extraction cases for dynamic module action blocks
        generateModuleActionCases(js);

        js.append("""
                        actionBlock = actionBlock.getNextBlock();
                    }
                    
                    return actions;
                }
                        
                // Fonction pour charger les triggers dans l'espace de travail
                function loadTriggersIntoWorkspace(triggersData) {
                    console.log('🔄 Chargement des triggers dans l\\'espace de travail...');
                    console.log('Données reçues:', triggersData);
                    
                    // Vider l'espace de travail avant de charger
                    workspace.clear();
                    
                    if (!triggersData || !triggersData.triggers) {
                        console.log('Aucun trigger à charger');
                        return;
                    }
                    
                    console.log('Nombre de triggers à charger:', triggersData.triggers.length);
                            
                    triggersData.triggers.forEach((trigger, index) => {
                        console.log('Chargement du trigger #' + (index + 1) + ':', trigger);
                        
                """);

        // Générer dynamiquement les cas de chargement pour chaque trigger
        for (Map.Entry<String, List<Class<? extends BlocklyTrigger>>> entry : triggersByCategory.entrySet()) {
            for (Class<? extends BlocklyTrigger> triggerClass : entry.getValue()) {
                generateTriggerLoadingCase(js, triggerClass);
            }
        }

        // Generate loading cases for dynamic module trigger blocks
        generateModuleTriggerLoadingCases(js);

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
                        let actionBlock = null;
                """);

        // Générer dynamiquement les cas de chargement pour chaque action
        for (Map.Entry<String, List<Class<? extends BlocklyAction>>> entry : actionsByCategory.entrySet()) {
            for (Class<? extends BlocklyAction> actionClass : entry.getValue()) {
                try {
                    BlocklyAction instance = actionClass.getDeclaredConstructor().newInstance();
                    if (instance.requiresCustomBlockGeneration()) {
                        instance.generateCustomActionLoadingCase(js);
                    } else {
                        generateActionLoadingCase(js, actionClass);
                    }
                } catch (Exception e) {
                    generateActionLoadingCase(js, actionClass);
                }
            }
        }

        // Generate loading cases for dynamic module action blocks
        generateModuleActionLoadingCases(js);

        // Add these helper functions in generateUtilityFunctions method:
        js.append("""
                                    if (actionBlock) {
                                        actionBlock.initSvg();
                                        actionBlock.render();
                                        if (index === 0) {
                                            actionsInput.connection.connect(actionBlock.previousConnection);
                                        } else if (previousActionBlock) {
                                            previousActionBlock.nextConnection.connect(actionBlock.previousConnection);
                                        }
                                        previousActionBlock = actionBlock;
                                    }
                            });
                }
                        // Helper function to extract actions from statement inputs (for IF/ELSE blocks)
                                  function getActionsFromStatementInput(block, inputName) {
                                      const actions = [];
                                      let actionBlock = block.getInputTargetBlock(inputName);
                                     \s
                                      while (actionBlock) {
                                          console.log('Action trouvée dans statement:', actionBlock.type);
                                         \s
                """);

        // Generate the same action cases as in the main getActionsFromBlock function
        // But this time we'll inline them properly with the actions array defined
        for (Map.Entry<String, List<Class<? extends BlocklyAction>>> entry : actionsByCategory.entrySet()) {
            for (Class<? extends BlocklyAction> actionClass : entry.getValue()) {
                try {
                    BlocklyAction instance = actionClass.getDeclaredConstructor().newInstance();
                    if (instance.requiresCustomBlockGeneration()) {
                        instance.generateCustomActionCase(js);
                    } else {
                        generateActionCase(js, actionClass);
                    }
                } catch (Exception e) {
                    generateActionCase(js, actionClass);
                }
            }
        }

        // Generate extraction cases for dynamic module action blocks
        generateModuleActionCases(js);

        js.append("""
                         actionBlock = actionBlock.getNextBlock();
                     }
                    \s
                     return actions;
                 }
                \s
                 // Helper function to load actions into statement inputs
                 function loadActionsIntoStatement(parentBlock, actions, statementName) {
                     if (!actions || actions.length === 0) return;
                    \s
                     let previousActionBlock = null;
                     const actionsInput = parentBlock.getInput(statementName);
                    \s
                     actions.forEach((action, index) => {
                        let actionBlock = null;
                """);

        // Generate the same action loading cases as in the main loadActionsIntoBlock function
        for (Map.Entry<String, List<Class<? extends BlocklyAction>>> entry : actionsByCategory.entrySet()) {
            for (Class<? extends BlocklyAction> actionClass : entry.getValue()) {
                try {
                    BlocklyAction instance = actionClass.getDeclaredConstructor().newInstance();
                    if (instance.requiresCustomBlockGeneration()) {
                        instance.generateCustomActionLoadingCase(js);
                    } else {
                        generateActionLoadingCase(js, actionClass);
                    }
                } catch (Exception e) {
                    generateActionLoadingCase(js, actionClass);
                }
            }
        }

        // Generate loading cases for dynamic module action blocks
        generateModuleActionLoadingCases(js);

        js.append("""
                                    if (actionBlock) {
                                        actionBlock.initSvg();
                                        actionBlock.render();
                                        if (index === 0) {
                                            actionsInput.connection.connect(actionBlock.previousConnection);
                                        } else if (previousActionBlock) {
                                            previousActionBlock.nextConnection.connect(actionBlock.previousConnection);
                                        }
                                        previousActionBlock = actionBlock;
                                    }
                                });
                            }
                            \s
                console.log('✅ Fonctions utilitaires auto-générées chargées');
                """);
    }

    /**
     * Génère un cas spécifique pour un trigger dans le code JavaScript.
     *
     * @param js           Le `StringBuilder` utilisé pour accumuler le code JavaScript généré.
     * @param triggerClass La classe du trigger à traiter, annotée avec `@BlocklyInfo`.
     *                     Cette classe est utilisée pour extraire les informations nécessaires
     *                     à la génération du bloc correspondant.
     */
    private void generateTriggerCase(StringBuilder js, Class<? extends BlocklyTrigger> triggerClass) {
        // Récupère les informations de l'annotation @BlocklyInfo de la classe du trigger
        BlocklyInfo info = triggerClass.getAnnotation(BlocklyInfo.class);

        // Détermine le type du trigger en convertissant le nom de la classe
        String triggerType = info.name();

        // Extrait les champs définis dans la classe du trigger
        List<BlocklyFieldExtractor.BlocklyFieldInfo> fields = BlocklyFieldExtractor.extractFields(triggerClass);

        // Génère le code JavaScript pour vérifier le type du bloc
        js.append("                if (block.type === '").append(triggerType).append("') {\n");
        js.append("                    triggers.push({\n");
        js.append("                        type: '").append(triggerType).append("',\n");
        js.append("                        name: '").append(triggerClass.getSimpleName()).append("_' + uuidv4(),\n");

        // Génère dynamiquement les champs associés au trigger
        for (BlocklyFieldExtractor.BlocklyFieldInfo field : fields) {
            js.append("                        ");
            generateFieldExtraction(js, field, "block");
            js.append(",\n");
        }

        // Ajoute les actions associées au trigger, si elles sont supportées
        try {
            BlocklyTrigger instance = triggerClass.getDeclaredConstructor().newInstance();
            if (instance.hasActions()) {
                js.append("                        actions: getActionsFromBlock(block)\n");
            } else {
                js.append("                        actions: []\n");
            }
        } catch (Exception e) {
            // En cas d'erreur, ajoute les actions par défaut
            js.append("                        actions: getActionsFromBlock(block)\n");
        }

        js.append("                    });\n");
        js.append("                }\n");
    }

    /**
     * Génère un cas spécifique pour une action dans le code JavaScript.
     *
     * @param js          Le `StringBuilder` utilisé pour accumuler le code JavaScript généré.
     * @param actionClass La classe de l'action à traiter, annotée avec `@BlocklyInfo`.
     *                    Cette classe est utilisée pour extraire les informations nécessaires
     *                    à la génération du bloc correspondant.
     */
    private void generateActionCase(StringBuilder js, Class<? extends BlocklyAction> actionClass) {
        // Récupère les informations de l'annotation @BlocklyInfo de la classe de l'action
        BlocklyInfo info = actionClass.getAnnotation(BlocklyInfo.class);

        // Détermine le type de l'action en convertissant le nom de la classe
        String actionType = info.name();

        // Extrait les champs définis dans la classe de l'action
        List<BlocklyFieldExtractor.BlocklyFieldInfo> fields = BlocklyFieldExtractor.extractFields(actionClass);

        // Génère le code JavaScript pour vérifier le type du bloc
        js.append("                if (actionBlock.type === '").append(actionType).append("') {\n");
        js.append("                    actions.push({\n");
        js.append("                        type: '").append(actionType).append("'");

        // Génère dynamiquement les champs associés à l'action
        for (BlocklyFieldExtractor.BlocklyFieldInfo field : fields) {
            js.append(",\n                        ");
            generateFieldExtraction(js, field, "actionBlock");
        }

        js.append("\n                    });\n");
        js.append("                }\n");
    }

    /**
     * Génère un cas spécifique pour charger un trigger dans l'espace de travail Blockly.
     *
     * @param js           Le `StringBuilder` utilisé pour accumuler le code JavaScript généré.
     * @param triggerClass La classe du trigger à traiter, annotée avec `@BlocklyInfo`.
     *                     Cette classe est utilisée pour extraire les informations nécessaires
     *                     à la génération du bloc correspondant.
     */
    private void generateTriggerLoadingCase(StringBuilder js, Class<? extends BlocklyTrigger> triggerClass) {
        // Récupère les informations de l'annotation @BlocklyInfo de la classe du trigger
        BlocklyInfo info = triggerClass.getAnnotation(BlocklyInfo.class);

        // Détermine le type du trigger en convertissant le nom de la classe
        String triggerType = info.name();

        // Extrait les champs définis dans la classe du trigger
        List<BlocklyFieldExtractor.BlocklyFieldInfo> fields = BlocklyFieldExtractor.extractFields(triggerClass);

        // Génère le code JavaScript pour charger un trigger spécifique
        js.append("                if (trigger.type === '").append(triggerType).append("') {\n");
        js.append("                    const triggerBlock = workspace.newBlock('").append(triggerType).append("');\n");

        // Génère dynamiquement le chargement des champs associés au trigger
        for (BlocklyFieldExtractor.BlocklyFieldInfo field : fields) {
            generateFieldLoading(js, field, "trigger");
        }

        js.append("                    \n");
        js.append("                    triggerBlock.initSvg();\n");
        js.append("                    triggerBlock.render();\n");
        js.append("                    \n"); // Try to put it after initSvg and render
        js.append("                    // Charger les actions\n");
        js.append("                    loadActionsIntoBlock(triggerBlock, trigger.actions);\n");
        js.append("                    \n"); // Added
        js.append("                    triggerBlock.moveBy(20 + (index * 300), 20);\n");
        js.append("                }\n");
    }

    /**
     * Génère un cas spécifique pour charger une action dans l'espace de travail Blockly.
     *
     * @param js          Le `StringBuilder` utilisé pour accumuler le code JavaScript généré.
     * @param actionClass La classe de l'action à traiter, annotée avec `@BlocklyInfo`.
     *                    Cette classe est utilisée pour extraire les informations nécessaires
     *                    à la génération du bloc correspondant.
     */
    private void generateActionLoadingCase(StringBuilder js, Class<? extends BlocklyAction> actionClass) {
        // Récupère les informations de l'annotation @BlocklyInfo de la classe de l'action
        BlocklyInfo info = actionClass.getAnnotation(BlocklyInfo.class);

        // Détermine le type de l'action en convertissant le nom de la classe
        String actionType = info.name();

        // Extrait les champs définis dans la classe de l'action
        List<BlocklyFieldExtractor.BlocklyFieldInfo> fields = BlocklyFieldExtractor.extractFields(actionClass);

        // Génère le code JavaScript pour charger une action spécifique
        js.append("                if (action.type === '").append(actionType).append("') {\n");
        js.append("                    actionBlock = workspace.newBlock('").append(actionType).append("');\n");

        // Génère dynamiquement le chargement des champs associés à l'action
        for (BlocklyFieldExtractor.BlocklyFieldInfo field : fields) {
            generateFieldLoading(js, field, "action");
        }

        /*js.append("                    \n");
        js.append("                    if (index === 0) {\n");
        js.append("                        // Premier bloc d'action, connecter au trigger\n");
        js.append("                        actionsInput.connection.connect(actionBlock.previousConnection);\n");
        js.append("                    } else {\n");
        js.append("                        // Blocs suivants, connecter au bloc précédent\n");
        js.append("                        if (previousActionBlock) {\n");
        js.append("                            previousActionBlock.nextConnection.connect(actionBlock.previousConnection);\n");
        js.append("                        }\n");
        js.append("                    }\n");
        js.append("                    \n"); // Try to put it after the connections
        js.append("                    actionBlock.initSvg();\n");
        js.append("                    actionBlock.render();\n");
        js.append("                    \n");
        js.append("                    previousActionBlock = actionBlock;\n");*/
        js.append("                }\n");
    }

    /**
     * Génère le code JavaScript pour extraire les valeurs des champs d'un bloc Blockly.
     *
     * @param js            Le `StringBuilder` utilisé pour accumuler le code JavaScript généré.
     * @param field         Une instance de `BlocklyFieldExtractor.BlocklyFieldInfo` contenant les informations
     *                      sur le champ à extraire (type, nom, valeur par défaut, etc.).
     * @param blockVariable Le nom de la variable représentant le bloc Blockly dans le code généré.
     */
    private void generateFieldExtraction(StringBuilder js, BlocklyFieldExtractor.BlocklyFieldInfo field, String blockVariable) {
        // Convertit le nom du champ en majuscules pour l'utiliser comme identifiant dans Blockly
        String fieldName = field.fieldName().toUpperCase();

        // Génère le code en fonction du type de champ
        switch (field.type()) {
            case TEXT_INPUT:
                // Extraction d'un champ de type texte avec une valeur par défaut
                js.append(field.fieldName())
                        .append(": ").append(blockVariable).append(".getFieldValue('").append(fieldName).append("') || '")
                        .append(escapeJavaScript(field.defaultValue())).append("'");
                break;

            case NUMBER_INPUT:
                // Extraction d'un champ de type nombre avec conversion en flottant et valeur par défaut
                js.append(field.fieldName())
                        .append(": parseFloat(").append(blockVariable).append(".getFieldValue('").append(fieldName).append("')) || ")
                        .append(field.defaultValue().isEmpty() ? "0" : field.defaultValue());
                break;

            case DROPDOWN:
                // Extraction d'un champ de type menu déroulant avec une option par défaut
                js.append(field.fieldName())
                        .append(": ").append(blockVariable).append(".getFieldValue('").append(fieldName).append("') || '")
                        .append(field.options().split(",")[0].trim()).append("'");
                break;

            case BOOLEAN_INPUT:
                // Extraction d'un champ de type booléen avec vérification du type de bloc connecté
                js.append(field.fieldName())
                        .append(": (() => {\n");
                js.append("                            const boolBlock = ").append(blockVariable).append(".getInputTargetBlock('").append(fieldName).append("');\n");
                js.append("                            return boolBlock ? boolBlock.type === 'boolean_true' : ")
                        .append(field.defaultValue().equals("true") ? "true" : "false").append(";\n");
                js.append("                        })()");
                break;

            case COLOR_INPUT:
                // Extraction d'un champ de type couleur avec une valeur par défaut
                js.append(field.fieldName())
                        .append(": ").append(blockVariable).append(".getFieldValue('").append(fieldName).append("') || '")
                        .append(field.defaultValue().isEmpty() ? "#ff0000" : field.defaultValue()).append("'");
                break;

            case CHECKBOX:
                // Extraction d'un champ de type case à cocher avec conversion en booléen
                js.append(field.fieldName())
                        .append(": ").append(blockVariable).append(".getFieldValue('").append(fieldName).append("') === 'TRUE'");
                break;

            case LOCATION_INPUT:
                // Extraction d'un bloc de location avec tous ses paramètres
                js.append(field.fieldName())
                        .append(": (() => {\n");
                js.append("                            const locBlock = ").append(blockVariable).append(".getInputTargetBlock('").append(fieldName).append("');\n");
                js.append("                            if (!locBlock) return null;\n");
                js.append("                            const location = {};\n");
                js.append("                            location.x = parseFloat(locBlock.getFieldValue('X')) || 0;\n");
                js.append("                            location.y = parseFloat(locBlock.getFieldValue('Y')) || 64;\n");
                js.append("                            location.z = parseFloat(locBlock.getFieldValue('Z')) || 0;\n");
                js.append("                            if (locBlock.type === 'location_xyz_world' || locBlock.type === 'location_full') {\n");
                js.append("                                location.world = locBlock.getFieldValue('WORLD') || 'world';\n");
                js.append("                                location.hasWorld = true;\n");
                js.append("                            }\n");
                js.append("                            if (locBlock.type === 'location_full') {\n");
                js.append("                                location.yaw = parseFloat(locBlock.getFieldValue('YAW')) || 0;\n");
                js.append("                                location.pitch = parseFloat(locBlock.getFieldValue('PITCH')) || 0;\n");
                js.append("                                location.hasRotation = true;\n");
                js.append("                            }\n");
                js.append("                            return location;\n");
                js.append("                        })()");
                break;
        }
    }

    /**
     * Génère le code JavaScript pour charger les valeurs des champs d'un bloc Blockly.
     *
     * @param js    Le `StringBuilder` utilisé pour accumuler le code JavaScript généré.
     * @param field Une instance de `BlocklyFieldExtractor.BlocklyFieldInfo` contenant les informations
     *              sur le champ à charger (type, nom, valeur par défaut, etc.).
     */
    private void generateFieldLoading(StringBuilder js, BlocklyFieldExtractor.BlocklyFieldInfo field, String blockType) {
        // Convertit le nom du champ en majuscules pour l'utiliser comme identifiant dans Blockly
        String fieldName = field.fieldName().toUpperCase();
        String objectField = field.fieldName();

        // Génère le code en fonction du type de champ
        switch (field.type()) {
            case TEXT_INPUT:
            case DROPDOWN:
            case COLOR_INPUT:
                // Charge une valeur de type texte, menu déroulant ou couleur
                js.append("                    ").append(blockType).append("Block.setFieldValue((").append(blockType).append("['").append(objectField).append("']")
                        .append(" || '").append(escapeJavaScript(field.defaultValue()))
                        .append("').toString(), '").append(fieldName).append("');\n");
                break;

            case NUMBER_INPUT:
                // Charge une valeur de type nombre avec une valeur par défaut
                js.append("                    ").append(blockType).append("Block.setFieldValue((").append(blockType).append("['").append(objectField).append("']")
                        .append(" || ").append(field.defaultValue().isEmpty() ? "0" : field.defaultValue())
                        .append(").toString(), '").append(fieldName).append("');\n");
                break;

            case BOOLEAN_INPUT:
                // Charge une valeur de type booléen en créant un bloc correspondant
                js.append("                    if (").append(blockType).append("['").append(objectField).append("'] !== undefined) {\n");
                js.append("                        const boolBlock = workspace.newBlock(").append(blockType).append("['")
                        .append(objectField).append("'] ? 'boolean_true' : 'boolean_false');\n");
                js.append("                        boolBlock.initSvg();\n");
                js.append("                        boolBlock.render();\n");
                js.append("                        ").append(blockType).append("Block.getInput('").append(fieldName)
                        .append("').connection.connect(boolBlock.outputConnection);\n");
                js.append("                    }\n");
                break;

            case CHECKBOX:
                // Charge une valeur de type case à cocher (vrai/faux)
                js.append("                    ").append(blockType).append("Block.setFieldValue(").append(blockType).append("['").append(objectField).append("']")
                        .append(" ? 'TRUE' : 'FALSE', '").append(fieldName).append("');\n");
                break;

            case LOCATION_INPUT:
                // Charge un bloc de location avec tous ses paramètres
                js.append("                    if (").append(blockType).append("['").append(objectField).append("']) {\n");
                js.append("                        const loc = ").append(blockType).append("['").append(objectField).append("'];\n");
                js.append("                        let locBlockType = 'location_xyz';\n");
                js.append("                        if (loc.hasRotation) {\n");
                js.append("                            locBlockType = 'location_full';\n");
                js.append("                        } else if (loc.hasWorld) {\n");
                js.append("                            locBlockType = 'location_xyz_world';\n");
                js.append("                        }\n");
                js.append("                        const locBlock = workspace.newBlock(locBlockType);\n");
                js.append("                        locBlock.setFieldValue((loc.x || 0).toString(), 'X');\n");
                js.append("                        locBlock.setFieldValue((loc.y || 64).toString(), 'Y');\n");
                js.append("                        locBlock.setFieldValue((loc.z || 0).toString(), 'Z');\n");
                js.append("                        if (locBlockType === 'location_xyz_world' || locBlockType === 'location_full') {\n");
                js.append("                            locBlock.setFieldValue((loc.world || 'world').toString(), 'WORLD');\n");
                js.append("                        }\n");
                js.append("                        if (locBlockType === 'location_full') {\n");
                js.append("                            locBlock.setFieldValue((loc.yaw || 0).toString(), 'YAW');\n");
                js.append("                            locBlock.setFieldValue((loc.pitch || 0).toString(), 'PITCH');\n");
                js.append("                        }\n");
                js.append("                        locBlock.initSvg();\n");
                js.append("                        locBlock.render();\n");
                js.append("                        ").append(blockType).append("Block.getInput('").append(fieldName)
                        .append("').connection.connect(locBlock.outputConnection);\n");
                js.append("                    }\n");
                break;
        }
    }

    /**
     * Échappe les caractères spéciaux dans une chaîne de texte pour qu'elle soit
     * compatible avec le format JavaScript. Cela inclut les caractères d'échappement,
     * les guillemets doubles, les sauts de ligne et les retours chariot.
     *
     * @param text La chaîne de texte à échapper.
     * @return La chaîne de texte échappée.
     */
    private String escapeJavaScript(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}