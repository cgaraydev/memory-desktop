package com.cgaraydev.memory.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MemoryTimerTest {

    private static final class FakeClock implements MemoryClock {

        private long nowMillis;

        @Override
        public long nowMillis() {
            return nowMillis;
        }

        void advance(long millis) {
            nowMillis += millis;
        }

        void setTime(long millis) {
            nowMillis = millis;
        }
    }

    @Test
    void timerShouldInitiallyBeStopped() {
        MemoryTimer timer = new MemoryTimer(new FakeClock());

        assertFalse(timer.isRunning());
        assertEquals(0L, timer.elapsedMillis());
    }

    @Test
    void timerShouldMeasureElapsedTime() {
        FakeClock clock = new FakeClock();
        MemoryTimer timer = new MemoryTimer(clock);

        timer.start();
        clock.advance(1_500);

        assertTrue(timer.isRunning());
        assertEquals(1_500L, timer.elapsedMillis());
    }

    @Test
    void pausedTimeShouldNotBeCounted() {
        FakeClock clock = new FakeClock();
        MemoryTimer timer = new MemoryTimer(clock);

        timer.start();
        clock.advance(1_000);
        timer.pause();

        clock.advance(5_000);

        assertFalse(timer.isRunning());
        assertEquals(1_000L, timer.elapsedMillis());
    }

    @Test
    void resumeShouldContinueAccumulatingTime() {
        FakeClock clock = new FakeClock();
        MemoryTimer timer = new MemoryTimer(clock);

        timer.start();
        clock.advance(1_000);
        timer.pause();

        clock.advance(3_000);
        timer.resume();

        clock.advance(2_000);

        assertEquals(3_000L, timer.elapsedMillis());
    }

    @Test
    void repeatedPauseAndResumeShouldNotChangeTimeIncorrectly() {
        FakeClock clock = new FakeClock();
        MemoryTimer timer = new MemoryTimer(clock);

        timer.start();
        clock.advance(1_000);

        timer.pause();
        timer.pause();

        clock.advance(3_000);

        timer.resume();
        timer.resume();

        clock.advance(500);

        assertEquals(1_500L, timer.elapsedMillis());
    }

    @Test
    void resetShouldClearPreviousState() {
        FakeClock clock = new FakeClock();
        MemoryTimer timer = new MemoryTimer(clock);

        timer.start();
        clock.advance(2_500);

        timer.reset();
        timer.resume();

        assertFalse(timer.isRunning());
        assertEquals(0L, timer.elapsedMillis());
    }

    @Test
    void startingAgainShouldRestartElapsedTime() {
        FakeClock clock = new FakeClock();
        MemoryTimer timer = new MemoryTimer(clock);

        timer.start();
        clock.advance(3_000);

        timer.start();
        clock.advance(750);

        assertEquals(750L, timer.elapsedMillis());
    }

    @Test
    void nonMonotonicClockShouldBeRejected() {
        FakeClock clock = new FakeClock();
        MemoryTimer timer = new MemoryTimer(clock);

        clock.setTime(1_000);
        timer.start();

        clock.setTime(500);

        assertThrows(
                IllegalStateException.class,
                timer::elapsedMillis
        );
    }
}