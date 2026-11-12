package com.cgaraydev.memory.records;

import com.cgaraydev.memory.application.GameResult;
import com.cgaraydev.memory.domain.Difficulty;

import java.time.Instant;
import java.util.Objects;

/** Resultado persistible de una partida; cada sesión solo puede registrarse una vez. */
public record GameRecord(
        String sessionId,
        Difficulty difficulty,
        int moves,
        long elapsedMillis,
        Instant completedAt
) {
    public GameRecord {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("El identificador de partida es obligatorio.");
        }
        Objects.requireNonNull(difficulty, "La dificultad es obligatoria.");
        Objects.requireNonNull(completedAt, "La fecha es obligatoria.");
        if (moves < difficulty.getPairs()) {
            throw new IllegalArgumentException("Los movimientos no pueden ser menores que las parejas.");
        }
        if (elapsedMillis < 0) {
            throw new IllegalArgumentException("La duración no puede ser negativa.");
        }
    }

    public static GameRecord from(GameResult result, Instant completedAt) {
        Objects.requireNonNull(result, "El resultado es obligatorio.");
        return new GameRecord(result.sessionId(), result.difficulty(),
                result.moves(), result.elapsedMillis(), completedAt);
    }

    public int failedAttempts() {
        return moves - difficulty.getPairs();
    }
}
