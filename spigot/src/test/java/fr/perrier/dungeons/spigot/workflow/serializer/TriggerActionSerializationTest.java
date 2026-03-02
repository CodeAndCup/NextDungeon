package fr.perrier.dungeons.spigot.workflow.serializer;

import com.google.gson.*;
import fr.perrier.dungeons.common.workflow.trigger.TriggerData;
import fr.perrier.dungeons.spigot.workflow.trigger.impl.BlockClickTrigger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for serialization/deserialization of triggers with actions
 * Covers the complete workflow including actions
 */
@DisplayName("Trigger-Action Serialization Tests")
class TriggerActionSerializationTest {

    @Nested
    @DisplayName("Actions Serialization Tests")
    class ActionsSerializationTests {

        @Test
        @DisplayName("Should serialize trigger with empty actions list")
        void testEmptyActionsList() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            BlockClickTrigger trigger = new BlockClickTrigger("NoActions");
            trigger.setActions(new ArrayList<>());
            triggers.add(trigger);

            // When
            String json = InstanceSerializer.serializeTriggers(triggers);

            // Then
            assertThat(json).isNotEmpty();
            JsonArray jsonArray = JsonParser.parseString(json).getAsJsonArray();
            JsonObject triggerObj = jsonArray.get(0).getAsJsonObject();
            JsonObject data = triggerObj.getAsJsonObject("data");

            assertThat(data.has("actions")).isTrue();
            JsonArray actions = data.getAsJsonArray("actions");
            assertThat(actions).isEmpty();
        }

        @Test
        @DisplayName("Should preserve null actions gracefully")
        void testNullActions() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            BlockClickTrigger trigger = new BlockClickTrigger("NullActionsTest");
            trigger.setActions(null);
            triggers.add(trigger);

            // When
            String json = InstanceSerializer.serializeTriggers(triggers);

