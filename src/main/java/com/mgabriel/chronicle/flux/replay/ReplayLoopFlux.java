package com.mgabriel.chronicle.flux.replay;

import java.time.Duration;
import java.util.function.Function;

import reactor.core.CoreSubscriber;
import reactor.core.Scannable;
import reactor.core.publisher.Flux;

public class ReplayLoopFlux<T> extends Flux<ReplayValue<T>> implements Scannable{

    private final Flux<T> source;
    private final Duration delayBeforeLoopRestart;

    public ReplayLoopFlux(Flux<T> source, Duration delayBeforeLoopRestart) {
        this.source = source;
        this.delayBeforeLoopRestart = delayBeforeLoopRestart;
    }

    @Override
    public void subscribe(CoreSubscriber<? super ReplayValue<T>> actual) {
        source.compose(new ReplayInLoop<>(delayBeforeLoopRestart)).subscribe(actual);
    }

    @Override
    public Object scanUnsafe(Scannable.Attr attr) {
        return getScannable().scanUnsafe(attr);
    }

    private Scannable getScannable() {
        return (Scannable) source;
    }
}
