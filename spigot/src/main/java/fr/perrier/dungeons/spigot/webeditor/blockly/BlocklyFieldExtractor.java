package fr.perrier.dungeons.spigot.webeditor.blockly;

import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyField;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for extracting and caching field metadata from classes annotated with {@link BlocklyField}.
 * <p>
 * This class uses reflection to introspect a target class and identify all fields that are
 * marked with the {@code @BlocklyField} annotation. The extracted field information is cached
 * in a thread-safe manner to avoid redundant reflection operations.
 * <p>
 * <strong>Usage:</strong>
 * <pre>
 * List&lt;BlocklyFieldInfo&gt; fields = BlocklyFieldExtractor.extractFields(MyClass.class);
 * fields.forEach(field -&gt; System.out.println(field.label() + ": " + field.type()));
 * </pre>
 * <p>
 * <strong>Caching Strategy:</strong> Results are cached per class using a {@link ConcurrentHashMap},
 * making this extractor suitable for repeated calls on the same class in concurrent environments.
 * The cache is populated lazily on first access.
 * <p>
 * <strong>Field Ordering:</strong> Extracted fields are automatically sorted by their
 * {@code order} attribute as defined in the {@code @BlocklyField} annotation, allowing
 * predictable field sequencing in UI representations.
 *
 * @see BlocklyField
 * @see BlocklyFieldInfo
 */
public class BlocklyFieldExtractor {

    /**
     * Thread-safe cache mapping classes to their extracted Blockly field metadata.
     * <p>
     * Key: A {@code Class&lt;?&gt;} to introspect.
     * Value: A cached list of {@link BlocklyFieldInfo} records extracted from that class,
     * sorted by field order.
     * <p>
     * Uses {@link ConcurrentHashMap} to support safe concurrent access without external synchronization.
     */
    private static final Map<Class<?>, List<BlocklyFieldInfo>> FIELD_CACHE = new ConcurrentHashMap<>();

    /**
     * Extracts and caches Blockly field information from a target class.
     * <p>
     * This method performs a cache lookup first via {@link Map#computeIfAbsent(Object, java.util.function.Function)},
     * and only calls {@link #computeFields(Class)} if the class is not already cached.
     * This lazy initialization minimizes reflection overhead on repeated invocations.
     *
     * @param clazz the class to introspect for {@code @BlocklyField} annotated fields
     * @return a list of {@link BlocklyFieldInfo} records extracted from {@code clazz},
     *         sorted by their {@code order} attribute in ascending order.
     *         Returns an empty list if no fields are annotated.
     * @throws NullPointerException if {@code clazz} is null
     * @see #computeFields(Class)
     */
    public static List<BlocklyFieldInfo> extractFields(Class<?> clazz) {
        return FIELD_CACHE.computeIfAbsent(clazz, BlocklyFieldExtractor::computeFields);
    }

    /**
     * Performs the actual field extraction and metadata assembly.
     * <p>
     * This method iterates over all declared fields in the target class via
     * {@link Class#getDeclaredFields()}. For each field marked with the {@code @BlocklyField}
     * annotation, a {@link BlocklyFieldInfo} record is created containing:
     * <ul>
     *   <li>Field name (unmodified from the source field)</li>
     *   <li>Blockly field type (from annotation)</li>
     *   <li>Display label (from annotation, or defaults to field name if empty)</li>
     *   <li>Default value string (from annotation)</li>
     *   <li>Options string (comma-separated or format-specific, from annotation)</li>
     *   <li>Minimum value constraint (from annotation)</li>
     *   <li>Maximum value constraint (from annotation)</li>
     *   <li>Display order (from annotation)</li>
     * </ul>
     * <p>
     * The resulting list is sorted in ascending order by the {@code order} field,
     * ensuring consistent field sequencing.
     * <p>
     * <strong>Note:</strong> This method is private and typically not called directly;
     * use {@link #extractFields(Class)} instead to benefit from caching.
     *
     * @param clazz the class to introspect
     * @return a sorted list of {@link BlocklyFieldInfo} records; empty list if no
     *         annotated fields are found
     */
    private static List<BlocklyFieldInfo> computeFields(Class<?> clazz) {
        List<BlocklyFieldInfo> fields = new ArrayList<>();

        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(BlocklyField.class)) {
                BlocklyField annotation = field.getAnnotation(BlocklyField.class);
                fields.add(new BlocklyFieldInfo(
                        field.getName(),
                        annotation.type(),
                        annotation.label().isEmpty() ? field.getName() : annotation.label(),
                        annotation.defaultValue(),
                        annotation.options(),
                        annotation.min(),
                        annotation.max(),
                        annotation.order()
                ));
            }
        }

        // Sort by order attribute
        fields.sort(Comparator.comparing(BlocklyFieldInfo::order));

        return fields;
    }

    /**
     * Immutable record containing metadata for a single Blockly field.
     * <p>
     * This record encapsulates all information required to represent a field in a Blockly
     * visual programming interface, including its type, constraints, and display properties.
     *
     * @param fieldName     the name of the Java field as declared in the source class
     * @param type          the Blockly field type (e.g., text, number, dropdown)
     * @param label         the human-readable display label for the field in the UI;
     *                      never null or empty (defaults to fieldName if not specified)
     * @param defaultValue  the default value to pre-populate the field, as a string;
     *                      may be empty for optional fields
     * @param options       a format-specific string encoding field options (e.g., comma-separated
     *                      values for dropdowns, or JSON for complex configurations);
     *                      may be empty if no options are applicable
     * @param min           the minimum numeric value constraint; used for numeric fields and ignored for others
     * @param max           the maximum numeric value constraint; used for numeric fields and ignored for others
     * @param order         the display order index; fields are sorted ascending by this value to
     *                      determine presentation sequence in the UI
     */
    public record BlocklyFieldInfo(String fieldName, BlocklyField.FieldType type, String label, String defaultValue,
                                   String options, double min, double max, int order) {
    }
}
