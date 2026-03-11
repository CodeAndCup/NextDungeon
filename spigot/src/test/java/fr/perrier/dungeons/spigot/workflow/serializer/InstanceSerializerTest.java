package fr.perrier.dungeons.spigot.workflow.serializer;

import com.google.gson.*;
import fr.perrier.dungeons.common.workflow.trigger.TriggerData;
import fr.perrier.dungeons.spigot.workflow.trigger.impl.BlockClickTrigger;
import fr.perrier.dungeons.spigot.workflow.trigger.impl.RegionTrigger;
import fr.perrier.dungeons.spigot.workflow.blocks.LocationBlock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("InstanceSerializer Tests")
class InstanceSerializerTest {

    @Nested
    @DisplayName("Serialization Tests")
    class SerializationTests {

        @Test
        @DisplayName("Should serialize null triggers list as empty array")
        void testSerializeNullTriggers() {
            // When
            String result = InstanceSerializer.serializeTriggers(null);

            // Then
            assertThat(result).isEqualTo("[]");
        }

        @Test
        @DisplayName("Should serialize single BlockClickTrigger to JSON")
        void testSerializeSingleBlockClickTrigger() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            BlockClickTrigger trigger = new BlockClickTrigger("TestTrigger");
            trigger.setClickType("left_click");
            trigger.setDetectionType("block");
            trigger.setEnabled(true);
            triggers.add(trigger);

            // When
            String json = InstanceSerializer.serializeTriggers(triggers);

            // Then
            assertThat(json).isNotEmpty();
            assertThat(json).contains("\"className\"");
            assertThat(json).contains("BlockClickTrigger");
            assertThat(json).contains("TestTrigger");

            // Validate JSON structure
            JsonArray jsonArray = JsonParser.parseString(json).getAsJsonArray();
            assertThat(jsonArray).hasSize(1);

            JsonObject trigger1 = jsonArray.get(0).getAsJsonObject();
            assertThat(trigger1.keySet()).containsOnly("className", "data");
            assertThat(trigger1.get("className").getAsString())
                    .isEqualTo("fr.perrier.dungeons.spigot.workflow.trigger.impl.BlockClickTrigger");
        }

        @Test
        @DisplayName("Should serialize RegionTrigger with LocationBlocks")
        void testSerializeRegionTrigger() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            RegionTrigger trigger = new RegionTrigger("RegionTest");
            trigger.setPos1(new LocationBlock(10, 20, 30, "world"));
            trigger.setPos2(new LocationBlock(50, 100, 40, "world"));
            trigger.setRegionEvent("enter");
            trigger.setEnabled(true);
            triggers.add(trigger);

            // When
            String json = InstanceSerializer.serializeTriggers(triggers);

            // Then
            assertThat(json).isNotEmpty();
            assertThat(json).contains("RegionTest");
            assertThat(json).contains("RegionTrigger");

            JsonArray jsonArray = JsonParser.parseString(json).getAsJsonArray();
            assertThat(jsonArray).hasSize(1);

