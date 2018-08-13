package com.mgabriel.chronicle.flux;

import java.time.Duration;
import java.time.Instant;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;

/**
 * @author mgabriel.
 */
public class ChronicleStoreDemo {

    public static void main(String[] args) throws InterruptedException {
        ChronicleStore<DummyObject> store = new ChronicleStore<>("demoChronicleStore", v -> v.toBinary(), DummyObject::fromBinary);

        Flux<String> source = Flux.just("one", "two", "three")
                .concatWith(Flux.just("four").delayElements(Duration.ofSeconds(1)))
                .concatWith(Flux.just("five").delayElements(Duration.ofSeconds(2)));

        //source.doOnNext(i -> System.out.println(Instant.now() + " " + i)).blockLast();

       // Disposable handle = store.store(source.map(v -> new DummyObject(System.currentTimeMillis(), v)));

        Thread.sleep(4000);

        store.replayHistory(DummyObject::getTimestamp)
                .withOriginalTiming()
                .inLoop(Duration.ofSeconds(1))

                .doOnNext(i -> System.out.println(Instant.now() + " " + i)).blockLast();
    }
}
