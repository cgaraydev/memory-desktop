package com.cgaraydev.memory.ui;

import com.cgaraydev.memory.domain.Difficulty;

import java.util.Objects;

/**
 * Calcula dimensiones del tablero sin depender de nodos JavaFX.
 * Cuando la ventana es pequeña, conserva un mínimo legible y permite scroll.
 */
public final class BoardSizing {

    public static final double CARD_RATIO = 1.12;
    public static final double CARD_GAP = 10.0;
    public static final double MIN_CARD_WIDTH = 64.0;
    public static final double MAX_CARD_WIDTH = 190.0;

    // Reserva espacio para los márgenes del contenedor y el borde del panel.
    private static final double HORIZONTAL_INSETS = 60.0;
    private static final double VERTICAL_INSETS = 58.0;

    private BoardSizing() {}

    public static double cardWidth(
            Difficulty difficulty,
            double viewportWidth,
            double viewportHeight
    ) {
        Objects.requireNonNull(difficulty);
        if (!Double.isFinite(viewportWidth) || !Double.isFinite(viewportHeight)
                || viewportWidth <= 0 || viewportHeight <= 0) {
            throw new IllegalArgumentException("El área del tablero debe ser positiva y finita.");
        }

        int columns = difficulty.getColumns();
        int rows = difficulty.getRows();

        double maxByWidth = (viewportWidth - HORIZONTAL_INSETS
                - (columns - 1) * CARD_GAP) / columns;
        double maxByHeight = (viewportHeight - VERTICAL_INSETS
                - (rows - 1) * CARD_GAP) / (rows * CARD_RATIO);

        return Math.max(
                MIN_CARD_WIDTH,
                Math.min(MAX_CARD_WIDTH, Math.min(maxByWidth, maxByHeight))
        );
    }
}