            JsonObject triggerObj = jsonArray.get(0).getAsJsonObject();
            JsonObject data = triggerObj.getAsJsonObject("data");
            assertThat(data.get("name").getAsString()).isEqualTo("RegionTest");
        }

        @Test
        @DisplayName("Should serialize multiple triggers in order")
        void testSerializeMultipleTriggers() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            BlockClickTrigger trigger1 = new BlockClickTrigger("Trigger1");
            BlockClickTrigger trigger2 = new BlockClickTrigger("Trigger2");
            triggers.add(trigger1);
            triggers.add(trigger2);

            // When
            String json = InstanceSerializer.serializeTriggers(triggers);

            // Then
            JsonArray jsonArray = JsonParser.parseString(json).getAsJsonArray();
            assertThat(jsonArray).hasSize(2);

            JsonObject firstTrigger = jsonArray.get(0).getAsJsonObject();
            JsonObject secondTrigger = jsonArray.get(1).getAsJsonObject();

            assertThat(firstTrigger.getAsJsonObject("data").get("name").getAsString())
                    .isEqualTo("Trigger1");
            assertThat(secondTrigger.getAsJsonObject("data").get("name").getAsString())
                    .isEqualTo("Trigger2");
        }

        @Test
        @DisplayName("Should preserve trigger properties during serialization")
        void testPreserveTriggerProperties() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            BlockClickTrigger trigger = new BlockClickTrigger("PropertyTest");
            trigger.setClickType("right_click");
            trigger.setDetectionType("interaction");
            trigger.setExactPositionOnly(true);
            trigger.setEnabled(false);
            triggers.add(trigger);

            // When
            String json = InstanceSerializer.serializeTriggers(triggers);

            // Then
            JsonArray jsonArray = JsonParser.parseString(json).getAsJsonArray();
            JsonObject data = jsonArray.get(0).getAsJsonObject().getAsJsonObject("data");

            assertThat(data.get("clickType").getAsString()).isEqualTo("right_click");
            assertThat(data.get("detectionType").getAsString()).isEqualTo("interaction");
            assertThat(data.get("exactPositionOnly").getAsBoolean()).isTrue();
            assertThat(data.get("enabled").getAsBoolean()).isFalse();
        }
    }

    @Nested
    @DisplayName("Deserialization Tests")
    class DeserializationTests {

        @Test
        @DisplayName("Should deserialize null json as empty list")
        void testDeserializeNullJson() {
            // When
            List<TriggerData> result = InstanceSerializer.deserializeTriggers(null);

            // Then
            assertThat(result).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Should deserialize empty array json")
        void testDeserializeEmptyArray() {
            // When
            List<TriggerData> result = InstanceSerializer.deserializeTriggers("[]");

            // Then
            assertThat(result).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Should deserialize empty string as empty list")
        void testDeserializeEmptyString() {
            // When
            List<TriggerData> result = InstanceSerializer.deserializeTriggers("");

            // Then
            assertThat(result).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Should deserialize BlockClickTrigger from JSON")
        void testDeserializeBlockClickTrigger() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            BlockClickTrigger original = new BlockClickTrigger("DeserializeTest");
            original.setClickType("left_click");
            original.setDetectionType("block");
            triggers.add(original);
            String json = InstanceSerializer.serializeTriggers(triggers);

            // When
            List<TriggerData> deserialized = InstanceSerializer.deserializeTriggers(json);

            // Then
            assertThat(deserialized).hasSize(1);
            TriggerData trigger = deserialized.getFirst();
            assertThat(trigger).isInstanceOf(BlockClickTrigger.class);

            BlockClickTrigger blockTrigger = (BlockClickTrigger) trigger;
            assertThat(blockTrigger.getName()).isEqualTo("DeserializeTest");
            assertThat(blockTrigger.getClickType()).isEqualTo("left_click");
            assertThat(blockTrigger.getDetectionType()).isEqualTo("block");
        }

        @Test
        @DisplayName("Should deserialize multiple triggers maintaining order")
        void testDeserializeMultipleTriggers() {
            // Given
            List<TriggerData> original = new ArrayList<>();
            original.add(new BlockClickTrigger("First"));
            original.add(new BlockClickTrigger("Second"));
            String json = InstanceSerializer.serializeTriggers(original);

            // When
            List<TriggerData> deserialized = InstanceSerializer.deserializeTriggers(json);

            // Then
            assertThat(deserialized).hasSize(2);
            assertThat(deserialized.get(0).getName()).isEqualTo("First");
            assertThat(deserialized.get(1).getName()).isEqualTo("Second");
        }

        @Test
        @DisplayName("Should handle invalid JSON gracefully")
        void testDeserializeInvalidJson() {
            // When
            List<TriggerData> result = InstanceSerializer.deserializeTriggers("{ invalid json }");

            // Then
            assertThat(result).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Should handle missing className field gracefully")
        void testDeserializeMissingClassNameField() {
            // Given
            JsonArray jsonArray = new JsonArray();
            JsonObject invalidTrigger = new JsonObject();
            invalidTrigger.addProperty("data", "{}");
            jsonArray.add(invalidTrigger);
            String json = jsonArray.toString();

            // When
            List<TriggerData> result = InstanceSerializer.deserializeTriggers(json);

            // Then
            assertThat(result).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Should handle missing data field gracefully")
        void testDeserializeMissingDataField() {
            // Given
            JsonArray jsonArray = new JsonArray();
            JsonObject invalidTrigger = new JsonObject();
            invalidTrigger.addProperty("className", "fr.perrier.dungeons.spigot.workflow.trigger.impl.BlockClickTrigger");
            jsonArray.add(invalidTrigger);
            String json = jsonArray.toString();

            // When
            List<TriggerData> result = InstanceSerializer.deserializeTriggers(json);

            // Then
            assertThat(result).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Should handle unknown trigger class gracefully")
        void testDeserializeUnknownClass() {
            // Given
            JsonArray jsonArray = new JsonArray();
            JsonObject trigger = new JsonObject();
            trigger.addProperty("className", "com.unknown.UnknownTrigger");
            trigger.add("data", new JsonObject());
            jsonArray.add(trigger);
            String json = jsonArray.toString();

            // When
            List<TriggerData> result = InstanceSerializer.deserializeTriggers(json);

            // Then
            assertThat(result).isNotNull().isEmpty();
        }
    }

    @Nested
    @DisplayName("Round-trip Tests (Serialize -> Deserialize)")
    class RoundTripTests {

        @Test
        @DisplayName("Should preserve BlockClickTrigger through serialize/deserialize cycle")
        void testRoundTripBlockClickTrigger() {
            // Given
            List<TriggerData> original = new ArrayList<>();
            BlockClickTrigger trigger = new BlockClickTrigger("RoundTripTest");
            trigger.setClickType("right_click");
            trigger.setDetectionType("interaction");
            trigger.setBlockMaterial("DIAMOND_BLOCK");
            trigger.setExactPositionOnly(true);
            trigger.setEnabled(true);
            original.add(trigger);

            // When
            String json = InstanceSerializer.serializeTriggers(original);
            List<TriggerData> deserialized = InstanceSerializer.deserializeTriggers(json);

            // Then
            assertThat(deserialized).hasSize(1);
            BlockClickTrigger result = (BlockClickTrigger) deserialized.getFirst();

            assertThat(result.getName()).isEqualTo("RoundTripTest");
            assertThat(result.getClickType()).isEqualTo("right_click");
            assertThat(result.getDetectionType()).isEqualTo("interaction");
            assertThat(result.getBlockMaterial()).isEqualTo("DIAMOND_BLOCK");
            assertThat(result.isExactPositionOnly()).isTrue();
            assertThat(result.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should preserve RegionTrigger with complex properties")
        void testRoundTripRegionTrigger() {
            // Given
            List<TriggerData> original = new ArrayList<>();
            RegionTrigger trigger = new RegionTrigger("RegionRoundTrip");
            trigger.setPos1(new LocationBlock(100, 50, 200, "world"));
            trigger.setPos2(new LocationBlock(200, 150, 300, "nether"));
            trigger.setRegionEvent("both");
            trigger.setEnabled(true);
            original.add(trigger);

            // When
            String json = InstanceSerializer.serializeTriggers(original);
            List<TriggerData> deserialized = InstanceSerializer.deserializeTriggers(json);

            // Then
            assertThat(deserialized).hasSize(1);
            RegionTrigger result = (RegionTrigger) deserialized.getFirst();

            assertThat(result.getName()).isEqualTo("RegionRoundTrip");
            assertThat(result.getRegionEvent()).isEqualTo("both");
            assertThat(result.isEnabled()).isTrue();
            // Validate positions
            assertThat(result.getPos1()).isNotNull();
            assertThat(result.getPos2()).isNotNull();
        }

        @Test
        @DisplayName("Should maintain JSON structure consistency")
        void testJsonConsistency() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            triggers.add(new BlockClickTrigger("Consistency1"));
            triggers.add(new BlockClickTrigger("Consistency2"));

            // When - serialize twice
            String json1 = InstanceSerializer.serializeTriggers(triggers);
            List<TriggerData> intermediate = InstanceSerializer.deserializeTriggers(json1);
            String json2 = InstanceSerializer.serializeTriggers(intermediate);

            // Then - deserialize both
            List<TriggerData> fromJson1 = InstanceSerializer.deserializeTriggers(json1);
            List<TriggerData> fromJson2 = InstanceSerializer.deserializeTriggers(json2);

            assertThat(fromJson1).hasSameSizeAs(fromJson2);
            for (int i = 0; i < fromJson1.size(); i++) {
                assertThat(fromJson1.get(i).getName())
                        .isEqualTo(fromJson2.get(i).getName());
            }
        }

        @Test
        @DisplayName("Should preserve empty actions list")
        void testPreserveEmptyActions() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            BlockClickTrigger trigger = new BlockClickTrigger("EmptyActions");
            trigger.setActions(new ArrayList<>());
            triggers.add(trigger);

            // When
            String json = InstanceSerializer.serializeTriggers(triggers);
            List<TriggerData> deserialized = InstanceSerializer.deserializeTriggers(json);

            // Then
            assertThat(deserialized).hasSize(1);
            assertThat(deserialized.getFirst().getActions()).isNotNull().isEmpty();
        }
    }

    @Nested
    @DisplayName("JSON Structure Validation Tests")
    class JsonStructureTests {

        @Test
        @DisplayName("Should create valid className/data wrapper structure")
        void testValidWrapperStructure() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            triggers.add(new BlockClickTrigger("StructureTest"));

            // When
            String json = InstanceSerializer.serializeTriggers(triggers);

            // Then
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            JsonObject trigger = array.get(0).getAsJsonObject();

            assertThat(trigger.has("className")).isTrue();
            assertThat(trigger.has("data")).isTrue();
            assertThat(trigger.size()).isEqualTo(2);

            JsonObject data = trigger.getAsJsonObject("data");
            assertThat(data.has("name")).isTrue();
            assertThat(data.has("enabled")).isTrue();
        }

        @Test
        @DisplayName("Should preserve proper JSON formatting")
        void testJsonFormatting() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            triggers.add(new BlockClickTrigger("FormatTest"));

            // When
            String json = InstanceSerializer.serializeTriggers(triggers);

            // Then - should be pretty printed
            assertThat(json).contains("\n");
            assertThat(json).contains("  ");
        }

        @Test
        @DisplayName("Should handle special characters in trigger names")
        void testSpecialCharactersInNames() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            BlockClickTrigger trigger = new BlockClickTrigger("Test \"Quoted\" & <Special> 'Chars'");
            triggers.add(trigger);

            // When
            String json = InstanceSerializer.serializeTriggers(triggers);
            List<TriggerData> deserialized = InstanceSerializer.deserializeTriggers(json);

            // Then
            assertThat(deserialized).hasSize(1);
            assertThat(deserialized.getFirst().getName())
                    .isEqualTo("Test \"Quoted\" & <Special> 'Chars'");
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle triggers with null actions")
        void testTriggersWithNullActions() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            BlockClickTrigger trigger = new BlockClickTrigger("NullActionsTest");
            trigger.setActions(null);
            triggers.add(trigger);

            // When
            String json = InstanceSerializer.serializeTriggers(triggers);

            // Then - should not fail
            assertThat(json).isNotEmpty();

            List<TriggerData> deserialized = InstanceSerializer.deserializeTriggers(json);
            assertThat(deserialized).hasSize(1);
        }

        @Test
        @DisplayName("Should handle whitespace in JSON")
        void testJsonWithWhitespace() {
            // Given
            String json = """
                    [
                      {
                        "className": "fr.perrier.dungeons.spigot.workflow.trigger.impl.BlockClickTrigger",
                        "data": {
                          "name": "WhitespaceTest",
                          "enabled": true
                        }
                      }
                    ]
                    """;

            // When
            List<TriggerData> result = InstanceSerializer.deserializeTriggers(json);

            // Then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should skip null triggers in list during serialization")
        void testNullTriggersInList() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            triggers.add(new BlockClickTrigger("Valid"));
            triggers.add(null);
            triggers.add(new BlockClickTrigger("AlsoValid"));

            // When
            String json = InstanceSerializer.serializeTriggers(triggers);

            // Then
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            assertThat(array).hasSize(2);
        }

        @Test
        @DisplayName("Should handle very long trigger names")
        void testLongTriggerNames() {
            // Given
            String longName = "A".repeat(1000);
            List<TriggerData> triggers = new ArrayList<>();
            BlockClickTrigger trigger = new BlockClickTrigger(longName);
            triggers.add(trigger);

            // When
            String json = InstanceSerializer.serializeTriggers(triggers);
            List<TriggerData> deserialized = InstanceSerializer.deserializeTriggers(json);

            // Then
            assertThat(deserialized).hasSize(1);
            assertThat(deserialized.getFirst().getName()).isEqualTo(longName);
        }
    }
}














