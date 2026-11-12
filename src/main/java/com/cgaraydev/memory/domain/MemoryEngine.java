package com.cgaraydev.memory.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

public final class MemoryEngine {

    public MemorySession start(
            String sessionId,
            Difficulty difficulty,
            List<String> availablePairIds,
            Random random
    ) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException(
                    "La partida debe tener un identificador."
            );
        }

        Objects.requireNonNull(
                difficulty,
                "La dificultad es obligatoria."
        );

        Objects.requireNonNull(
                availablePairIds,
                "El banco de parejas es obligatorio."
        );

        Objects.requireNonNull(
                random,
                "El generador aleatorio es obligatorio."
        );

        if (availablePairIds.stream().anyMatch(
                id -> id == null || id.isBlank()
        )) {
            throw new IllegalArgumentException(
                    "Las parejas deben tener identificadores válidos."
            );
        }

        if (new HashSet<>(availablePairIds).size()
                != availablePairIds.size()) {
            throw new IllegalArgumentException(
                    "El banco no puede contener parejas duplicadas."
            );
        }

        if (availablePairIds.size() < difficulty.getPairs()) {
            throw new IllegalArgumentException(
                    "No hay suficientes parejas para esta dificultad."
            );
        }

        List<String> shuffledPairIds =
                new ArrayList<>(availablePairIds);

        Collections.shuffle(shuffledPairIds, random);

        List<String> selectedPairIds = shuffledPairIds.subList(
                0,
                difficulty.getPairs()
        );

        List<MemoryCard> cards = new ArrayList<>();

        for (int index = 0; index < selectedPairIds.size(); index++) {
            String pairId = selectedPairIds.get(index);

            cards.add(new MemoryCard(index * 2, pairId));
            cards.add(new MemoryCard(index * 2 + 1, pairId));
        }

        Collections.shuffle(cards, random);

        return new MemorySession(
                sessionId,
                difficulty,
                cards
        );
    }

    public MemorySession start(
            String sessionId,
            Difficulty difficulty,
            List<String> availablePairIds
    ) {
        return start(
                sessionId,
                difficulty,
                availablePairIds,
                new Random()
        );
    }

    public MemorySession select(
            MemorySession session,
            String sessionId,
            int cardId
    ) {
        Objects.requireNonNull(session);

        if (!session.id().equals(sessionId)
                || session.phase() != MemoryPhase.SELECTING) {
            return session;
        }

        MemoryCard selectedCard = session.cards()
                .stream()
                .filter(card -> card.id() == cardId)
                .findFirst()
                .orElse(null);

        if (selectedCard == null
                || selectedCard.faceUp()
                || selectedCard.matched()) {
            return session;
        }

        boolean selectingSecondCard =
                session.revealedCards().size() == 1;

        List<MemoryCard> updatedCards = session.cards()
                .stream()
                .map(card -> card.id() == cardId
                        ? card.reveal()
                        : card)
                .toList();

        int updatedMoves = session.moves()
                + (selectingSecondCard ? 1 : 0);

        MemoryPhase updatedPhase = selectingSecondCard
                ? MemoryPhase.RESOLVING
                : MemoryPhase.SELECTING;

        return session.withState(
                updatedCards,
                updatedMoves,
                updatedPhase
        );
    }

    public MemorySession resolve(
            MemorySession session,
            String sessionId,
            int move
    ) {
        Objects.requireNonNull(session);

        if (!session.id().equals(sessionId)
                || session.phase() != MemoryPhase.RESOLVING
                || session.moves() != move) {
            return session;
        }

        List<MemoryCard> revealed = session.revealedCards();

        if (revealed.size() != 2) {
            throw new IllegalStateException(
                    "Debe haber exactamente dos cartas por resolver."
            );
        }

        boolean matched = revealed.get(0).pairId()
                .equals(revealed.get(1).pairId());

        Set<Integer> revealedIds = Set.of(
                revealed.get(0).id(),
                revealed.get(1).id()
        );

        List<MemoryCard> resolvedCards = session.cards()
                .stream()
                .map(card -> {
                    if (!revealedIds.contains(card.id())) {
                        return card;
                    }

                    return matched
                            ? card.match()
                            : card.hide();
                })
                .toList();

        boolean finished = resolvedCards.stream()
                .allMatch(MemoryCard::matched);

        return session.withState(
                resolvedCards,
                session.moves(),
                finished
                        ? MemoryPhase.FINISHED
                        : MemoryPhase.SELECTING
        );
    }
}