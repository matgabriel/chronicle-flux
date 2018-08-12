package com.mgabriel.chronicle.flux.replay;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

public class ReplayInLoop<T> implements Function<Flux<T>, Publisher<ReplayValue<T>>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReplayInLoop.class);
    private final Duration delayBeforeRestart;

    public ReplayInLoop(Duration delayBeforeRestart) {
        this.delayBeforeRestart = delayBeforeRestart;
    }

    @Override
    public Publisher<ReplayValue<T>> apply(Flux<T> source) {
        Flux<Flux<ReplayValue<T>>> generate = Flux.create(sink -> {
                    while (!sink.isCancelled()) {
                        long requested = sink.requestedFromDownstream();
                        if (requested > 0) {
                            wrapValues(source, sink);
                        } else {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                LOGGER.info("interrupted " + e);
                            }
                        }
                    }
                }
        );
        Flux<Flux<ReplayValue<T>>> limited = generate.limitRate(1);
        return Flux.concat(limited);
    }

    private void wrapValues(Flux<T> source, FluxSink<Flux<ReplayValue<T>>> sink) {
        AtomicBoolean firstValueSent = new AtomicBoolean(false);
        Flux<ReplayValue<T>> nextFlux = source.delaySequence(delayBeforeRestart).map(wrapAsReplayValue(firstValueSent)).doOnNext(v -> System.out
                .println("\t\t\taaaa "+ Instant.now()+"   " +v));
        sink.next(Flux.defer(() -> nextFlux));
    }

    @NotNull
    private Function<T, ReplayValue<T>> wrapAsReplayValue(AtomicBoolean firstValueSent) {
        return v -> {
            if (!firstValueSent.getAndSet(true)) {
                return new ReplayValueImpl<>(true, v);
            }
            return new ReplayValueImpl<>(v);
        };
    }
}
