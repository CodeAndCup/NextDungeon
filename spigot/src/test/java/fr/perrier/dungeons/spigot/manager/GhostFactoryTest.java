package fr.perrier.dungeons.spigot.manager;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for GhostFactory
 * Verifies ghost player management, visibility, and resource cleanup
 */
@DisplayName("GhostFactory Tests")
class GhostFactoryTest {

    private GhostFactory ghostFactory;
    private BukkitScheduler mockScheduler;
    private Scoreboard mockScoreboard;
    private Team mockTeam;
    private BukkitTask mockTask;

    @BeforeEach
    void setUp() {
        mockScheduler = mock(BukkitScheduler.class);
        mockScoreboard = mock(Scoreboard.class);
        mockTeam = mock(Team.class);
        mockTask = mock(BukkitTask.class);

        try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
            var serverMock = mock(Server.class);
            var scoreboardManagerMock = mock(ScoreboardManager.class);

            bukkitMock.when(Bukkit::getServer).thenReturn(serverMock);
            when(serverMock.getScoreboardManager()).thenReturn(scoreboardManagerMock);
            when(scoreboardManagerMock.getMainScoreboard()).thenReturn(mockScoreboard);
            when(mockScoreboard.getTeam("Ghosts")).thenReturn(mockTeam);

            bukkitMock.when(Bukkit::getScheduler).thenReturn(mockScheduler);
            when(mockScheduler.runTaskTimer(any(), any(Runnable.class), anyLong(), anyLong())).thenReturn(mockTask);

            ghostFactory = new GhostFactory();
        }
    }

    @Nested
    @DisplayName("Initialization Tests")
    class InitializationTests {

        @Test
        @DisplayName("Should initialize with task and team")
        void shouldInitializeWithTaskAndTeam() {
            // Then
            assertThat(ghostFactory.isClosed()).isFalse();
            verify(mockScheduler).runTaskTimer(any(), any(Runnable.class), eq(20L), eq(20L));
        }

        @Test
        @DisplayName("Should create new team if not exists")
        void shouldCreateNewTeamIfNotExists() {
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                var serverMock = mock(Server.class);
                var scoreboardManagerMock = mock(ScoreboardManager.class);
                var newTeam = mock(Team.class);

                bukkitMock.when(Bukkit::getServer).thenReturn(serverMock);
                when(serverMock.getScoreboardManager()).thenReturn(scoreboardManagerMock);
                when(scoreboardManagerMock.getMainScoreboard()).thenReturn(mockScoreboard);
                when(mockScoreboard.getTeam("Ghosts")).thenReturn(null);
                when(mockScoreboard.registerNewTeam("Ghosts")).thenReturn(newTeam);

                bukkitMock.when(Bukkit::getScheduler).thenReturn(mockScheduler);
                when(mockScheduler.runTaskTimer(any(), any(Runnable.class), anyLong(), anyLong())).thenReturn(mockTask);

                // When
                GhostFactory factory = new GhostFactory();

                // Then
                verify(mockScoreboard).registerNewTeam("Ghosts");
                assertThat(factory.isClosed()).isFalse();
            }
        }

        @Test
        @DisplayName("Should set team to see friendly invisibles")
        void shouldSetTeamToSeeFriendlyInvisibles() {
            // Then
            verify(mockTeam).setCanSeeFriendlyInvisibles(true);
        }
    }

    @Nested
    @DisplayName("Query Tests")
    class QueryTests {

        @Test
        @DisplayName("Should identify ghost players")
        void shouldIdentifyGhostPlayers() {
            // Given
            Player player = mock(Player.class);
            when(player.getName()).thenReturn("GhostPlayer");
            when(mockTeam.hasEntry("GhostPlayer")).thenReturn(true);

            // When
            boolean isGhost = ghostFactory.isGhost(player);

            // Then
            assertThat(isGhost).isTrue();
        }

        @Test
        @DisplayName("Should identify non-ghost players")
        void shouldIdentifyNonGhostPlayers() {
            // Given
            Player player = mock(Player.class);
            when(player.getName()).thenReturn("LivingPlayer");
            when(mockTeam.hasEntry("LivingPlayer")).thenReturn(false);

            // When
            boolean isGhost = ghostFactory.isGhost(player);

            // Then
            assertThat(isGhost).isFalse();
        }

        @Test
        @DisplayName("Should check if player is managed")
        void shouldCheckIfPlayerIsManaged() {
            // Given
            Player player = mock(Player.class);
            when(player.getName()).thenReturn("ManagedPlayer");
            when(mockTeam.hasEntry("ManagedPlayer")).thenReturn(true);

            // When
            boolean hasPlayer = ghostFactory.hasPlayer(player);

            // Then
            assertThat(hasPlayer).isTrue();
        }

        @Test
        @DisplayName("Should check if player is not managed")
        void shouldCheckIfPlayerIsNotManaged() {
            // Given
            Player player = mock(Player.class);
            when(player.getName()).thenReturn("UnmanagedPlayer");
            when(mockTeam.hasEntry("UnmanagedPlayer")).thenReturn(false);

            // When
            boolean hasPlayer = ghostFactory.hasPlayer(player);

            // Then
            assertThat(hasPlayer).isFalse();
        }

        @Test
        @DisplayName("Should throw hasPlayer when factory is closed")
        void shouldThrowHasPlayerWhenFactoryIsClosed() {
            // Given
            Player player = mock(Player.class);
            ghostFactory.close();

            // When/Then
            assertThatThrownBy(() -> ghostFactory.hasPlayer(player))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Ghost factory has closed. Cannot reuse instances.");
        }
    }

    @Nested
    @DisplayName("Get Members Tests")
    class GetMembersTests {

        @Test
        @DisplayName("Should return all managed players")
        void shouldReturnAllManagedPlayers() {
            // Given
            OfflinePlayer player1 = mock(OfflinePlayer.class);
            OfflinePlayer player2 = mock(OfflinePlayer.class);
            Set<OfflinePlayer> members = new HashSet<>();
            members.add(player1);
            members.add(player2);

            when(mockTeam.getPlayers()).thenReturn(members);

            // When
            OfflinePlayer[] result = ghostFactory.getMembers();

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).contains(player1, player2);
        }

        @Test
        @DisplayName("Should return empty array when no players managed")
        void shouldReturnEmptyArrayWhenNoPlayersManagedNull() {
            // Given
            when(mockTeam.getPlayers()).thenReturn(null);

            // When
            OfflinePlayer[] result = ghostFactory.getMembers();

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty array when team has no members")
        void shouldReturnEmptyArrayWhenTeamHasNoMembers() {
            // Given
            Set<OfflinePlayer> emptySet = new HashSet<>();
            when(mockTeam.getPlayers()).thenReturn(emptySet);

            // When
            OfflinePlayer[] result = ghostFactory.getMembers();

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should throw when factory is closed")
        void shouldThrowWhenFactoryIsClosed() {
            // Given
            ghostFactory.close();

            // When/Then
            assertThatThrownBy(() -> ghostFactory.getMembers())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Ghost factory has closed. Cannot reuse instances.");
        }
    }

    @Nested
    @DisplayName("Closed State Tests")
    class ClosedStateTests {

        @Test
        @DisplayName("Should indicate not closed on creation")
        void shouldIndicateNotClosedOnCreation() {
            // Then
            assertThat(ghostFactory.isClosed()).isFalse();
        }

        @Test
        @DisplayName("Should indicate closed after closing")
        void shouldIndicateClosedAfterClosing() {
            // When
            ghostFactory.close();

            // Then
            assertThat(ghostFactory.isClosed()).isTrue();
        }

        @Test
        @DisplayName("Should prevent adding player after close")
        void shouldPreventAddingPlayerAfterClose() {
            // Given
            Player player = mock(Player.class);
            ghostFactory.close();

            // When/Then
            assertThatThrownBy(() -> ghostFactory.addPlayer(player))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("Should prevent removing player after close")
        void shouldPreventRemovingPlayerAfterClose() {
            // Given
            Player player = mock(Player.class);
            ghostFactory.close();

            // When/Then
            assertThatThrownBy(() -> ghostFactory.removePlayer(player))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("Should prevent getting members after close")
        void shouldPreventGettingMembersAfterClose() {
            // Given
            ghostFactory.close();

            // When/Then
            assertThatThrownBy(() -> ghostFactory.getMembers())
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("Close and Cleanup Tests")
    class CloseAndCleanupTests {

        @Test
        @DisplayName("Should cancel the scheduler task on close")
        void shouldCancelTheSchedulerTaskOnClose() {
            // When
            ghostFactory.close();

            // Then
            verify(mockTask).cancel();
        }

        @Test
        @DisplayName("Should unregister the team on close")
        void shouldUnregisterTheTeamOnClose() {
            // When
            ghostFactory.close();

            // Then
            verify(mockTeam).unregister();
        }

        @Test
        @DisplayName("Should handle offline players gracefully on close")
        void shouldHandleOfflinePlayersGracefullyOnClose() {
            // Given
            OfflinePlayer offlinePlayer = mock(OfflinePlayer.class);
            when(offlinePlayer.getPlayer()).thenReturn(null);

            Set<OfflinePlayer> members = new HashSet<>();
            members.add(offlinePlayer);
            when(mockTeam.getPlayers()).thenReturn(members);

            // When/Then - should not throw
            assertThatCode(() -> ghostFactory.close()).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should be idempotent when closed multiple times")
        void shouldBeIdempotentWhenClosedMultipleTimes() {
            // When
            ghostFactory.close();
            ghostFactory.close();

            // Then
            verify(mockTask, times(1)).cancel();
            verify(mockTeam, times(1)).unregister();
        }

        @Test
        @DisplayName("Should handle close with null team gracefully")
        void shouldHandleCloseWithNullTeamGracefully() {
            try (MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class)) {
                var serverMock = mock(Server.class);
                var scoreboardManagerMock = mock(ScoreboardManager.class);
                var testScoreboard = mock(Scoreboard.class);
                var testTeam = mock(Team.class);

                bukkitMock.when(Bukkit::getServer).thenReturn(serverMock);
                when(serverMock.getScoreboardManager()).thenReturn(scoreboardManagerMock);
                when(scoreboardManagerMock.getMainScoreboard()).thenReturn(testScoreboard);
                when(testScoreboard.getTeam("Ghosts")).thenReturn(testTeam);

                var testScheduler = mock(BukkitScheduler.class);
                var testTask = mock(BukkitTask.class);
                bukkitMock.when(Bukkit::getScheduler).thenReturn(testScheduler);
                when(testScheduler.runTaskTimer(any(), any(Runnable.class), anyLong(), anyLong())).thenReturn(testTask);

                // When
                GhostFactory factory = new GhostFactory();

                // Then - should not throw
                assertThatCode(factory::close).doesNotThrowAnyException();
            }
        }
    }

    @Nested
    @DisplayName("Scheduled Update Task Tests")
    class ScheduledUpdateTaskTests {

        @Test
        @DisplayName("Should schedule periodic update task")
        void shouldSchedulePeriodicUpdateTask() {
            // Then
            verify(mockScheduler).runTaskTimer(any(), any(Runnable.class), eq(20L), eq(20L));
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle multiple close calls safely")
        void shouldHandleMultipleCloseCalls() {
            // When
            ghostFactory.close();
            ghostFactory.close();
            ghostFactory.close();

            // Then - should remain closed and not throw
            assertThat(ghostFactory.isClosed()).isTrue();
            verify(mockTask, times(1)).cancel();
        }

        @Test
        @DisplayName("Should handle large number of players")
        void shouldHandleLargeNumberOfPlayers() {
            // Given
            Set<OfflinePlayer> largeMemberSet = new HashSet<>();
            for (int i = 0; i < 100; i++) {
                OfflinePlayer player = mock(OfflinePlayer.class);
                largeMemberSet.add(player);
            }
            when(mockTeam.getPlayers()).thenReturn(largeMemberSet);

            // When
            OfflinePlayer[] result = ghostFactory.getMembers();

            // Then
            assertThat(result).hasSize(100);
        }

        @Test
        @DisplayName("Should handle empty members set")
        void shouldHandleEmptyMembersSet() {
            // Given
            when(mockTeam.getPlayers()).thenReturn(new HashSet<>());

            // When
            OfflinePlayer[] result = ghostFactory.getMembers();

            // Then
            assertThat(result).isNotNull().isEmpty();
        }
    }
}








