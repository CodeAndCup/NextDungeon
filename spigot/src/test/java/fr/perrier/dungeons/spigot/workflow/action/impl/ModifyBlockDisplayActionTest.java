package fr.perrier.dungeons.spigot.workflow.action.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for ModifyBlockDisplayAction
 * Verifies the modification and update of existing BlockDisplay entities
 */
@DisplayName("ModifyBlockDisplayAction Tests")
class ModifyBlockDisplayActionTest {

    private ModifyBlockDisplayAction action;

    @BeforeEach
    void setUp() {
        action = new ModifyBlockDisplayAction();
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create action with default constructor")
        void testDefaultConstructor() {
            // When
            ModifyBlockDisplayAction result = new ModifyBlockDisplayAction();

            // Then
            assertThat(result.getDisplayId()).isEmpty();
            assertThat(result.getPropertyToModify()).isEqualTo("ALL");
            assertThat(result.getBlockType()).isEmpty();
            assertThat(result.getScaleX()).isEqualTo(1.0f);
            assertThat(result.getScaleY()).isEqualTo(1.0f);
            assertThat(result.getScaleZ()).isEqualTo(1.0f);
        }

        @Test
        @DisplayName("Should create action with all parameters")
        void testParameterizedConstructor() {
            // Given
            String displayId = "test_display";
            String propertyToModify = "BLOCK_TYPE";
            String blockType = "STONE";
            float scaleX = 2.0f, scaleY = 1.5f, scaleZ = 0.5f;

            // When
            ModifyBlockDisplayAction result = new ModifyBlockDisplayAction(
                    displayId, propertyToModify, blockType,
                    scaleX, scaleY, scaleZ,
                    0, 0, 0,
                    0, 0, 0, 1,
                    0, 0, 0, 1
            );

            // Then
            assertThat(result.getDisplayId()).isEqualTo(displayId);
            assertThat(result.getPropertyToModify()).isEqualTo(propertyToModify);
            assertThat(result.getBlockType()).isEqualTo(blockType);
            assertThat(result.getScaleX()).isEqualTo(scaleX);
        }
    }

    @Nested
    @DisplayName("Property Modification Tests")
    class PropertyModificationTests {

        @Test
        @DisplayName("Should set displayId property")
        void testDisplayIdModification() {
            // When
            action.setDisplayId("new_display_id");

            // Then
            assertThat(action.getDisplayId()).isEqualTo("new_display_id");
        }

        @Test
        @DisplayName("Should set propertyToModify to valid values")
        void testPropertyToModifyValues() {
            // Given
            String[] validProperties = {"BLOCK_TYPE", "SCALE", "TRANSLATION", "ROTATION", "ALL"};

            // When/Then
            for (String property : validProperties) {
                action.setPropertyToModify(property);
                assertThat(action.getPropertyToModify()).isEqualTo(property);
            }
        }

        @Test
        @DisplayName("Should set blockType independently from other properties")
        void testBlockTypeModification() {
            // When
            action.setBlockType("DIAMOND_BLOCK");

            // Then
            assertThat(action.getBlockType()).isEqualTo("DIAMOND_BLOCK");
            // Other properties should be unchanged
            assertThat(action.getScaleX()).isEqualTo(1.0f);
        }

        @Test
        @DisplayName("Should set scale X,Y,Z independently")
        void testScaleModification() {
            // When
            action.setScaleX(3.0f);
            action.setScaleY(2.0f);
            action.setScaleZ(1.0f);

            // Then
            assertThat(action.getScaleX()).isEqualTo(3.0f);
            assertThat(action.getScaleY()).isEqualTo(2.0f);
            assertThat(action.getScaleZ()).isEqualTo(1.0f);
        }

        @Test
        @DisplayName("Should set translation X,Y,Z independently")
        void testTranslationModification() {
            // When
            action.setTranslationX(5.0f);
            action.setTranslationY(10.0f);
            action.setTranslationZ(-5.0f);

            // Then
            assertThat(action.getTranslationX()).isEqualTo(5.0f);
            assertThat(action.getTranslationY()).isEqualTo(10.0f);
            assertThat(action.getTranslationZ()).isEqualTo(-5.0f);
        }

        @Test
        @DisplayName("Should set left rotation quaternion")
        void testLeftRotationModification() {
            // When
            action.setLeftRotationX(0.707f);
            action.setLeftRotationY(0.0f);
            action.setLeftRotationZ(0.0f);
            action.setLeftRotationW(0.707f);

            // Then
            assertThat(action.getLeftRotationX()).isEqualTo(0.707f);
            assertThat(action.getLeftRotationW()).isEqualTo(0.707f);
        }

        @Test
        @DisplayName("Should set right rotation quaternion")
        void testRightRotationModification() {
            // When
            action.setRightRotationX(0.0f);
            action.setRightRotationY(0.707f);
            action.setRightRotationZ(0.0f);
            action.setRightRotationW(0.707f);

            // Then
            assertThat(action.getRightRotationY()).isEqualTo(0.707f);
            assertThat(action.getRightRotationW()).isEqualTo(0.707f);
        }
    }

    @Nested
    @DisplayName("Modification Strategy Tests")
    class ModificationStrategyTests {

