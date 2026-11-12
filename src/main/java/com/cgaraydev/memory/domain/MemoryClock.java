package com.cgaraydev.memory.domain;

@FunctionalInterface
public interface MemoryClock {

    long nowMillis();
}