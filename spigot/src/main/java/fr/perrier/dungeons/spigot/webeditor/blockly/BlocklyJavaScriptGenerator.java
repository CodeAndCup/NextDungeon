package fr.perrier.dungeons.spigot.webeditor.blockly;

import fr.perrier.dungeons.common.module.ModuleBlockDescriptor;
import fr.perrier.dungeons.spigot.Main;
import fr.perrier.dungeons.spigot.module.ModuleLoader;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyField;
import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyInfo;
import org.bukkit.entity.Player;
import org.reflections.Reflections;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dynamically generates the JavaScript used by the web Blockly editor of the Spigot plugin.
 *
 * <p>This generator scans classes annotated with {@code @BlocklyInfo} to group triggers and
 * actions by category, then builds block definitions, the toolbox and utility functions into a
 * single JavaScript string ready to be injected into the editor page.</p>
 *
 * <h2>Bug fixes in this version</h2>
 * <ul>
 *   <li><b>Fixed silent exception in {@link #generateAllActionCases(StringBuilder)}</b>:
 *       the previous empty {@code catch} block silently swallowed exceptions raised by
 *       actions returning {@code true} from {@code requiresCustomBlockGeneration()} when
 *       they did not override {@code generateCustomActionCase()}. This caused affected
 *       actions (e.g. {@code summon_mob_action}, {@code send_message_action},
 *       {@code send_title_action}, {@code delay_action}, {@code broadcast_command_action})
 *       to be <b>silently dropped from the JSON payload on save</b>, while their
 *       definitions and loading paths kept working. The catch now falls back to
 *       {@link #buildActionExtractionCase(StringBuilder, Class)} like its siblings.</li>
 *   <li><b>Consistent fallbacks across all three generation phases</b>: definition,
 *       extraction and loading now all fall back to the annotation-driven path when a
 *       custom hook throws.</li>
 *   <li><b>Startup consistency check</b> ({@link #validateConsistency()}): if a class
 *       declares {@code requiresCustomBlockGeneration() == true} but one of the three
 *       custom hooks throws, a warning is logged so the problem becomes visible instead
 *       of manifesting as missing data.</li>
 *   <li><b>Improved logging</b>: swallowed exceptions are now logged at FINE/WARNING so
 *       future regressions surface in the server log.</li>
 * </ul>
 *
 * @since 1.0.4-SNAPSHOT (2026-03-11)
 * @see fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyInfo
 * @see fr.perrier.dungeons.spigot.webeditor.blockly.BlocklyFieldExtractor
 */
public class BlocklyJavaScriptGenerator {

    private static final Logger LOGGER = Logger.getLogger(BlocklyJavaScriptGenerator.class.getName());

    private final Map<String, List<Class<? extends BlocklyTrigger>>> triggersByCategory = new HashMap<>();
    private final Map<String, List<Class<? extends BlocklyAction>>> actionsByCategory = new HashMap<>();

    /**
     * Creates a new generator instance and initializes the catalog of available Blockly components
     * by scanning the classpath.
     *
     * <p>This initialization collects trigger and action classes annotated with {@code @BlocklyInfo}
     * and organizes them by category. It also runs a consistency check that warns about classes
     * whose {@code requiresCustomBlockGeneration()} is {@code true} but whose custom hooks throw.</p>
     *
     * @since 1.0.4-SNAPSHOT (2026-03-11)
     */
    public BlocklyJavaScriptGenerator() {
        scanForComponents();
        validateConsistency();
    }

    /**
     * Scans packages to find triggers and actions annotated with {@code @BlocklyInfo}
     * and organizes them by category.
     */
    private void scanForComponents() {
        Reflections reflections = new Reflections("fr.perrier.dungeons");

        Set<Class<? extends BlocklyTrigger>> triggerClasses = reflections.getSubTypesOf(BlocklyTrigger.class);
        for (Class<? extends BlocklyTrigger> clazz : triggerClasses) {
            if (clazz.isAnnotationPresent(BlocklyInfo.class)) {
                BlocklyInfo info = clazz.getAnnotation(BlocklyInfo.class);
                String category = info.category().isEmpty() ? "Triggers" : info.category();
                triggersByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(clazz);
            }
        }

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
     * Validates that every action declaring {@code requiresCustomBlockGeneration() == true} also
     * provides working implementations of all three custom hooks.
     *
     * <p>This guards against the regression that previously caused actions such as
     * {@code summon_mob_action} and {@code send_message_action} to be silently dropped from
     * the JSON payload: their {@code generateCustomActionCase()} threw, the exception was
     * swallowed, and no extraction case was emitted for them.</p>
     *
     * <p>Problems are logged as warnings rather than thrown so a misconfigured action does
     * not prevent the whole editor from loading — the generator will automatically fall back
     * to the annotation-driven path for such actions.</p>
     */
    private void validateConsistency() {
        for (List<Class<? extends BlocklyAction>> classes : actionsByCategory.values()) {
            for (Class<? extends BlocklyAction> actionClass : classes) {
                try {
                    BlocklyAction instance = actionClass.getDeclaredConstructor().newInstance();
                    if (!instance.requiresCustomBlockGeneration()) continue;

                    StringBuilder sink = new StringBuilder();
                    checkHook(actionClass, "generateCustomBlock",             () -> instance.generateCustomBlock(sink));
                    checkHook(actionClass, "generateCustomActionCase",        () -> instance.generateCustomActionCase(sink));
                    checkHook(actionClass, "generateCustomActionLoadingCase", () -> instance.generateCustomActionLoadingCase(sink));
                } catch (ReflectiveOperationException e) {
                    LOGGER.log(Level.WARNING,
                            "Cannot instantiate action class " + actionClass.getName()
                            + " — it will be skipped by the Blockly generator",
                            e);
                }
            }
        }
    }

    /**
     * Runs a single custom hook and logs a warning if it throws. Used by {@link #validateConsistency()}.
     *
     * @param cls      the action class owning the hook, used for contextual logging
     * @param hookName human-readable hook name for the log message
     * @param runnable the hook invocation
     */
    private void checkHook(Class<?> cls, String hookName, ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING,
                    cls.getSimpleName() + "." + hookName + "() threw — "
                    + "annotation-based fallback will be used. "
                    + "Override requiresCustomBlockGeneration() or implement this hook "
                    + "properly to silence this warning.",
                    e);
        }
    }

    /** Minimal functional interface allowing checked-exception-aware lambdas inside {@link #validateConsistency()}. */
    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    /**
     * Generates the complete JavaScript code for the Blockly editor.
     *
     * <p>The returned string contains block definitions (triggers, actions, functions, utilities),
     * the toolbox and utility JavaScript functions required by the web editor.</p>
     *
     * @param editor the player using the editor; used to include contextual information
     *               (for example the name in logs). Must not be {@code null} in normal usage.
     * @return the complete JavaScript string ready to be injected into the page
     * @since 1.0.4-SNAPSHOT (2026-03-11)
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

        buildAllTriggerBlockDefinitions(js);
        buildAllActionBlockDefinitions(js);
        buildFunctionBlockDefinitions(js);
        buildUtilityBlockDefinitions(js);
        buildVariableBlockDefinitions(js);
        buildDynamicModuleBlocks(js);
        buildToolbox(js);
        buildUtilityFunctions(js);

        js.append("console.log('✅ Tous les blocs ont été générés automatiquement!');\n");

        return js.toString();
    }

    /**
     * Builds block definitions for all trigger blocks organized by category.
     *
     * @param js the StringBuilder to accumulate the generated JavaScript code
     */
    private void buildAllTriggerBlockDefinitions(StringBuilder js) {
        js.append("// ===== TRIGGER BLOCKS (AUTO-GENERATED) =====\n");

        for (Map.Entry<String, List<Class<? extends BlocklyTrigger>>> entry : triggersByCategory.entrySet()) {
            js.append("// Category: ").append(entry.getKey()).append("\n");

            for (Class<? extends BlocklyTrigger> triggerClass : entry.getValue()) {
                buildSingleTriggerBlockDefinition(js, triggerClass);
            }
        }
        js.append("\n");
    }

    /**
     * Builds the JavaScript code for a single trigger block definition.
     *
     * @param js the StringBuilder to accumulate the generated JavaScript code
     * @param triggerClass the trigger class to generate, annotated with {@code @BlocklyInfo}
     */
    private void buildSingleTriggerBlockDefinition(StringBuilder js, Class<? extends BlocklyTrigger> triggerClass) {
        BlocklyInfo info = triggerClass.getAnnotation(BlocklyInfo.class);
        String blockName = info.name().isEmpty()
                ? triggerClass.getSimpleName().toLowerCase().replace("trigger", "_trigger")
                : info.name();

        List<BlocklyFieldExtractor.BlocklyFieldInfo> fields = BlocklyFieldExtractor.extractFields(triggerClass);

        js.append("Blockly.Blocks['").append(blockName).append("'] = {\n");
        js.append("    init: function() {\n");

        js.append("        this.appendDummyInput()\n");
        js.append("            .appendField(\"").append(escapeJavaScript(info.displayText())).append("\");\n");

        for (BlocklyFieldExtractor.BlocklyFieldInfo field : fields) {
            buildBlocklyFieldDefinition(js, field);
        }

        try {
            BlocklyTrigger instance = triggerClass.getDeclaredConstructor().newInstance();
            if (instance.hasActions()) {
                js.append("        this.appendStatementInput(\"ACTIONS\")\n");
                js.append("            .setCheck(\"Action\")\n");
                js.append("            .appendField(\"Execute:\");\n");
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE,
                    "Cannot instantiate trigger " + triggerClass.getSimpleName()
                    + " — defaulting to hasActions()=true",
                    e);
            js.append("        this.appendStatementInput(\"ACTIONS\")\n");
            js.append("            .setCheck(\"Action\")\n");
            js.append("            .appendField(\"Execute:\");\n");
        }

        js.append("        this.setColour('").append(info.color()).append("');\n");
        if (!info.tooltip().isEmpty()) {
            js.append("        this.setTooltip(\"").append(escapeJavaScript(info.tooltip())).append("\");\n");
        }

        js.append("    }\n");
        js.append("};\n\n");
    }

    /**
     * Builds block definitions for all action blocks organized by category.
     *
     * <p>Actions declaring {@code requiresCustomBlockGeneration() == true} are generated via
     * their custom hook; if that hook throws, the generator falls back to the annotation-driven
     * path so no action is ever silently missing from the output.</p>
     *
     * @param js the StringBuilder to accumulate the generated JavaScript code
     */
    private void buildAllActionBlockDefinitions(StringBuilder js) {
        js.append("// ===== ACTION BLOCKS (AUTO-GENERATED) =====\n");

        for (Map.Entry<String, List<Class<? extends BlocklyAction>>> entry : actionsByCategory.entrySet()) {
            js.append("// Category: ").append(entry.getKey()).append("\n");
            for (Class<? extends BlocklyAction> actionClass : entry.getValue()) {
                try {
                    BlocklyAction instance = actionClass.getDeclaredConstructor().newInstance();
                    if (instance.requiresCustomBlockGeneration()) {
                        try {
                            instance.generateCustomBlock(js);
                        } catch (Exception customEx) {
                            LOGGER.log(Level.WARNING,
                                    actionClass.getSimpleName() + ".generateCustomBlock() threw — "
                                    + "falling back to annotation-based generation",
                                    customEx);
                            buildSingleActionBlockDefinition(js, actionClass);
                        }
                    } else {
                        buildSingleActionBlockDefinition(js, actionClass);
                    }
                } catch (ReflectiveOperationException e) {
                    LOGGER.log(Level.WARNING,
                            "Cannot instantiate " + actionClass.getSimpleName()
                            + " — using annotation-based definition",
                            e);
                    buildSingleActionBlockDefinition(js, actionClass);
                }
            }
        }
        js.append("\n");
    }

    /**
     * Builds the JavaScript code for a single action block definition.
     *
     * @param js the StringBuilder to accumulate the generated JavaScript code
     * @param actionClass the action class to generate, annotated with {@code @BlocklyInfo}
     */
    private void buildSingleActionBlockDefinition(StringBuilder js, Class<? extends BlocklyAction> actionClass) {
        BlocklyInfo info = actionClass.getAnnotation(BlocklyInfo.class);
        String blockName = info.name().isEmpty()
                ? actionClass.getSimpleName().toLowerCase().replace("action", "_action")
                : info.name();

        List<BlocklyFieldExtractor.BlocklyFieldInfo> fields = BlocklyFieldExtractor.extractFields(actionClass);

        js.append("Blockly.Blocks['").append(blockName).append("'] = {\n");
        js.append("    init: function() {\n");

        js.append("        this.appendDummyInput()\n");
        js.append("            .appendField(\"").append(escapeJavaScript(info.displayText())).append("\");\n");

        for (BlocklyFieldExtractor.BlocklyFieldInfo field : fields) {
            buildBlocklyFieldDefinition(js, field);
        }

        try {
            BlocklyAction instance = actionClass.getDeclaredConstructor().newInstance();
            if (instance.isChainable()) {
                js.append("        this.setPreviousStatement(true, \"Action\");\n");
                js.append("        this.setNextStatement(true, \"Action\");\n");
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE,
                    "Cannot instantiate action " + actionClass.getSimpleName()
                    + " — defaulting to chainable",
                    e);
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
     * Builds the JavaScript code for a specific field of a Blockly block.
     *
     * @param js the StringBuilder to accumulate the generated JavaScript code
     * @param field a BlocklyFieldInfo instance containing information about the field to generate
     */
    private void buildBlocklyFieldDefinition(StringBuilder js, BlocklyFieldExtractor.BlocklyFieldInfo field) {
        // Value-input fields (LOCATION_INPUT, BOOLEAN_INPUT) emit their own label + input,
        // so we skip the leading dummy input used by simple appendField-based fields.
        if (field.type() == BlocklyField.FieldType.LOCATION_INPUT
            || field.type() == BlocklyField.FieldType.BOOLEAN_INPUT) {
            switch (field.type()) {
                case BOOLEAN_INPUT  -> buildBooleanFieldDefinition(js, field);
                case LOCATION_INPUT -> buildLocationFieldDefinition(js, field);
            }
            return;
        }

        js.append("        this.appendDummyInput()\n");

        if (!field.label().isEmpty()) {
            js.append("            .appendField(\"").append(escapeJavaScript(field.label())).append("\")\n");
        }

        switch (field.type()) {
            case TEXT_INPUT   -> buildTextFieldDefinition(js, field);
            case NUMBER_INPUT -> buildNumberFieldDefinition(js, field);
            case DROPDOWN     -> buildDropdownFieldDefinition(js, field);
            case COLOR_INPUT  -> buildColorFieldDefinition(js, field);
            case CHECKBOX     -> buildCheckboxFieldDefinition(js, field);
        }
    }

    /** Builds a text input field definition. */
    private void buildTextFieldDefinition(StringBuilder js, BlocklyFieldExtractor.BlocklyFieldInfo field) {
        js.append("            .appendField(new Blockly.FieldTextInput(\"")
                .append(escapeJavaScript(field.defaultValue()))
                .append("\"), \"").append(field.fieldName()).append("\");\n");
    }

    /** Builds a number input field definition with optional bounds. */
    private void buildNumberFieldDefinition(StringBuilder js, BlocklyFieldExtractor.BlocklyFieldInfo field) {
        js.append("            .appendField(new Blockly.FieldNumber(")
                .append(field.defaultValue().isEmpty() ? "0" : field.defaultValue());
        if (field.min() != Double.MIN_VALUE) {
            js.append(", ").append(field.min());
        }
        if (field.max() != Double.MAX_VALUE) {
            js.append(", ").append(field.max());
        }
        js.append("), \"").append(field.fieldName()).append("\");\n");
    }

    /** Builds a dropdown/select field definition. */
    private void buildDropdownFieldDefinition(StringBuilder js, BlocklyFieldExtractor.BlocklyFieldInfo field) {
        js.append("            .appendField(new Blockly.FieldDropdown([");
        String[] options = field.options().split(",");
        for (int i = 0; i < options.length; i++) {
            if (i > 0) js.append(", ");
            js.append("[\"").append(options[i].trim()).append("\", \"").append(options[i].trim()).append("\"]");
        }
        js.append("]), \"").append(field.fieldName()).append("\");\n");
    }

    /** Builds a boolean value input field connected to another block. */
    private void buildBooleanFieldDefinition(StringBuilder js, BlocklyFieldExtractor.BlocklyFieldInfo field) {
        if (!field.label().isEmpty()) {
            js.append("        this.appendDummyInput()\n");
            js.append("            .appendField(\"").append(escapeJavaScript(field.label())).append("\");\n");
        }
        js.append("        this.appendValueInput(\"").append(field.fieldName()).append("\")\n");
        js.append("            .setCheck(\"Boolean\");\n");
    }

    /** Builds a color picker field definition. */
    private void buildColorFieldDefinition(StringBuilder js, BlocklyFieldExtractor.BlocklyFieldInfo field) {
        js.append("            .appendField(new Blockly.FieldColour(\"")
                .append(field.defaultValue().isEmpty() ? "#ff0000" : field.defaultValue())
                .append("\"), \"").append(field.fieldName()).append("\");\n");
    }

    /** Builds a checkbox field definition. */
    private void buildCheckboxFieldDefinition(StringBuilder js, BlocklyFieldExtractor.BlocklyFieldInfo field) {
        js.append("            .appendField(new Blockly.FieldCheckbox(")
                .append(field.defaultValue().equalsIgnoreCase("true") ? "true" : "false")
                .append("), \"").append(field.fieldName()).append("\");\n");
    }

    /** Builds a location value input field connected to a location block. */
    private void buildLocationFieldDefinition(StringBuilder js, BlocklyFieldExtractor.BlocklyFieldInfo field) {
        if (!field.label().isEmpty()) {
            js.append("        this.appendDummyInput()\n");
            js.append("            .appendField(\"").append(escapeJavaScript(field.label())).append("\");\n");
        }
        js.append("        this.appendValueInput(\"").append(field.fieldName()).append("\")\n");
        js.append("            .setCheck(\"Location\");\n");
    }

    /**
     * Placeholder kept for backward compatibility. The function_trigger and
     * call_function_action block definitions are now auto-generated from their
     * annotated Java classes (FunctionTrigger, CallFunctionAction) via the
     * normal trigger/action build paths, so no hardcoded definitions are needed.
     *
     * @param js unused; kept so the calling sequence in {@link #generateJavaScript(Player)}
     *           does not have to be rewritten if function blocks regain hardcoded parts later
     */
    private void buildFunctionBlockDefinitions(StringBuilder js) {
        // Intentionally empty: block definitions auto-generated from @BlocklyField annotations.
    }

    /**
     * Builds Blockly utility block definitions such as boolean and location blocks.
     *
     * @param js the StringBuilder to accumulate the generated JavaScript code
     */
    private void buildUtilityBlockDefinitions(StringBuilder js) {
        js.append("// ===== UTILITY BLOCKS =====\n");

        js.append("""
                Blockly.Blocks['boolean_true'] = {
                    init: function() {
                        this.appendDummyInput()
                            .appendField("✅ True");
                        this.setOutput(true, "Boolean");
                        this.setColour('#4CAF50');
                    }
                };
                       \s
                Blockly.Blocks['boolean_false'] = {
                    init: function() {
                        this.appendDummyInput()
                            .appendField("❌ False");
                        this.setOutput(true, "Boolean");
                        this.setColour('#F44336');
                    }
                };
               \s
                Blockly.Blocks['text'] = {
                    init: function() {
                        this.appendDummyInput().appendField(new Blockly.FieldTextInput(""), "TEXT");
                        this.setOutput(true, null);
                        this.setColour('#5C6BC0');
                        this.setTooltip('Text block for values');
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
                        this.setTooltip('Defines a position with X, Y, Z coordinates');
                    }
                };
               \s
                Blockly.Blocks['location_xyz_world'] = {
                    init: function() {
                        this.appendDummyInput()
                            .appendField("🌍 Position + World");
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
                            .appendField("World:")
                            .appendField(new Blockly.FieldTextInput("world"), "WORLD");
                        this.setOutput(true, "Location");
                        this.setColour('#FF9800');
                        this.setTooltip('Defines a position with coordinates and world');
                    }
                };
               \s
                Blockly.Blocks['location_full'] = {
                    init: function() {
                        this.appendDummyInput()
                            .appendField("🎯 Full Position");
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
                            .appendField("World:")
                            .appendField(new Blockly.FieldTextInput("world"), "WORLD");
                        this.appendDummyInput()
                            .appendField("Yaw:")
                            .appendField(new Blockly.FieldNumber(0), "YAW");
                        this.appendDummyInput()
                            .appendField("Pitch:")
                            .appendField(new Blockly.FieldNumber(0), "PITCH");
                        this.setOutput(true, "Location");
                        this.setColour('#FF9800');
                        this.setTooltip('Defines a position with coordinates, world and rotation');
                    }
                };

               \s""");
    }

    /**
     * Builds block definitions for variable management in Blockly.
     * Includes blocks to get and set variables with different scopes.
     *
     * @param js the StringBuilder to accumulate the generated JavaScript code
     */
    private void buildVariableBlockDefinitions(StringBuilder js) {
        js.append("// ===== VARIABLE BLOCKS (AUTO-GENERATED) =====\n");

        js.append("""
                Blockly.Blocks['get_variable'] = {
                    init: function() {
                        this.appendDummyInput()
                            .appendField("📊 Get variable")
                            .appendField(new Blockly.FieldTextInput("{global.my_variable}"), "TEXT");
                        this.setOutput(true, null);
                        this.setColour('#5C6BC0');
                        this.setTooltip('Block to get the value of a variable (e.g: {global.my_variable}, {player.my_variable})');
                    }
                };
                """);
    }

    /**
     * Generates Blockly block definitions for all blocks registered by dynamic modules.
     *
     * @param js the StringBuilder to accumulate the generated JavaScript code
     */
    private void buildDynamicModuleBlocks(StringBuilder js) {
        ModuleLoader moduleLoader = Main.getInstance().getModuleLoader();
        if (moduleLoader == null) return;

        List<ModuleBlockDescriptor> allBlocks = moduleLoader.getBlockRegistry().getAllBlocks();
        if (allBlocks.isEmpty()) return;

        js.append("// ===== BLOCS MODULES DYNAMIQUES =====\n");

        for (ModuleBlockDescriptor descriptor : allBlocks) {
            String blockName = descriptor.getId();
            String color = descriptor.getColor() != null ? descriptor.getColor() : "#9C27B0";

            js.append("Blockly.Blocks['").append(blockName).append("'] = {\n");
            js.append("    init: function() {\n");
            js.append("        this.appendDummyInput()\n");
            js.append("            .appendField(\"").append(escapeJavaScript(descriptor.getLabel())).append("\");\n");

            if (descriptor.getParameters() != null) {
                buildModuleParameterFields(js, descriptor);
            }

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
     * Builds field definitions for a module block based on its parameters. Supports different field types.
     *
     * @param js the StringBuilder of the generated JavaScript
     * @param descriptor the module block descriptor containing parameter information
     */
    private void buildModuleParameterFields(StringBuilder js, ModuleBlockDescriptor descriptor) {
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

    /**
     * Generates toolbox categories for dynamic module blocks grouped by category.
     *
     * @param js the StringBuilder to accumulate the generated JavaScript code
     */
    private void buildModuleToolboxCategories(StringBuilder js) {
        ModuleLoader moduleLoader = Main.getInstance().getModuleLoader();
        if (moduleLoader == null) return;

        List<ModuleBlockDescriptor> allBlocks = moduleLoader.getBlockRegistry().getAllBlocks();
        if (allBlocks.isEmpty()) return;

        Map<String, List<ModuleBlockDescriptor>> byCategory = new LinkedHashMap<>();
        for (ModuleBlockDescriptor block : allBlocks) {
            String category = block.getCategory() != null ? block.getCategory() : block.getModuleId();
            byCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(block);
        }

        for (Map.Entry<String, List<ModuleBlockDescriptor>> entry : byCategory.entrySet()) {
            List<ModuleBlockDescriptor> categoryBlocks = entry.getValue();
            String catColor = categoryBlocks.getFirst().getColor() != null ? categoryBlocks.get(0).getColor() : "#9C27B0";
            js.append("        {\n");
            js.append("            \"kind\": \"category\",\n");
            js.append("            \"name\": \"🧩 ").append(escapeJavaScript(entry.getKey())).append("\",\n");
            js.append("            \"colour\": \"").append(catColor).append("\",\n");
            js.append("            \"contents\": [\n");

            for (ModuleBlockDescriptor block : categoryBlocks) {
                String blockName = block.getId();
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
     *
     * @param js the StringBuilder to accumulate the generated JavaScript code
     */
    private void buildModuleTriggerExtractionCases(StringBuilder js) {
        ModuleLoader moduleLoader = Main.getInstance().getModuleLoader();
        if (moduleLoader == null) return;

        List<ModuleBlockDescriptor> allBlocks = moduleLoader.getBlockRegistry().getAllBlocks();
        for (ModuleBlockDescriptor descriptor : allBlocks) {
            if (descriptor.getType() != ModuleBlockDescriptor.BlockType.TRIGGER) continue;

            String blockName = descriptor.getId();
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
     *
     * @param js the StringBuilder to accumulate the generated JavaScript code
     */
    private void buildModuleTriggerLoadingCases(StringBuilder js) {
        ModuleLoader moduleLoader = Main.getInstance().getModuleLoader();
        if (moduleLoader == null) return;

        List<ModuleBlockDescriptor> allBlocks = moduleLoader.getBlockRegistry().getAllBlocks();
        for (ModuleBlockDescriptor descriptor : allBlocks) {
            if (descriptor.getType() != ModuleBlockDescriptor.BlockType.TRIGGER) continue;

            String blockName = descriptor.getId();
            js.append("                if (trigger.type === '").append(blockName).append("') {\n");
            js.append("                    const triggerBlock = workspace.newBlock('").append(blockName).append("');\n");
            js.append("                    triggerBlock.initSvg();\n");
            js.append("                    triggerBlock.render();\n");

            if (descriptor.getParameters() != null) {
                for (ModuleBlockDescriptor.BlockParameter param : descriptor.getParameters()) {
                    String fieldName = param.getName();
                    String type = param.getType() != null ? param.getType() : "string";
                    if ("boolean".equals(type)) {
                        js.append("                    triggerBlock.setFieldValue(trigger.").append(fieldName)
                                .append(" ? 'TRUE' : 'FALSE', '").append(fieldName).append("');\n");
                    } else {
                        js.append("                    if (trigger.").append(fieldName).append(" !== undefined) triggerBlock.setFieldValue(String(trigger.")
                                .append(fieldName).append("), '").append(fieldName).append("');\n");
                    }
                }
            }

            js.append("                    loadActionsIntoBlock(triggerBlock, trigger.actions);\n");
            js.append("                    triggerBlock.moveBy(20 + (index * 300), 20);\n");
            js.append("                }\n");
        }
    }

    /**
     * Generates JavaScript extraction cases for dynamic module action blocks.
     * When a module action block is encountered in the workspace, this extracts
     * all its field values and creates an action object with the correct type.
     *
     * @param js the StringBuilder to accumulate the generated JavaScript code
     */
    private void buildModuleActionExtractionCases(StringBuilder js) {
        ModuleLoader moduleLoader = Main.getInstance().getModuleLoader();
        if (moduleLoader == null) return;

        List<ModuleBlockDescriptor> allBlocks = moduleLoader.getBlockRegistry().getAllBlocks();
        for (ModuleBlockDescriptor descriptor : allBlocks) {
            if (descriptor.getType() != ModuleBlockDescriptor.BlockType.ACTION) continue;

            String blockName = descriptor.getId();
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
     *
     * @param js the StringBuilder to accumulate the generated JavaScript code
     */
    private void buildModuleActionLoadingCases(StringBuilder js) {
        ModuleLoader moduleLoader = Main.getInstance().getModuleLoader();
        if (moduleLoader == null) return;

        List<ModuleBlockDescriptor> allBlocks = moduleLoader.getBlockRegistry().getAllBlocks();
        for (ModuleBlockDescriptor descriptor : allBlocks) {
            if (descriptor.getType() != ModuleBlockDescriptor.BlockType.ACTION) continue;

            String blockName = descriptor.getId();
            js.append("                if (action.type === '").append(blockName).append("') {\n");
            js.append("                    actionBlock = workspace.newBlock('").append(blockName).append("');\n");
            js.append("                    actionBlock.initSvg();\n");
            js.append("                    actionBlock.render();\n");

            if (descriptor.getParameters() != null) {
                for (ModuleBlockDescriptor.BlockParameter param : descriptor.getParameters()) {
                    String fieldName = param.getName();
                    String type = param.getType() != null ? param.getType() : "string";
                    if ("boolean".equals(type)) {
                        js.append("                    actionBlock.setFieldValue(action.").append(fieldName)
                                .append(" ? 'TRUE' : 'FALSE', '").append(fieldName).append("');\n");
                    } else {
                        js.append("                    if (action.").append(fieldName).append(" !== undefined) actionBlock.setFieldValue(String(action.")
                                .append(fieldName).append("), '").append(fieldName).append("');\n");
                    }
                }
            }

            js.append("                }\n");
        }
    }

    /**
     * Builds the toolbox for Blockly, which contains categories and associated blocks.
     *
     * @param js the StringBuilder to accumulate the generated JavaScript code.
     *           The toolbox is defined as a JSON object and includes categories
     *           for triggers, actions, and utility blocks.
     */
    private void buildToolbox(StringBuilder js) {
        js.append("// ===== AUTO-GENERATED TOOLBOX =====\n");
        js.append("window.DUNGEON_TOOLBOX = {\n");
        js.append("    \"kind\": \"categoryToolbox\",\n");
        js.append("    \"contents\": [\n");

        for (Map.Entry<String, List<Class<? extends BlocklyTrigger>>> entry : triggersByCategory.entrySet()) {
            js.append("        {\n");
            js.append("            \"kind\": \"category\",\n");
            js.append("            \"name\": \"🎯 ").append(entry.getKey()).append("\",\n");
            js.append("            \"colour\": \"#FF6B6B\",\n");
            js.append("            \"contents\": [\n");

            for (Class<? extends BlocklyTrigger> triggerClass : entry.getValue()) {
                BlocklyInfo info = triggerClass.getAnnotation(BlocklyInfo.class);
                String blockName = info.name().isEmpty()
                        ? triggerClass.getSimpleName().toLowerCase().replace("trigger", "_trigger")
                        : info.name();
                js.append("                {\"kind\": \"block\", \"type\": \"").append(blockName).append("\"},\n");
            }

            js.append("            ]\n");
            js.append("        },\n");
        }

        for (Map.Entry<String, List<Class<? extends BlocklyAction>>> entry : actionsByCategory.entrySet()) {
            if (entry.getKey().equals("Actions")) {
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
                String blockName = info.name().isEmpty()
                        ? actionClass.getSimpleName().toLowerCase().replace("action", "_action")
                        : info.name();
                js.append("                {\"kind\": \"block\", \"type\": \"").append(blockName).append("\"},\n");
            }

            js.append("            ]\n");
            js.append("        },\n");
        }

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

        js.append("""
                        {
                            "kind": "category",
                            "name": "🔧 Utilities",
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

        buildModuleToolboxCategories(js);

        js.append("""
                    ]
                };

                console.log('📦 Auto-generated toolbox:', window.DUNGEON_TOOLBOX);

                """);
    }

    /**
     * Builds utility JavaScript functions for Blockly, such as trigger generation,
     * action extraction, and workspace data loading.
     *
     * @param js the StringBuilder to accumulate the generated JavaScript code
     */
    private void buildUtilityFunctions(StringBuilder js) {
        js.append("// ===== AUTO-GENERATED UTILITY FUNCTIONS =====\n");

        buildValueInputHelperFunctions(js);
        buildTriggerGenerationFunction(js);
        buildActionExtractionFunction(js);
        buildTriggerLoadingFunction(js);
        buildActionLoadingFunctions(js);
        buildHelperFunctions(js);

        js.append("""
                console.log('✅ Utility functions auto-generated and loaded');
                """);
    }

    /**
     * Emits JavaScript helpers for value-input fields (LOCATION_INPUT, BOOLEAN_INPUT).
     * These fields attach a child block via appendValueInput and cannot be read/written
     * through get/setFieldValue — they require walking the connection graph.
     *
     * @param js the StringBuilder to accumulate the generated JavaScript code
     */
    private void buildValueInputHelperFunctions(StringBuilder js) {
        js.append("""
                // Reads a Location child block connected to a value input and returns a LocationBlock-shaped object.
                function extractLocationFromInput(block, inputName) {
                    const child = block.getInputTargetBlock(inputName);
                    if (!child) return null;
                    const hasWorld = child.type === 'location_xyz_world' || child.type === 'location_full';
                    const hasRotation = child.type === 'location_full';
                    return {
                        x: Number(child.getFieldValue('X')),
                        y: Number(child.getFieldValue('Y')),
                        z: Number(child.getFieldValue('Z')),
                        worldName: hasWorld ? child.getFieldValue('WORLD') : 'world',
                        yaw: hasRotation ? Number(child.getFieldValue('YAW')) : 0,
                        pitch: hasRotation ? Number(child.getFieldValue('PITCH')) : 0,
                        hasWorld: hasWorld,
                        hasRotation: hasRotation
                    };
                }

                // Creates and connects a Location child block from saved LocationBlock data.
                // Caller must have initSvg/render'd the parent before invoking this helper.
                function loadLocationIntoInput(parentBlock, inputName, locData) {
                    if (!locData) return;
                    const type = locData.hasRotation ? 'location_full'
                               : (locData.hasWorld ? 'location_xyz_world' : 'location_xyz');
                    const child = workspace.newBlock(type);
                    child.setFieldValue(String(locData.x != null ? locData.x : 0), 'X');
                    child.setFieldValue(String(locData.y != null ? locData.y : 0), 'Y');
                    child.setFieldValue(String(locData.z != null ? locData.z : 0), 'Z');
                    if (type !== 'location_xyz') {
                        child.setFieldValue(String(locData.worldName != null ? locData.worldName : 'world'), 'WORLD');
                    }
                    if (type === 'location_full') {
                        child.setFieldValue(String(locData.yaw != null ? locData.yaw : 0), 'YAW');
                        child.setFieldValue(String(locData.pitch != null ? locData.pitch : 0), 'PITCH');
                    }
                    child.initSvg();
                    child.render();
                    const input = parentBlock.getInput(inputName);
                    if (input && input.connection && child.outputConnection) {
                        input.connection.connect(child.outputConnection);
                    }
                }

                // Reads a Boolean child block connected to a value input.
                function extractBooleanFromInput(block, inputName) {
                    const child = block.getInputTargetBlock(inputName);
                    if (!child) return false;
                    return child.type === 'boolean_true';
                }

                // Creates and connects a Boolean child block from saved boolean data.
                // Caller must have initSvg/render'd the parent before invoking this helper.
                function loadBooleanIntoInput(parentBlock, inputName, value) {
                    if (value === null || value === undefined) return;
                    const child = workspace.newBlock(value ? 'boolean_true' : 'boolean_false');
                    child.initSvg();
                    child.render();
                    const input = parentBlock.getInput(inputName);
                    if (input && input.connection && child.outputConnection) {
                        input.connection.connect(child.outputConnection);
                    }
                }

                """);
    }

    /**
     * Builds the JavaScript function for generating triggers from the workspace.
     *
     * @param js the StringBuilder to accumulate the generated JavaScript code
     */
    private void buildTriggerGenerationFunction(StringBuilder js) {
        js.append("""
                // Trigger generation from workspace
                function generateTriggersFromWorkspace() {
                    console.log('🔄 Generating triggers...');
                    const triggers = [];
                    const blocks = workspace.getTopBlocks();
                   \s
                    blocks.forEach(block => {
                        console.log('Block found:', block.type);
                       \s
               \s""");

        generateAllTriggerCases(js);
        buildModuleTriggerExtractionCases(js);

        js.append("""
                    });
                   \s
                    console.log('Triggers generated:', triggers);
                    return triggers;
                }
                       \s
                """);
    }

    /**
     * Builds the JavaScript function for extracting actions from a block.
     *
     * @param js the StringBuilder to accumulate the generated JavaScript code
     */
    private void buildActionExtractionFunction(StringBuilder js) {
        js.append("""
                // Action extraction from block
                function getActionsFromBlock(block) {
                    const actions = [];
                    let actionBlock = block.getInputTargetBlock('ACTIONS');
                   \s
                    while (actionBlock) {
                        console.log('Action found:', actionBlock.type);
                       \s
               \s""");

        generateAllActionCases(js);
        buildModuleActionExtractionCases(js);

        js.append("""
                        actionBlock = actionBlock.getNextBlock();
                    }
                   \s
                    return actions;
                }
                       \s
                """);
    }

    /**
     * Builds the JavaScript function for loading triggers into the workspace.
     *
     * @param js the StringBuilder to accumulate the generated JavaScript code
     */
    private void buildTriggerLoadingFunction(StringBuilder js) {
        js.append("""
                // Function to load triggers into workspace
                function loadTriggersIntoWorkspace(triggersData) {
                    console.log('🔄 Loading triggers into workspace...');
                    console.log('Received data:', triggersData);
                   \s
                    // Clear workspace before loading
                    workspace.clear();
                   \s
                    if (!triggersData || !triggersData.triggers) {
                        console.log('No triggers to load');
                        return;
                    }
                   \s
                    console.log('Number of triggers to load:', triggersData.triggers.length);
                           \s
                    triggersData.triggers.forEach((trigger, index) => {
                        console.log('Loading trigger #' + (index + 1) + ':', trigger);
                       \s
               \s""");

        generateAllTriggerLoadingCases(js);
        buildModuleTriggerLoadingCases(js);

        js.append("""
                    });
                   \s
                    console.log('✅ Triggers loaded into workspace');
                }
                       \s
                """);
    }

    /**
     * Builds JavaScript functions for loading actions into blocks.
     *
     * @param js the StringBuilder to accumulate the generated JavaScript code
     */
    private void buildActionLoadingFunctions(StringBuilder js) {
        js.append("""
                // Function to load actions into a block
                function loadActionsIntoBlock(triggerBlock, actions) {
                    if (!actions || actions.length === 0) return;
                   \s
                    let previousActionBlock = null;
                    const actionsInput = triggerBlock.getInput('ACTIONS');
                   \s
                    actions.forEach((action, index) => {
                        let actionBlock = null;
               \s""");

        generateAllActionLoadingCases(js);
        buildModuleActionLoadingCases(js);

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
                """);
    }

    /**
     * Builds helper JavaScript functions for action and statement handling.
     *
     * @param js the StringBuilder to accumulate the generated JavaScript code
     */
    private void buildHelperFunctions(StringBuilder js) {
        js.append("""
                // Helper function to extract actions from statement inputs (for IF/ELSE blocks)
                function getActionsFromStatementInput(block, inputName) {
                    const actions = [];
                    let actionBlock = block.getInputTargetBlock(inputName);
                   \s
                    while (actionBlock) {
                        console.log('Action found in statement:', actionBlock.type);
                       \s
                """);

        generateAllActionCases(js);
        buildModuleActionExtractionCases(js);

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

        generateActionsLoadingCasesPerCategory(js);
        buildModuleActionLoadingCases(js);

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
                """);
    }

    /**
     * Builds JavaScript generation cases for all trigger blocks and inserts them.
     *
     * @param js the StringBuilder to accumulate the generated JavaScript code
     */
    private void generateAllTriggerCases(StringBuilder js) {
        for (Map.Entry<String, List<Class<? extends BlocklyTrigger>>> entry : triggersByCategory.entrySet()) {
            for (Class<? extends BlocklyTrigger> triggerClass : entry.getValue()) {
                buildTriggerExtractionCase(js, triggerClass);
            }
        }
    }

    /**
     * Builds JavaScript extraction cases for all action blocks and inserts them.
     *
     * <p><b>Bug fix</b>: previously, when a class returned {@code true} from
     * {@code requiresCustomBlockGeneration()} and its {@code generateCustomActionCase()}
     * threw (for instance because the subclass did not override it), the exception was
     * silently swallowed by an empty {@code catch} block. As a result, these actions
     * were <b>never emitted in the {@code getActionsFromBlock} switch</b>, so they were
     * dropped from the JSON payload on save while their definitions and loading cases
     * kept working. The fix now mirrors the fallback behaviour already present in
     * {@link #generateActionsLoadingCasesPerCategory(StringBuilder)}.</p>
     *
     * @param js the StringBuilder to accumulate the generated JavaScript code
     */
    private void generateAllActionCases(StringBuilder js) {
        for (Map.Entry<String, List<Class<? extends BlocklyAction>>> entry : actionsByCategory.entrySet()) {
            for (Class<? extends BlocklyAction> actionClass : entry.getValue()) {
                try {
                    BlocklyAction instance = actionClass.getDeclaredConstructor().newInstance();
                    if (instance.requiresCustomBlockGeneration()) {
                        try {
                            instance.generateCustomActionCase(js);
                        } catch (Exception customEx) {
                            LOGGER.log(Level.WARNING,
                                    actionClass.getSimpleName() + ".generateCustomActionCase() threw — "
                                    + "falling back to annotation-based extraction. "
                                    + "Previously this exception was swallowed, causing the action "
                                    + "to be silently dropped from the JSON payload on save.",
                                    customEx);
                            buildActionExtractionCase(js, actionClass);
                        }
                    } else {
                        buildActionExtractionCase(js, actionClass);
                    }
                } catch (ReflectiveOperationException e) {
                    LOGGER.log(Level.WARNING,
                            "Cannot instantiate " + actionClass.getSimpleName()
                            + " — using annotation-based extraction",
                            e);
                    buildActionExtractionCase(js, actionClass);
                }
            }
        }
    }

    /**
     * Builds JavaScript loading cases for all trigger blocks.
     *
     * @param js the StringBuilder to accumulate the generated JavaScript code
     */
    private void generateAllTriggerLoadingCases(StringBuilder js) {
        for (Map.Entry<String, List<Class<? extends BlocklyTrigger>>> entry : triggersByCategory.entrySet()) {
            for (Class<? extends BlocklyTrigger> triggerClass : entry.getValue()) {
                buildTriggerLoadingCase(js, triggerClass);
            }
        }
    }

    /**
     * Builds action loading cases for each action category.
     *
     * <p>This method already had a correct fallback for swallowed exceptions — it is used
     * as the reference for the fix applied to {@link #generateAllActionCases(StringBuilder)}.</p>
     *
     * @param js the StringBuilder to accumulate the generated JavaScript code
     */
    private void generateActionsLoadingCasesPerCategory(StringBuilder js) {
        for (Map.Entry<String, List<Class<? extends BlocklyAction>>> entry : actionsByCategory.entrySet()) {
            for (Class<? extends BlocklyAction> actionClass : entry.getValue()) {
                try {
                    BlocklyAction instance = actionClass.getDeclaredConstructor().newInstance();
                    if (instance.requiresCustomBlockGeneration()) {
                        try {
                            instance.generateCustomActionLoadingCase(js);
                        } catch (Exception customEx) {
                            LOGGER.log(Level.WARNING,
                                    actionClass.getSimpleName() + ".generateCustomActionLoadingCase() threw — "
                                    + "falling back to annotation-based loading",
                                    customEx);
                            buildActionLoadingCase(js, actionClass);
                        }
                    } else {
                        buildActionLoadingCase(js, actionClass);
                    }
                } catch (ReflectiveOperationException e) {
                    LOGGER.log(Level.WARNING,
                            "Cannot instantiate " + actionClass.getSimpleName()
                            + " — using annotation-based loading",
                            e);
                    buildActionLoadingCase(js, actionClass);
                }
            }
        }
    }

    /**
     * Builds JavaScript loading cases for all action blocks.
     *
     * @param js the StringBuilder to accumulate the generated JavaScript code
     */
    private void generateAllActionLoadingCases(StringBuilder js) {
        generateActionsLoadingCasesPerCategory(js);
    }

    /**
     * Escapes special characters in a text string so it is compatible with JavaScript format.
     * This includes escape characters, double quotes, line breaks, and carriage returns.
     *
     * @param text the text string to escape
     * @return the escaped text string
     */
    private String escapeJavaScript(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    /**
     * Builds a specific extraction case for a trigger in the JavaScript code.
     *
     * @param js the StringBuilder to accumulate the generated JavaScript code
     * @param triggerClass the trigger class to process, annotated with {@code @BlocklyInfo}
     */
    private void buildTriggerExtractionCase(StringBuilder js, Class<? extends BlocklyTrigger> triggerClass) {
        BlocklyInfo info = triggerClass.getAnnotation(BlocklyInfo.class);
        String triggerType = info.name();
        List<BlocklyFieldExtractor.BlocklyFieldInfo> fields = BlocklyFieldExtractor.extractFields(triggerClass);

        js.append("                if (block.type === '").append(triggerType).append("') {\n");
        js.append("                    triggers.push({\n");
        js.append("                        type: '").append(triggerType).append("',\n");
        js.append("                        name: '").append(triggerClass.getSimpleName()).append("_' + uuidv4(),\n");

        for (BlocklyFieldExtractor.BlocklyFieldInfo field : fields) {
            js.append("                        ");
            buildFieldValueExtraction(js, field, "block");
            js.append(",\n");
        }

        try {
            BlocklyTrigger instance = triggerClass.getDeclaredConstructor().newInstance();
            if (instance.hasActions()) {
                js.append("                        actions: getActionsFromBlock(block)\n");
            } else {
                js.append("                        actions: []\n");
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE,
                    "Cannot instantiate " + triggerClass.getSimpleName()
                    + " while building extraction case — defaulting to hasActions()=true",
                    e);
            js.append("                        actions: getActionsFromBlock(block)\n");
        }

        js.append("                    });\n");
        js.append("                }\n");
    }

    /**
     * Builds a specific extraction case for an action in the JavaScript code.
     *
     * @param js the StringBuilder to accumulate the generated JavaScript code
     * @param actionClass the action class to process, annotated with {@code @BlocklyInfo}
     */
    private void buildActionExtractionCase(StringBuilder js, Class<? extends BlocklyAction> actionClass) {
        BlocklyInfo info = actionClass.getAnnotation(BlocklyInfo.class);
        String actionType = info.name();
        List<BlocklyFieldExtractor.BlocklyFieldInfo> fields = BlocklyFieldExtractor.extractFields(actionClass);

        js.append("                if (actionBlock.type === '").append(actionType).append("') {\n");
        js.append("                    actions.push({\n");
        js.append("                        type: '").append(actionType).append("'");

        for (BlocklyFieldExtractor.BlocklyFieldInfo field : fields) {
            js.append(",\n                        ");
            buildFieldValueExtraction(js, field, "actionBlock");
        }

        js.append("\n                    });\n");
        js.append("                }\n");
    }

    /**
     * Builds a specific case for loading a trigger into the Blockly workspace.
     *
     * @param js the StringBuilder to accumulate the generated JavaScript code
     * @param triggerClass the trigger class to process, annotated with {@code @BlocklyInfo}
     */
    private void buildTriggerLoadingCase(StringBuilder js, Class<? extends BlocklyTrigger> triggerClass) {
        BlocklyInfo info = triggerClass.getAnnotation(BlocklyInfo.class);
        String triggerType = info.name();
        List<BlocklyFieldExtractor.BlocklyFieldInfo> fields = BlocklyFieldExtractor.extractFields(triggerClass);

        js.append("                if (trigger.type === '").append(triggerType).append("') {\n");
        js.append("                    const triggerBlock = workspace.newBlock('").append(triggerType).append("');\n");
        js.append("                    triggerBlock.initSvg();\n");
        js.append("                    triggerBlock.render();\n");

        for (BlocklyFieldExtractor.BlocklyFieldInfo field : fields) {
            buildFieldValueLoading(js, field, "triggerBlock", "trigger");
        }

        js.append("                    loadActionsIntoBlock(triggerBlock, trigger.actions);\n");
        js.append("                    triggerBlock.moveBy(20 + (index * 300), 20);\n");
        js.append("                }\n");
    }

    /**
     * Builds a specific case for loading an action into the Blockly workspace.
     *
     * @param js the StringBuilder to accumulate the generated JavaScript code
     * @param actionClass the action class to process, annotated with {@code @BlocklyInfo}
     */
    private void buildActionLoadingCase(StringBuilder js, Class<? extends BlocklyAction> actionClass) {
        BlocklyInfo info = actionClass.getAnnotation(BlocklyInfo.class);
        String actionType = info.name();
        List<BlocklyFieldExtractor.BlocklyFieldInfo> fields = BlocklyFieldExtractor.extractFields(actionClass);

        js.append("                if (action.type === '").append(actionType).append("') {\n");
        js.append("                    actionBlock = workspace.newBlock('").append(actionType).append("');\n");
        js.append("                    actionBlock.initSvg();\n");
        js.append("                    actionBlock.render();\n");

        for (BlocklyFieldExtractor.BlocklyFieldInfo field : fields) {
            buildFieldValueLoading(js, field, "actionBlock", "action");
        }

        js.append("                }\n");
    }

    /**
     * Builds the JavaScript code for a specific field value extraction.
     *
     * @param js the StringBuilder to accumulate the generated JavaScript code
     * @param field the field information
     * @param source the source object (e.g., "block" or "actionBlock")
     */
    private void buildFieldValueExtraction(StringBuilder js, BlocklyFieldExtractor.BlocklyFieldInfo field, String source) {
        switch (field.type()) {
            case TEXT_INPUT     -> js.append(field.fieldName()).append(": ").append(source).append(".getFieldValue('").append(field.fieldName()).append("')");
            case NUMBER_INPUT   -> js.append(field.fieldName()).append(": Number(").append(source).append(".getFieldValue('").append(field.fieldName()).append("'))");
            case DROPDOWN       -> js.append(field.fieldName()).append(": ").append(source).append(".getFieldValue('").append(field.fieldName()).append("')");
            case BOOLEAN_INPUT  -> js.append(field.fieldName()).append(": extractBooleanFromInput(").append(source).append(", '").append(field.fieldName()).append("')");
            case COLOR_INPUT    -> js.append(field.fieldName()).append(": ").append(source).append(".getFieldValue('").append(field.fieldName()).append("')");
            case CHECKBOX       -> js.append(field.fieldName()).append(": ").append(source).append(".getFieldValue('").append(field.fieldName()).append("') === 'TRUE'");
            case LOCATION_INPUT -> js.append(field.fieldName()).append(": extractLocationFromInput(").append(source).append(", '").append(field.fieldName()).append("')");
        }
    }

    /**
     * Generates the JavaScript that pushes a single field value onto a Blockly block.
     *
     * @param js the StringBuilder to accumulate the generated JavaScript code
     * @param field the field information
     * @param targetBlock JS identifier of the Blockly block receiving the value
     *                    (e.g. {@code "triggerBlock"} or {@code "actionBlock"}) —
     *                    only this object exposes {@code setFieldValue}
     * @param sourceData  JS identifier of the plain data object holding the value
     *                    (e.g. {@code "trigger"} or {@code "action"})
     */
    private void buildFieldValueLoading(StringBuilder js, BlocklyFieldExtractor.BlocklyFieldInfo field,
                                        String targetBlock, String sourceData) {
        String name = field.fieldName();
        String prefix = "                    " + targetBlock + ".setFieldValue(";
        String suffix = ", '" + name + "');\n";
        switch (field.type()) {
            case TEXT_INPUT, DROPDOWN, COLOR_INPUT ->
                    js.append(prefix).append(sourceData).append('.').append(name).append(suffix);
            case NUMBER_INPUT ->
                    js.append(prefix).append("Number(").append(sourceData).append('.').append(name).append(")").append(suffix);
            case CHECKBOX ->
                    js.append(prefix).append(sourceData).append('.').append(name)
                            .append(" ? 'TRUE' : 'FALSE'").append(suffix);
            case LOCATION_INPUT ->
                    js.append("                    loadLocationIntoInput(").append(targetBlock)
                            .append(", '").append(name).append("', ").append(sourceData).append('.').append(name).append(");\n");
            case BOOLEAN_INPUT ->
                    js.append("                    loadBooleanIntoInput(").append(targetBlock)
                            .append(", '").append(name).append("', ").append(sourceData).append('.').append(name).append(");\n");
        }
    }
}