package fr.perrier.dungeons.common.model.dungeon.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for Requirements with removeCompletion feature
 * Verifies floor removal configuration on dungeon completion
 */
@DisplayName("Requirements RemoveCompletion Tests")
class RequirementsRemoveCompletionTest {

    private Requirements requirements;

    @BeforeEach
    void setUp() {
        requirements = new Requirements();
    }

    @Nested
    @DisplayName("RemoveCompletion Property Tests")
    class RemoveCompletionPropertyTests {

        @Test
        @DisplayName("Should initialize with null removeCompletion")
        void testInitialRemoveCompletion() {
            // Then
            assertThat(requirements.getRemoveCompletion()).isNull();
        }

        @Test
        @DisplayName("Should set removeCompletion list")
        void testSetRemoveCompletion() {
            // Given
            List<String> floorsToRemove = Arrays.asList("floor1", "floor2", "floor3");

            // When
            requirements.setRemoveCompletion(floorsToRemove);

            // Then
            assertThat(requirements.getRemoveCompletion()).isNotNull();
            assertThat(requirements.getRemoveCompletion()).hasSize(3);
            assertThat(requirements.getRemoveCompletion()).containsExactly("floor1", "floor2", "floor3");
        }

        @Test
        @DisplayName("Should handle empty removeCompletion list")
        void testEmptyRemoveCompletion() {
            // When
            requirements.setRemoveCompletion(Arrays.asList());

            // Then
            assertThat(requirements.getRemoveCompletion()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Should handle null removeCompletion")
        void testNullRemoveCompletion() {
            // When
            requirements.setRemoveCompletion(null);

            // Then
            assertThat(requirements.getRemoveCompletion()).isNull();
        }

        @Test
        @DisplayName("Should allow single floor in removeCompletion")
        void testSingleFloorRemoveCompletion() {
            // When
            requirements.setRemoveCompletion(Arrays.asList("floor1"));

            // Then
            assertThat(requirements.getRemoveCompletion()).hasSize(1);
            assertThat(requirements.getRemoveCompletion().get(0)).isEqualTo("floor1");
        }

        @Test
        @DisplayName("Should allow multiple floors in removeCompletion")
        void testMultipleFloorsRemoveCompletion() {
            // Given
            List<String> floors = Arrays.asList("F1", "F2", "F3", "F4", "F5");

            // When
            requirements.setRemoveCompletion(floors);

            // Then
            assertThat(requirements.getRemoveCompletion()).hasSize(5);
            assertThat(requirements.getRemoveCompletion()).containsAll(floors);
        }
    }

    @Nested
    @DisplayName("Floor ID Format Tests")
    class FloorIdFormatTests {

        @Test
        @DisplayName("Should accept simple floor IDs")
        void testSimpleFloorIds() {
            // Given
            List<String> floorIds = Arrays.asList("floor1", "floor2", "stage_one");

            // When
            requirements.setRemoveCompletion(floorIds);

            // Then
            assertThat(requirements.getRemoveCompletion()).containsExactly("floor1", "floor2", "stage_one");
        }

        @Test
        @DisplayName("Should accept full dungeon_floor IDs")
        void testFullFloorIds() {
            // Given
            List<String> floorIds = Arrays.asList("dungeon1_floor1", "dungeon1_floor2", "dungeon2_floor1");

            // When
            requirements.setRemoveCompletion(floorIds);

            // Then
            assertThat(requirements.getRemoveCompletion()).hasSize(3);
            assertThat(requirements.getRemoveCompletion()).contains("dungeon1_floor1");
        }

        @Test
        @DisplayName("Should accept floor IDs with numbers and underscores")
        void testFloorIdWithNumbersAndUnderscores() {
            // Given
            List<String> floorIds = Arrays.asList("dungeon_123_floor_45", "level_1_stage_2");

            // When
            requirements.setRemoveCompletion(floorIds);

            // Then
            assertThat(requirements.getRemoveCompletion()).contains("dungeon_123_floor_45", "level_1_stage_2");
        }
    }

    @Nested
    @DisplayName("Integration with Other Requirements Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should coexist with requiredFloorsId")
        void testRemoveCompletionWithRequiredFloors() {
            // When
            requirements.setRequiredFloorsId(Arrays.asList("floor1", "floor2"));
            requirements.setRemoveCompletion(Arrays.asList("floor1", "floor2"));

            // Then
            assertThat(requirements.getRequiredFloorsId()).hasSize(2);
            assertThat(requirements.getRemoveCompletion()).hasSize(2);
            assertThat(requirements.getRequiredFloorsId()).containsExactly("floor1", "floor2");
            assertThat(requirements.getRemoveCompletion()).containsExactly("floor1", "floor2");
        }

        @Test
        @DisplayName("Should support different floors in required vs remove")
        void testDifferentFloorsRequiredVsRemove() {
            // When
            requirements.setRequiredFloorsId(Arrays.asList("F1", "F2", "F3"));
            requirements.setRemoveCompletion(Arrays.asList("F1", "F2"));

            // Then - Floor F3 is required but not removed
            assertThat(requirements.getRequiredFloorsId()).contains("F1", "F2", "F3");
            assertThat(requirements.getRemoveCompletion()).contains("F1", "F2");
            assertThat(requirements.getRemoveCompletion()).doesNotContain("F3");
        }

        @Test
        @DisplayName("Should work with minLevel requirement")
        void testRemoveCompletionWithMinLevel() {
            // When
            requirements.setMinLevel(10);
            requirements.setRemoveCompletion(Arrays.asList("floor1", "floor2"));

            // Then
            assertThat(requirements.getMinLevel()).isEqualTo(10);
            assertThat(requirements.getRemoveCompletion()).hasSize(2);
        }

        @Test
        @DisplayName("Should work with party requirements")
        void testRemoveCompletionWithPartyRequirements() {
            // Given
            Requirements.PartyRequirements party = new Requirements.PartyRequirements();
            party.setMinSize(2);
            party.setMaxSize(4);

            // When
            requirements.setPartyRequirements(party);
            requirements.setRemoveCompletion(Arrays.asList("f1", "f2"));

            // Then
            assertThat(requirements.getPartyRequirements()).isNotNull();
            assertThat(requirements.getRemoveCompletion()).hasSize(2);
        }

        @Test
        @DisplayName("Should work with item requirements")
        void testRemoveCompletionWithItemRequirements() {
            // When
            requirements.setRequiredItems(Arrays.asList("diamond_sword", "golden_apple"));
            requirements.setRemoveCompletion(Arrays.asList("floor1"));

            // Then
            assertThat(requirements.getRequiredItems()).hasSize(2);
            assertThat(requirements.getRemoveCompletion()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Use Case Tests")
    class UseCaseTests {

        @Test
        @DisplayName("Should support reset floors on completion (F1->F2->F3 with reset)")
        void testResetFloorsUseCase() {
            // Given: F3 requires F1 and F2, and resets them on completion
            requirements.setRequiredFloorsId(Arrays.asList("dungeon_F1", "dungeon_F2"));
            requirements.setRemoveCompletion(Arrays.asList("dungeon_F1", "dungeon_F2"));

            // Then
            assertThat(requirements.getRequiredFloorsId()).hasSize(2);
            assertThat(requirements.getRemoveCompletion()).hasSize(2);
            assertThat(requirements.getRequiredFloorsId()).containsAll(requirements.getRemoveCompletion());
        }

        @Test
        @DisplayName("Should support partial reset (F3 resets F1 only)")
        void testPartialResetUseCase() {
            // Given: F3 requires F1 and F2, but only resets F1
            requirements.setRequiredFloorsId(Arrays.asList("F1", "F2"));
            requirements.setRemoveCompletion(Arrays.asList("F1"));

            // Then
            assertThat(requirements.getRequiredFloorsId()).contains("F1", "F2");
            assertThat(requirements.getRemoveCompletion()).containsOnly("F1");
            assertThat(requirements.getRemoveCompletion()).doesNotContain("F2");
        }

        @Test
        @DisplayName("Should support bonus floor with reset")
        void testBonusFloorUseCase() {
            // Given: F4 is a bonus that doesn't require F3 but resets F1 and F2 if completed
            requirements.setRequiredFloorsId(Arrays.asList("F1", "F2")); // Optional
            requirements.setRemoveCompletion(Arrays.asList("F1", "F2"));

            // Then
            assertThat(requirements.getRemoveCompletion()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Getter/Setter Tests")
    class GetterSetterTests {

        @Test
        @DisplayName("Should get and set removeCompletion correctly")
        void testGetSetRemoveCompletion() {
            // Given
            List<String> floors = Arrays.asList("floor_a", "floor_b", "floor_c");

            // When
            requirements.setRemoveCompletion(floors);
            List<String> result = requirements.getRemoveCompletion();

            // Then
            assertThat(result).isEqualTo(floors);
            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("Should update removeCompletion multiple times")
        void testMultipleSetOperations() {
            // Given
            requirements.setRemoveCompletion(Arrays.asList("floor1"));

            // When
            requirements.setRemoveCompletion(Arrays.asList("floor1", "floor2"));

            // Then
            assertThat(requirements.getRemoveCompletion()).hasSize(2);
        }

        @Test
        @DisplayName("Should clear removeCompletion by setting empty list")
        void testClearRemoveCompletion() {
            // Given
            requirements.setRemoveCompletion(Arrays.asList("floor1", "floor2"));

            // When
            requirements.setRemoveCompletion(Arrays.asList());

            // Then
            assertThat(requirements.getRemoveCompletion()).isEmpty();
        }
    }

    @Nested
    @DisplayName("ToString Tests")
    class ToStringTests {

        @Test
        @DisplayName("Should include removeCompletion in toString")
        void testToStringIncludesRemoveCompletion() {
            // Given
            requirements.setRemoveCompletion(Arrays.asList("floor1", "floor2"));

            // When
            String result = requirements.toString();

            // Then
            assertThat(result).contains("removeCompletion");
        }

        @Test
        @DisplayName("Should produce valid toString output")
        void testToStringFormatting() {
            // Given
            requirements.setMinLevel(5);
            requirements.setRequiredFloorsId(Arrays.asList("F1"));
            requirements.setRemoveCompletion(Arrays.asList("F1"));

            // When
            String result = requirements.toString();

            // Then
            assertThat(result).isNotEmpty();
            assertThat(result).contains("Requirements{");
            assertThat(result).contains("minLevel=5");
        }

        @Test
        @DisplayName("Should handle toString with null removeCompletion")
        void testToStringWithNullRemoveCompletion() {
            // When
            requirements.setRemoveCompletion(null);
            String result = requirements.toString();

            // Then
            assertThat(result).isNotEmpty();
            assertThat(result).contains("null");
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle very large list of floors")
        void testLargeFloorList() {
            // Given
            List<String> manyFloors = new java.util.ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                manyFloors.add("floor_" + i);
            }

            // When
            requirements.setRemoveCompletion(manyFloors);

            // Then
            assertThat(requirements.getRemoveCompletion()).hasSize(1000);
        }

        @Test
        @DisplayName("Should handle duplicate floor IDs in removeCompletion")
        void testDuplicateFloorIds() {
            // Given
            List<String> floorsWithDuplicates = Arrays.asList("floor1", "floor1", "floor2");

            // When
            requirements.setRemoveCompletion(floorsWithDuplicates);

            // Then - May contain duplicates, depending on implementation
            assertThat(requirements.getRemoveCompletion()).hasSize(3);
        }

        @Test
        @DisplayName("Should handle special characters in floor IDs")
        void testSpecialCharactersInFloorIds() {
            // Given
            List<String> specialIds = Arrays.asList("floor-1", "floor.2", "floor_3_a");

            // When
            requirements.setRemoveCompletion(specialIds);

            // Then
            assertThat(requirements.getRemoveCompletion()).contains("floor-1", "floor.2", "floor_3_a");
        }
    }
}

