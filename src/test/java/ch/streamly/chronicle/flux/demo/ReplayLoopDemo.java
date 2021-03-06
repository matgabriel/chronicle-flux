package ch.streamly.chronicle.flux.demo;

import static java.time.Duration.ofSeconds;

import java.time.Instant;

import ch.streamly.chronicle.flux.replay.ReplayInLoop;
import ch.streamly.domain.ReplayValue;
import reactor.core.publisher.Flux;

class ReplayLoopDemo {

    public static void main(String[] args) {
        Flux<Long> source = Flux.just(0L, 1L, 2L, 3L, 4L, 5L);
        Flux<ReplayValue<Long>> result = source.transform(new ReplayInLoop<>(ofSeconds(2)));
        result.doOnNext(i -> System.out.println(Instant.now() + " " + i))
                .blockLast();

    }

}
