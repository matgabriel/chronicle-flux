package com.mgabriel.chronicle.flux.replay;

import java.time.Duration;
import java.util.function.Function;

import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;

public class ReplayFlux<T> extends Flux<T> {

    private final Flux<T> source;
    private final Function<T, Long> timestampExtractor;

    public ReplayFlux(Flux<T> source, Function<T, Long> timestampExtractor) {
        this.source = source;
        this.timestampExtractor = timestampExtractor;
    }

    @Override
    public void subscribe(CoreSubscriber<? super T> actual) {

    }

    public ReplayLoopFlux<T> inLoop(){
        return inLoop(Duration.ofMillis(0));
    }

    public ReplayLoopFlux<T> inLoop(Duration delayBeforeLoopRestart){
        return new ReplayLoopFlux<>(source, timestampExtractor, delayBeforeLoopRestart);
    }
}