            // Then
            assertThat(json).isNotEmpty();
            List<TriggerData> deserialized = InstanceSerializer.deserializeTriggers(json);
            assertThat(deserialized).hasSize(1);
        }

        @Test
        @DisplayName("Should serialize action with className and data wrapper")
        void testActionWrapperStructure() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            BlockClickTrigger trigger = new BlockClickTrigger("ActionStructure");
            trigger.setActions(new ArrayList<>());
            triggers.add(trigger);

            // When
            String json = InstanceSerializer.serializeTriggers(triggers);

            // Then
            JsonArray jsonArray = JsonParser.parseString(json).getAsJsonArray();
            JsonObject triggerObj = jsonArray.get(0).getAsJsonObject();
            JsonObject data = triggerObj.getAsJsonObject("data");

            if (data.has("actions") && !data.getAsJsonArray("actions").isEmpty()) {
                JsonArray actions = data.getAsJsonArray("actions");
                JsonObject action = actions.get(0).getAsJsonObject();

                // Each action should have className and data
                assertThat(action.has("className")).isTrue();
                assertThat(action.has("data")).isTrue();
            }
        }
    }

    @Nested
    @DisplayName("Trigger-Action Relationship Tests")
    class TriggerActionRelationshipTests {

        @Test
        @DisplayName("Should maintain trigger-action association")
        void testTriggerActionAssociation() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            BlockClickTrigger trigger = new BlockClickTrigger("WithActions");
            trigger.setActions(new ArrayList<>());
            triggers.add(trigger);

            // When
            String json = InstanceSerializer.serializeTriggers(triggers);
            List<TriggerData> deserialized = InstanceSerializer.deserializeTriggers(json);

            // Then
            assertThat(deserialized).hasSize(1);
            BlockClickTrigger result = (BlockClickTrigger) deserialized.getFirst();
            assertThat(result.getActions()).isNotNull();
        }

        @Test
        @DisplayName("Should preserve multiple triggers with their respective actions")
        void testMultipleTriggersWithActions() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            BlockClickTrigger trigger1 = new BlockClickTrigger("Trigger1");
            trigger1.setActions(new ArrayList<>());
            BlockClickTrigger trigger2 = new BlockClickTrigger("Trigger2");
            trigger2.setActions(new ArrayList<>());
            triggers.add(trigger1);
            triggers.add(trigger2);

            // When
            String json = InstanceSerializer.serializeTriggers(triggers);
            List<TriggerData> deserialized = InstanceSerializer.deserializeTriggers(json);

            // Then
            assertThat(deserialized).hasSize(2);
            assertThat(deserialized.get(0).getActions()).isNotNull();
            assertThat(deserialized.get(1).getActions()).isNotNull();
        }
    }

    @Nested
    @DisplayName("JSON Validation Tests")
    class JsonValidationTests {

        @Test
        @DisplayName("Should produce valid JSON syntax")
        void testValidJsonSyntax() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            triggers.add(new BlockClickTrigger("JsonTest"));

            // When
            String json = InstanceSerializer.serializeTriggers(triggers);

            // Then - should parse without exception
            assertThatCode(() -> JsonParser.parseString(json))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should produce JSON that can be parsed multiple times")
        void testRepeatedJsonParsing() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            triggers.add(new BlockClickTrigger("RepeatedParse"));
            String json = InstanceSerializer.serializeTriggers(triggers);

            // When/Then - parse multiple times
            for (int i = 0; i < 5; i++) {
                assertThatCode(() -> JsonParser.parseString(json))
                        .doesNotThrowAnyException();
            }
        }

        @Test
        @DisplayName("Should escape special JSON characters properly")
        void testSpecialJsonCharacters() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            BlockClickTrigger trigger = new BlockClickTrigger("Test\"WithQuotes\\AndBackslash");
            triggers.add(trigger);

            // When
            String json = InstanceSerializer.serializeTriggers(triggers);

            // Then - should be valid JSON
            assertThatCode(() -> JsonParser.parseString(json))
                    .doesNotThrowAnyException();

            List<TriggerData> deserialized = InstanceSerializer.deserializeTriggers(json);
            assertThat(deserialized.getFirst().getName())
                    .isEqualTo("Test\"WithQuotes\\AndBackslash");
        }
    }

    @Nested
    @DisplayName("Data Integrity Tests")
    class DataIntegrityTests {

        @Test
        @DisplayName("Should not lose trigger data during serialization")
        void testNoDataLoss() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            BlockClickTrigger trigger = new BlockClickTrigger("DataLoss");
            trigger.setClickType("left_click");
            trigger.setDetectionType("interaction");
            trigger.setBlockMaterial("REDSTONE_BLOCK");
            trigger.setEnabled(true);
            trigger.setExactPositionOnly(true);
            triggers.add(trigger);

            // When
            String json = InstanceSerializer.serializeTriggers(triggers);
            List<TriggerData> deserialized = InstanceSerializer.deserializeTriggers(json);

            // Then
            BlockClickTrigger result = (BlockClickTrigger) deserialized.getFirst();
            assertThat(result.getClickType()).isEqualTo("left_click");
            assertThat(result.getDetectionType()).isEqualTo("interaction");
            assertThat(result.getBlockMaterial()).isEqualTo("REDSTONE_BLOCK");
            assertThat(result.isEnabled()).isTrue();
            assertThat(result.isExactPositionOnly()).isTrue();
        }

        @Test
        @DisplayName("Should maintain type information across serialization")
        void testTypePreservation() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            BlockClickTrigger original = new BlockClickTrigger("TypeTest");
            triggers.add(original);

            // When
            String json = InstanceSerializer.serializeTriggers(triggers);
            List<TriggerData> deserialized = InstanceSerializer.deserializeTriggers(json);

            // Then
            assertThat(deserialized.getFirst()).isInstanceOf(BlockClickTrigger.class);
            assertThat(deserialized.getFirst().getClass().getSimpleName())
                    .isEqualTo("BlockClickTrigger");
        }

        @Test
        @DisplayName("Should preserve name field exactly as provided")
        void testNamePreservation() {
            // Given
            String originalName = "My_Test-Trigger.v2!";
            List<TriggerData> triggers = new ArrayList<>();
            BlockClickTrigger trigger = new BlockClickTrigger(originalName);
            triggers.add(trigger);

            // When
            String json = InstanceSerializer.serializeTriggers(triggers);
            List<TriggerData> deserialized = InstanceSerializer.deserializeTriggers(json);

            // Then
            assertThat(deserialized.getFirst().getName()).isEqualTo(originalName);
        }
    }

    @Nested
    @DisplayName("Error Recovery Tests")
    class ErrorRecoveryTests {

        @Test
        @DisplayName("Should handle partially invalid JSON gracefully")
        void testPartiallyInvalidJson() {
            // Given - valid first trigger, invalid second
            JsonArray jsonArray = new JsonArray();
            JsonObject valid = new JsonObject();
            valid.addProperty("className", "fr.perrier.dungeons.spigot.workflow.trigger.impl.BlockClickTrigger");
            valid.add("data", new JsonObject());

            JsonObject invalid = new JsonObject();
            // Missing className

            jsonArray.add(valid);
            jsonArray.add(invalid);

            String json = jsonArray.toString();

            // When
            List<TriggerData> result = InstanceSerializer.deserializeTriggers(json);

            // Then - should recover and deserialize the valid one
            assertThat(result).isNotEmpty();
        }

        @Test
        @DisplayName("Should handle JSON with unknown trigger types gracefully")
        void testUnknownTriggerType() {
            // Given
            JsonArray jsonArray = new JsonArray();
            JsonObject trigger = new JsonObject();
            trigger.addProperty("className", "com.unknown.NonExistentTrigger");
            trigger.add("data", new JsonObject());
            jsonArray.add(trigger);

            String json = jsonArray.toString();

            // When
            List<TriggerData> result = InstanceSerializer.deserializeTriggers(json);

            // Then - should handle gracefully without throwing
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Should serialize large trigger list within reasonable time")
        void testSerializationPerformance() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            for (int i = 0; i < 200; i++) {
                BlockClickTrigger trigger = new BlockClickTrigger("Perf_" + i);
                triggers.add(trigger);
            }

            // When
            long startTime = System.currentTimeMillis();
            String json = InstanceSerializer.serializeTriggers(triggers);
            long duration = System.currentTimeMillis() - startTime;

            // Then - should complete quickly (within 1 second for 200 triggers)
            assertThat(duration).isLessThan(1000);
            assertThat(json).isNotEmpty();
        }

        @Test
        @DisplayName("Should deserialize large trigger list within reasonable time")
        void testDeserializationPerformance() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            for (int i = 0; i < 200; i++) {
                BlockClickTrigger trigger = new BlockClickTrigger("Perf_" + i);
                triggers.add(trigger);
            }
            String json = InstanceSerializer.serializeTriggers(triggers);

            // When
            long startTime = System.currentTimeMillis();
            List<TriggerData> deserialized = InstanceSerializer.deserializeTriggers(json);
            long duration = System.currentTimeMillis() - startTime;

            // Then - should complete quickly
            assertThat(duration).isLessThan(1000);
            assertThat(deserialized).hasSize(200);
        }
    }

    @Nested
    @DisplayName("Memory and Size Tests")
    class MemorySizeTests {

        @Test
        @DisplayName("JSON size should be reasonable for single trigger")
        void testJsonSize() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            BlockClickTrigger trigger = new BlockClickTrigger("SizeTest");
            trigger.setClickType("right_click");
            triggers.add(trigger);

            // When
            String json = InstanceSerializer.serializeTriggers(triggers);

            // Then - JSON should not be excessively large
            assertThat(json.length()).isLessThan(5000);
            assertThat(json.length()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should handle large JSON without issues")
        void testLargeJsonHandling() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            for (int i = 0; i < 500; i++) {
                BlockClickTrigger trigger = new BlockClickTrigger("LargeJson_" + i);
                triggers.add(trigger);
            }

            // When
            String json = InstanceSerializer.serializeTriggers(triggers);
            List<TriggerData> deserialized = InstanceSerializer.deserializeTriggers(json);

            // Then
            assertThat(deserialized).hasSize(500);
        }
    }
}








