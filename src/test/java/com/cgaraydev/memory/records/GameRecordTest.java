package com.cgaraydev.memory.records;

import com.cgaraydev.memory.application.GameResult;
import com.cgaraydev.memory.domain.Difficulty;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class GameRecordTest {
    @Test void mapsCompletedGameWithoutLosingData() {
        Instant date = Instant.parse("2026-11-12T12:00:00Z");
        var source = new GameResult("abc", Difficulty.HARD, 15, 23, 105_000);
        GameRecord record = GameRecord.from(source, date);
        assertEquals("abc", record.sessionId());
        assertEquals(Difficulty.HARD, record.difficulty());
        assertEquals(23, record.moves());
        assertEquals(105_000, record.elapsedMillis());
        assertEquals(date, record.completedAt());
        assertEquals(8, record.failedAttempts());
    }

    @Test void refusesInvalidValues() {
        Instant date = Instant.EPOCH;
        assertThrows(IllegalArgumentException.class,
                () -> new GameRecord("", Difficulty.EASY, 6, 1, date));
        assertThrows(IllegalArgumentException.class,
                () -> new GameRecord("x", Difficulty.EASY, 5, 1, date));
        assertThrows(IllegalArgumentException.class,
                () -> new GameRecord("x", Difficulty.EASY, 6, -1, date));
    }
}
