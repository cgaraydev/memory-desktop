package com.cgaraydev.memory.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class MemoryPendingResolutionTest {

    private static final class FakeClock implements MemoryClock {

        private long time;

        @Override
        public long nowMillis() {
            return time;
        }

        void advance(long millis) {
            time += millis;
        }
    }

    @Test
    void resolutionShouldBeReadyAfterExposure() {
        FakeClock clock = new FakeClock();
        MemoryPendingResolution pending =
                new MemoryPendingResolution(clock, 700);

        pending.schedule("game", 1);

        clock.advance(699);

        assertEquals(1L, pending.remainingMillis());
        assertNull(pending.takeReady());

        clock.advance(1);

        assertEquals(
                new MemoryResolutionToken("game", 1),
                pending.takeReady()
        );

        assertNull(pending.takeReady());
        assertEquals(0L, pending.remainingMillis());
        assertFalse(pending.hasPendingResolution());
    }

    @Test
    void pauseShouldPreserveRemainingTime() {
        FakeClock clock = new FakeClock();
        MemoryPendingResolution pending =
                new MemoryPendingResolution(clock, 700);

        pending.schedule("game", 1);

        clock.advance(250);
        pending.pause();

        clock.advance(10_000);

        assertEquals(450L, pending.remainingMillis());
        assertNull(pending.takeReady());

        pending.resume();
        clock.advance(449);

        assertNull(pending.takeReady());

        clock.advance(1);

        assertEquals(
                new MemoryResolutionToken("game", 1),
                pending.takeReady()
        );
    }

    @Test
    void resolutionShouldNotRunWhilePaused() {
        FakeClock clock = new FakeClock();
        MemoryPendingResolution pending =
                new MemoryPendingResolution(clock, 700);

        pending.schedule("game", 1);

        clock.advance(700);
        pending.pause();

        assertEquals(0L, pending.remainingMillis());
        assertNull(pending.takeReady());

        pending.resume();

        assertEquals(
                new MemoryResolutionToken("game", 1),
                pending.takeReady()
        );
    }

    @Test
    void schedulingSameMoveShouldNotRestartTimer() {
        FakeClock clock = new FakeClock();
        MemoryPendingResolution pending =
                new MemoryPendingResolution(clock, 700);

        pending.schedule("game", 1);
        clock.advance(400);

        pending.schedule("game", 1);

        assertEquals(300L, pending.remainingMillis());

        clock.advance(300);

        assertEquals(
                new MemoryResolutionToken("game", 1),
                pending.takeReady()
        );
    }

    @Test
    void cancellingShouldDiscardOldResolution() {
        FakeClock clock = new FakeClock();
        MemoryPendingResolution pending =
                new MemoryPendingResolution(clock, 700);

        pending.schedule("old-game", 1);

        clock.advance(300);
        pending.cancel();

        clock.advance(5_000);
        pending.resume();

        assertNull(pending.takeReady());

        pending.schedule("new-game", 1);
        clock.advance(700);

        assertEquals(
                new MemoryResolutionToken("new-game", 1),
                pending.takeReady()
        );
    }

    @Test
    void cannotOverwriteUnresolvedMove() {
        FakeClock clock = new FakeClock();
        MemoryPendingResolution pending =
                new MemoryPendingResolution(clock);

        pending.schedule("game", 1);

        assertThrows(
                IllegalStateException.class,
                () -> pending.schedule("game", 2)
        );
    }

    @Test
    void invalidExposureShouldBeRejected() {
        FakeClock clock = new FakeClock();

        assertThrows(
                IllegalArgumentException.class,
                () -> new MemoryPendingResolution(clock, 0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new MemoryPendingResolution(clock, -1)
        );
    }

    @Test
    void invalidResolutionTokenShouldBeRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new MemoryResolutionToken("", 1)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new MemoryResolutionToken("game", 0)
        );
    }

    @Test
    void staleTokenShouldNotResolveDifferentSession() {
        FakeClock clock = new FakeClock();
        MemoryPendingResolution pending =
                new MemoryPendingResolution(clock, 700);

        pending.schedule("old-game", 1);
        clock.advance(700);

        MemoryResolutionToken oldToken = pending.takeReady();

        MemoryEngine engine = new MemoryEngine();

        List<String> bank = IntStream.range(0, 20)
                .mapToObj(i -> "character-" + i)
                .toList();

        MemorySession session = engine.start(
                "new-game",
                Difficulty.EASY,
                bank,
                new Random(42)
        );

        session = engine.select(
                session,
                session.id(),
                session.cards().get(0).id()
        );

        session = engine.select(
                session,
                session.id(),
                session.cards().get(1).id()
        );

        assertEquals(
                MemoryPhase.RESOLVING,
                session.phase()
        );

        MemorySession result = engine.resolve(
                session,
                oldToken.sessionId(),
                oldToken.move()
        );

        assertSame(session, result);
    }
}