package com.cgaraydev.memory.application;

import com.cgaraydev.memory.domain.Difficulty;
import com.cgaraydev.memory.domain.MemoryClock;
import com.cgaraydev.memory.domain.MemoryEngine;
import com.cgaraydev.memory.domain.MemoryPendingResolution;
import com.cgaraydev.memory.domain.MemoryPhase;
import com.cgaraydev.memory.domain.MemoryResolutionToken;
import com.cgaraydev.memory.domain.MemorySession;
import com.cgaraydev.memory.domain.MemoryTimer;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

public final class MemoryGameCoordinator {

    private final MemoryEngine engine;
    private final MemoryTimer timer;
    private final MemoryPendingResolution pendingResolution;
    private final Random random;

    private MemorySession session;
    private List<String> availablePairIds = List.of();
    private boolean paused;

    public MemoryGameCoordinator(MemoryClock clock) {
        this(new MemoryEngine(), clock, new Random());
    }

    public MemoryGameCoordinator(
            MemoryEngine engine,
            MemoryClock clock,
            Random random
    ) {
        this.engine = Objects.requireNonNull(engine);
        this.random = Objects.requireNonNull(random);

        Objects.requireNonNull(clock);

        this.timer = new MemoryTimer(clock);
        this.pendingResolution =
                new MemoryPendingResolution(clock);
    }

    public MemorySession session() {
        return session;
    }

    public boolean hasSession() {
        return session != null;
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isFinished() {
        return session != null
                && session.phase() == MemoryPhase.FINISHED;
    }

    public boolean canSelectCard() {
        return session != null
                && !paused
                && session.phase() == MemoryPhase.SELECTING;
    }

    public long elapsedMillis() {
        return timer.elapsedMillis();
    }

    public long remainingResolutionMillis() {
        return pendingResolution.remainingMillis();
    }

    public void start(
            Difficulty difficulty,
            List<String> pairIds
    ) {
        if (session != null) {
            return;
        }

        beginGame(difficulty, pairIds);
    }

    public void restart() {
        if (session == null) {
            return;
        }

        beginGame(
                session.difficulty(),
                availablePairIds
        );
    }

    public void returnToIntro() {
        pendingResolution.cancel();
        timer.reset();

        session = null;
        availablePairIds = List.of();
        paused = false;
    }

    public void selectCard(
            String sessionId,
            int cardId
    ) {
        if (!canSelectCard()) {
            return;
        }

        MemorySession selected = engine.select(
                session,
                sessionId,
                cardId
        );

        if (selected == session) {
            return;
        }

        session = selected;

        if (session.phase() == MemoryPhase.RESOLVING) {
            pendingResolution.schedule(
                    session.id(),
                    session.moves()
            );
        }
    }

    public void pause() {
        if (session == null || isFinished() || paused) {
            return;
        }

        paused = true;

        timer.pause();
        pendingResolution.pause();
    }

    public void resume() {
        if (session == null || isFinished() || !paused) {
            return;
        }

        paused = false;

        timer.resume();
        pendingResolution.resume();
    }

    public void update() {
        if (session == null || paused || isFinished()) {
            return;
        }

        MemoryResolutionToken token =
                pendingResolution.takeReady();

        if (token != null) {
            session = engine.resolve(
                    session,
                    token.sessionId(),
                    token.move()
            );
        }

        if (isFinished()) {
            timer.pause();
        }
    }

    private void beginGame(
            Difficulty difficulty,
            List<String> pairIds
    ) {
        // Validamos el tablero antes de sustituir la partida.
        MemorySession newSession = engine.start(
                UUID.randomUUID().toString(),
                difficulty,
                pairIds,
                random
        );

        List<String> newBank = List.copyOf(pairIds);

        pendingResolution.cancel();
        timer.start();

        session = newSession;
        availablePairIds = newBank;
        paused = false;
    }
}