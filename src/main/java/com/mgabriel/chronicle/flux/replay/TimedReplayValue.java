package com.mgabriel.chronicle.flux.replay;

/**
 * Wraps a {@link ReplayValue} with its timestamp.
 *
 * @param <T> data type
 */
interface TimedReplayValue<T> extends Timed<T>, ReplayValue<T>{
}
