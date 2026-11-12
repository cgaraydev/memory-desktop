package com.cgaraydev.memory.application;

import com.cgaraydev.memory.domain.Difficulty;
import com.cgaraydev.memory.domain.MemoryCard;
import com.cgaraydev.memory.domain.MemoryClock;
import com.cgaraydev.memory.domain.MemoryEngine;
import com.cgaraydev.memory.domain.MemoryPhase;
import com.cgaraydev.memory.domain.MemorySession;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class MemoryGameCoordinatorTest {

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

    private final MemoryGameCoordinator coordinator =
            new MemoryGameCoordinator(
                    new MemoryEngine(),
                    clock,
                    new Random(42)
            );

    private List<String> pairIds() {
        return IntStream.range(0, 20)
                .mapToObj(index -> "pair-" + index)
                .toList();
    }

    private void startGame() {
        coordinator.start(Difficulty.EASY, pairIds());
    }

    private void selectTwoCards() {
        MemorySession session = coordinator.session();

        coordinator.selectCard(
                session.id(),
                session.cards().get(0).id()
        );

        coordinator.selectCard(
                session.id(),
                session.cards().get(1).id()
        );
    }

    @Test
    void initialStateShouldHaveNoGame() {
        assertFalse(coordinator.hasSession());
        assertFalse(coordinator.isPaused());
        assertFalse(coordinator.isFinished());
        assertFalse(coordinator.canSelectCard());

        assertNull(coordinator.session());
        assertEquals(0L, coordinator.elapsedMillis());
        assertEquals(0L, coordinator.remainingResolutionMillis());

        coordinator.update();
        coordinator.pause();
        coordinator.resume();

        assertFalse(coordinator.hasSession());
    }

    @Test
    void startingGameShouldStartTimer() {
        startGame();

        assertTrue(coordinator.hasSession());
        assertTrue(coordinator.canSelectCard());
        assertFalse(coordinator.isPaused());

        assertEquals(
                MemoryPhase.SELECTING,
                coordinator.session().phase()
        );

        clock.advance(1_500);

        assertEquals(1_500L, coordinator.elapsedMillis());
        assertEquals(0, coordinator.session().moves());
    }

    @Test
    void secondCardShouldScheduleResolution() {
        startGame();

        MemorySession session = coordinator.session();

        coordinator.selectCard(
                session.id(),
                session.cards().get(0).id()
        );

        assertEquals(0, coordinator.session().moves());
        assertTrue(coordinator.canSelectCard());

        coordinator.selectCard(
                session.id(),
                session.cards().get(1).id()
        );

        assertEquals(1, coordinator.session().moves());
        assertFalse(coordinator.canSelectCard());

        assertEquals(
                MemoryPhase.RESOLVING,
                coordinator.session().phase()
        );

        assertEquals(
                700L,
                coordinator.remainingResolutionMillis()
        );
    }

    @Test
    void updateShouldResolveAfterExposure() {
        startGame();
        selectTwoCards();

        clock.advance(699);
        coordinator.update();

        assertEquals(
                MemoryPhase.RESOLVING,
                coordinator.session().phase()
        );

        clock.advance(1);
        coordinator.update();

        assertEquals(
                MemoryPhase.SELECTING,
                coordinator.session().phase()
        );

        assertEquals(0, coordinator.session().revealedCards().size());
        assertEquals(1, coordinator.session().moves());
        assertEquals(0L, coordinator.remainingResolutionMillis());
    }

    @Test
    void pauseShouldFreezeBothTimers() {
        startGame();
        selectTwoCards();

        clock.advance(250);
        coordinator.pause();

        clock.advance(10_000);
        coordinator.update();

        assertTrue(coordinator.isPaused());
        assertEquals(250L, coordinator.elapsedMillis());
        assertEquals(450L, coordinator.remainingResolutionMillis());

        assertEquals(
                MemoryPhase.RESOLVING,
                coordinator.session().phase()
        );

        coordinator.resume();

        clock.advance(449);
        coordinator.update();

        assertEquals(
                MemoryPhase.RESOLVING,
                coordinator.session().phase()
        );

        clock.advance(1);
        coordinator.update();

        assertEquals(
                MemoryPhase.SELECTING,
                coordinator.session().phase()
        );

        assertEquals(700L, coordinator.elapsedMillis());
    }

    @Test
    void pausedGameShouldRejectSelections() {
        startGame();

        MemorySession original = coordinator.session();

        coordinator.pause();

        coordinator.selectCard(
                original.id(),
                original.cards().get(0).id()
        );

        assertSame(original, coordinator.session());
        assertFalse(coordinator.canSelectCard());

        coordinator.resume();

        assertTrue(coordinator.canSelectCard());
    }

    @Test
    void restartShouldResetGameAndDiscardOldEvents() {
        startGame();

        String oldSessionId = coordinator.session().id();

        selectTwoCards();
        clock.advance(350);

        coordinator.restart();

        MemorySession restarted = coordinator.session();

        assertNotEquals(oldSessionId, restarted.id());
        assertEquals(0, restarted.moves());
        assertEquals(0L, coordinator.elapsedMillis());
        assertEquals(0L, coordinator.remainingResolutionMillis());

        coordinator.selectCard(
                oldSessionId,
                restarted.cards().get(0).id()
        );

        assertSame(restarted, coordinator.session());

        clock.advance(700);
        coordinator.update();

        assertEquals(
                MemoryPhase.SELECTING,
                coordinator.session().phase()
        );
    }

    @Test
    void returningToIntroShouldClearGame() {
        startGame();
        selectTwoCards();

        coordinator.returnToIntro();

        assertFalse(coordinator.hasSession());
        assertFalse(coordinator.isPaused());
        assertEquals(0L, coordinator.elapsedMillis());
        assertEquals(0L, coordinator.remainingResolutionMillis());

        startGame();

        clock.advance(1_000);
        coordinator.update();

        assertEquals(
                MemoryPhase.SELECTING,
                coordinator.session().phase()
        );
    }

    @Test
    void invalidBankShouldNotStartGame() {
        assertThrows(
                IllegalArgumentException.class,
                () -> coordinator.start(
                        Difficulty.EASY,
                        List.of("only-one-pair")
                )
        );

        assertFalse(coordinator.hasSession());
        assertEquals(0L, coordinator.elapsedMillis());
    }

    @Test
    void completedGameShouldStopTimer() {
        startGame();

        List<String> selectedPairs = coordinator.session()
                .cards()
                .stream()
                .map(MemoryCard::pairId)
                .distinct()
                .toList();

        for (String pairId : selectedPairs) {
            MemorySession session = coordinator.session();

            List<MemoryCard> pair = session.cards()
                    .stream()
                    .filter(card -> card.pairId().equals(pairId))
                    .toList();

            coordinator.selectCard(
                    session.id(),
                    pair.get(0).id()
            );

            coordinator.selectCard(
                    session.id(),
                    pair.get(1).id()
            );

            clock.advance(700);
            coordinator.update();
        }

        assertTrue(coordinator.isFinished());
        assertFalse(coordinator.canSelectCard());
        assertEquals(6, coordinator.session().matchedPairs());
        assertEquals(6, coordinator.session().moves());

        long finishedTime = coordinator.elapsedMillis();

        clock.advance(5_000);
        coordinator.update();
        coordinator.pause();
        coordinator.resume();

        assertEquals(finishedTime, coordinator.elapsedMillis());
    }

    @Test
    void startingAgainShouldNotReplaceActiveGame() {
        startGame();

        MemorySession original = coordinator.session();

        coordinator.start(
                Difficulty.HARD,
                pairIds()
        );

        assertSame(original, coordinator.session());
        assertEquals(Difficulty.EASY, coordinator.session().difficulty());
    }
}