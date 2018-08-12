package com.mgabriel.chronicle.flux.replay;

import com.mgabriel.chronicle.flux.WrappedValue;

public interface ReplayValue<T> extends WrappedValue<T> {

    /**
     * @return true if this object is the loop reset signal (meaning that the replay loop has restarted from the beginning)
     */
    boolean isLoopReset();
}
