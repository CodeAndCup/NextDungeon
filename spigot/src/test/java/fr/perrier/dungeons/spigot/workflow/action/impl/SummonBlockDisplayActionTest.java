package fr.perrier.dungeons.spigot.workflow.action.impl;

import fr.perrier.dungeons.spigot.workflow.blocks.LocationBlock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for SummonBlockDisplayAction
 * Verifies the creation and configuration of BlockDisplay entities
 */
@DisplayName("SummonBlockDisplayAction Tests")
class SummonBlockDisplayActionTest {

    private SummonBlockDisplayAction action;
    private LocationBlock testLocation;

    @BeforeEach
    void setUp() {
        action = new SummonBlockDisplayAction();
        testLocation = new LocationBlock(100, 64, 100, "world");
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create action with default constructor")
        void testDefaultConstructor() {
            // When
            SummonBlockDisplayAction result = new SummonBlockDisplayAction();

            // Then
            assertThat(result.getBlockType()).isEqualTo("DIAMOND_BLOCK");
            assertThat(result.getScaleX()).isEqualTo(1.0f);
            assertThat(result.getScaleY()).isEqualTo(1.0f);
            assertThat(result.getScaleZ()).isEqualTo(1.0f);
            assertThat(result.getTranslationX()).isEqualTo(0);
            assertThat(result.getTranslationY()).isEqualTo(0);
            assertThat(result.getTranslationZ()).isEqualTo(0);
            assertThat(result.getDisplayId()).isEmpty();
        }

        @Test
        @DisplayName("Should create action with all parameters")
        void testParameterizedConstructor() {
            // Given
            String blockType = "GOLD_BLOCK";
            float scaleX = 2.0f, scaleY = 1.5f, scaleZ = 0.5f;
            float transX = 1.0f, transY = 2.0f, transZ = 3.0f;
            float leftRotX = 0.0f, leftRotY = 1.0f, leftRotZ = 0.0f, leftRotW = 1.0f;
            float rightRotX = 0.0f, rightRotY = 0.0f, rightRotZ = 1.0f, rightRotW = 1.0f;
            String displayId = "test_display";

            // When
            SummonBlockDisplayAction result = new SummonBlockDisplayAction(
                    blockType, testLocation, scaleX, scaleY, scaleZ,
                    transX, transY, transZ,
                    leftRotX, leftRotY, leftRotZ, leftRotW,
                    rightRotX, rightRotY, rightRotZ, rightRotW,
                    displayId
            );

            // Then
            assertThat(result.getBlockType()).isEqualTo(blockType);
            assertThat(result.getScaleX()).isEqualTo(scaleX);
            assertThat(result.getScaleY()).isEqualTo(scaleY);
            assertThat(result.getScaleZ()).isEqualTo(scaleZ);
            assertThat(result.getTranslationX()).isEqualTo(transX);
            assertThat(result.getDisplayId()).isEqualTo(displayId);
        }

        @Test
        @DisplayName("Should handle null displayId gracefully")
        void testNullDisplayId() {
            // When
            SummonBlockDisplayAction result = new SummonBlockDisplayAction(
                    "STONE", testLocation, 1.0f, 1.0f, 1.0f,
                    0, 0, 0,
                    0, 0, 0, 1,
                    0, 0, 0, 1,
                    null
            );

            // Then
            assertThat(result.getDisplayId()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Property Tests")
    class PropertyTests {

        @Test
        @DisplayName("Should set and get blockType")
        void testBlockTypeProperty() {
            // When
            action.setBlockType("IRON_BLOCK");

            // Then
            assertThat(action.getBlockType()).isEqualTo("IRON_BLOCK");
        }

        @Test
        @DisplayName("Should set and get scale values independently")
        void testScaleProperties() {
            // When
            action.setScaleX(2.0f);
            action.setScaleY(1.5f);
            action.setScaleZ(0.75f);

            // Then
            assertThat(action.getScaleX()).isEqualTo(2.0f);
            assertThat(action.getScaleY()).isEqualTo(1.5f);
            assertThat(action.getScaleZ()).isEqualTo(0.75f);
        }

        @Test
        @DisplayName("Should set and get translation values")
        void testTranslationProperties() {
            // When
            action.setTranslationX(5.5f);
            action.setTranslationY(10.0f);
            action.setTranslationZ(-3.25f);

            // Then
            assertThat(action.getTranslationX()).isEqualTo(5.5f);
            assertThat(action.getTranslationY()).isEqualTo(10.0f);
            assertThat(action.getTranslationZ()).isEqualTo(-3.25f);
        }

        @Test
        @DisplayName("Should set and get rotation values")
        void testRotationProperties() {
            // When - Left rotation
            action.setLeftRotationX(0.707f);
            action.setLeftRotationY(0.0f);
            action.setLeftRotationZ(0.0f);
            action.setLeftRotationW(0.707f);

            // Then
            assertThat(action.getLeftRotationX()).isEqualTo(0.707f);
            assertThat(action.getLeftRotationW()).isEqualTo(0.707f);
        }

        @Test
        @DisplayName("Should set and get displayId")
        void testDisplayIdProperty() {
            // When
            action.setDisplayId("my_custom_display");

            // Then
            assertThat(action.getDisplayId()).isEqualTo("my_custom_display");
        }
    }

    @Nested
    @DisplayName("Cache Management Tests")
    class CacheManagementTests {

        @Test
        @DisplayName("Should retrieve stored BlockDisplay by ID")
        void testGetBlockDisplay() {
            // Note: This test will return null in unit tests due to no Bukkit environment
            // but verifies the method structure
            String testId = "test_display_123";
            
            // When/Then
            // The method should not throw exception
            assertThatCode(() -> SummonBlockDisplayAction.getBlockDisplay(testId))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should remove BlockDisplay from cache")
        void testRemoveBlockDisplay() {
            // When/Then - should not throw exception
            assertThatCode(() -> SummonBlockDisplayAction.removeBlockDisplay("test_id"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should clear all cached BlockDisplays")
        void testClearAllBlockDisplays() {
            // When/Then - should not throw exception
            assertThatCode(SummonBlockDisplayAction::clearAllBlockDisplays)
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should accept various block types")
        void testValidBlockTypes() {
            // When/Then
            String[] validBlocks = {"DIAMOND_BLOCK", "GOLD_BLOCK", "IRON_BLOCK", "STONE", "DIRT"};
            for (String blockType : validBlocks) {
                action.setBlockType(blockType);
                assertThat(action.getBlockType()).isEqualTo(blockType);
            }
        }

        @Test
        @DisplayName("Should accept scale values >= 0")
        void testValidScaleValues() {
            // When/Then
            float[] validScales = {0.1f, 0.5f, 1.0f, 2.0f, 10.0f};
            for (float scale : validScales) {
                action.setScaleX(scale);
                assertThat(action.getScaleX()).isEqualTo(scale);
            }
        }

        @Test
        @DisplayName("Should accept positive and negative translations")
        void testValidTranslationValues() {
            // When/Then
            float[] validTranslations = {-100.0f, -10.5f, 0.0f, 10.5f, 100.0f};
            for (float translation : validTranslations) {
                action.setTranslationX(translation);
                assertThat(action.getTranslationX()).isEqualTo(translation);
            }
        }

        @Test
        @DisplayName("Should accept quaternion rotation values")
        void testValidQuaternionValues() {
            // When - Set a normalized quaternion (45-degree rotation around Z)
            action.setLeftRotationX(0.0f);
            action.setLeftRotationY(0.0f);
            action.setLeftRotationZ(0.383f);
            action.setLeftRotationW(0.924f);

            // Then
            assertThat(action.getLeftRotationZ()).isEqualTo(0.383f);
            assertThat(action.getLeftRotationW()).isEqualTo(0.924f);
        }
    }

    @Nested
    @DisplayName("Type and Name Tests")
    class TypeAndNameTests {

        @Test
        @DisplayName("Should have correct action type")
        void testActionType() {
            // Then
            assertThat(action.getType()).isEqualTo("summon_block_display_action");
        }

        @Test
        @DisplayName("Should have correct action name")
        void testActionName() {
            // Then
            assertThat(action.getName()).isEqualTo("Summon Block Display");
        }

        @Test
        @DisplayName("Should implement BlocklyAction")
        void testBlocklyActionImplementation() {
            // Then
            assertThat(action).isInstanceOf(fr.perrier.dungeons.spigot.webeditor.blockly.BlocklyAction.class);
        }
    }
}

