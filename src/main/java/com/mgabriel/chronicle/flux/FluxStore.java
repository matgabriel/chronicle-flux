package com.mgabriel.chronicle.flux;

import java.util.function.Function;

import com.mgabriel.chronicle.flux.replay.ReplayFlux;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

/**
 * Reactive store used to store and replay a Flux.
 *
 * @param <T> the value type
 */
public interface FluxStore<T> {

    /**
     * Stores all items of the given stream in the chronicle store.
     *
     * @param toStore data stream to store.
     */
    Disposable store(Publisher<T> toStore);

    /**
     * Stores one item in the chronicle store.
     *
     * @param item item to store.
     */
    void store(T item);

    /**
     * @return all values present in the store and new values being stored in this FluxStore.
     */
    default Flux<T> retrieveAll() {
        return retrieveAll(false);
    }

    /**
     * @param deleteAfterRead if true, the file storing the data on disk will be deleted once it has been read.
     * @return all values present in the store and new values being stored in this FluxStore.
     */
    Flux<T> retrieveAll(boolean deleteAfterRead);

    /**
     * @return all values present in the store and completes the stream.
     */
    Flux<T> retrieveHistory();

    /**
     * @return the stream of new values being stored in this FluxStore (history is ignored).
     */
    Flux<T> retrieveNewValues();

    /**
     * @param timestampExtractor a function to extract the epoch time from the values.
     * @return a Flux that can be used to replay the history with multiple strategies.
     */
    ReplayFlux<T> replayHistory(Function<T, Long> timestampExtractor);

}
