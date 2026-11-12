package com.cgaraydev.memory.domain;

public record MemoryCard(
        int id,
        String pairId,
        boolean faceUp,
        boolean matched
) {

    public MemoryCard {
        if (id < 0) {
            throw new IllegalArgumentException(
                    "El ID de la carta no puede ser negativo."
            );
        }

        if (pairId == null || pairId.isBlank()) {
            throw new IllegalArgumentException(
                    "La carta debe tener un identificador de pareja."
            );
        }

        if (matched && !faceUp) {
            throw new IllegalArgumentException(
                    "Una carta emparejada debe permanecer visible."
            );
        }
    }

    public MemoryCard(int id, String pairId) {
        this(id, pairId, false, false);
    }

    public MemoryCard reveal() {
        if (faceUp || matched) {
            return this;
        }

        return new MemoryCard(id, pairId, true, false);
    }

    public MemoryCard hide() {
        if (!faceUp || matched) {
            return this;
        }

        return new MemoryCard(id, pairId, false, false);
    }

    public MemoryCard match() {
        if (matched) {
            return this;
        }

        return new MemoryCard(id, pairId, true, true);
    }
}
