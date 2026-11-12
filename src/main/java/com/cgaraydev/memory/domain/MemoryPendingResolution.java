package com.cgaraydev.memory.domain;

import java.util.Objects;

public final class MemoryPendingResolution {

    public static final long DEFAULT_EXPOSURE_MILLIS = 700L;

    private final MemoryTimer timer;
    private final long exposureMillis;

    private MemoryResolutionToken pending;

    public MemoryPendingResolution(MemoryClock clock) {
        this(clock, DEFAULT_EXPOSURE_MILLIS);
    }

    public MemoryPendingResolution(
            MemoryClock clock,
            long exposureMillis
    ) {
        if (exposureMillis <= 0L) {
            throw new IllegalArgumentException(
                    "El tiempo de exposición debe ser positivo."
            );
        }

        this.timer = new MemoryTimer(
                Objects.requireNonNull(clock)
        );

        this.exposureMillis = exposureMillis;
    }

    public boolean hasPendingResolution() {
        return pending != null;
    }

    public long remainingMillis() {
        if (pending == null) {
            return 0L;
        }

        return Math.max(
                0L,
                exposureMillis - timer.elapsedMillis()
        );
    }

    public void schedule(
            String sessionId,
            int move
    ) {
        MemoryResolutionToken token =
                new MemoryResolutionToken(sessionId, move);

        // La misma solicitud no reinicia la espera.
        if (token.equals(pending)) {
            return;
        }

        if (pending != null) {
            throw new IllegalStateException(
                    "Debe resolverse o cancelarse la pareja anterior."
            );
        }

        pending = token;
        timer.start();
    }

    public void pause() {
        timer.pause();
    }

    public void resume() {
        if (pending == null) {
            return;
        }

        timer.resume();
    }

    public MemoryResolutionToken takeReady() {
        if (pending == null) {
            return null;
        }

        if (!timer.isRunning()
                || timer.elapsedMillis() < exposureMillis) {
            return null;
        }

        MemoryResolutionToken ready = pending;

        cancel();

        return ready;
    }

    public void cancel() {
        pending = null;
        timer.reset();
    }
}