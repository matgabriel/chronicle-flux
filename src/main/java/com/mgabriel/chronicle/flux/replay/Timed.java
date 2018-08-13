package com.mgabriel.chronicle.flux.replay;

public interface Timed<T>  extends WrappedValue<T> {
    long time();
}
