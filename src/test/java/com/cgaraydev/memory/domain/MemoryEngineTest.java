package com.cgaraydev.memory.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class MemoryEngineTest {

    private final MemoryEngine engine = new MemoryEngine();

    private List<String> pairIds() {
        return IntStream.range(0, 20)
                .mapToObj(index -> "pair-" + index)
                .toList();
    }

    private MemorySession startSession() {
        return engine.start(
                "session-1",
                Difficulty.EASY,
                pairIds(),
                new Random(42)
        );
    }

    private MemorySession selectPair(
            MemorySession session,
            MemoryCard first,
            MemoryCard second
    ) {
        MemorySession selecting = engine.select(
                session,
                session.id(),
                first.id()
        );

        return engine.select(
                selecting,
                session.id(),
                second.id()
        );
    }

    @Test
    void startShouldCreateValidBoard() {
        MemorySession session = startSession();

        assertEquals(12, session.cards().size());
        assertEquals(6, session.totalPairs());
        assertEquals(0, session.moves());
        assertEquals(MemoryPhase.SELECTING, session.phase());

        for (String pairId : session.cards().stream()
                .map(MemoryCard::pairId)
                .distinct()
                .toList()) {

            long count = session.cards().stream()
                    .filter(card -> card.pairId().equals(pairId))
                    .count();

            assertEquals(2, count);
        }
    }

    @Test
    void startShouldRejectInvalidPairBanks() {
        assertThrows(
                IllegalArgumentException.class,
                () -> engine.start(
                        "session-1",
                        Difficulty.EASY,
                        List.of("one"),
                        new Random(42)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> engine.start(
                        "session-1",
                        Difficulty.EASY,
                        List.of("a", "a", "b", "c", "d", "e"),
                        new Random(42)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> engine.start(
                        "",
                        Difficulty.EASY,
                        pairIds(),
                        new Random(42)
                )
        );
    }

    @Test
    void firstSelectionShouldRevealCardWithoutCountingMove() {
        MemorySession original = startSession();

        int cardId = original.cards().get(0).id();

        MemorySession updated = engine.select(
                original,
                original.id(),
                cardId
        );

        assertEquals(1, updated.revealedCards().size());
        assertEquals(0, updated.moves());
        assertEquals(MemoryPhase.SELECTING, updated.phase());

        assertTrue(original.revealedCards().isEmpty());
    }

    @Test
    void secondSelectionShouldCountMoveAndEnterResolving() {
        MemorySession session = startSession();

        MemoryCard first = session.cards().get(0);
        MemoryCard second = session.cards().get(1);

        MemorySession updated = selectPair(
                session,
                first,
                second
        );

        assertEquals(2, updated.revealedCards().size());
        assertEquals(1, updated.moves());
        assertEquals(MemoryPhase.RESOLVING, updated.phase());
    }

    @Test
    void invalidSelectionsShouldBeIgnored() {
        MemorySession session = startSession();

        assertSame(
                session,
                engine.select(session, "wrong-session", 0)
        );

        assertSame(
                session,
                engine.select(session, session.id(), -1)
        );

        int cardId = session.cards().get(0).id();

        MemorySession revealed = engine.select(
                session,
                session.id(),
                cardId
        );

        assertSame(
                revealed,
                engine.select(revealed, revealed.id(), cardId)
        );
    }

    @Test
    void resolvingMismatchShouldHideBothCards() {
        MemorySession session = startSession();

        MemoryCard first = session.cards().get(0);

        MemoryCard second = session.cards().stream()
                .filter(card -> !card.pairId().equals(first.pairId()))
                .findFirst()
                .orElseThrow();

        MemorySession resolving = selectPair(
                session,
                first,
                second
        );

        MemorySession resolved = engine.resolve(
                resolving,
                resolving.id(),
                resolving.moves()
        );

        assertTrue(resolved.revealedCards().isEmpty());
        assertEquals(0, resolved.matchedPairs());
        assertEquals(1, resolved.moves());
        assertEquals(MemoryPhase.SELECTING, resolved.phase());
    }

    @Test
    void resolvingMatchShouldKeepBothCardsVisible() {
        MemorySession session = startSession();

        MemoryCard first = session.cards().get(0);

        MemoryCard second = session.cards().stream()
                .filter(card -> card.id() != first.id())
                .filter(card -> card.pairId().equals(first.pairId()))
                .findFirst()
                .orElseThrow();

        MemorySession resolving = selectPair(
                session,
                first,
                second
        );

        MemorySession resolved = engine.resolve(
                resolving,
                resolving.id(),
                resolving.moves()
        );

        assertEquals(1, resolved.matchedPairs());
        assertEquals(MemoryPhase.SELECTING, resolved.phase());

        assertTrue(resolved.cards().stream()
                .filter(card -> card.pairId().equals(first.pairId()))
                .allMatch(card -> card.faceUp() && card.matched()));
    }

    @Test
    void resolvingShouldRejectStaleOperations() {
        MemorySession session = startSession();

        MemorySession resolving = selectPair(
                session,
                session.cards().get(0),
                session.cards().get(1)
        );

        assertSame(
                resolving,
                engine.resolve(
                        resolving,
                        "previous-session",
                        resolving.moves()
                )
        );

        assertSame(
                resolving,
                engine.resolve(
                        resolving,
                        resolving.id(),
                        resolving.moves() - 1
                )
        );

        assertSame(
                resolving,
                engine.select(
                        resolving,
                        resolving.id(),
                        resolving.cards().get(2).id()
                )
        );
    }

    @Test
    void completingAllPairsShouldFinishSession() {
        MemorySession session = startSession();

        List<String> selectedPairs = session.cards()
                .stream()
                .map(MemoryCard::pairId)
                .distinct()
                .toList();

        for (String pairId : selectedPairs) {
            List<MemoryCard> pair = session.cards()
                    .stream()
                    .filter(card -> card.pairId().equals(pairId))
                    .toList();

            MemorySession resolving = selectPair(
                    session,
                    pair.get(0),
                    pair.get(1)
            );

            session = engine.resolve(
                    resolving,
                    resolving.id(),
                    resolving.moves()
            );
        }

        assertEquals(6, session.matchedPairs());
        assertEquals(6, session.moves());
        assertEquals(MemoryPhase.FINISHED, session.phase());

        assertSame(
                session,
                engine.select(
                        session,
                        session.id(),
                        session.cards().get(0).id()
                )
        );
    }

    @Test
    void sameRandomSeedShouldProduceSameBoard() {
        MemorySession first = engine.start(
                "session-1",
                Difficulty.NORMAL,
                pairIds(),
                new Random(123)
        );

        MemorySession second = engine.start(
                "session-1",
                Difficulty.NORMAL,
                pairIds(),
                new Random(123)
        );

        assertEquals(first.cards(), second.cards());
    }
}
