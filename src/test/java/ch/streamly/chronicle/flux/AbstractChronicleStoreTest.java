package ch.streamly.chronicle.flux;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ch.streamly.chronicle.flux.AbstractChronicleStore.AbstractChronicleStoreBuilder;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.ReadBytesMarshallable;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.RollCycle;
import net.openhft.chronicle.queue.impl.WireStore;
import net.openhft.chronicle.queue.impl.single.SingleChronicleQueue;
import reactor.core.Disposable;
import reactor.test.StepVerifier;

class AbstractChronicleStoreTest {

    private static final String TEST_VALUE = "testValue";
    private AbstractChronicleStore<String, String> store;

    private File file;
    private SingleChronicleQueue queue;
    private ArgumentCaptor<ReadBytesMarshallable> reader;
    private ExcerptTailer tailer;
    private WireStore wireStore;

    @BeforeEach
    void setUp() {

        RollCycle rollCycle = Mockito.mock(RollCycle.class);
        file = Mockito.mock(File.class);
        queue = Mockito.mock(SingleChronicleQueue.class);
        reader = ArgumentCaptor.forClass(ReadBytesMarshallable.class);
        tailer = Mockito.mock(ExcerptTailer.class);
        wireStore = Mockito.mock(WireStore.class);

        when(queue.createTailer()).thenReturn(tailer);
        when(tailer.readBytes(any(ReadBytesMarshallable.class))).thenReturn(true);
        when(tailer.queue()).thenReturn(queue);
        when(queue.file()).thenReturn(file);
        when(file.getAbsolutePath()).thenReturn("");
        when(rollCycle.toCycle(anyLong())).thenReturn(0).thenReturn(1);
        when(queue.storeForCycle(anyInt(), anyLong(), anyBoolean())).thenReturn(wireStore);
        when(wireStore.file()).thenReturn(file);
        when(file.delete()).thenReturn(true);

        AbstractChronicleStoreBuilder<String> builder = new AbstractChronicleStoreBuilder<String>() {
        };
        builder.rollCycle(rollCycle);

        store = new AbstractChronicleStore<String, String>(builder) {

            @Override
            SingleChronicleQueue createQueue(String path) {
                return queue;
            }

            @Override
            protected String deserializeValue(BytesIn rawData) {
                return TEST_VALUE;
            }
        };
    }

    @Test
    @DisplayName("tests that the file is deleted once it has rolled")
    void shouldDeleteAfterRead() {
        Disposable sub = store.retrieveAll(true).subscribe();
        verify(file, timeout(100)).delete();
        sub.dispose();
    }

    @Test
    @DisplayName("tests that the an exception on file deletion is swallowed")
    void shouldSwallowExceptionOnFileDelete() {
        when(file.delete()).thenThrow(new RuntimeException("Simulated for unit test"));
        Disposable sub = store.retrieveAll(true).subscribe();
        verify(file, timeout(100)).delete();
        sub.dispose();
    }

    @Test
    @DisplayName("tests that failure of file deletion does not send an exception")
    void shouldNotFailIfFileNotDeleted() {
        when(file.delete()).thenReturn(false);
        Disposable sub = store.retrieveAll(true).subscribe();
        verify(file, timeout(100)).delete();
        sub.dispose();
    }

    @Test
    @DisplayName("tests that a null wirestore throws no exception")
    void testNullWirestore() {
        when(queue.storeForCycle(anyInt(), anyLong(), anyBoolean())).thenReturn(null);
        subscribeToValues();
    }

    private void subscribeToValues() {
        StepVerifier.create(store.retrieveAll(true))
                .expectSubscription()
                .then(() -> {
                    verify(tailer, timeout(100).atLeastOnce()).readBytes(reader.capture());
                    reader.getValue().readMarshallable(Bytes.elasticByteBuffer());
                    reader.getValue().readMarshallable(Bytes.elasticByteBuffer());
                    reader.getValue().readMarshallable(Bytes.elasticByteBuffer());
                })
                .expectNext(TEST_VALUE)
                .expectNext(TEST_VALUE)
                .expectNext(TEST_VALUE)
                .thenCancel()
                .verify(Duration.ofMillis(500));
    }

    @Test
    @DisplayName("tests that a null file throws no exception")
    void testNullFile() {
        when(queue.storeForCycle(anyInt(), anyLong(), anyBoolean())).thenReturn(wireStore);
        when(wireStore.file()).thenReturn(null);
        subscribeToValues();
    }
}