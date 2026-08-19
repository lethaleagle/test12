package com.example.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NumbersTest {

    @Test
    void hidesFloatingPointDust() {
        assertEquals("0.3", Numbers.format(0.1 + 0.2));
        assertEquals("0.9", Numbers.format(0.3 * 3));
        assertEquals("3", Numbers.format(2.9999999999999996));
    }

    @Test
    void dropsTrailingZerosAndNegativeZero() {
        assertEquals("2", Numbers.format(2.0));
        assertEquals("2.5", Numbers.format(2.50));
        assertEquals("0", Numbers.format(-0.0));
        assertEquals("1000", Numbers.format(1000.0));
    }

    @Test
    void usesExponentsOnlyForExtremes() {
        assertEquals("1e15", Numbers.format(1e15));
        assertEquals("1.5e-8", Numbers.format(1.5e-8));
        assertEquals("999999999999", Numbers.format(999999999999.0));
        assertEquals("0.000001", Numbers.format(1e-6));
    }

    @Test
    void plainNeverUsesExponents() {
        assertEquals("1000000000000000", Numbers.plain(1e15));
        assertEquals("0.000000015", Numbers.plain(1.5e-8));
    }

    @Test
    void groupsLongNumbersOnly() {
        assertEquals("1234", Numbers.group("1234"));
        assertEquals("12,345", Numbers.group("12345"));
        assertEquals("1,234,567", Numbers.group("1234567"));
    }

    @Test
    void groupingLeavesOperatorsAndDecimalsAlone() {
        assertEquals("12,345+67,890.12345", Numbers.group("12345+67890.12345"));
        assertEquals("12,345×2", Numbers.group("12345×2"));
        assertEquals("−12,345", Numbers.group("−12345"));
        assertEquals("1234.", Numbers.group("1234."));
    }
}
