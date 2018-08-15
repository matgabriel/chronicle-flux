package com.mgabriel.chronicle.flux.replay;

import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;

import java.time.Duration;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.mgabriel.chronicle.flux.DummyObject;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

/**
 * @author mgabriel.
 */
@SuppressWarnings("javadoc")
class ReplayFluxTest {

    private static final String ONE = "one";
    private static final String TWO = "two";
    private static final String THREE = "three";
    private static final String FOUR = "four";
    private static final Duration ONE_SECOND = ofSeconds(1);
    private static final Duration TWO_SECONDS = ofSeconds(2);
    private static final Duration THREE_SECONDS = ofSeconds(3);
    private static final Duration MILLIS_500 = ofMillis(500);

    private static Flux<DummyObject> source = Flux.just(new DummyObject(10000, ONE),
            new DummyObject(11000, TWO),
            new DummyObject(12000, THREE),
            new DummyObject(15000, FOUR)
    );

    private static ReplayFlux<DummyObject> replayFlux = new ReplayFlux<>(source, DummyObject::timestamp);

    @BeforeEach
    void setUp() {
    }

    @Test
    @DisplayName("tests that the flux is replayed with the original timing")
    void shouldRespectOriginalTiming() {
        StepVerifier.withVirtualTime(() -> replayFlux.withOriginalTiming())
                .expectSubscription()
                .assertNext(i -> Assertions.assertEquals(ONE, i.value()))
                .expectNoEvent(ONE_SECOND)
                .assertNext(i -> Assertions.assertEquals(TWO, i.value()))
                .expectNoEvent(ONE_SECOND)
                .assertNext(i -> Assertions.assertEquals(THREE, i.value()))
                .expectNoEvent(THREE_SECONDS)
                .assertNext(i -> Assertions.assertEquals(FOUR, i.value()))
                .expectComplete()
                .verify(ofMillis(500));
    }

    @Test
    @DisplayName("tests that the flux is replayed with a time acceleration over the original timing")
    void shouldReplayWithTimeAcceleration() {
        StepVerifier.withVirtualTime(() -> replayFlux.withTimeAcceleration(2))
                .expectSubscription()
                .assertNext(i -> Assertions.assertEquals(ONE, i.value()))
                .expectNoEvent(MILLIS_500)
                .assertNext(i -> Assertions.assertEquals(TWO, i.value()))
                .expectNoEvent(MILLIS_500)
                .assertNext(i -> Assertions.assertEquals(THREE, i.value()))
                .expectNoEvent(ONE_SECOND.plus(MILLIS_500))
                .assertNext(i -> Assertions.assertEquals(FOUR, i.value()))
                .expectComplete()
                .verify(ofMillis(500));
    }

    @Test
    @DisplayName("tests that the flux is replayed with a time deceleration over the original timing")
    void shouldReplayWithTimeDeceleration() {
        StepVerifier.withVirtualTime(() -> replayFlux.withTimeAcceleration(0.5))
                .expectSubscription()
                .assertNext(i -> Assertions.assertEquals(ONE, i.value()))
                .expectNoEvent(TWO_SECONDS)
                .assertNext(i -> Assertions.assertEquals(TWO, i.value()))
                .expectNoEvent(TWO_SECONDS)
                .assertNext(i -> Assertions.assertEquals(THREE, i.value()))
                .expectNoEvent(ofSeconds(6))
                .assertNext(i -> Assertions.assertEquals(FOUR, i.value()))
                .expectComplete()
                .verify(ofMillis(500));
    }

    @Test
    @DisplayName("tests that the flux is replayed in a loop")
    void shouldReplayInLoop() {
        StepVerifier.create(replayFlux.inLoop())
                .expectSubscription()
                .assertNext(i -> {
                    Assertions.assertEquals(ONE, i.value().value());
                    Assertions.assertTrue(i.isLoopRestart());
                })
                .assertNext(assertValue(TWO))
                .assertNext(assertValue(THREE))
                .assertNext(assertValue(FOUR))
                .assertNext(i -> {
                    Assertions.assertEquals(ONE, i.value().value());
                    Assertions.assertTrue(i.isLoopRestart());
                })
                .assertNext(assertValue(TWO))
                .assertNext(assertValue(THREE))
                .assertNext(assertValue(FOUR))
                .thenCancel()
                .verify(ofMillis(500));
    }

    private static Consumer<ReplayValue<DummyObject>> assertValue(String expected) {
        return i -> Assertions.assertEquals(expected, i.value().value());
    }

    @Test
    @DisplayName("tests that the flux is replayed in a loop with a delay before each loop restart")
    void shouldReplayInLoopWithRestartDelay() {
        StepVerifier.withVirtualTime(() -> replayFlux.inLoop(TWO_SECONDS))
                .expectSubscription()
                .expectNoEvent(TWO_SECONDS)
                .assertNext(assertLoopRestart())
                .assertNext(assertValue(TWO))
                .assertNext(assertValue(THREE))
                .assertNext(assertValue(FOUR))
                .expectNoEvent(TWO_SECONDS)
                .assertNext(assertLoopRestart())
                .assertNext(assertValue(TWO))
                .assertNext(assertValue(THREE))
                .assertNext(assertValue(FOUR))
                .expectNoEvent(TWO_SECONDS)
                .thenCancel()
                .verify(ofMillis(500));
    }

    private static Consumer<ReplayValue<DummyObject>> assertLoopRestart() {
        return i -> {
            Assertions.assertEquals(ONE, i.value().value());
            Assertions.assertTrue(i.isLoopRestart());
        };
    }
}