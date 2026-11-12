package com.cgaraydev.memory.application;

import com.cgaraydev.memory.domain.Difficulty;
import com.cgaraydev.memory.domain.MemoryCard;
import com.cgaraydev.memory.domain.MemoryPhase;
import com.cgaraydev.memory.domain.MemorySession;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameResultTest {

    private MemorySession completedSession(int moves) {
        List<MemoryCard> cards = new ArrayList<>();
        for (int index = 0; index < 6; index++) {
            cards.add(new MemoryCard(index * 2, "pair-" + index).match());
            cards.add(new MemoryCard(index * 2 + 1, "pair-" + index).match());
        }
        return new MemorySession(
                "session-1", Difficulty.EASY, cards, moves, MemoryPhase.FINISHED
        );
    }

    @Test
    void shouldCreateResultFromCompletedGame() {
        GameResult result = GameResult.from(completedSession(8), 52_400);
        assertEquals("session-1", result.sessionId());
        assertEquals(Difficulty.EASY, result.difficulty());
        assertEquals(6, result.pairs());
        assertEquals(8, result.moves());
        assertEquals(52_400L, result.elapsedMillis());
    }

    @Test
    void minimumMovementsShouldHaveNoFailedAttempts() {
        GameResult result = GameResult.from(completedSession(6), 12_000);
        assertEquals(0, result.failedAttempts());
    }

    @Test
    void shouldCountFailedAttempts() {
        GameResult result = GameResult.from(completedSession(8), 38_000);
        assertEquals(2, result.failedAttempts());
    }

    @Test
    void shouldCountFailedAttemptsForLongerGames() {
        GameResult result = GameResult.from(completedSession(15), 100_000);
        assertEquals(9, result.failedAttempts());
    }

    @Test
    void shouldCountFailedAttemptsInNormalDifficulty() {
        GameResult result = new GameResult(
                "session-normal", Difficulty.NORMAL, 10, 18, 72_000
        );
        assertEquals(8, result.failedAttempts());
    }

    @Test
    void shouldCountFailedAttemptsInHardDifficulty() {
        GameResult result = new GameResult(
                "session-hard", Difficulty.HARD, 15, 35, 180_000
        );
        assertEquals(20, result.failedAttempts());
    }

    @Test
    void shouldRejectUnfinishedGame() {
        MemorySession unfinished = new MemorySession(
                "session-1", Difficulty.EASY, hiddenCards()
        );
        assertThrows(IllegalArgumentException.class,
                () -> GameResult.from(unfinished, 500));
    }

    @Test
    void shouldRejectInvalidResultData() {
        assertThrows(IllegalArgumentException.class,
                () -> new GameResult("", Difficulty.EASY, 6, 6, 100));
        assertThrows(IllegalArgumentException.class,
                () -> new GameResult("session", Difficulty.EASY, 6, 5, 100));
        assertThrows(IllegalArgumentException.class,
                () -> new GameResult("session", Difficulty.EASY, 6, 6, -1));
        assertThrows(IllegalArgumentException.class,
                () -> new GameResult("session", Difficulty.EASY, 5, 6, 100));
    }

    private List<MemoryCard> hiddenCards() {
        List<MemoryCard> cards = new ArrayList<>();
        for (int index = 0; index < 6; index++) {
            cards.add(new MemoryCard(index * 2, "pair-" + index));
            cards.add(new MemoryCard(index * 2 + 1, "pair-" + index));
        }
        return cards;
    }
}
