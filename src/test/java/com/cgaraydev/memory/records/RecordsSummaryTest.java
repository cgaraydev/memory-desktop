package com.cgaraydev.memory.records;

import com.cgaraydev.memory.domain.Difficulty;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecordsSummaryTest {

    private static GameRecord record(String id, Difficulty difficulty,
                                     int moves, long elapsedMillis, String date) {
        return new GameRecord(id, difficulty, moves, elapsedMillis, Instant.parse(date));
    }

    private static final String EARLIER = "2026-11-01T08:00:00Z";
    private static final String LATER = "2026-11-12T12:00:00Z";

    @Test void emptyRankingHasNoBestOrLatest() {
        RecordsSummary summary = RecordsSummary.from(Difficulty.EASY, List.of());
        assertEquals(Difficulty.EASY, summary.difficulty());
        assertEquals(0, summary.rankedResults());
        assertTrue(summary.best().isEmpty());
        assertTrue(summary.latest().isEmpty());
        assertEquals(0.0, summary.averageFailedAttempts());
    }

    @Test void bestResultPrioritizesMovementsOverTime() {
        GameRecord slowerButBetter = record("best", Difficulty.EASY, 6, 90_000, EARLIER);
        GameRecord quickButWorse = record("quick", Difficulty.EASY, 7, 20_000, LATER);
        RecordsSummary summary = RecordsSummary.from(Difficulty.EASY,
                List.of(quickButWorse, slowerButBetter));
        assertEquals(slowerButBetter, summary.best().orElseThrow());
    }

    @Test void bestResultUsesTimeToBreakMovementTies() {
        GameRecord slower = record("slow", Difficulty.NORMAL, 12, 90_000, EARLIER);
        GameRecord faster = record("fast", Difficulty.NORMAL, 12, 70_000, LATER);
        RecordsSummary summary = RecordsSummary.from(Difficulty.NORMAL, List.of(slower, faster));
        assertEquals(faster, summary.best().orElseThrow());
    }

    @Test void latestResultUsesDateInsteadOfRankingPosition() {
        GameRecord best = record("best", Difficulty.HARD, 15, 120_000, EARLIER);
        GameRecord latest = record("latest", Difficulty.HARD, 19, 70_000, LATER);
        RecordsSummary summary = RecordsSummary.from(Difficulty.HARD, List.of(latest, best));
        assertEquals(latest, summary.latest().orElseThrow());
    }

    @Test void averageFailedAttemptsCanBeFractional() {
        GameRecord first = record("a", Difficulty.EASY, 7, 10_000, EARLIER);
        GameRecord second = record("b", Difficulty.EASY, 8, 20_000, LATER);
        RecordsSummary summary = RecordsSummary.from(Difficulty.EASY, List.of(first, second));
        assertEquals(1.5, summary.averageFailedAttempts(), 0.00001);
        assertEquals(2, summary.rankedResults());
    }

    @Test void perfectGameHasZeroAverageFailedAttempts() {
        GameRecord entry = record("perfect", Difficulty.NORMAL, 10, 10_000, EARLIER);
        assertEquals(0.0,
                RecordsSummary.from(Difficulty.NORMAL, List.of(entry)).averageFailedAttempts());
    }

    @Test void rejectsRecordsFromAnotherDifficulty() {
        GameRecord wrong = record("wrong", Difficulty.NORMAL, 12, 10_000, EARLIER);
        assertThrows(IllegalArgumentException.class,
                () -> RecordsSummary.from(Difficulty.EASY, List.of(wrong)));
    }

    @Test void refusesMoreThanTenEntries() {
        List<GameRecord> entries = java.util.stream.IntStream.range(0, 11)
                .mapToObj(i -> record("s" + i, Difficulty.EASY, 6 + i, 10_000, EARLIER))
                .toList();
        assertThrows(IllegalArgumentException.class,
                () -> RecordsSummary.from(Difficulty.EASY, entries));
    }

    @Test void latestDateTieUsesStableSessionIdOrdering() {
        GameRecord a = record("a", Difficulty.EASY, 7, 10_000, EARLIER);
        GameRecord z = record("z", Difficulty.EASY, 7, 10_000, EARLIER);
        RecordsSummary summary = RecordsSummary.from(Difficulty.EASY, List.of(z, a));
        assertEquals(z, summary.latest().orElseThrow());
    }
}
