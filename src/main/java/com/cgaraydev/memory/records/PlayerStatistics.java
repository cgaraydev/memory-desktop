package com.cgaraydev.memory.records;

import com.cgaraydev.memory.domain.Difficulty;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Métricas basadas en TODAS las partidas conocidas, no únicamente el top 10. */
public record PlayerStatistics(
        Difficulty difficulty,
        int completedGames,
        long totalElapsedMillis,
        double averageFailedAttempts,
        Optional<GameRecord> latest,
        boolean partialLegacyHistory
) {
    public PlayerStatistics {
        Objects.requireNonNull(difficulty);
        Objects.requireNonNull(latest);
        if (completedGames < 0 || totalElapsedMillis < 0
                || !Double.isFinite(averageFailedAttempts) || averageFailedAttempts < 0) {
            throw new IllegalArgumentException("Estadísticas históricas inválidas.");
        }
        if ((completedGames == 0) != latest.isEmpty()) {
            throw new IllegalArgumentException("El último resultado no coincide con el historial.");
        }
    }

    public static PlayerStatistics from(
            Difficulty difficulty, List<GameRecord> history, boolean partialLegacyHistory
    ) {
        Objects.requireNonNull(difficulty);
        List<GameRecord> records = List.copyOf(history).stream()
                .filter(record -> record.difficulty() == difficulty).toList();
        long totalTime = 0L;
        long failed = 0L;
        for (GameRecord record : records) {
            totalTime = Math.addExact(totalTime, record.elapsedMillis());
            failed = Math.addExact(failed, record.failedAttempts());
        }
        Optional<GameRecord> latest = records.stream().max(Comparator
                .comparing(GameRecord::completedAt)
                .thenComparing(GameRecord::sessionId));
        return new PlayerStatistics(difficulty, records.size(), totalTime,
                records.isEmpty() ? 0.0 : (double) failed / records.size(),
                latest, partialLegacyHistory);
    }
}
