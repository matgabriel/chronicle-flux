package com.mgabriel.chronicle.flux.replay;

import java.util.Objects;

public class ReplayValueImpl<T> implements ReplayValue<T>{
    private final boolean isLoopReset;
    private final T value;

    public ReplayValueImpl(T value) {
        this.isLoopReset = false;
        this.value = value;
    }

    public ReplayValueImpl(boolean isLoopReset, T value) {
        this.isLoopReset = isLoopReset;
        this.value = value;
    }

    @Override
    public boolean isLoopRestart() {
        return isLoopReset;
    }

    @Override
    public T value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ReplayValueImpl<?> that = (ReplayValueImpl<?>) o;
        return isLoopReset == that.isLoopReset &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isLoopReset, value);
    }

    @Override
    public String toString() {
        return "ReplayValueImpl{" +
                "isLoopRestart=" + isLoopReset +
                ", value=" + value +
                '}';
    }
}
