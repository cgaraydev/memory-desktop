package com.cgaraydev.memory.records;

import com.cgaraydev.memory.domain.Difficulty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JsonRecordStoreTest {
    @TempDir Path tempDir;

    private GameRecord record() {
        return new GameRecord("session-A", Difficulty.NORMAL, 14,
                83_200, Instant.parse("2026-11-12T12:00:00Z"));
    }

    @Test void missingFileProducesEmptyArchive() throws IOException {
        assertEquals(RecordArchive.empty(),
                new JsonRecordStore(tempDir.resolve("missing.json")).load());
    }

    @Test void writesAndReadsV2AcrossStoreInstances() throws IOException {
        Path file = tempDir.resolve("folder").resolve("records.json");
        RecordArchive data = new RecordArchive(List.of(record()), List.of(record()), Set.of());
        new JsonRecordStore(file).save(data);
        assertEquals(data, new JsonRecordStore(file).load());
        String contents = Files.readString(file);
        assertTrue(contents.contains("\"version\" : 2"));
        assertTrue(contents.contains("\"history\""));
    }

    @Test void rewritesV2WithoutTemporaryFiles() throws IOException {
        Path file = tempDir.resolve("records.json");
        JsonRecordStore store = new JsonRecordStore(file);
        store.save(new RecordArchive(List.of(record()), List.of(record()), Set.of()));
        store.save(RecordArchive.empty());
        assertEquals(RecordArchive.empty(), store.load());
        try (var files = Files.list(tempDir)) {
            assertEquals(List.of(file), files.toList());
        }
    }

    @Test void migratesV1WithoutLosingTopAndCreatesOriginalBackup() throws IOException {
        Path file = tempDir.resolve("records.json");
        String oldJson = """
                {"version":1,"records":[{"sessionId":"session-A","difficulty":"NORMAL",
                "moves":14,"elapsedMillis":83200,"completedAt":"2026-11-12T12:00:00Z"}]}
                """;
        Files.writeString(file, oldJson);
        JsonRecordStore store = new JsonRecordStore(file);
        RecordArchive migrated = store.load();
        assertTrue(migrated.legacyDifficulties().contains(Difficulty.NORMAL));
        assertEquals(List.of(record()), migrated.records());
        assertEquals(List.of(record()), migrated.history());
        // La lectura por sí sola no altera el archivo original.
        assertEquals(oldJson, Files.readString(file));
        assertFalse(Files.exists(tempDir.resolve("records.json.v1.bak")));
        store.save(migrated);
        assertEquals(oldJson, Files.readString(tempDir.resolve("records.json.v1.bak")));
        assertEquals(migrated, store.load());
        assertTrue(Files.readString(file).contains("\"version\" : 2"));
    }

    @Test void refusesToOverwriteMalformedJson() throws IOException {
        Path file = tempDir.resolve("records.json");
        String invalid = "{no-es-json";
        Files.writeString(file, invalid);
        JsonRecordStore store = new JsonRecordStore(file);
        assertThrows(IOException.class, store::load);
        assertThrows(IOException.class, () -> store.save(RecordArchive.empty()));
        assertEquals(invalid, Files.readString(file));
    }

    @Test void rejectsUnsupportedSchemaVersion() throws IOException {
        Path file = tempDir.resolve("records.json");
        Files.writeString(file, "{\"version\":3,\"records\":[],\"history\":[],\"legacyDifficulties\":[]}");
        assertThrows(IOException.class, () -> new JsonRecordStore(file).load());
    }

    @Test void rejectsInvalidEntryWithoutDroppingIt() throws IOException {
        Path file = tempDir.resolve("records.json");
        String content = """
                {"version":1,"records":[{"sessionId":"s","difficulty":"EASY",
                "moves":2,"elapsedMillis":50,"completedAt":"2026-11-12T12:00:00Z"}]}
                """;
        Files.writeString(file, content);
        assertThrows(IOException.class, () -> new JsonRecordStore(file).load());
        assertEquals(content, Files.readString(file));
    }

    @Test void refusesArchiveWithInconsistentTop() throws IOException {
        Path file = tempDir.resolve("records.json");
        String invalid = """
                {"version":2,"legacyDifficulties":[],
                "records":[{"sessionId":"one","difficulty":"EASY","moves":6,
                  "elapsedMillis":100,"completedAt":"2026-11-12T12:00:00Z"}],
                "history":[]}
                """;
        Files.writeString(file, invalid);
        assertThrows(IOException.class, () -> new JsonRecordStore(file).load());
    }
}
