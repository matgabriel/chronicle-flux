package com.mgabriel.chronicle.flux.replay;

import com.mgabriel.chronicle.flux.WrappedValue;

class ValueToDelay<T> implements WrappedValue<T> {
    private final long delay;
    private final T value;

    ValueToDelay(long delay, T value) {
        this.delay = delay;
        this.value = value;
    }

    @Override
    public T value() {
        return value;
    }

    public long delay() {
        return delay;
    }
}
