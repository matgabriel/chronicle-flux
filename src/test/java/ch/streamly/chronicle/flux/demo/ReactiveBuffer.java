package ch.streamly.chronicle.flux.demo;

import static ch.streamly.chronicle.flux.util.ChronicleStoreCleanup.deleteStoreIfItExists;

import java.time.Duration;

import ch.streamly.chronicle.flux.ChronicleStore;
import ch.streamly.chronicle.flux.DummyObject;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

/**
 * This is a demo usage of a Chronicle store as an off-heap buffer for a reactive stream.
 * The persisted files will be automatically deleted after the rollover (daily by default).
 */
public class ReactiveBuffer {

    private static final String PATH = "demoReactiveBuffer";

    public static void main(String[] args) {
        deleteStoreIfItExists(PATH);

        Flux<DummyObject> source = Flux.interval(Duration.ofSeconds(1)).map(i -> new DummyObject(i, String.valueOf(i)));

        ChronicleStore<DummyObject> chronicleStore = new ChronicleStore<>(PATH, DummyObject::toBinary,
                DummyObject::fromBinary);
        Disposable storage = chronicleStore.store(source);

        chronicleStore.retrieveAll(true)
                .doOnNext(System.out::println)
                .take(100)
                .blockLast();

        storage.dispose();
    }
}