        @Test
        @DisplayName("Should handle BLOCK_TYPE modification")
        void testBlockTypeModificationStrategy() {
            // When
            action.setPropertyToModify("BLOCK_TYPE");
            action.setBlockType("GOLD_BLOCK");

            // Then
            assertThat(action.getPropertyToModify()).isEqualTo("BLOCK_TYPE");
            assertThat(action.getBlockType()).isEqualTo("GOLD_BLOCK");
        }

        @Test
        @DisplayName("Should handle SCALE modification")
        void testScaleModificationStrategy() {
            // When
            action.setPropertyToModify("SCALE");
            action.setScaleX(2.0f);
            action.setScaleY(2.0f);
            action.setScaleZ(2.0f);

            // Then
            assertThat(action.getPropertyToModify()).isEqualTo("SCALE");
            assertThat(action.getScaleX()).isEqualTo(2.0f);
        }

        @Test
        @DisplayName("Should handle TRANSLATION modification")
        void testTranslationModificationStrategy() {
            // When
            action.setPropertyToModify("TRANSLATION");
            action.setTranslationX(10.0f);
            action.setTranslationY(20.0f);
            action.setTranslationZ(30.0f);

            // Then
            assertThat(action.getPropertyToModify()).isEqualTo("TRANSLATION");
            assertThat(action.getTranslationX()).isEqualTo(10.0f);
        }

        @Test
        @DisplayName("Should handle ROTATION modification")
        void testRotationModificationStrategy() {
            // When
            action.setPropertyToModify("ROTATION");
            action.setLeftRotationX(0.5f);
            action.setRightRotationY(0.5f);

            // Then
            assertThat(action.getPropertyToModify()).isEqualTo("ROTATION");
        }

        @Test
        @DisplayName("Should handle ALL modification")
        void testAllModificationStrategy() {
            // When
            action.setPropertyToModify("ALL");
            action.setBlockType("IRON_BLOCK");
            action.setScaleX(1.5f);
            action.setTranslationY(5.0f);

            // Then
            assertThat(action.getPropertyToModify()).isEqualTo("ALL");
            assertThat(action.getBlockType()).isEqualTo("IRON_BLOCK");
            assertThat(action.getScaleX()).isEqualTo(1.5f);
            assertThat(action.getTranslationY()).isEqualTo(5.0f);
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should accept various block types for modification")
        void testBlockTypeValidation() {
            // Given
            String[] blockTypes = {"DIAMOND_BLOCK", "GOLD_BLOCK", "IRON_BLOCK", "STONE", "DIRT", "GRASS_BLOCK"};

            // When/Then
            for (String blockType : blockTypes) {
                action.setBlockType(blockType);
                assertThat(action.getBlockType()).isEqualTo(blockType);
            }
        }

        @Test
        @DisplayName("Should accept empty blockType (for partial modifications)")
        void testEmptyBlockTypeAllowed() {
            // When
            action.setBlockType("");

            // Then
            assertThat(action.getBlockType()).isEmpty();
        }

        @Test
        @DisplayName("Should accept scale modifications in valid range")
        void testScaleValidation() {
            // Given
            float[] validScales = {0.1f, 0.5f, 1.0f, 2.0f, 5.0f, 10.0f};

            // When/Then
            for (float scale : validScales) {
                action.setScaleX(scale);
                assertThat(action.getScaleX()).isEqualTo(scale);
            }
        }

        @Test
        @DisplayName("Should accept zero scale")
        void testZeroScaleAllowed() {
            // When
            action.setScaleX(0);

            // Then
            assertThat(action.getScaleX()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Type and Name Tests")
    class TypeAndNameTests {

        @Test
        @DisplayName("Should have correct action type")
        void testActionType() {
            // Then
            assertThat(action.getType()).isEqualTo("modify_block_display_action");
        }

        @Test
        @DisplayName("Should have correct action name")
        void testActionName() {
            // Then
            assertThat(action.getName()).isEqualTo("Modify Block Display");
        }

        @Test
        @DisplayName("Should implement BlocklyAction")
        void testBlocklyActionImplementation() {
            // Then
            assertThat(action).isInstanceOf(fr.perrier.dungeons.spigot.webeditor.blockly.BlocklyAction.class);
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle empty displayId")
        void testEmptyDisplayId() {
            // When
            action.setDisplayId("");

            // Then
            assertThat(action.getDisplayId()).isEmpty();
        }

        @Test
        @DisplayName("Should handle null displayId safely")
        void testNullDisplayIdHandling() {
            // When
            action.setDisplayId(null);

            // Then
            assertThat(action.getDisplayId()).isNull();
        }

        @Test
        @DisplayName("Should handle large scale values")
        void testLargeScaleValues() {
            // When
            action.setScaleX(1000.0f);
            action.setScaleY(999.99f);

            // Then
            assertThat(action.getScaleX()).isEqualTo(1000.0f);
            assertThat(action.getScaleY()).isEqualTo(999.99f);
        }

        @Test
        @DisplayName("Should handle negative translation values")
        void testNegativeTranslationValues() {
            // When
            action.setTranslationX(-100.0f);
            action.setTranslationY(-50.5f);
            action.setTranslationZ(-10.1f);

            // Then
            assertThat(action.getTranslationX()).isEqualTo(-100.0f);
            assertThat(action.getTranslationY()).isEqualTo(-50.5f);
            assertThat(action.getTranslationZ()).isEqualTo(-10.1f);
        }
    }
}

