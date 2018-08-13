package com.mgabriel.chronicle.flux.replay;

import java.time.Duration;
import java.util.function.Function;

import reactor.core.CoreSubscriber;
import reactor.core.Scannable;
import reactor.core.publisher.Flux;

public class ReplayFlux<T> extends Flux<T> implements Scannable {
    private final Flux<T> source;
    private final Function<T, Long> timestampExtractor;

    public ReplayFlux(Flux<T> source, Function<T, Long> timestampExtractor) {
        this.source = source;
        this.timestampExtractor = timestampExtractor;
    }

    @Override
    public void subscribe(CoreSubscriber<? super T> actual) {
        source.subscribe(actual);
    }

    @Override
    public Object scanUnsafe(Attr attr) {
        return getScannable().scanUnsafe(attr);
    }

    private Scannable getScannable() {
        return (Scannable) source;
    }

    public ReplayFlux<T> withOriginalTiming(){
        return new ReplayFlux<>(source.compose(new ReplayWithOriginalTiming<>(timestampExtractor)), timestampExtractor);
    }

    public ReplayFlux<T> withTimeAcceleration(double acceleration){
        return new ReplayFlux<>(source.compose(new ReplayWithOriginalTiming<>(timestampExtractor, acceleration)), timestampExtractor);
    }

    public Flux<ReplayValue<T>> inLoop(){
        return inLoop(Duration.ofMillis(0));
    }

    public Flux<ReplayValue<T>> inLoop(Duration delayBeforeLoopRestart){
        return source.compose(new ReplayInLoop<>(delayBeforeLoopRestart));
    }
}
