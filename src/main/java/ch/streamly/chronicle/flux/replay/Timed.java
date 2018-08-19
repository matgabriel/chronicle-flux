package ch.streamly.chronicle.flux.replay;

/**
 * Wraps a value with its timestamp.
 *
 * @param <T> data type
 */
interface Timed<T>  extends WrappedValue<T> {
    long time();
}
