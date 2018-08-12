package com.mgabriel.chronicle.flux.replay;

import java.time.Duration;
import java.util.function.Function;

import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;

public class ReplayLoopFlux<T> extends Flux<ReplayValue<T>> {

    private final Flux<T> source;
    private final Function<T, Long> timestampExtractor;
    private final Duration delayBeforeLoopRestart;

    public ReplayLoopFlux(Flux<T> source, Function<T, Long> timestampExtractor, Duration delayBeforeLoopRestart) {
        this.source = source;
        this.timestampExtractor = timestampExtractor;
        this.delayBeforeLoopRestart = delayBeforeLoopRestart;
    }

    @Override
    public void subscribe(CoreSubscriber<? super ReplayValue<T>> actual) {

    }
}
