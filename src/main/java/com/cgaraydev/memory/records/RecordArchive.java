package com.cgaraydev.memory.records;

import com.cgaraydev.memory.domain.Difficulty;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Estado persistible: ranking independiente del historial completo conocido.
 * Las entradas importadas desde v1 son solo las que se conservaron en el top 10.
 */
public record RecordArchive(
        List<GameRecord> records,
        List<GameRecord> history,
        Set<Difficulty> legacyDifficulties
) {
    public RecordArchive {
        records = List.copyOf(Objects.requireNonNull(records, "Los récords son obligatorios."));
        history = List.copyOf(Objects.requireNonNull(history, "El historial es obligatorio."));
        legacyDifficulties = Set.copyOf(Objects.requireNonNull(legacyDifficulties));

        Map<String, GameRecord> known = new HashMap<>();
        for (GameRecord record : history) {
            if (known.putIfAbsent(record.sessionId(), record) != null) {
                throw new IllegalArgumentException("El historial contiene sesiones duplicadas.");
            }
        }
        Map<Difficulty, Integer> count = new EnumMap<>(Difficulty.class);
        Map<String, GameRecord> ranked = new HashMap<>();
        for (GameRecord record : records) {
            if (ranked.putIfAbsent(record.sessionId(), record) != null) {
                throw new IllegalArgumentException("El top contiene sesiones duplicadas.");
            }
            if (!record.equals(known.get(record.sessionId()))) {
                throw new IllegalArgumentException("Un récord no coincide con el historial.");
            }
            int size = count.merge(record.difficulty(), 1, Integer::sum);
            if (size > RecordsService.TOP_LIMIT) {
                throw new IllegalArgumentException("Más de diez récords en una dificultad.");
            }
        }
    }

    public static RecordArchive empty() {
        return new RecordArchive(List.of(), List.of(), Set.of());
    }

    public static RecordArchive fromLegacy(List<GameRecord> legacyRecords) {
        return new RecordArchive(legacyRecords, legacyRecords,
                legacyRecords.stream().map(GameRecord::difficulty).collect(Collectors.toSet()));
    }
}
