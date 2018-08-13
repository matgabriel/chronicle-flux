package com.mgabriel.chronicle.flux;

import java.nio.charset.Charset;
import java.util.Objects;

import com.google.common.primitives.Longs;

/**
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

    public String getValue() {
        return value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public byte[] toBinary(){
        byte[] time = Longs.toByteArray(timestamp);
        byte[] val = value.getBytes(UTF_8);
        byte[] result = new byte[time.length + val.length];
        System.arraycopy(time, 0, result, 0, time.length);
        System.arraycopy(val, 0, result, time.length, val.length);
        return result;
    }

    public static DummyObject fromBinary(byte[] bytes){
        byte[] time = new byte[8];
        byte[] val = new byte[bytes.length - 8];
        System.arraycopy(bytes, 0, time, 0, time.length);
        System.arraycopy(bytes, 8, val, 0, val.length);
        return new DummyObject(Longs.fromByteArray(time), new String(val, UTF_8));
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
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "DummyObject{" +
                "timestamp=" + timestamp +
                ", value='" + value + '\'' +
                '}';
    }
}
