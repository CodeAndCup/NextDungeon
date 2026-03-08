package fr.perrier.dungeons.spigot.webeditor.blockly;

import fr.perrier.dungeons.spigot.webeditor.blockly.annotations.BlocklyField;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BlocklyFieldExtractor {

    private static final Map<Class<?>, List<BlocklyFieldInfo>> FIELD_CACHE = new ConcurrentHashMap<>();

    public static List<BlocklyFieldInfo> extractFields(Class<?> clazz) {
        return FIELD_CACHE.computeIfAbsent(clazz, BlocklyFieldExtractor::computeFields);
    }

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

        // Trier par ordre
        fields.sort(Comparator.comparing(BlocklyFieldInfo::order));

        return fields;
    }

    public record BlocklyFieldInfo(String fieldName, BlocklyField.FieldType type, String label, String defaultValue,
                                   String options, double min, double max, int order) {
    }
}
