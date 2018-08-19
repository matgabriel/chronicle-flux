package ch.streamly.chronicle.flux;

import static ch.streamly.chronicle.flux.util.ChronicleStoreCleanup.deleteStoreIfItExists;
import static java.time.Duration.ofSeconds;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

class ChronicleStoreTest {
    private static final String PATH = "ChronicleStoreTest";
    private static final String ONE = "one";
    private static final String TWO = "two";
    private static final String THREE = "three";
    private static final String FOUR = "four";
    private static final DummyObject FIRST = new DummyObject(10000, ONE);
    private static final DummyObject SECOND = new DummyObject(11000, TWO);
    private static final DummyObject THIRD = new DummyObject(12000, THREE);
    private static final DummyObject FOURTH = new DummyObject(15000, FOUR);
    private static final Flux<DummyObject> source = Flux.just(FIRST, SECOND, THIRD, FOURTH
    );
    private ChronicleStore<DummyObject> store;

    @BeforeEach
    void setUp() {
        deleteStoreIfItExists(PATH);
        store = new ChronicleStore<>(PATH, DummyObject::toBinary, DummyObject::fromBinary);
    }

        @AfterEach
        void tearDown() {
            store.close();
        }

    @Test
    @DisplayName("tests that a data stream is store in the Chronicle store")
    void shouldStoreStream() {
        store.store(source);
        StepVerifier.create(store.retrieveAll())
                .expectSubscription()
                .expectNext(FIRST)
                .expectNext(SECOND)
                .expectNext(THIRD)
                .expectNext(FOURTH)
                .thenCancel()
                .verify(Duration.ofMillis(500));
    }

    @Test
    @DisplayName("tests that individual items are stored in the Chronicle store")
    void shouldStoreIndividualItems() {
        store.store(FIRST);
        store.store(THIRD);

        StepVerifier.create(store.retrieveAll())
                .expectSubscription()
                .expectNext(FIRST)
                .expectNext(THIRD)
                .thenCancel()
                .verify(Duration.ofMillis(500));
    }


    @Test
    @DisplayName("tests that the history is retrieved from the Chronicle store")
    void shouldRetrieveHistory() {
        store.store(FIRST);
        store.store(SECOND);
        store.store(THIRD);

        StepVerifier.create(store.retrieveHistory())
                .expectSubscription()
                .expectNext(FIRST)
                .expectNext(SECOND)
                .expectNext(THIRD)
                .expectComplete()
                .verify(Duration.ofMillis(500));
    }

    @Test
    @DisplayName("tests that new values are retrieved from the Chronicle store")
    void retrieveNewValues() {
        store.store(FIRST);
        StepVerifier.withVirtualTime(() -> store.retrieveNewValues())
                .expectSubscription()
                .expectNoEvent(ofSeconds(1))
                .then( () -> store.store(SECOND))
                .expectNext(SECOND)
                .then( () -> store.store(THIRD))
                .expectNext(THIRD)
                .thenCancel()
                .verify(Duration.ofMillis(500));
    }

    @Test
    @DisplayName("tests that the Chronicle store can replay history")
    void replayHistory() {
        store.store(source);
        StepVerifier.withVirtualTime(() -> store.replayHistory(DummyObject::timestamp).withOriginalTiming())
                .expectSubscription()
                .expectNext(FIRST)
                .expectNoEvent(ofSeconds(1))
                .expectNext(SECOND)
                .expectNoEvent(ofSeconds(1))
                .expectNext(THIRD)
                .expectNoEvent(ofSeconds(3))
                .expectNext(FOURTH)
                .thenCancel()
                .verify(Duration.ofMillis(500));
    }
}