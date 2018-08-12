package com.mgabriel.chronicle.flux.replay;

import java.time.Duration;
import java.time.Instant;

import reactor.core.publisher.Flux;

public class ReplayLoopDemo {

    public static void main(String[] args) {

        Flux<Long> just = Flux.just(0L, 1L, 2L, 3L, 4L, 5L);

        Flux<ReplayValue<Long>> result = just.compose(new ReplayInLoop<>(Duration.ofSeconds(2)));

        result.doOnNext(i -> System.out.println(Instant.now() + " " + i)).blockLast();

    }

}
