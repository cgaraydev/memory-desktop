package com.cgaraydev.memory.ui;

import com.cgaraydev.memory.domain.Difficulty;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoardSizingTest {

    @Test
    void largeWindowShouldUseMaximumCardSize() {
        assertEquals(190.0, BoardSizing.cardWidth(Difficulty.EASY, 1600, 1100));
    }

    @Test
    void smallerViewportShouldReduceCardSize() {
        double wide = BoardSizing.cardWidth(Difficulty.EASY, 1000, 700);
        double small = BoardSizing.cardWidth(Difficulty.EASY, 800, 450);
        assertTrue(small < wide);
    }

    @Test
    void sameViewportShouldOfferSmallerCardsForLargerBoards() {
        double easy = BoardSizing.cardWidth(Difficulty.EASY, 1000, 660);
        double normal = BoardSizing.cardWidth(Difficulty.NORMAL, 1000, 660);
        double hard = BoardSizing.cardWidth(Difficulty.HARD, 1000, 660);
        assertTrue(easy > normal);
        assertTrue(normal > hard);
    }

    @Test
    void smallViewportShouldPreserveReadableMinimum() {
        assertEquals(64.0, BoardSizing.cardWidth(Difficulty.HARD, 600, 350));
    }

    @Test
    void cardDimensionsShouldFitWhenViewportIsLargeEnough() {
        for (Difficulty difficulty : Difficulty.values()) {
            double width = BoardSizing.cardWidth(difficulty, 1100, 850);
            double gridWidth = width * difficulty.getColumns()
                    + (difficulty.getColumns() - 1) * BoardSizing.CARD_GAP;
            double gridHeight = width * BoardSizing.CARD_RATIO * difficulty.getRows()
                    + (difficulty.getRows() - 1) * BoardSizing.CARD_GAP;
            assertTrue(gridWidth <= 1100 - 60);
            assertTrue(gridHeight <= 850 - 58);
        }
    }

    @Test
    void invalidViewportShouldBeRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> BoardSizing.cardWidth(Difficulty.EASY, 0, 600));
        assertThrows(IllegalArgumentException.class,
                () -> BoardSizing.cardWidth(Difficulty.EASY, 900, Double.NaN));
    }
}
