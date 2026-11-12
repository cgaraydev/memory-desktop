package com.cgaraydev.memory.domain;

import java.util.Objects;

public final class MemoryTimer {

    private final MemoryClock clock;

    private boolean started;
    private long accumulatedMillis;
    private Long runningSinceMillis;

    public MemoryTimer(MemoryClock clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    public boolean isRunning() {
        return runningSinceMillis != null;
    }

    public void start() {
        accumulatedMillis = 0L;
        started = true;
        runningSinceMillis = clock.nowMillis();
    }

    public void pause() {
        if (!isRunning()) {
            return;
        }

        accumulatedMillis += elapsedSince(runningSinceMillis);
        runningSinceMillis = null;
    }

    public void resume() {
        if (!started || isRunning()) {
            return;
        }

        runningSinceMillis = clock.nowMillis();
    }

    public long elapsedMillis() {
        if (!isRunning()) {
            return accumulatedMillis;
        }

        return accumulatedMillis
                + elapsedSince(runningSinceMillis);
    }

    public void reset() {
        started = false;
        accumulatedMillis = 0L;
        runningSinceMillis = null;
    }

    private long elapsedSince(long startMillis) {
        long nowMillis = clock.nowMillis();

        if (nowMillis < startMillis) {
            throw new IllegalStateException(
                    "El reloj de Memory debe ser monotónico."
            );
        }

        return nowMillis - startMillis;
    }
}