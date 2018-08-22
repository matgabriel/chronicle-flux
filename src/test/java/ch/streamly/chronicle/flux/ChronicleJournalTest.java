package ch.streamly.chronicle.flux;

import static ch.streamly.chronicle.flux.util.ChronicleStoreCleanup.deleteStoreIfItExists;
import static java.time.Duration.ofSeconds;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.streamly.domain.WrappedValue;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

class ChronicleJournalTest {
    private static final String PREFIX = "ChronicleJournalTest";
    private static final String ONE = "one";
    private static final String TWO = "two";
    private static final String THREE = "three";
    private static final String FOUR = "four";
    private static final DummyObject FIRST = new DummyObject(10000, ONE);
    private static final DummyObject SECOND = new DummyObject(11000, TWO);
    private static final DummyObject THIRD = new DummyObject(12000, THREE);
    private static final DummyObject FOURTH = new DummyObject(15000, FOUR);
    private static final Flux<DummyObject> source = Flux.just(FIRST, SECOND, THIRD, FOURTH);
    private static final long TIME_1 = 1000L;
    private static final long TIME_2 = 2000L;
    private static final long TIME_3 = 3000L;
    private static final long TIME_4 = 7000L;
    private ChronicleJournal<DummyObject> journal;
    private String path;

    @BeforeEach
    void setUp() {
        path = PREFIX + UUID.randomUUID().toString();
        ConcurrentLinkedQueue<Long> timeQueue = new ConcurrentLinkedQueue<>();
        timeQueue.add(TIME_1);
        timeQueue.add(TIME_2);
        timeQueue.add(TIME_3);
        timeQueue.add(TIME_4);
        journal = new ChronicleJournal<DummyObject>(path, DummyObject::toBinary, DummyObject::fromBinary) {
            @Override
            long getCurrentTime() {
                return timeQueue.poll();
            }
        };
    }

    @AfterEach
    void tearDown() {
        journal.close();
        deleteStoreIfItExists(path);
    }

    @Test
    @DisplayName("tests that a timed data stream is stored in the Chronicle journal")
    void shouldStoreStreamWithTimeStamps() {
        verifyBasicOperations();
    }

    @Test
    @DisplayName("tests store creation with builder")
    void testStoreCreationWithBuilder() {
        journal = ChronicleJournal.<DummyObject>newBuilder()
                .path(path)
                .serializer(DummyObject::toBinary)
                .deserializer(DummyObject::fromBinary)
                .build();

    }

    private void verifyBasicOperations() {
        journal.store(source);
        StepVerifier.create(journal.retrieveAll())
                .expectSubscription()
                .assertNext(i -> {
                    Assertions.assertEquals(FIRST, i.value());
                    Assertions.assertEquals(TIME_1, i.time());
                })
                .assertNext(i -> {
                    Assertions.assertEquals(SECOND, i.value());
                    Assertions.assertEquals(TIME_2, i.time());
                })
                .assertNext(i -> {
                    Assertions.assertEquals(THIRD, i.value());
                    Assertions.assertEquals(TIME_3, i.time());
                })
                .thenCancel()
                .verify(Duration.ofMillis(500));
    }

    @Test
    @DisplayName("tests that the Chronicle journal can replay history with the timestamps assigned by the journal")
    void replayHistoryWithJournalTime() {
        journal.store(source);
        StepVerifier.withVirtualTime(() -> journal.replayHistory().withOriginalTiming().map(WrappedValue::value))
                .expectSubscription()
                .expectNext(FIRST)
                .expectNoEvent(ofSeconds(1))
                .expectNext(SECOND)
                .expectNoEvent(ofSeconds(1))
                .expectNext(THIRD)
                .expectNoEvent(ofSeconds(4))
                .expectNext(FOURTH)
                .thenCancel()
                .verify(Duration.ofMillis(500));
    }

}