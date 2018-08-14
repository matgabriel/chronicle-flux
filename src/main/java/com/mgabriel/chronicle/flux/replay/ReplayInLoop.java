package com.mgabriel.chronicle.flux.replay;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

/**
 * A transformer that takes a source flux and replays it in a loop.
 * The values are wrapped in a {@link ReplayValue} object to indicate when the loop restarts.
 * This information can be used by the application to perform some action when the loop restarts (clear caches, etc.)
 *
 * It is possible to specify a delay before each loop restart.
 * Please note that if you chain
 *
 * @param <T> data type
 */
public class ReplayInLoop<T> implements Function<Flux<T>, Publisher<ReplayValue<T>>> {
    private final Duration delayBeforeRestart;

    public ReplayInLoop(Duration delayBeforeRestart) {
        this.delayBeforeRestart = delayBeforeRestart;
    }

    @Override
    public Publisher<ReplayValue<T>> apply(Flux<T> source) {
        Flux<Flux<ReplayValue<T>>> fluxLoop = Flux.create(sink -> {
                    while (!sink.isCancelled()) {
                        long requested = sink.requestedFromDownstream();
                        if (requested > 0) {
                            sink.next(wrapValues(source));
                        } else {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                //interrupt can happen when the flux is cancelled
                                Thread.currentThread().interrupt();
                            }
                        }
                    }
                }
        );
        return Flux.concat(fluxLoop.limitRate(1)); // limit the rate to avoid creating too many source flux in advance.
    }

    private Flux<ReplayValue<T>> wrapValues(Flux<T> source) {
        AtomicBoolean firstValueSent = new AtomicBoolean(false);
        return source.delaySubscription(delayBeforeRestart)
                .map(wrapAsReplayValue(firstValueSent));

    }

    @NotNull
    private Function<T, ReplayValue<T>> wrapAsReplayValue(AtomicBoolean firstValueSent) {
        return val -> {
            if (!firstValueSent.getAndSet(true)) {
                return new ReplayValueImpl<>(true, val);
            }
            return new ReplayValueImpl<>(val);
        };
    }
}
