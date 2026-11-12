package com.cgaraydev.memory.records;

import com.cgaraydev.memory.application.GameResult;
import com.cgaraydev.memory.domain.Difficulty;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.HashSet;
import java.util.Set;

/** Ranking de diez por dificultad y registro de todas las partidas completadas. */
public final class RecordsService {
    public static final int TOP_LIMIT = 10;

    private static final Comparator<GameRecord> ORDER = Comparator
            .comparing(GameRecord::difficulty)
            .thenComparingInt(GameRecord::moves)
            .thenComparingLong(GameRecord::elapsedMillis)
            .thenComparing(GameRecord::completedAt)
            .thenComparing(GameRecord::sessionId);

    private final RecordStore store;
    private final Clock clock;
    private RecordArchive archive; // null hasta la primera carga correcta

    public RecordsService(RecordStore store, Clock clock) {
        this.store = Objects.requireNonNull(store);
        this.clock = Objects.requireNonNull(clock);
    }

    public synchronized List<GameRecord> top(Difficulty difficulty) throws IOException {
        Objects.requireNonNull(difficulty);
        ensureLoaded();
        return archive.records().stream()
                .filter(record -> record.difficulty() == difficulty).toList();
    }

    /** Historial conocido, ordenado de más reciente a más antiguo. */
    public synchronized List<GameRecord> history(Difficulty difficulty) throws IOException {
        Objects.requireNonNull(difficulty);
        ensureLoaded();
        return archive.history().stream()
                .filter(record -> record.difficulty() == difficulty)
                .sorted(Comparator.comparing(GameRecord::completedAt)
                        .thenComparing(GameRecord::sessionId).reversed())
                .toList();
    }

    public synchronized PlayerStatistics statistics(Difficulty difficulty) throws IOException {
        Objects.requireNonNull(difficulty);
        ensureLoaded();
        return PlayerStatistics.from(difficulty, archive.history(),
                archive.legacyDifficulties().contains(difficulty));
    }

    /**
     * Registra siempre una partida completa, aunque no entre al top 10.
     * Devuelve su posición 1..10, o vacío si quedó fuera del ranking.
     */
    public synchronized OptionalInt submit(GameResult result) throws IOException {
        Objects.requireNonNull(result);
        ensureLoaded();
        GameRecord previous = archive.history().stream()
                .filter(record -> record.sessionId().equals(result.sessionId()))
                .findFirst().orElse(null);
        if (previous != null) {
            if (previous.difficulty() != result.difficulty()
                    || previous.moves() != result.moves()
                    || previous.elapsedMillis() != result.elapsedMillis()) {
                throw new IllegalArgumentException(
                        "Una sesión existente no puede cambiar su resultado.");
            }
            return rank(previous, archive.records());
        }
        GameRecord candidate = GameRecord.from(result, Instant.now(clock));
        List<GameRecord> ranking = new ArrayList<>(archive.records());
        ranking.add(candidate);
        List<GameRecord> history = new ArrayList<>(archive.history());
        history.add(candidate);
        RecordArchive next = new RecordArchive(compact(ranking), history,
                archive.legacyDifficulties());
        OptionalInt position = rank(candidate, next.records());
        store.save(next); // si falla, el estado en memoria sigue intacto
        archive = next;
        return position;
    }

    /** Borra exclusivamente las posiciones del ranking; preserva el historial. */
    public synchronized void clear(Difficulty difficulty) throws IOException {
        Objects.requireNonNull(difficulty);
        ensureLoaded();
        List<GameRecord> nextRanking = archive.records().stream()
                .filter(record -> record.difficulty() != difficulty).toList();
        if (nextRanking.size() != archive.records().size()) {
            persist(new RecordArchive(nextRanking, archive.history(), archive.legacyDifficulties()));
        }
    }

    /** Elimina todas las marcas Y partidas conocidas de la dificultad seleccionada. */
    public synchronized void clearAll(Difficulty difficulty) throws IOException {
        Objects.requireNonNull(difficulty);
        ensureLoaded();
        List<GameRecord> nextRanking = archive.records().stream()
                .filter(record -> record.difficulty() != difficulty).toList();
        List<GameRecord> nextHistory = archive.history().stream()
                .filter(record -> record.difficulty() != difficulty).toList();
        if (nextHistory.size() == archive.history().size()) {
            return;
        }
        Set<Difficulty> oldDifficulties = new HashSet<>(archive.legacyDifficulties());
        oldDifficulties.remove(difficulty);
        persist(new RecordArchive(nextRanking, nextHistory, oldDifficulties));
    }

    private void persist(RecordArchive next) throws IOException {
        store.save(next);
        archive = next;
    }

    private void ensureLoaded() throws IOException {
        if (archive == null) {
            RecordArchive loaded = store.load();
            archive = new RecordArchive(compact(loaded.records()),
                    loaded.history(), loaded.legacyDifficulties());
        }
    }

    private static List<GameRecord> compact(List<GameRecord> all) {
        List<GameRecord> ordered = new ArrayList<>(all);
        ordered.sort(ORDER);
        List<GameRecord> ranking = new ArrayList<>();
        for (Difficulty difficulty : Difficulty.values()) {
            ordered.stream().filter(record -> record.difficulty() == difficulty)
                    .limit(TOP_LIMIT).forEach(ranking::add);
        }
        return List.copyOf(ranking);
    }

    private static OptionalInt rank(GameRecord candidate, List<GameRecord> ranking) {
        int position = 0;
        for (GameRecord record : ranking) {
            if (record.difficulty() == candidate.difficulty()) {
                position++;
                if (record.equals(candidate)) {
                    return OptionalInt.of(position);
                }
            }
        }
        return OptionalInt.empty();
    }
}
