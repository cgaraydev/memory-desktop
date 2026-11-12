
package com.cgaraydev.memory.domain;

public enum Difficulty {

    EASY(6, 3, 4),
    NORMAL(10, 4, 5),
    HARD(15, 5, 6);

    private final int pairs;
    private final int rows;
    private final int columns;

    Difficulty(int pairs, int rows, int columns) {
        this.pairs = pairs;
        this.rows = rows;
        this.columns = columns;
    }

    public int getPairs() {
        return pairs;
    }

    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }

    public int getTotalCards() {
        return pairs * 2;
    }
}
