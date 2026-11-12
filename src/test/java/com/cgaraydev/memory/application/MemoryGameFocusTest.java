package com.cgaraydev.memory.application;

import com.cgaraydev.memory.domain.Difficulty;
import com.cgaraydev.memory.domain.MemoryClock;
import com.cgaraydev.memory.domain.MemoryEngine;
import com.cgaraydev.memory.domain.MemoryPhase;
import com.cgaraydev.memory.domain.MemorySession;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class MemoryGameFocusTest {

    private static final class FakeClock implements MemoryClock {
        private long time;

        @Override
        public long nowMillis() {
            return time;
        }

        void advance(long millis) {
            time += millis;
        }
    }

    private final FakeClock clock = new FakeClock();
    private final MemoryGameCoordinator game = new MemoryGameCoordinator(
            new MemoryEngine(), clock, new Random(42)
    );

    private static List<String> bank() {
        return IntStream.range(0, 20)
                .mapToObj(index -> "pair-" + index)
                .toList();
    }

    private void start() {
        game.start(Difficulty.EASY, bank());
    }

    private void selectTwoCards() {
        MemorySession session = game.session();
        game.selectCard(session.id(), session.cards().get(0).id());
        game.selectCard(session.id(), session.cards().get(1).id());
    }

    @Test
    void losingFocusShouldPauseAndRecoveringShouldResumeAutomatically() {
        start();
        clock.advance(250);
        game.onWindowFocusChanged(false);
        clock.advance(5_000);

        assertTrue(game.isPaused());
        assertFalse(game.isManuallyPaused());
        assertFalse(game.canSelectCard());
        assertEquals(250L, game.elapsedMillis());

        game.onWindowFocusChanged(true);
        clock.advance(300);

        assertFalse(game.isPaused());
        assertEquals(550L, game.elapsedMillis());
        assertTrue(game.canSelectCard());
    }

    @Test
    void manualPauseShouldPersistWhenWindowRegainsFocus() {
        start();
        game.pause();
        game.onWindowFocusChanged(false);
        clock.advance(3_000);
        game.onWindowFocusChanged(true);

        assertTrue(game.isPaused());
        assertTrue(game.isManuallyPaused());
        assertEquals(0L, game.elapsedMillis());

        game.resume();
        clock.advance(400);
        assertFalse(game.isPaused());
        assertEquals(400L, game.elapsedMillis());
    }

    @Test
    void clearingManualPauseWhileUnfocusedShouldStayPaused() {
        start();
        game.pause();
        game.onWindowFocusChanged(false);
        game.resume();
        clock.advance(1_000);

        assertFalse(game.isManuallyPaused());
        assertTrue(game.isPaused());
        assertEquals(0L, game.elapsedMillis());

        game.onWindowFocusChanged(true);
        clock.advance(200);
        assertEquals(200L, game.elapsedMillis());
    }

    @Test
    void losingFocusShouldPreservePendingResolutionTime() {
        start();
        selectTwoCards();
        clock.advance(250);
        game.onWindowFocusChanged(false);
        clock.advance(10_000);
        game.update();

        assertEquals(MemoryPhase.RESOLVING, game.session().phase());
        assertEquals(450L, game.remainingResolutionMillis());
        assertEquals(250L, game.elapsedMillis());

        game.onWindowFocusChanged(true);
        clock.advance(449);
        game.update();
        assertEquals(MemoryPhase.RESOLVING, game.session().phase());

        clock.advance(1);
        game.update();
        assertEquals(MemoryPhase.SELECTING, game.session().phase());
        assertEquals(700L, game.elapsedMillis());
    }

    @Test
    void startingUnfocusedShouldNotRunTimerUntilFocusReturns() {
        game.onWindowFocusChanged(false);
        start();
        clock.advance(10_000);

        assertTrue(game.isPaused());
        assertEquals(0L, game.elapsedMillis());

        game.onWindowFocusChanged(true);
        clock.advance(800);
        assertEquals(800L, game.elapsedMillis());
    }

    @Test
    void restartingWhileUnfocusedShouldResetAndRemainPaused() {
        start();
        selectTwoCards();
        game.onWindowFocusChanged(false);
        game.restart();
        clock.advance(1_500);

        assertTrue(game.isPaused());
        assertEquals(0, game.session().moves());
        assertEquals(0L, game.elapsedMillis());
        assertEquals(0L, game.remainingResolutionMillis());

        game.onWindowFocusChanged(true);
        clock.advance(100);
        assertEquals(100L, game.elapsedMillis());
    }
}
