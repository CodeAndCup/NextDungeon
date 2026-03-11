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

/**
 * Advanced serialization tests covering complex scenarios,
 * backwards compatibility, and edge cases
 */
@DisplayName("Advanced Serialization Tests")
class AdvancedSerializationTest {

    @Nested
    @DisplayName("LocationBlock Serialization Tests")
    class LocationBlockSerializationTests {

        @Test
        @DisplayName("Should serialize LocationBlock with all properties")
        void testSerializeLocationBlock() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            RegionTrigger trigger = new RegionTrigger("LocationTest");
            LocationBlock pos1 = new LocationBlock(100, 64, 200, "world");
            LocationBlock pos2 = new LocationBlock(200, 128, 300, "nether");
            trigger.setPos1(pos1);
            trigger.setPos2(pos2);
            triggers.add(trigger);

            // When
            String json = InstanceSerializer.serializeTriggers(triggers);

            // Then
            assertThat(json).contains("100");
            assertThat(json).contains("64");
            assertThat(json).contains("200");
            assertThat(json).contains("world");
            assertThat(json).contains("nether");
        }

        @Test
        @DisplayName("Should deserialize LocationBlock coordinates correctly")
        void testDeserializeLocationBlock() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            RegionTrigger trigger = new RegionTrigger("LocationDeserialize");
            trigger.setPos1(new LocationBlock(50, 75, 100, "custom_world"));
            trigger.setPos2(new LocationBlock(150, 180, 200, "custom_world"));
            triggers.add(trigger);

            String json = InstanceSerializer.serializeTriggers(triggers);

            // When
            List<TriggerData> deserialized = InstanceSerializer.deserializeTriggers(json);

            // Then
            assertThat(deserialized).hasSize(1);
            RegionTrigger result = (RegionTrigger) deserialized.getFirst();

            assertThat(result.getPos1()).isNotNull();
            assertThat(result.getPos1().getX()).isEqualTo(50);
            assertThat(result.getPos1().getY()).isEqualTo(75);
            assertThat(result.getPos1().getZ()).isEqualTo(100);
            assertThat(result.getPos1().getWorldName()).isEqualTo("custom_world");

