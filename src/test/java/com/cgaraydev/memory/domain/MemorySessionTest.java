package com.cgaraydev.memory.domain;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MemorySessionTest {

    private List<MemoryCard> createCards() {
        List<MemoryCard> cards = new ArrayList<>();

        for (int pair = 0; pair < 6; pair++) {
            String pairId = "pair-" + pair;

            cards.add(new MemoryCard(pair * 2, pairId));
            cards.add(new MemoryCard(pair * 2 + 1, pairId));
        }

        return cards;
    }

    @Test
    void newSessionShouldStartInSelectingPhase() {
        MemorySession session = new MemorySession(
                "session-1",
                Difficulty.EASY,
                createCards()
        );

        assertEquals(0, session.moves());
        assertEquals(MemoryPhase.SELECTING, session.phase());
        assertEquals(6, session.totalPairs());
        assertEquals(0, session.matchedPairs());
        assertTrue(session.revealedCards().isEmpty());
    }

    @Test
    void sessionShouldCountMatchedPairs() {
        List<MemoryCard> cards = createCards();

        cards.set(0, cards.get(0).match());
        cards.set(1, cards.get(1).match());

        MemorySession session = new MemorySession(
                "session-1",
                Difficulty.EASY,
                cards
        );

        assertEquals(1, session.matchedPairs());
        assertEquals(6, session.totalPairs());
    }

    @Test
    void revealedCardsShouldExcludeMatchedCards() {
        List<MemoryCard> cards = createCards();

        cards.set(0, cards.get(0).match());
        cards.set(1, cards.get(1).match());
        cards.set(2, cards.get(2).reveal());

        MemorySession session = new MemorySession(
                "session-1",
                Difficulty.EASY,
                cards
        );

        assertEquals(1, session.revealedCards().size());
        assertEquals(2, session.revealedCards().get(0).id());
    }

    @Test
    void updatingStateShouldPreserveOriginalSession() {
        MemorySession original = new MemorySession(
                "session-1",
                Difficulty.EASY,
                createCards()
        );

        List<MemoryCard> updatedCards =
                new ArrayList<>(original.cards());

        updatedCards.set(0, updatedCards.get(0).reveal());

        MemorySession updated = original.withState(
                updatedCards,
                1,
                MemoryPhase.RESOLVING
        );

        assertEquals(0, original.moves());
        assertEquals(MemoryPhase.SELECTING, original.phase());

        assertEquals(1, updated.moves());
        assertEquals(MemoryPhase.RESOLVING, updated.phase());
        assertEquals(1, updated.revealedCards().size());
    }

    @Test
    void sessionCardsShouldBeImmutable() {
        List<MemoryCard> cards = createCards();

        MemorySession session = new MemorySession(
                "session-1",
                Difficulty.EASY,
                cards
        );

        cards.clear();

        assertEquals(12, session.cards().size());

        assertThrows(
                UnsupportedOperationException.class,
                () -> session.cards().clear()
        );
    }

    @Test
    void invalidSessionShouldBeRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new MemorySession(
                        "",
                        Difficulty.EASY,
                        createCards()
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new MemorySession(
                        "session-1",
                        Difficulty.EASY,
                        List.of()
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new MemorySession(
                        "session-1",
                        Difficulty.EASY,
                        createCards(),
                        -1,
                        MemoryPhase.SELECTING
                )
        );
    }
}
