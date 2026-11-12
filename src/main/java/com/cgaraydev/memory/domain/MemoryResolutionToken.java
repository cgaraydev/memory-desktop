package com.cgaraydev.memory.domain;

public record MemoryResolutionToken(
        String sessionId,
        int move
) {

    public MemoryResolutionToken {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException(
                    "La partida debe tener un identificador."
            );
        }

        if (move <= 0) {
            throw new IllegalArgumentException(
                    "La resolución debe corresponder a un movimiento."
            );
        }
    }
}