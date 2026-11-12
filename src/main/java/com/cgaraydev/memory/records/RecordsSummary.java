package com.cgaraydev.memory.records;

import com.cgaraydev.memory.domain.Difficulty;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Estadísticas calculadas únicamente a partir de las entradas conservadas en el top 10.
 * No representa el total histórico de partidas completadas.
 */
public record RecordsSummary(
        Difficulty difficulty,
        int rankedResults,
        Optional<GameRecord> best,
        Optional<GameRecord> latest,
        double averageFailedAttempts
) {
    private static final Comparator<GameRecord> BEST_FIRST = Comparator
            .comparingInt(GameRecord::moves)
            .thenComparingLong(GameRecord::elapsedMillis)
            .thenComparing(GameRecord::completedAt)
            .thenComparing(GameRecord::sessionId);

    private static final Comparator<GameRecord> MOST_RECENT_FIRST = Comparator
            .comparing(GameRecord::completedAt)
            .thenComparing(GameRecord::sessionId);

    public RecordsSummary {
        Objects.requireNonNull(difficulty, "La dificultad es obligatoria.");
        Objects.requireNonNull(best, "El mejor resultado es obligatorio.");
        Objects.requireNonNull(latest, "El resultado más reciente es obligatorio.");
        if (rankedResults < 0 || rankedResults > RecordsService.TOP_LIMIT) {
            throw new IllegalArgumentException("Número inválido de entradas en el ranking.");
        }
        if (best.isPresent() != latest.isPresent()
                || (rankedResults == 0) != best.isEmpty()) {
            throw new IllegalArgumentException("Las estadísticas no coinciden con el ranking.");
        }
        if (!Double.isFinite(averageFailedAttempts) || averageFailedAttempts < 0.0) {
            throw new IllegalArgumentException("La media de desaciertos no es válida.");
        }
    }

    public static RecordsSummary from(Difficulty difficulty, List<GameRecord> rankedRecords) {
        Objects.requireNonNull(difficulty, "La dificultad es obligatoria.");
        List<GameRecord> records = List.copyOf(rankedRecords);
        if (records.size() > RecordsService.TOP_LIMIT) {
            throw new IllegalArgumentException("El ranking no puede superar el top 10.");
        }
        if (records.stream().anyMatch(record -> record.difficulty() != difficulty)) {
            throw new IllegalArgumentException("El ranking contiene otra dificultad.");
        }
        return new RecordsSummary(
                difficulty,
                records.size(),
                records.stream().min(BEST_FIRST),
                records.stream().max(MOST_RECENT_FIRST),
                records.stream().mapToInt(GameRecord::failedAttempts).average().orElse(0.0)
        );
    }
}
