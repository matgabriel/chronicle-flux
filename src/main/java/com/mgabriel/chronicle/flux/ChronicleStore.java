package com.mgabriel.chronicle.flux;

import java.io.File;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mgabriel.chronicle.flux.replay.ReplayFlux;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.RollCycle;
import net.openhft.chronicle.queue.RollCycles;
import net.openhft.chronicle.queue.impl.WireStore;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueueBuilder;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

/**
 * @author mgabriel.
 */
public class ChronicleStore<T> implements FluxStore<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChronicleStore.class);

    private final Function<T, byte[]> serializer;
    private final Function<byte[], T> deserializer;
    private final SingleChronicleQueue queue;
    private final RollCycle rollCycle;

    public ChronicleStore(String path, Function<T, byte[]> serializer,
            Function<byte[], T> deserializer) {
        this(ChronicleStore.<T>newBuilder()
                .path(path)
                .serializer(serializer)
                .deserializer(deserializer));
    }

    private ChronicleStore(ChronicleStoreBuilder<T> builder) {
        String path = builder.path;
        serializer = builder.serializer;
        deserializer = builder.deserializer;
        rollCycle = builder.rollCycle;
        this.queue = SingleChronicleQueueBuilder.binary(path).rollCycle(rollCycle).build();

    }

    public static <BT> ChronicleStoreBuilder<BT> newBuilder() {
        return new ChronicleStoreBuilder<>();
    }

    @Override
    public Disposable store(Publisher<T> toStore) {
        ExcerptAppender appender = queue.acquireAppender();
        return Flux.from(toStore)
                .doOnError(err -> LOGGER.error("Error received", err))
                .subscribe(v -> {
                    byte[] bytesToStore = serializer.apply(v);
                    appender.writeBytes(b -> b.writeInt(bytesToStore.length).write(bytesToStore));
                });
    }

    @Override
    public void store(T item) {
        ExcerptAppender appender = queue.acquireAppender();
        byte[] bytesToStore = serializer.apply(item);
        appender.writeBytes(b -> b.writeInt(bytesToStore.length).write(bytesToStore));
    }

    private enum ReaderType {
        ALL,
        ONLY_HISTORY
    }

    private void readTailer(ExcerptTailer tailer, FluxSink<T> sink,
            ReaderType readerType, boolean deleteAfterRead) {
        int previousCycle = 0;
        try {
            while (!sink.isCancelled()) {
                if (sink.requestedFromDownstream() > 0) {
                    boolean present = tailer.readBytes(b -> readAndSendValue(sink, b));
                    if (!present) {
                        if (readerType == ReaderType.ONLY_HISTORY) {
                            sink.complete();
                        } else {
                            try {
                                Thread.sleep(10); //waiting for data to appear on the queue
                            } catch (InterruptedException e) {
                                traceInterrupt(e);
                            }
                        }
                    }
                } else {
                    try {
                        Thread.sleep(100); //waiting for requests on the flux
                    } catch (InterruptedException e) {
                        traceInterrupt(e);
                    }
                }
                int cycle = rollCycle.toCycle(tailer.index());
                if (cycle != previousCycle) {
                    if (deleteAfterRead) {
                        deleteFile(previousCycle);
                    }
                    previousCycle = cycle;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error while tailing on queue {}", tailer.queue().file().getAbsolutePath(), e);
        }
    }

    private void traceInterrupt(InterruptedException e) {
        Thread.currentThread().interrupt();
        LOGGER.trace("interrupted " + e);
    }

    private void deleteFile(int previousCycle) {
        WireStore wireStore = queue.storeForCycle(previousCycle, 0, false);
        if (wireStore != null) {
            File file = wireStore.file();
            if (file != null) {
                try {
                    boolean deleted = file.delete();
                    if (deleted) {
                        LOGGER.trace("file {} deleted after read", file.getAbsolutePath());
                    } else {
                        LOGGER.error("Could not delete file {}", file.getAbsolutePath());
                    }
                } catch (Exception e) {
                    LOGGER.error("Could not delete file {}", file.getAbsolutePath(), e);
                }
            } else {
                LOGGER.error("Could not find file for cycle {}", previousCycle);
            }
        } else {
            LOGGER.trace("wirestore is null for cycle {}", previousCycle);
        }
    }

    private void readAndSendValue(FluxSink<T> sink, BytesIn b) {
        int size = b.readInt();
        byte[] bytes = new byte[size];
        b.read(bytes);
        T value = deserializer.apply(bytes);
        sink.next(value);
    }

    @Override
    public Flux<T> retrieveAll(boolean deleteAfterRead) {
        return Flux.create(sink -> launchTailer(sink, ReaderType.ALL, deleteAfterRead));
    }

    private void launchTailer(FluxSink<T> sink, ReaderType readerType, boolean deleteAfterRead) {
        launchTailer(sink, queue.createTailer(), readerType, deleteAfterRead);
    }

    private void launchTailer(FluxSink<T> sink, ExcerptTailer tailer, ReaderType readerType, boolean deleteAfterRead) {
        String path = tailer.queue().file().getAbsolutePath();
        Thread t = new Thread(
                () -> readTailer(tailer, sink, readerType, deleteAfterRead),
                "ChronicleStoreRetrieve_" + path);
        t.setDaemon(true);
        t.run();
    }

    @Override
    public Flux<T> retrieveHistory() {
        return Flux.create(sink -> launchTailer(sink, ReaderType.ONLY_HISTORY, false));
    }

    @Override
    public Flux<T> retrieveNewValues() {
        ExcerptTailer tailer = queue.createTailer().toEnd();
        return Flux.create(sink -> launchTailer(sink, tailer, ReaderType.ALL, false));
    }

    @Override
    public ReplayFlux<T> replayHistory(Function<T, Long> timestampExtractor) {
        Flux<T> historySource = Flux.defer(this::retrieveHistory);
        return new ReplayFlux<>(historySource, timestampExtractor);
    }

    public static final class ChronicleStoreBuilder<T> {
        private String path;
        private Function<T, byte[]> serializer;
        private Function<byte[], T> deserializer;
        private RollCycle rollCycle = RollCycles.DAILY;

        private ChronicleStoreBuilder() {
        }

        public ChronicleStoreBuilder<T> path(String path) {
            this.path = path;
            return this;
        }

        public ChronicleStoreBuilder<T> serializer(Function<T, byte[]> serializer) {
            this.serializer = serializer;
            return this;
        }

        public ChronicleStoreBuilder<T> deserializer(Function<byte[], T> deserializer) {
            this.deserializer = deserializer;
            return this;
        }

        public ChronicleStoreBuilder<T> rollCycle(RollCycle rollCycle) {
            this.rollCycle = rollCycle;
            return this;
        }

        public ChronicleStore<T> build() {
            return new ChronicleStore<>(this);
        }
    }
}