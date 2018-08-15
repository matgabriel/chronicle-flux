package com.mgabriel.chronicle.flux.demo;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import com.mgabriel.chronicle.flux.ChronicleStore;
import com.mgabriel.chronicle.flux.DummyObject;
import org.apache.commons.io.FileUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

/**
 * @author mgabriel.
 */
public class ChronicleStoreDemo {

    private static final String PATH = "demoChronicleStore";

    public static void main(String[] args) throws InterruptedException {

        deleteStoreIfItExists();

        ChronicleStore<DummyObject> store = new ChronicleStore<>(PATH, DummyObject::toBinary, DummyObject::fromBinary);

        Flux<String> source = Flux.just("one", "two", "three")
                .concatWith(Flux.just("four").delayElements(Duration.ofSeconds(1)))
                .concatWith(Flux.just("five").delayElements(Duration.ofSeconds(2)));

        System.out.println("Storing source flux...");
        Disposable handle = store.store(source.map(v -> new DummyObject(System.currentTimeMillis(), v)));

        SECONDS.sleep(4); //wait until all items are stored
        handle.dispose();

        System.out.println("Storage achieved, replaying from store");

        store.replayHistory(DummyObject::timestamp)
                .withOriginalTiming()
                .inLoop(Duration.ofSeconds(1))
                .doOnNext(i -> System.out.println(Instant.now() + " " + i))
                .blockLast();
    }

    private static void deleteStoreIfItExists() {
        File directory = new File(".");
        try {
            String FULL_PATH = directory.getCanonicalPath() + File.separator + PATH;
            File storePath = new File(FULL_PATH);
            if (storePath.exists()) {
                FileUtils.deleteDirectory(storePath);
                System.out.println("Deleted existing store");
            }
        } catch (IOException e) {
            System.err.println("Error while deleting store");
        }
    }
}
