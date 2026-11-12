package com.cgaraydev.memory.records;

import com.cgaraydev.memory.domain.Difficulty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;

/** V1 (solo top) -> V2 (top e historial), con copia y escritura atómica. */
public final class JsonRecordStore implements RecordStore {
    private static final int FORMAT_VERSION = 2;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Path file;

    public JsonRecordStore(Path file) {
        this.file = Objects.requireNonNull(file).toAbsolutePath();
    }

    @Override
    public RecordArchive load() throws IOException {
        if (Files.notExists(file)) {
            return RecordArchive.empty();
        }
        JsonNode root = readRoot();
        int version = requireVersion(root);
        try {
            if (version == 1) {
                return RecordArchive.fromLegacy(readRecords(root, "records"));
            }
            if (version == FORMAT_VERSION) {
                JsonNode imported = root.get("legacyDifficulties");
                if (imported == null || !imported.isArray()) {
                    throw new IllegalArgumentException("Faltan dificultades de migración.");
                }
                Set<Difficulty> oldDifficulties = new HashSet<>();
                for (JsonNode entry : imported) {
                    if (!entry.isTextual()
                            || !oldDifficulties.add(Difficulty.valueOf(entry.textValue()))) {
                        throw new IllegalArgumentException("Dificultad migrada inválida o duplicada.");
                    }
                }
                return new RecordArchive(readRecords(root, "records"),
                        readRecords(root, "history"), oldDifficulties);
            }
            throw new IOException("Versión de records.json no admitida: " + version);
        } catch (IllegalArgumentException | DateTimeParseException ex) {
            throw new IOException("Datos inválidos en records.json. No se sobrescribió el archivo.", ex);
        }
    }

    @Override
    public void save(RecordArchive archive) throws IOException {
        Objects.requireNonNull(archive);
        Path parent = file.getParent();
        Files.createDirectories(parent);

        // No reemplazar un archivo con esquema desconocido ni JSON dañado.
        // La copia v1 se crea antes de escribir v2, una sola vez.
        if (Files.exists(file)) {
            load(); // también valida la integridad antes de sobrescribir
            int previousVersion = requireVersion(readRoot());
            if (previousVersion == 1) {
                Path backup = file.resolveSibling(file.getFileName() + ".v1.bak");
                if (Files.notExists(backup)) {
                    Files.copy(file, backup);
                }
            }
        }

        ObjectNode root = mapper.createObjectNode();
        root.put("version", FORMAT_VERSION);
        ArrayNode imported = root.putArray("legacyDifficulties");
        for (Difficulty difficulty : Difficulty.values()) {
            if (archive.legacyDifficulties().contains(difficulty)) {
                imported.add(difficulty.name());
            }
        }
        writeRecords(root.putArray("records"), archive.records());
        writeRecords(root.putArray("history"), archive.history());

        Path temp = Files.createTempFile(parent, "records-", ".tmp");
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(temp.toFile(), root);
            try {
                Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private JsonNode readRoot() throws IOException {
        JsonNode root;
        try (var input = Files.newInputStream(file)) {
            root = mapper.readTree(input);
        }
        if (root == null || !root.isObject()) {
            throw new IOException("Raíz de records.json inválida.");
        }
        return root;
    }

    private static int requireVersion(JsonNode root) throws IOException {
        JsonNode version = root.get("version");
        if (version == null || !version.isInt()) {
            throw new IOException("Falta versión válida de records.json.");
        }
        return version.intValue();
    }

    private static List<GameRecord> readRecords(JsonNode root, String field) {
        JsonNode entries = root.get(field);
        if (entries == null || !entries.isArray()) {
            throw new IllegalArgumentException("Falta la lista de " + field);
        }
        List<GameRecord> records = new ArrayList<>();
        for (JsonNode node : entries) {
            if (!node.isObject()) {
                throw new IllegalArgumentException("Entrada no válida en " + field);
            }
            records.add(new GameRecord(
                    requiredText(node, "sessionId"),
                    Difficulty.valueOf(requiredText(node, "difficulty")),
                    requiredInt(node, "moves"),
                    requiredLong(node, "elapsedMillis"),
                    Instant.parse(requiredText(node, "completedAt"))
            ));
        }
        return List.copyOf(records);
    }

    private static void writeRecords(ArrayNode entries, List<GameRecord> records) {
        for (GameRecord record : records) {
            ObjectNode item = entries.addObject();
            item.put("sessionId", record.sessionId());
            item.put("difficulty", record.difficulty().name());
            item.put("moves", record.moves());
            item.put("elapsedMillis", record.elapsedMillis());
            item.put("completedAt", record.completedAt().toString());
        }
    }

    private static String requiredText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.textValue().isBlank()) {
            throw new IllegalArgumentException("Falta campo: " + field);
        }
        return value.textValue();
    }

    private static int requiredInt(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToInt()) {
            throw new IllegalArgumentException("Número inválido: " + field);
        }
        return value.intValue();
    }

    private static long requiredLong(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong()) {
            throw new IllegalArgumentException("Número inválido: " + field);
        }
        return value.longValue();
    }
}
