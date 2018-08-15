package com.mgabriel.chronicle.flux.demo;

import static com.mgabriel.chronicle.flux.util.ChronicleStoreCleanup.deleteStoreIfItExists;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.time.Duration;
import java.time.Instant;

import com.mgabriel.chronicle.flux.ChronicleStore;
import com.mgabriel.chronicle.flux.DummyObject;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

/**
 * Simple demo showing how to store values in a Chronicle store and replay them in a loop with the same timing as the
 * original values
 *
 * @author mgabriel.
 */
class ChronicleStoreDemo {

    private static final String PATH = "demoChronicleStore";

    public static void main(String[] args) throws InterruptedException {
        deleteStoreIfItExists(PATH);

        ChronicleStore<DummyObject> chronicleStore = new ChronicleStore<>(PATH, DummyObject::toBinary,
                DummyObject::fromBinary);

        Flux<String> source = Flux.just("one", "two", "three")
                .concatWith(Flux.just("four").delayElements(Duration.ofSeconds(1)))
                .concatWith(Flux.just("five").delayElements(Duration.ofSeconds(2)));

        System.out.println("Storing source flux...");
        Disposable handle = chronicleStore.store(source.map(v -> new DummyObject(System.currentTimeMillis(), v)));

        SECONDS.sleep(4); //wait until all items are stored
        handle.dispose();

        System.out.println("Storage achieved, replaying from chronicleStore");

        chronicleStore.replayHistory(DummyObject::timestamp)
                .withOriginalTiming()
                .inLoop(Duration.ofSeconds(1))
                .doOnNext(i -> System.out.println(Instant.now() + " " + i))
                .blockLast();
    }

}
