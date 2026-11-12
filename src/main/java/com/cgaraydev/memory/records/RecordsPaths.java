package com.cgaraydev.memory.records;

import java.nio.file.Path;

public final class RecordsPaths {
    private RecordsPaths() {}

    /** Windows: %LOCALAPPDATA%\MemoryDesktop\records.json.
     * Otros sistemas: ~/.memory-desktop/records.json.
     */
    public static Path defaultFile() {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.isBlank()) {
            return Path.of(localAppData, "MemoryDesktop", "records.json");
        }
        return Path.of(System.getProperty("user.home"), ".memory-desktop", "records.json");
    }
}
