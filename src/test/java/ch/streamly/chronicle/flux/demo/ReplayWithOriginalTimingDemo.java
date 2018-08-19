package ch.streamly.chronicle.flux.demo;

import java.time.Instant;

import ch.streamly.chronicle.flux.replay.ReplayWithOriginalTiming;
import reactor.core.publisher.Flux;

class ReplayWithOriginalTimingDemo {

    public static void main(String[] args) {
        Flux<Long> source = Flux.just(0L, 1000L, 2000L, 3000L, 4000L, 7000L);
        Flux<Long> result = source.transform(new ReplayWithOriginalTiming<>(l -> l));
        result.doOnNext(i -> System.out.println(Instant.now() + " " + i)).blockLast();
    }

}
