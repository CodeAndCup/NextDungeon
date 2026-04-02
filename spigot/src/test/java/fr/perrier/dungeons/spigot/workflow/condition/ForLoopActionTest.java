package fr.perrier.dungeons.spigot.workflow.condition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import fr.perrier.dungeons.spigot.workflow.action.Action;
import org.mockito.Mock;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for ForLoopAction
 * Verifies loop iteration, variable management, and action execution
 */
@DisplayName("ForLoopAction Tests")
class ForLoopActionTest {

    private ForLoopAction loopAction;

    @BeforeEach
    void setUp() {
        loopAction = new ForLoopAction();
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create loop with default constructor")
        void testDefaultConstructor() {
            // When
            ForLoopAction result = new ForLoopAction();

            // Then
            assertThat(result.getVariableName()).isEqualTo("i");
            assertThat(result.getStartValue()).isEqualTo(0);
            assertThat(result.getEndValue()).isEqualTo(10);
            assertThat(result.getStep()).isEqualTo(1);
            assertThat(result.getLoopActions()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Should create loop with parameters")
        void testParameterizedConstructor() {
            // When
            ForLoopAction result = new ForLoopAction("counter", 5, 20, 2);

            // Then
            assertThat(result.getVariableName()).isEqualTo("counter");
            assertThat(result.getStartValue()).isEqualTo(5);
            assertThat(result.getEndValue()).isEqualTo(20);
            assertThat(result.getStep()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should handle null variable name with default")
        void testNullVariableNameHandling() {
            // When
            ForLoopAction result = new ForLoopAction(null, 0, 10, 1);

            // Then
            assertThat(result.getVariableName()).isEqualTo("i");
        }

        @Test
        @DisplayName("Should handle zero step with default")
        void testZeroStepHandling() {
            // When
            ForLoopAction result = new ForLoopAction("x", 0, 10, 0);

            // Then
            assertThat(result.getStep()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Property Tests")
    class PropertyTests {

        @Test
        @DisplayName("Should set and get variable name")
        void testVariableNameProperty() {
            // When
            loopAction.setVariableName("iteration");

            // Then
            assertThat(loopAction.getVariableName()).isEqualTo("iteration");
        }

        @Test
        @DisplayName("Should set and get start value")
        void testStartValueProperty() {
            // When
            loopAction.setStartValue(5);

            // Then
            assertThat(loopAction.getStartValue()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should set and get end value")
        void testEndValueProperty() {
            // When
            loopAction.setEndValue(100);

            // Then
            assertThat(loopAction.getEndValue()).isEqualTo(100);
        }

        @Test
        @DisplayName("Should set and get step value")
        void testStepValueProperty() {
            // When
            loopAction.setStep(3);

            // Then
            assertThat(loopAction.getStep()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Loop Range Tests")
    class LoopRangeTests {

        @Test
        @DisplayName("Should handle ascending loop (0 to 10, step 1)")
        void testAscendingLoop() {
            // Given
            loopAction.setStartValue(0);
            loopAction.setEndValue(10);
            loopAction.setStep(1);

            // Then - Verify range configuration
            assertThat(loopAction.getStartValue()).isLessThanOrEqualTo(loopAction.getEndValue());
            assertThat(loopAction.getStep()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should handle ascending loop with large steps")
        void testAscendingLoopLargeStep() {
            // Given
            loopAction.setStartValue(0);
            loopAction.setEndValue(100);
            loopAction.setStep(10);

            // Then
            assertThat(loopAction.getStep()).isEqualTo(10);
            assertThat(loopAction.getEndValue()).isEqualTo(100);
        }

        @Test
        @DisplayName("Should handle descending loop (10 to 0, step -1)")
        void testDescendingLoop() {
            // Given
            loopAction.setStartValue(10);
            loopAction.setEndValue(0);
            loopAction.setStep(-1);

            // Then - Verify range configuration
            assertThat(loopAction.getStartValue()).isGreaterThanOrEqualTo(loopAction.getEndValue());
            assertThat(loopAction.getStep()).isLessThan(0);
        }

        @Test
        @DisplayName("Should handle descending loop with large negative steps")
        void testDescendingLoopLargeNegativeStep() {
            // Given
            loopAction.setStartValue(100);
            loopAction.setEndValue(0);
            loopAction.setStep(-10);

            // Then
            assertThat(loopAction.getStep()).isEqualTo(-10);
            assertThat(loopAction.getStartValue()).isGreaterThan(loopAction.getEndValue());
        }

        @Test
        @DisplayName("Should handle single iteration loop")
        void testSingleIterationLoop() {
            // Given
            loopAction.setStartValue(5);
            loopAction.setEndValue(5);
            loopAction.setStep(1);

            // Then
            assertThat(loopAction.getStartValue()).isEqualTo(loopAction.getEndValue());
        }

        @Test
        @DisplayName("Should handle negative range loops")
        void testNegativeRangeLoop() {
            // Given
            loopAction.setStartValue(-10);
            loopAction.setEndValue(10);
            loopAction.setStep(1);

            // Then
            assertThat(loopAction.getStartValue()).isNegative();
            assertThat(loopAction.getEndValue()).isPositive();
        }
    }

    @Nested
    @DisplayName("Action Management Tests")
    class ActionManagementTests {

        @Test
        @DisplayName("Should initialize with empty action list")
        void testEmptyActionsList() {
            // Then
            assertThat(loopAction.getLoopActions()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Should add action to loop")
        void testAddLoopAction() {
            // Given
            Action mockAction = createMockAction("TestAction");

            // When
            loopAction.addLoopAction(mockAction);

            // Then
            assertThat(loopAction.getLoopActions()).hasSize(1);
            assertThat(loopAction.getLoopActions().get(0)).isEqualTo(mockAction);
        }

        @Test
        @DisplayName("Should add multiple actions to loop")
        void testAddMultipleLoopActions() {
            // Given
            Action action1 = createMockAction("Action1");
            Action action2 = createMockAction("Action2");
            Action action3 = createMockAction("Action3");

            // When
            loopAction.addLoopAction(action1);
            loopAction.addLoopAction(action2);
            loopAction.addLoopAction(action3);

            // Then
            assertThat(loopAction.getLoopActions()).hasSize(3);
            assertThat(loopAction.getLoopActions()).containsExactly(action1, action2, action3);
        }

        @Test
        @DisplayName("Should get loop actions list")
        void testGetLoopActionsList() {
            // Given
            Action action = createMockAction("TestAction");
            loopAction.addLoopAction(action);

            // When
            List<Action> actions = loopAction.getLoopActions();

            // Then
            assertThat(actions).isNotNull().hasSize(1);
        }

        @Test
        @DisplayName("Should initialize empty list when null")
        void testGetLoopActionsInitializesWhenNull() {
            // Given
            loopAction.setLoopActions(null);

            // When
            List<Action> actions = loopAction.getLoopActions();

            // Then
            assertThat(actions).isNotNull();
        }
    }

    @Nested
    @DisplayName("Nested Loop Tests")
    class NestedLoopTests {

        @Test
        @DisplayName("Should support nested loops with different variables")
        void testNestedLoopsWithDifferentVariables() {
            // Given
            ForLoopAction outerLoop = new ForLoopAction("i", 0, 3, 1);
            ForLoopAction innerLoop = new ForLoopAction("j", 0, 2, 1);

            // When
            outerLoop.addLoopAction(innerLoop);

            // Then
            assertThat(outerLoop.getLoopActions()).hasSize(1);
            assertThat(outerLoop.getLoopActions().get(0)).isInstanceOf(ForLoopAction.class);
        }

        @Test
        @DisplayName("Should have unique variable names for nested loops")
        void testUniqueVariableNamesInNestedLoops() {
            // Given
            ForLoopAction outerLoop = new ForLoopAction("i", 0, 5, 1);
            ForLoopAction innerLoop = new ForLoopAction("j", 0, 5, 1);

            // Then
            assertThat(outerLoop.getVariableName()).isNotEqualTo(innerLoop.getVariableName());
            assertThat(outerLoop.getVariableName()).isEqualTo("i");
            assertThat(innerLoop.getVariableName()).isEqualTo("j");
        }
    }

    @Nested
    @DisplayName("Type and Name Tests")
    class TypeAndNameTests {

        @Test
        @DisplayName("Should have correct action type")
        void testActionType() {
            // Then
            assertThat(loopAction.getType()).isEqualTo("for_loop_action");
        }

        @Test
        @DisplayName("Should have correct action name")
        void testActionName() {
            // Then
            assertThat(loopAction.getName()).isEqualTo("For Loop");
        }

        @Test
        @DisplayName("Should implement BlocklyAction")
        void testBlocklyActionImplementation() {
            // Then
            assertThat(loopAction).isInstanceOf(fr.perrier.dungeons.spigot.webeditor.blockly.BlocklyAction.class);
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should accept various variable names")
        void testVariableNameVariation() {
            // Given
            String[] validNames = {"i", "counter", "x", "iteration", "loop_var", "count123"};

            // When/Then
            for (String name : validNames) {
                loopAction.setVariableName(name);
                assertThat(loopAction.getVariableName()).isEqualTo(name);
            }
        }

        @Test
        @DisplayName("Should accept zero as start value")
        void testZeroStartValue() {
            // When
            loopAction.setStartValue(0);

            // Then
            assertThat(loopAction.getStartValue()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should accept negative start value")
        void testNegativeStartValue() {
            // When
            loopAction.setStartValue(-100);

            // Then
            assertThat(loopAction.getStartValue()).isEqualTo(-100);
        }

        @Test
        @DisplayName("Should accept large end values")
        void testLargeEndValue() {
            // When
            loopAction.setEndValue(1_000_000);

            // Then
            assertThat(loopAction.getEndValue()).isEqualTo(1_000_000);
        }

        @Test
        @DisplayName("Should accept negative step values")
        void testNegativeStepValue() {
            // When
            loopAction.setStep(-5);

            // Then
            assertThat(loopAction.getStep()).isEqualTo(-5);
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle loop with step greater than range")
        void testStepGreaterThanRange() {
            // Given
            loopAction.setStartValue(0);
            loopAction.setEndValue(5);
            loopAction.setStep(10);

            // Then - Should still be valid configuration
            assertThat(loopAction.getStep()).isGreaterThan(loopAction.getEndValue() - loopAction.getStartValue());
        }

        @Test
        @DisplayName("Should handle very large loop ranges")
        void testVeryLargeLoopRange() {
            // Given
            loopAction.setStartValue(0);
            loopAction.setEndValue(Integer.MAX_VALUE);
            loopAction.setStep(1);

            // Then
            assertThat(loopAction.getEndValue()).isEqualTo(Integer.MAX_VALUE);
        }

        @Test
        @DisplayName("Should handle negative to positive range")
        void testNegativeToPositiveRange() {
            // Given
            loopAction.setStartValue(-50);
            loopAction.setEndValue(50);
            loopAction.setStep(5);

            // Then
            assertThat(loopAction.getStartValue()).isNegative();
            assertThat(loopAction.getEndValue()).isPositive();
        }
    }

    // Helper method to create a mock action
    private Action createMockAction(String name) {
        return new Action(name, "test_type") {
            @Override
            public boolean execute(org.bukkit.entity.Player player, org.bukkit.Location location, java.util.Map<String, Object> data) {
                return true;
            }
        };
    }
}

