
package com.cgaraydev.memory.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DifficultyTest {

    @Test
    void easyDifficultyShouldHaveSixPairs() {
        assertEquals(6, Difficulty.EASY.getPairs());
        assertEquals(12, Difficulty.EASY.getTotalCards());
    }

    @Test
    void normalDifficultyShouldHaveTenPairs() {
        assertEquals(10, Difficulty.NORMAL.getPairs());
        assertEquals(20, Difficulty.NORMAL.getTotalCards());
    }

    @Test
    void hardDifficultyShouldHaveFifteenPairs() {
        assertEquals(15, Difficulty.HARD.getPairs());
        assertEquals(30, Difficulty.HARD.getTotalCards());
    }

    @Test
    void boardDimensionsShouldMatchCardCount() {
        for (Difficulty difficulty : Difficulty.values()) {
            int boardCapacity =
                    difficulty.getRows() * difficulty.getColumns();

            assertEquals(
                    difficulty.getTotalCards(),
                    boardCapacity
            );
        }
    }
}
