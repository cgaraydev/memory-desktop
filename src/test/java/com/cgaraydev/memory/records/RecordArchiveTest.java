package com.cgaraydev.memory.records;

import com.cgaraydev.memory.domain.Difficulty;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class RecordArchiveTest {
    private static GameRecord record(String id) {
        return new GameRecord(id, Difficulty.EASY, 6, 1000,
                Instant.parse("2026-11-12T12:00:00Z"));
    }
    @Test void emptyArchiveIsValid() {
        assertEquals(RecordArchive.empty(), new RecordArchive(List.of(), List.of(), Set.of()));
    }
    @Test void legacyTopAlsoBecomesKnownHistory() {
        assertEquals(1, RecordArchive.fromLegacy(List.of(record("a"))).history().size());
        assertTrue(RecordArchive.fromLegacy(List.of(record("a"))).legacyDifficulties().contains(Difficulty.EASY));
    }
    @Test void rejectsDuplicatesInHistory() {
        assertThrows(IllegalArgumentException.class,
                () -> new RecordArchive(List.of(), List.of(record("a"),record("a")), Set.of()));
    }
    @Test void rejectsOrphanRankedRecord() {
        assertThrows(IllegalArgumentException.class,
                () -> new RecordArchive(List.of(record("a")), List.of(), Set.of()));
    }
    @Test void rejectsMoreThanTenRankedEntries() {
        List<GameRecord> many = new ArrayList<>();
        for (int i=0; i<11; i++) many.add(record("s-"+i));
        assertThrows(IllegalArgumentException.class,
                () -> new RecordArchive(many, many, Set.of()));
    }
}
