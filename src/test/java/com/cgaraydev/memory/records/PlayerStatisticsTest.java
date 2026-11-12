package com.cgaraydev.memory.records;

import com.cgaraydev.memory.domain.Difficulty;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PlayerStatisticsTest {
    private static GameRecord record(String id, int moves, long time, String date) {
        return new GameRecord(id, Difficulty.EASY, moves, time, Instant.parse(date));
    }
    @Test void emptyHistoryHasZeroStats() {
        var stats = PlayerStatistics.from(Difficulty.EASY, List.of(), false);
        assertEquals(0, stats.completedGames());
        assertEquals(0, stats.totalElapsedMillis());
        assertTrue(stats.latest().isEmpty());
    }
    @Test void countsUnrankedAndRankedGames() {
        var stats = PlayerStatistics.from(Difficulty.EASY, List.of(
                record("a", 7, 30_000, "2026-11-10T00:00:00Z"),
                record("b", 25, 90_000, "2026-11-12T00:00:00Z")), false);
        assertEquals(2, stats.completedGames());
        assertEquals(120_000L, stats.totalElapsedMillis());
        assertEquals(10.0, stats.averageFailedAttempts());
        assertEquals("b", stats.latest().orElseThrow().sessionId());
    }
    @Test void separatesDifficulties() {
        var stats = PlayerStatistics.from(Difficulty.NORMAL,
                List.of(record("a", 7, 10_000, "2026-11-12T00:00:00Z")), false);
        assertEquals(0, stats.completedGames());
    }
    @Test void preservesMigrationNotice() {
        assertTrue(PlayerStatistics.from(Difficulty.EASY,
                List.of(record("a", 7, 10_000, "2026-11-12T00:00:00Z")), true)
                .partialLegacyHistory());
    }
    @Test void rejectsNullDifficulty() {
        assertThrows(NullPointerException.class,
                () -> PlayerStatistics.from(null, List.of(), false));
    }
}
