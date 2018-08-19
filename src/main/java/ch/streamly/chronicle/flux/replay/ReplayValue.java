package ch.streamly.chronicle.flux.replay;

/**
 * A value wrapper that indicates if the current value is the first value replayed in the replay loop.
 * @see ReplayInLoop
 * @param <T> data type
 */
public interface ReplayValue<T> extends WrappedValue<T> {

    /**
     * @return true if this object is the loop restart signal (meaning that the replay loop has restarted from the beginning)
     */
    boolean isLoopRestart();
}
