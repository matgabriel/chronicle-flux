package com.mgabriel.chronicle.flux.replay;

import static java.time.Duration.ofMillis;

import java.util.function.Function;
import java.util.function.Predicate;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

public class ReplayWithOriginalTiming<T> implements Function<Flux<T>, Publisher<T>> {
    private final Function<T, Long> timestampExtractor;
    private final Timed<T> TOKEN = new TimedValue<>(0, null);

    public ReplayWithOriginalTiming(Function<T, Long> timestampExtractor) {
        this.timestampExtractor = timestampExtractor;
    }

    @Override
    public Publisher<T> apply(Flux<T> source) {
        Flux<Timed<T>> timedFlux = source.map(v -> new TimedValue<>(timestampExtractor.apply(v), v));
        return timedFlux.scan(new TimedValuePair<>(TOKEN, TOKEN),
                (acc, val) -> new TimedValuePair<>(acc.second, val))
                .filter(filterFirstValue())
                .map(calculateDelay())
                .delayUntil(applyDelay())
                .map(ValueToDelay::value);
    }

    private Predicate<TimedValuePair<T>> filterFirstValue() {
        return tvp -> tvp.second != TOKEN;
    }

    private Function<TimedValuePair<T>, ValueToDelay<T>> calculateDelay() {
        return tvp -> {
            long timeDifference = tvp.timeDifference();
            if (timeDifference < 0 || tvp.first == TOKEN) {
                timeDifference = 0;
            }
            return new ValueToDelay<>(timeDifference, tvp.second.value());
        };
    }

    private Function<ValueToDelay<T>, Publisher<?>> applyDelay() {
        return vtd -> {
            return Flux.just(TOKEN).delayElements(ofMillis(vtd.delay()));};
    }

    private static class TimedValuePair<T> {
        private final Timed<T> first;
        private final Timed<T> second;

        private TimedValuePair(Timed<T> first, Timed<T> second) {
            if (first == null || second == null) {
                throw new IllegalArgumentException("values should not be null");
            }
            this.first = first;
            this.second = second;
        }

        long timeDifference() {
            return second.time() - first.time();
        }
    }
}
