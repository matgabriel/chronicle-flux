package ch.streamly.chronicle.flux.demo;

import static ch.streamly.chronicle.flux.util.ChronicleStoreCleanup.deleteStoreIfItExists;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.time.Duration;
import java.time.Instant;

import ch.streamly.chronicle.flux.ChronicleJournal;
import ch.streamly.chronicle.flux.DummyObject;
import ch.streamly.chronicle.flux.FluxJournal;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

/**
 * Simple demo showing how to store values in a Chronicle journal and replay them with the same timing as they were received by the journal.
 *
 * @author mgabriel.
 */
class ChronicleJournalDemo {

    private static final String PATH = "demoChronicleJournal";

    public static void main(String[] args) throws InterruptedException {
        deleteStoreIfItExists(PATH);

        FluxJournal<DummyObject> chronicleJournal = new ChronicleJournal<>(PATH, DummyObject::toBinary,
                DummyObject::fromBinary);

        Flux<String> source = Flux.just("one", "two", "three")
                .concatWith(Flux.just("four").delayElements(Duration.ofSeconds(1)))
                .concatWith(Flux.just("five").delayElements(Duration.ofSeconds(2)));

        System.out.println("Storing source flux...");
        // we give a dummy timestamp, the timestamp used to replay the value will be the one assigned by the journal.
        Disposable handle = chronicleJournal.store(source.map(v -> new DummyObject(1, v)));

        SECONDS.sleep(4); //wait until all items are stored
        handle.dispose();

        System.out.println("Storage achieved, replaying from chronicleJournal");

        chronicleJournal.replayHistory()
                .withOriginalTiming()
                .inLoop(Duration.ofSeconds(1))
                .doOnNext(i -> System.out.println(Instant.now() + " " + i))
                .blockLast();
    }

}
