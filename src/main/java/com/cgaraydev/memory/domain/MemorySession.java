package com.cgaraydev.memory.domain;

import java.util.List;
import java.util.Objects;

public record MemorySession(
        String id,
        Difficulty difficulty,
        List<MemoryCard> cards,
        int moves,
        MemoryPhase phase
) {

    public MemorySession {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(
                    "La partida debe tener un identificador."
            );
        }

        Objects.requireNonNull(
                difficulty,
                "La dificultad es obligatoria."
        );

        Objects.requireNonNull(
                phase,
                "La fase es obligatoria."
        );

        cards = List.copyOf(
                Objects.requireNonNull(
                        cards,
                        "Las cartas son obligatorias."
                )
        );

        if (cards.size() != difficulty.getTotalCards()) {
            throw new IllegalArgumentException(
                    "El número de cartas no corresponde a la dificultad."
            );
        }

        if (moves < 0) {
            throw new IllegalArgumentException(
                    "Los movimientos no pueden ser negativos."
            );
        }
    }

    public MemorySession(
            String id,
            Difficulty difficulty,
            List<MemoryCard> cards
    ) {
        this(
                id,
                difficulty,
                cards,
                0,
                MemoryPhase.SELECTING
        );
    }

    public int totalPairs() {
        return cards.size() / 2;
    }

    public int matchedPairs() {
        return (int) cards.stream()
                .filter(MemoryCard::matched)
                .count() / 2;
    }

    public List<MemoryCard> revealedCards() {
        return cards.stream()
                .filter(card -> card.faceUp() && !card.matched())
                .toList();
    }

    public MemorySession withState(
            List<MemoryCard> updatedCards,
            int updatedMoves,
            MemoryPhase updatedPhase
    ) {
        return new MemorySession(
                id,
                difficulty,
                updatedCards,
                updatedMoves,
                updatedPhase
        );
    }
}
