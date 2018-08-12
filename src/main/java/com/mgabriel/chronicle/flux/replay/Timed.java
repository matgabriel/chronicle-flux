package com.mgabriel.chronicle.flux.replay;

import com.mgabriel.chronicle.flux.WrappedValue;

public interface Timed<T>  extends WrappedValue<T> {
    long time();
}
