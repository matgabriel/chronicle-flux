package com.mgabriel.chronicle.flux.replay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TimedValueTest {

    private static final long TIME = 42;

    @Test
    @DisplayName("test equals and hashcode")
    void testDataClass() {
        String value = "testValue";
        TimedValue<String> first = new TimedValue<>(TIME, value);
        TimedValue<String> second = new TimedValue<>(TIME, value);
        assertEquals(first, second);
        assertEquals(first, first);
        assertNotEquals(first, TIME);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    @DisplayName("test equals and hashcode with null value")
    void testDataClassWithNullValue() {
        TimedValue<?> first = new TimedValue<>(TIME, null);
        TimedValue<?> second = new TimedValue<>(TIME, null);
        assertEquals(first, second);
        assertEquals(first, first);
        assertEquals(first.hashCode(), second.hashCode());
    }

}