package com.cgaraydev.memory.application;

import com.cgaraydev.memory.domain.MemoryClock;

import java.util.concurrent.TimeUnit;

public final class SystemMemoryClock implements MemoryClock {

    @Override
    public long nowMillis() {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
    }
}