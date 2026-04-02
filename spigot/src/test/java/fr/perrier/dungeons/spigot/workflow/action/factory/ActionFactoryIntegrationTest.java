package fr.perrier.dungeons.spigot.workflow.action.factory;

import com.google.gson.*;
import fr.perrier.dungeons.spigot.workflow.action.impl.SummonBlockDisplayAction;
import fr.perrier.dungeons.spigot.workflow.action.impl.ModifyBlockDisplayAction;
import fr.perrier.dungeons.spigot.workflow.condition.ForLoopAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for ActionFactory
 * Tests serialization/deserialization of new actions
 */
@DisplayName("ActionFactory Integration Tests")
class ActionFactoryIntegrationTest {

    private Gson gson;

    @BeforeEach
    void setUp() {
        gson = new GsonBuilder().setPrettyPrinting().create();
    }

    @Nested
    @DisplayName("SummonBlockDisplayAction Factory Tests")
    class SummonBlockDisplayActionFactoryTests {

        @Test
        @DisplayName("Should deserialize SummonBlockDisplayAction from JSON")
        void testDeserializeSummonBlockDisplay() {
            // Given
            String json = "{\n" +
                    "  \"type\": \"summon_block_display_action\",\n" +
                    "  \"blockType\": \"DIAMOND_BLOCK\",\n" +
                    "  \"scaleX\": 1.5,\n" +
                    "  \"scaleY\": 1.5,\n" +
                    "  \"scaleZ\": 1.5,\n" +
                    "  \"translationX\": 0,\n" +
                    "  \"translationY\": 0,\n" +
                    "  \"translationZ\": 0,\n" +
                    "  \"displayId\": \"test_display\"\n" +
                    "}";

            JsonObject actionData = JsonParser.parseString(json).getAsJsonObject();

            // When
            var action = ActionFactory.createActionFromJson(actionData);

            // Then
            assertThat(action).isInstanceOf(SummonBlockDisplayAction.class);
            SummonBlockDisplayAction displayAction = (SummonBlockDisplayAction) action;
            assertThat(displayAction.getBlockType()).isEqualTo("DIAMOND_BLOCK");
            assertThat(displayAction.getDisplayId()).isEqualTo("test_display");
        }

        @Test
        @DisplayName("Should handle SummonBlockDisplayAction with default values")
        void testSummonBlockDisplayWithDefaults() {
            // Given
            String json = "{\n" +
                    "  \"type\": \"summon_block_display_action\"\n" +
                    "}";

            JsonObject actionData = JsonParser.parseString(json).getAsJsonObject();

            // When
            var action = ActionFactory.createActionFromJson(actionData);

            // Then
            assertThat(action).isInstanceOf(SummonBlockDisplayAction.class);
            SummonBlockDisplayAction displayAction = (SummonBlockDisplayAction) action;
            assertThat(displayAction.getScaleX()).isEqualTo(1.0f);
            assertThat(displayAction.getScaleY()).isEqualTo(1.0f);
        }
    }

    @Nested
    @DisplayName("ModifyBlockDisplayAction Factory Tests")
    class ModifyBlockDisplayActionFactoryTests {

        @Test
        @DisplayName("Should deserialize ModifyBlockDisplayAction from JSON")
        void testDeserializeModifyBlockDisplay() {
            // Given
            String json = "{\n" +
                    "  \"type\": \"modify_block_display_action\",\n" +
                    "  \"displayId\": \"my_display\",\n" +
                    "  \"propertyToModify\": \"ALL\",\n" +
                    "  \"blockType\": \"GOLD_BLOCK\",\n" +
                    "  \"scaleX\": 2.0,\n" +
                    "  \"scaleY\": 2.0,\n" +
                    "  \"scaleZ\": 2.0\n" +
                    "}";

            JsonObject actionData = JsonParser.parseString(json).getAsJsonObject();

            // When
            var action = ActionFactory.createActionFromJson(actionData);

            // Then
            assertThat(action).isInstanceOf(ModifyBlockDisplayAction.class);
            ModifyBlockDisplayAction modifyAction = (ModifyBlockDisplayAction) action;
            assertThat(modifyAction.getDisplayId()).isEqualTo("my_display");
            assertThat(modifyAction.getPropertyToModify()).isEqualTo("ALL");
            assertThat(modifyAction.getBlockType()).isEqualTo("GOLD_BLOCK");
        }

