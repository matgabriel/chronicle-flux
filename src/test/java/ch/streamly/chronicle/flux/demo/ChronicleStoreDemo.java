package ch.streamly.chronicle.flux.demo;

import static ch.streamly.chronicle.flux.util.ChronicleStoreCleanup.deleteStoreIfItExists;
import static java.time.Duration.ofSeconds;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.time.Instant;

import ch.streamly.chronicle.flux.ChronicleStore;
import ch.streamly.chronicle.flux.DummyObject;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

/**
 * Simple demo showing how to store values in a Chronicle store and replay them in a loop with the timing contained in the values
 *
 * @author mgabriel.
 */
class ChronicleStoreDemo {

    private static final String PATH = "demoChronicleStore";

    public static void main(String[] args) throws InterruptedException {
        deleteStoreIfItExists(PATH);

        ChronicleStore<DummyObject> chronicleStore = new ChronicleStore<>(PATH, DummyObject::toBinary,
                DummyObject::fromBinary);

        DummyObject one = new DummyObject(1000, "one");
        DummyObject two = new DummyObject(2000, "two");
        DummyObject three = new DummyObject(3000, "three");
        DummyObject four = new DummyObject(6000, "four");
        DummyObject five = new DummyObject(9000, "five");

        Flux<DummyObject> source = Flux.just(one, two, three, four, five);

        System.out.println("Storing source flux...");
        Disposable handle = chronicleStore.store(source);

        SECONDS.sleep(4); //wait until all items are stored
        handle.dispose();

        System.out.println("Storage achieved, replaying from chronicleStore");

        chronicleStore.replayHistory(DummyObject::timestamp)
                .withOriginalTiming()
                .inLoop(ofSeconds(1))
                .doOnNext(i -> System.out.println(Instant.now() + " " + i))
                .blockLast();
    }

}
