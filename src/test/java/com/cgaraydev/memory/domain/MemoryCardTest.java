package com.cgaraydev.memory.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MemoryCardTest {

    @Test
    void newCardShouldStartFaceDownAndUnmatched() {
        MemoryCard card = new MemoryCard(0, "dragon");

        assertEquals(0, card.id());
        assertEquals("dragon", card.pairId());
        assertFalse(card.faceUp());
        assertFalse(card.matched());
    }

    @Test
    void revealingCardShouldReturnNewVisibleCard() {
        MemoryCard original = new MemoryCard(0, "dragon");

        MemoryCard revealed = original.reveal();

        assertTrue(revealed.faceUp());
        assertFalse(revealed.matched());
        assertFalse(original.faceUp());
        assertNotSame(original, revealed);
    }

    @Test
    void hidingCardShouldKeepItUnmatched() {
        MemoryCard card = new MemoryCard(0, "dragon")
                .reveal();

        MemoryCard hidden = card.hide();

        assertFalse(hidden.faceUp());
        assertFalse(hidden.matched());
    }

    @Test
    void matchingCardShouldKeepItVisible() {
        MemoryCard card = new MemoryCard(0, "dragon");

        MemoryCard matched = card.match();

        assertTrue(matched.faceUp());
        assertTrue(matched.matched());
    }

    @Test
    void matchedCardCannotBeHidden() {
        MemoryCard card = new MemoryCard(0, "dragon")
                .match();

        assertSame(card, card.hide());
    }

    @Test
    void repeatedTransitionsShouldBeIdempotent() {
        MemoryCard revealed = new MemoryCard(0, "dragon")
                .reveal();

        assertSame(revealed, revealed.reveal());

        MemoryCard matched = revealed.match();

        assertSame(matched, matched.match());
    }

    @Test
    void invalidCardDataShouldBeRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new MemoryCard(-1, "dragon")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new MemoryCard(0, "")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new MemoryCard(0, null)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new MemoryCard(0, "dragon", false, true)
        );
    }
}