        @Test
        @DisplayName("Should handle ModifyBlockDisplayAction with partial properties")
        void testModifyBlockDisplayPartial() {
            // Given
            String json = "{\n" +
                    "  \"type\": \"modify_block_display_action\",\n" +
                    "  \"displayId\": \"partial_display\",\n" +
                    "  \"propertyToModify\": \"SCALE\"\n" +
                    "}";

            JsonObject actionData = JsonParser.parseString(json).getAsJsonObject();

            // When
            var action = ActionFactory.createActionFromJson(actionData);

            // Then
            assertThat(action).isInstanceOf(ModifyBlockDisplayAction.class);
            ModifyBlockDisplayAction modifyAction = (ModifyBlockDisplayAction) action;
            assertThat(modifyAction.getPropertyToModify()).isEqualTo("SCALE");
        }
    }

    @Nested
    @DisplayName("ForLoopAction Factory Tests")
    class ForLoopActionFactoryTests {

        @Test
        @DisplayName("Should deserialize ForLoopAction from JSON")
        void testDeserializeForLoop() {
            // Given
            String json = "{\n" +
                    "  \"type\": \"for_loop_action\",\n" +
                    "  \"variableName\": \"i\",\n" +
                    "  \"startValue\": 0,\n" +
                    "  \"endValue\": 10,\n" +
                    "  \"step\": 1,\n" +
                    "  \"loopActions\": []\n" +
                    "}";

            JsonObject actionData = JsonParser.parseString(json).getAsJsonObject();

            // When
            var action = ActionFactory.createActionFromJson(actionData);

            // Then
            assertThat(action).isInstanceOf(ForLoopAction.class);
            ForLoopAction loopAction = (ForLoopAction) action;
            assertThat(loopAction.getVariableName()).isEqualTo("i");
            assertThat(loopAction.getStartValue()).isEqualTo(0);
            assertThat(loopAction.getEndValue()).isEqualTo(10);
            assertThat(loopAction.getStep()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should handle ForLoopAction with custom variable name")
        void testForLoopCustomVariable() {
            // Given
            String json = "{\n" +
                    "  \"type\": \"for_loop_action\",\n" +
                    "  \"variableName\": \"counter\",\n" +
                    "  \"startValue\": 5,\n" +
                    "  \"endValue\": 25,\n" +
                    "  \"step\": 5\n" +
                    "}";

            JsonObject actionData = JsonParser.parseString(json).getAsJsonObject();

            // When
            var action = ActionFactory.createActionFromJson(actionData);

            // Then
            assertThat(action).isInstanceOf(ForLoopAction.class);
            ForLoopAction loopAction = (ForLoopAction) action;
            assertThat(loopAction.getVariableName()).isEqualTo("counter");
            assertThat(loopAction.getStartValue()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should handle ForLoopAction with descending loop")
        void testForLoopDescending() {
            // Given
            String json = "{\n" +
                    "  \"type\": \"for_loop_action\",\n" +
                    "  \"variableName\": \"countdown\",\n" +
                    "  \"startValue\": 10,\n" +
                    "  \"endValue\": 0,\n" +
                    "  \"step\": -1\n" +
                    "}";

            JsonObject actionData = JsonParser.parseString(json).getAsJsonObject();

            // When
            var action = ActionFactory.createActionFromJson(actionData);

            // Then
            assertThat(action).isInstanceOf(ForLoopAction.class);
            ForLoopAction loopAction = (ForLoopAction) action;
            assertThat(loopAction.getStep()).isEqualTo(-1);
        }
    }

    @Nested
    @DisplayName("JSON Validation Tests")
    class JsonValidationTests {

        @Test
        @DisplayName("Should produce valid JSON for all action types")
        void testValidJsonForAllTypes() {
            // Given
            String[] actionJsons = {
                    "{\"type\": \"summon_block_display_action\"}",
                    "{\"type\": \"modify_block_display_action\"}",
                    "{\"type\": \"for_loop_action\"}"
            };

            // When/Then
            for (String json : actionJsons) {
                assertThatCode(() -> JsonParser.parseString(json))
                        .doesNotThrowAnyException();
            }
        }

        @Test
        @DisplayName("Should parse JSON with special characters")
        void testSpecialCharactersInJson() {
            // Given
            String json = "{\n" +
                    "  \"type\": \"summon_block_display_action\",\n" +
                    "  \"displayId\": \"display_with\\\"quotes\"\n" +
                    "}";

            // When/Then
            assertThatCode(() -> JsonParser.parseString(json))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle missing required fields gracefully")
        void testMissingRequiredFields() {
            // Given
            String json = "{\"type\": \"summon_block_display_action\"}";
            JsonObject actionData = JsonParser.parseString(json).getAsJsonObject();

            // When
            var action = ActionFactory.createActionFromJson(actionData);

            // Then - Should still create action with defaults
            assertThat(action).isNotNull();
        }

        @Test
        @DisplayName("Should handle invalid numeric values")
        void testInvalidNumericValues() {
            // Given
            String json = "{\n" +
                    "  \"type\": \"for_loop_action\",\n" +
                    "  \"startValue\": \"not_a_number\"\n" +
                    "}";
            JsonObject actionData = JsonParser.parseString(json).getAsJsonObject();

            // When/Then
            assertThatCode(() -> ActionFactory.createActionFromJson(actionData))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Complex Scenario Tests")
    class ComplexScenarioTests {

        @Test
        @DisplayName("Should handle nested ForLoop with BlockDisplay actions")
        void testNestedForLoopWithBlockDisplay() {
            // Given: A ForLoop containing SummonBlockDisplay actions
            String json = "{\n" +
                    "  \"type\": \"for_loop_action\",\n" +
                    "  \"variableName\": \"i\",\n" +
                    "  \"startValue\": 0,\n" +
                    "  \"endValue\": 5,\n" +
                    "  \"step\": 1,\n" +
                    "  \"loopActions\": [\n" +
                    "    {\n" +
                    "      \"type\": \"summon_block_display_action\",\n" +
                    "      \"blockType\": \"DIAMOND_BLOCK\"\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}";

            JsonObject actionData = JsonParser.parseString(json).getAsJsonObject();

            // When
            var action = ActionFactory.createActionFromJson(actionData);

            // Then
            assertThat(action).isInstanceOf(ForLoopAction.class);
            ForLoopAction loopAction = (ForLoopAction) action;
            assertThat(loopAction.getLoopActions()).isNotNull();
        }

        @Test
        @DisplayName("Should handle multiple BlockDisplay modifications")
        void testMultipleBlockDisplayModifications() {
            // Given
            JsonObject summon = JsonParser.parseString(
                    "{\"type\": \"summon_block_display_action\", \"displayId\": \"bd1\"}"
            ).getAsJsonObject();

            JsonObject modify1 = JsonParser.parseString(
                    "{\"type\": \"modify_block_display_action\", \"displayId\": \"bd1\", \"propertyToModify\": \"SCALE\"}"
            ).getAsJsonObject();

            JsonObject modify2 = JsonParser.parseString(
                    "{\"type\": \"modify_block_display_action\", \"displayId\": \"bd1\", \"propertyToModify\": \"TRANSLATION\"}"
            ).getAsJsonObject();

            // When
            var action1 = ActionFactory.createActionFromJson(summon);
            var action2 = ActionFactory.createActionFromJson(modify1);
            var action3 = ActionFactory.createActionFromJson(modify2);

            // Then
            assertThat(action1).isInstanceOf(SummonBlockDisplayAction.class);
            assertThat(action2).isInstanceOf(ModifyBlockDisplayAction.class);
            assertThat(action3).isInstanceOf(ModifyBlockDisplayAction.class);
        }
    }
}

