package com.cgaraydev.memory.application;

import com.cgaraydev.memory.domain.Difficulty;
import com.cgaraydev.memory.domain.MemoryPhase;
import com.cgaraydev.memory.domain.MemorySession;

import java.util.Objects;

/** Resultado inmutable de una partida completa. Se registra al finalizar la partida. */
public record GameResult(
        String sessionId,
        Difficulty difficulty,
        int pairs,
        int moves,
        long elapsedMillis
) {

    public GameResult {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("El identificador de partida es obligatorio.");
        }
        Objects.requireNonNull(difficulty, "La dificultad es obligatoria.");
        if (pairs != difficulty.getPairs() || moves < pairs) {
            throw new IllegalArgumentException("Los movimientos y parejas no son válidos.");
        }
        if (elapsedMillis < 0) {
            throw new IllegalArgumentException("El tiempo no puede ser negativo.");
        }
    }

    public static GameResult from(MemorySession session, long elapsedMillis) {
        Objects.requireNonNull(session, "La partida es obligatoria.");
        if (session.phase() != MemoryPhase.FINISHED
                || session.matchedPairs() != session.totalPairs()) {
            throw new IllegalArgumentException("La partida aún no ha finalizado.");
        }
        return new GameResult(
                session.id(), session.difficulty(), session.totalPairs(),
                session.moves(), elapsedMillis
        );
    }

    /** Intentos en que las dos cartas no formaron pareja. */
    public int failedAttempts() {
        return moves - pairs;
    }
}
