package com.mgabriel.chronicle.flux.replay;

import java.time.Duration;
import java.time.Instant;

import reactor.core.publisher.Flux;

public class ReplayWithOriginalTimingDemo {

    public static void main(String[] args) {

        Flux<Long> just = Flux.just(0L, 1000L, 2000L, 3000L, 4000L, 7000L);

//        Flux<ReplayValue<Long>> result = just.compose(new ReplayWithOriginalTiming<>(l -> l)).compose(new ReplayInLoop<>(Duration.ofSeconds(1)));
        Flux<ReplayValue<Long>> result = just.compose(new ReplayInLoop<>(Duration.ofSeconds(1))).compose(new ReplayWithOriginalTiming<>(l -> l.value()));

        result.doOnNext(i -> System.out.println(Instant.now() + " " + i)).blockLast();

    }

}
