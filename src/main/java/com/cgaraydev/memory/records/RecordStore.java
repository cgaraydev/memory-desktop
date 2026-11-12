package com.cgaraydev.memory.records;

import java.io.IOException;

/** Persistencia versionada de récords e historial. */
public interface RecordStore {
    RecordArchive load() throws IOException;
    void save(RecordArchive archive) throws IOException;
}
