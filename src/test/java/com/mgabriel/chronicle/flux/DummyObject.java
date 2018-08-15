package com.mgabriel.chronicle.flux;

import java.nio.charset.Charset;
import java.util.Objects;

import com.google.common.primitives.Longs;

/**
 * A dummy test object that can be serialized / deserialized as binary.
 *
 * Please note that for production it makes more sense to rely on a binary protocol such as Protobuf, Avro, etc.
 *
 * @author mgabriel.
 */
public class DummyObject {
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private final long timestamp;
    private final String value;

    public DummyObject(long timestamp, String value) {
        this.timestamp = timestamp;
        this.value = value;
    }

    public static DummyObject fromBinary(byte[] bytes) {
        byte[] time = new byte[8];
        byte[] val = new byte[bytes.length - 8];
        System.arraycopy(bytes, 0, time, 0, time.length);
        System.arraycopy(bytes, 8, val, 0, val.length);
        return new DummyObject(Longs.fromByteArray(time), new String(val, UTF_8));
    }

    public String value() {
        return value;
    }

    public long timestamp() {
        return timestamp;
    }

    public byte[] toBinary() {
        byte[] time = Longs.toByteArray(timestamp);
        byte[] val = value.getBytes(UTF_8);
        byte[] result = new byte[time.length + val.length];
        System.arraycopy(time, 0, result, 0, time.length);
        System.arraycopy(val, 0, result, time.length, val.length);
        return result;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        DummyObject that = (DummyObject) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public String toString() {
        return "DummyObject{" +
                "timestamp=" + timestamp +
                ", value='" + value + '\'' +
                '}';
    }
}
