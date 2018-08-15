package com.mgabriel.chronicle.flux.demo;

import static java.time.Duration.ofSeconds;

import java.time.Instant;

import com.mgabriel.chronicle.flux.replay.ReplayFlux;
import reactor.core.publisher.Flux;

class ReplayFluxDemo {

    public static void main(String[] args) {

        Flux<Long> source = Flux.just(0L, 1000L, 2000L, 3000L, 4000L, 7000L);
        ReplayFlux<Long> replayFlux = new ReplayFlux<>(source, v -> v);
        replayFlux.withTimeAcceleration(2)
                .inLoop(ofSeconds(1))
                .doOnNext(i -> System.out.println(Instant.now() + " " + i))
                .blockLast();

    }
}