            assertThat(result.getPos2()).isNotNull();
            assertThat(result.getPos2().getX()).isEqualTo(150);
            assertThat(result.getPos2().getY()).isEqualTo(180);
            assertThat(result.getPos2().getZ()).isEqualTo(200);
        }

        @Test
        @DisplayName("Should handle negative coordinates")
        void testNegativeCoordinates() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            RegionTrigger trigger = new RegionTrigger("NegativeCoords");
            trigger.setPos1(new LocationBlock(-100, -64, -200, "world"));
            trigger.setPos2(new LocationBlock(-50, -32, -100, "world"));
            triggers.add(trigger);

            // When
            String json = InstanceSerializer.serializeTriggers(triggers);
            List<TriggerData> deserialized = InstanceSerializer.deserializeTriggers(json);

            // Then
            assertThat(deserialized).hasSize(1);
            RegionTrigger result = (RegionTrigger) deserialized.getFirst();
            assertThat(result.getPos1().getX()).isEqualTo(-100);
            assertThat(result.getPos1().getY()).isEqualTo(-64);
            assertThat(result.getPos2().getX()).isEqualTo(-50);
        }

        @Test
        @DisplayName("Should handle maximum and minimum coordinate values")
        void testExtremeCoordinates() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            RegionTrigger trigger = new RegionTrigger("ExtremeCoords");
            trigger.setPos1(new LocationBlock(Integer.MAX_VALUE, 255, Integer.MIN_VALUE, "world"));
            triggers.add(trigger);

            // When
            String json = InstanceSerializer.serializeTriggers(triggers);
            List<TriggerData> deserialized = InstanceSerializer.deserializeTriggers(json);

            // Then
            assertThat(deserialized).hasSize(1);
            RegionTrigger result = (RegionTrigger) deserialized.getFirst();
            assertThat(result.getPos1().getX()).isEqualTo(Integer.MAX_VALUE);
            assertThat(result.getPos1().getY()).isEqualTo(255);
            assertThat(result.getPos1().getZ()).isEqualTo(Integer.MIN_VALUE);
        }
    }

    @Nested
    @DisplayName("Multiple Trigger Types Tests")
    class MultipleTriggerTypesTests {

        @Test
        @DisplayName("Should serialize mixed trigger types")
        void testMixedTriggerTypes() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            triggers.add(new BlockClickTrigger("ClickTrigger"));
            triggers.add(new RegionTrigger("RegionTrigger"));
            triggers.add(new BlockClickTrigger("AnotherClick"));

            // When
            String json = InstanceSerializer.serializeTriggers(triggers);

            // Then
            assertThat(json).contains("BlockClickTrigger");
            assertThat(json).contains("RegionTrigger");
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            assertThat(array).hasSize(3);
        }

        @Test
        @DisplayName("Should deserialize mixed trigger types preserving class information")
        void testDeserializeMixedTypes() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            BlockClickTrigger click = new BlockClickTrigger("Click");
            RegionTrigger region = new RegionTrigger("Region");
            triggers.add(click);
            triggers.add(region);

            String json = InstanceSerializer.serializeTriggers(triggers);

            // When
            List<TriggerData> deserialized = InstanceSerializer.deserializeTriggers(json);

            // Then
            assertThat(deserialized).hasSize(2);
            assertThat(deserialized.getFirst()).isInstanceOf(BlockClickTrigger.class);
            assertThat(deserialized.get(1)).isInstanceOf(RegionTrigger.class);
        }
    }

    @Nested
    @DisplayName("Boolean and Primitive Types Tests")
    class PrimitiveTypesTests {

        @Test
        @DisplayName("Should serialize boolean fields correctly")
        void testBooleanSerialization() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            BlockClickTrigger trigger = new BlockClickTrigger("BooleanTest");
            trigger.setEnabled(true);
            trigger.setExactPositionOnly(true);
            triggers.add(trigger);

            // When
            String json = InstanceSerializer.serializeTriggers(triggers);

            // Then
            assertThat(json).contains("\"enabled\": true");
            assertThat(json).contains("\"exactPositionOnly\": true");
        }

        @Test
        @DisplayName("Should deserialize false boolean values")
        void testFalseBooleanDeserialization() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            BlockClickTrigger trigger = new BlockClickTrigger("FalseTest");
            trigger.setEnabled(false);
            trigger.setExactPositionOnly(false);
            triggers.add(trigger);

            String json = InstanceSerializer.serializeTriggers(triggers);

            // When
            List<TriggerData> deserialized = InstanceSerializer.deserializeTriggers(json);

            // Then
            BlockClickTrigger result = (BlockClickTrigger) deserialized.getFirst();
            assertThat(result.isEnabled()).isFalse();
            assertThat(result.isExactPositionOnly()).isFalse();
        }

        @Test
        @DisplayName("Should handle integer properties")
        void testIntegerProperties() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            RegionTrigger trigger = new RegionTrigger("IntegerTest");
            trigger.setPos1(new LocationBlock(42, 13, 99, "world"));
            triggers.add(trigger);

            // When
            String json = InstanceSerializer.serializeTriggers(triggers);
            List<TriggerData> deserialized = InstanceSerializer.deserializeTriggers(json);

            // Then
            RegionTrigger result = (RegionTrigger) deserialized.getFirst();
            assertThat(result.getPos1().getX()).isEqualTo(42);
            assertThat(result.getPos1().getY()).isEqualTo(13);
            assertThat(result.getPos1().getZ()).isEqualTo(99);
        }
    }

    @Nested
    @DisplayName("String Properties Tests")
    class StringPropertiesTests {

        @Test
        @DisplayName("Should preserve dropdown string values")
        void testDropdownValues() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            BlockClickTrigger trigger = new BlockClickTrigger("DropdownTest");
            trigger.setClickType("both");
            trigger.setDetectionType("interaction");
            triggers.add(trigger);

            // When
            String json = InstanceSerializer.serializeTriggers(triggers);
            List<TriggerData> deserialized = InstanceSerializer.deserializeTriggers(json);

            // Then
            BlockClickTrigger result = (BlockClickTrigger) deserialized.getFirst();
            assertThat(result.getClickType()).isEqualTo("both");
            assertThat(result.getDetectionType()).isEqualTo("interaction");
        }

        @Test
        @DisplayName("Should preserve material names correctly")
        void testMaterialNames() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            BlockClickTrigger trigger = new BlockClickTrigger("MaterialTest");
            trigger.setBlockMaterial("EMERALD_ORE");
            triggers.add(trigger);

            // When
            String json = InstanceSerializer.serializeTriggers(triggers);
            List<TriggerData> deserialized = InstanceSerializer.deserializeTriggers(json);

            // Then
            BlockClickTrigger result = (BlockClickTrigger) deserialized.get(0);
            assertThat(result.getBlockMaterial()).isEqualTo("EMERALD_ORE");
        }

        @Test
        @DisplayName("Should handle world names with special characters")
        void testWorldNames() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            RegionTrigger trigger = new RegionTrigger("WorldTest");
            trigger.setPos1(new LocationBlock(0, 64, 0, "world_the_nether-2"));
            trigger.setPos2(new LocationBlock(10, 64, 10, "world_the_nether-2"));
            triggers.add(trigger);

            // When
            String json = InstanceSerializer.serializeTriggers(triggers);
            List<TriggerData> deserialized = InstanceSerializer.deserializeTriggers(json);

            // Then
            RegionTrigger result = (RegionTrigger) deserialized.getFirst();
            assertThat(result.getPos1().getWorldName()).isEqualTo("world_the_nether-2");
        }
    }

    @Nested
    @DisplayName("Consistency and Idempotency Tests")
    class ConsistencyTests {

        @Test
        @DisplayName("Serializing the same trigger multiple times produces identical JSON")
        void testSerializationIdempotency() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            BlockClickTrigger trigger = new BlockClickTrigger("Idempotency");
            trigger.setClickType("left_click");
            triggers.add(trigger);

            // When
            String json1 = InstanceSerializer.serializeTriggers(triggers);
            String json2 = InstanceSerializer.serializeTriggers(triggers);

            // Then
            assertThat(json1).isEqualTo(json2);
        }

        @Test
        @DisplayName("Multiple serialize/deserialize cycles maintain data integrity")
        void testMultipleCycles() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            BlockClickTrigger original = new BlockClickTrigger("MultiCycle");
            original.setClickType("right_click");
            original.setExactPositionOnly(true);
            triggers.add(original);

            // When
            String json1 = InstanceSerializer.serializeTriggers(triggers);
            List<TriggerData> cycle1 = InstanceSerializer.deserializeTriggers(json1);

            String json2 = InstanceSerializer.serializeTriggers(cycle1);
            List<TriggerData> cycle2 = InstanceSerializer.deserializeTriggers(json2);

            String json3 = InstanceSerializer.serializeTriggers(cycle2);
            List<TriggerData> cycle3 = InstanceSerializer.deserializeTriggers(json3);

            // Then
            BlockClickTrigger result1 = (BlockClickTrigger) cycle1.getFirst();
            BlockClickTrigger result2 = (BlockClickTrigger) cycle2.getFirst();
            BlockClickTrigger result3 = (BlockClickTrigger) cycle3.getFirst();

            assertThat(result1.getClickType()).isEqualTo(result2.getClickType()).isEqualTo(result3.getClickType());
            assertThat(result1.isExactPositionOnly()).isEqualTo(result2.isExactPositionOnly()).isEqualTo(result3.isExactPositionOnly());
        }
    }

    @Nested
    @DisplayName("Large Scale Tests")
    class LargeScaleTests {

        @Test
        @DisplayName("Should handle large number of triggers (100+)")
        void testLargeNumberOfTriggers() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                BlockClickTrigger trigger = new BlockClickTrigger("Trigger_" + i);
                triggers.add(trigger);
            }

            // When
            String json = InstanceSerializer.serializeTriggers(triggers);
            List<TriggerData> deserialized = InstanceSerializer.deserializeTriggers(json);

            // Then
            assertThat(deserialized).hasSize(100);
            for (int i = 0; i < 100; i++) {
                assertThat(deserialized.get(i).getName()).isEqualTo("Trigger_" + i);
            }
        }

        @Test
        @DisplayName("Should preserve order with large trigger list")
        void testOrderPreservation() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                BlockClickTrigger trigger = new BlockClickTrigger("Item_" + String.format("%03d", i));
                triggers.add(trigger);
            }

            // When
            String json = InstanceSerializer.serializeTriggers(triggers);
            List<TriggerData> deserialized = InstanceSerializer.deserializeTriggers(json);

            // Then
            for (int i = 0; i < 50; i++) {
                assertThat(deserialized.get(i).getName()).isEqualTo("Item_" + String.format("%03d", i));
            }
        }
    }

    @Nested
    @DisplayName("Unicode and Encoding Tests")
    class UnicodeTests {

        @Test
        @DisplayName("Should handle Unicode characters in trigger names")
        void testUnicodeCharacters() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            BlockClickTrigger trigger = new BlockClickTrigger("Test_🎮_游戏_текст");
            triggers.add(trigger);

            // When
            String json = InstanceSerializer.serializeTriggers(triggers);
            List<TriggerData> deserialized = InstanceSerializer.deserializeTriggers(json);

            // Then
            assertThat(deserialized).hasSize(1);
            assertThat(deserialized.getFirst().getName()).isEqualTo("Test_🎮_游戏_текст");
        }

        @Test
        @DisplayName("Should handle various language scripts")
        void testMultilingualNames() {
            // Given
            List<TriggerData> triggers = new ArrayList<>();
            triggers.add(new BlockClickTrigger("English"));
            triggers.add(new BlockClickTrigger("Français"));
            triggers.add(new BlockClickTrigger("Deutsch"));
            triggers.add(new BlockClickTrigger("日本語"));
            triggers.add(new BlockClickTrigger("Русский"));

            // When
            String json = InstanceSerializer.serializeTriggers(triggers);
            List<TriggerData> deserialized = InstanceSerializer.deserializeTriggers(json);

            // Then
            assertThat(deserialized).hasSize(5);
            assertThat(deserialized.get(0).getName()).isEqualTo("English");
            assertThat(deserialized.get(1).getName()).isEqualTo("Français");
            assertThat(deserialized.get(2).getName()).isEqualTo("Deutsch");
            assertThat(deserialized.get(3).getName()).isEqualTo("日本語");
            assertThat(deserialized.get(4).getName()).isEqualTo("Русский");
        }
    }
}













