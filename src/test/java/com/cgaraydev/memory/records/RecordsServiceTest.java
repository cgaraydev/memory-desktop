package com.cgaraydev.memory.records;

import com.cgaraydev.memory.application.GameResult;
import com.cgaraydev.memory.domain.Difficulty;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RecordsServiceTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-11-12T12:00:00Z"), ZoneOffset.UTC);

    static final class InMemoryStore implements RecordStore {
        RecordArchive saved = RecordArchive.empty();
        int saves;
        boolean shouldFail;
        @Override public RecordArchive load() { return saved; }
        @Override public void save(RecordArchive archive) throws IOException {
            if (shouldFail) throw new IOException("Fallo simulado");
            saved = archive;
            saves++;
        }
    }

    private final InMemoryStore store = new InMemoryStore();
    private final RecordsService service = new RecordsService(store, CLOCK);

    static GameResult result(String id, Difficulty difficulty, int moves, long millis) {
        return new GameResult(id, difficulty, difficulty.getPairs(), moves, millis);
    }

    @Test void startsWithEmptyRankings() throws IOException {
        for (Difficulty difficulty : Difficulty.values()) {
            assertTrue(service.top(difficulty).isEmpty());
            assertEquals(0, service.statistics(difficulty).completedGames());
        }
    }

    @Test void sortsByFewestMovesThenShortestTime() throws IOException {
        service.submit(result("slow", Difficulty.EASY, 9, 95_000));
        service.submit(result("fast", Difficulty.EASY, 9, 50_000));
        service.submit(result("optimal", Difficulty.EASY, 6, 200_000));
        assertEquals(List.of("optimal", "fast", "slow"),
                service.top(Difficulty.EASY).stream().map(GameRecord::sessionId).toList());
    }

    @Test void keepsSeparateTopTenPerDifficulty() throws IOException {
        for (Difficulty difficulty : Difficulty.values()) {
            for (int i = 0; i < 14; i++) {
                service.submit(result(difficulty.name() + i, difficulty,
                        difficulty.getPairs() + i, 1_000));
            }
            assertEquals(10, service.top(difficulty).size());
            assertEquals(14, service.statistics(difficulty).completedGames());
            assertEquals(difficulty.getPairs(), service.top(difficulty).getFirst().moves());
        }
        assertEquals(30, store.saved.records().size());
        assertEquals(42, store.saved.history().size());
    }

    @Test void worseThanTopTenStillEntersHistory() throws IOException {
        for (int i = 0; i < 10; i++) {
            service.submit(result("initial-" + i, Difficulty.EASY, 6 + i, 2_000));
        }
        int savesBefore = store.saves;
        OptionalInt rank = service.submit(result("worse", Difficulty.EASY, 30, 1_000));
        assertTrue(rank.isEmpty());
        assertEquals(savesBefore + 1, store.saves);
        assertEquals(10, store.saved.records().size());
        assertEquals(11, store.saved.history().size());
    }

    @Test void qualifyingResultEvictsWorstEntryButRetainsItsHistory() throws IOException {
        for (int i = 0; i < 10; i++) {
            service.submit(result("initial-" + i, Difficulty.EASY, 10 + i, 2_000));
        }
        OptionalInt rank = service.submit(result("new-best", Difficulty.EASY, 6, 3_000));
        assertEquals(1, rank.orElseThrow());
        assertEquals(10, service.top(Difficulty.EASY).size());
        assertEquals(11, service.history(Difficulty.EASY).size());
        assertTrue(service.top(Difficulty.EASY).stream()
                .noneMatch(record -> record.sessionId().equals("initial-9")));
    }

    @Test void duplicateSubmissionDoesNotWriteAgain() throws IOException {
        GameResult game = result("session", Difficulty.NORMAL, 12, 90_000);
        assertEquals(1, service.submit(game).orElseThrow());
        assertEquals(1, service.submit(game).orElseThrow());
        assertEquals(1, store.saves);
        assertEquals(1, service.top(Difficulty.NORMAL).size());
        assertEquals(1, service.history(Difficulty.NORMAL).size());
    }

    @Test void duplicateOutsideTopTenDoesNotWriteAgain() throws IOException {
        for (int i = 0; i < 10; i++) {
            service.submit(result("top-" + i, Difficulty.EASY, 6 + i, 1_000));
        }
        GameResult game = result("unranked", Difficulty.EASY, 50, 5_000);
        assertTrue(service.submit(game).isEmpty());
        int before = store.saves;
        assertTrue(service.submit(game).isEmpty());
        assertEquals(before, store.saves);
    }

    @Test void duplicateIdWithConflictingResultIsRejected() throws IOException {
        service.submit(result("session", Difficulty.EASY, 8, 12_000));
        assertThrows(IllegalArgumentException.class,
                () -> service.submit(result("session", Difficulty.EASY, 9, 12_000)));
    }

    @Test void clearingRankingsPreservesHistoryAndOtherDifficulties() throws IOException {
        service.submit(result("a", Difficulty.EASY, 6, 1_000));
        service.submit(result("b", Difficulty.HARD, 15, 1_000));
        service.clear(Difficulty.EASY);
        assertTrue(service.top(Difficulty.EASY).isEmpty());
        assertEquals(1, service.statistics(Difficulty.EASY).completedGames());
        assertEquals(1, service.top(Difficulty.HARD).size());
    }

    @Test void clearingHistoryAlsoRemovesRankingsOfThatDifficulty() throws IOException {
        service.submit(result("a", Difficulty.EASY, 6, 1_000));
        service.submit(result("b", Difficulty.HARD, 15, 1_000));
        service.clearAll(Difficulty.EASY);
        assertEquals(0, service.statistics(Difficulty.EASY).completedGames());
        assertTrue(service.top(Difficulty.EASY).isEmpty());
        assertEquals(1, service.statistics(Difficulty.HARD).completedGames());
    }

    @Test void failedSaveDoesNotMutateMemory() throws IOException {
        service.submit(result("a", Difficulty.EASY, 10, 1_000));
        store.shouldFail = true;
        assertThrows(IOException.class,
                () -> service.submit(result("b", Difficulty.EASY, 6, 1_000)));
        assertEquals("a", service.top(Difficulty.EASY).getFirst().sessionId());
        assertEquals(1, service.statistics(Difficulty.EASY).completedGames());
        assertThrows(IOException.class, () -> service.clear(Difficulty.EASY));
        assertThrows(IOException.class, () -> service.clearAll(Difficulty.EASY));
        assertEquals(1, service.top(Difficulty.EASY).size());
    }

    @Test void rejectsDuplicateSessionIdsInSourceFile() {
        GameRecord entry = GameRecord.from(
                result("same", Difficulty.EASY, 6, 1_000), Instant.now(CLOCK));
        assertThrows(IllegalArgumentException.class,
                () -> new RecordArchive(List.of(entry, entry), List.of(entry), Set.of()));
    }

    @Test void tiesAreOrderedDeterministically() throws IOException {
        service.submit(result("z", Difficulty.EASY, 8, 1_000));
        service.submit(result("a", Difficulty.EASY, 8, 1_000));
        assertEquals(List.of("a", "z"),
                service.top(Difficulty.EASY).stream().map(GameRecord::sessionId).toList());
    }

    @Test void historyIsSortedByDate() throws IOException {
        service.submit(result("z", Difficulty.EASY, 8, 1_000));
        store.saved = new RecordArchive(store.saved.records(),
                List.of(new GameRecord("older", Difficulty.EASY, 10, 2_000,
                        Instant.parse("2026-01-01T00:00:00Z")),
                        store.saved.history().getFirst()), Set.of());
        RecordsService reloaded = new RecordsService(store, CLOCK);
        assertEquals(List.of("z", "older"),
                reloaded.history(Difficulty.EASY).stream().map(GameRecord::sessionId).toList());
    }
}
