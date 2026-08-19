package com.example.calculator;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/** Turns doubles into the strings the display shows, and back. */
public final class Numbers {

    /** Doubles carry ~15-17 significant digits; trimming to 12 hides 0.1+0.2 noise. */
    private static final MathContext DISPLAY_PRECISION = new MathContext(12, RoundingMode.HALF_UP);

    private static final double SCI_UPPER = 1e12;
    private static final double SCI_LOWER = 1e-6;

    private Numbers() {
    }

    /**
     * Formats a result for the display: no trailing zeros, no exponent unless
     * the magnitude really needs one, and no floating-point dust.
     */
    public static String format(double value) {
        if (value == 0.0) {
            return "0"; // also collapses -0.0
        }
        double abs = Math.abs(value);
        if (abs >= SCI_UPPER || abs < SCI_LOWER) {
            return scientific(value);
        }
        return plain(value);
    }

    /**
     * Like {@link #format} but never exponential, so the result can be fed back
     * into the expression and parsed again.
     */
    public static String plain(double value) {
        if (value == 0.0) {
            return "0";
        }
        BigDecimal rounded = new BigDecimal(value, DISPLAY_PRECISION).stripTrailingZeros();
        if (rounded.scale() < 0) {
            rounded = rounded.setScale(0); // 1E+3 -> 1000
        }
        return rounded.toPlainString();
    }

    private static String scientific(double value) {
        String s = String.format(java.util.Locale.US, "%.6e", value);
        String[] parts = s.split("e");
        String mantissa = parts[0];
        if (mantissa.contains(".")) {
            mantissa = mantissa.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        int exponent = Integer.parseInt(parts[1]);
        return mantissa + "e" + exponent;
    }

    /**
     * Adds thousands separators to every number in {@code expression}, leaving
     * operators and any half-typed decimal point untouched.
     */
    public static String group(String expression) {
        StringBuilder out = new StringBuilder(expression.length() + 8);
        int i = 0;
        while (i < expression.length()) {
            char c = expression.charAt(i);
            if (!isNumberChar(c)) {
                out.append(c);
                i++;
                continue;
            }
            int start = i;
            while (i < expression.length() && isNumberChar(expression.charAt(i))) {
                i++;
            }
            out.append(groupToken(expression.substring(start, i)));
        }
        return out.toString();
    }

    private static String groupToken(String token) {
        int dot = token.indexOf('.');
        String whole = dot < 0 ? token : token.substring(0, dot);
        String rest = dot < 0 ? "" : token.substring(dot);
        if (whole.length() < 5) {
            return whole + rest; // 1000 reads fine; 10,000 is where separators earn their keep
        }
        StringBuilder grouped = new StringBuilder();
        int lead = whole.length() % 3;
        if (lead > 0) {
            grouped.append(whole, 0, lead);
        }
        for (int i = lead; i < whole.length(); i += 3) {
            if (grouped.length() > 0) {
                grouped.append(',');
            }
            grouped.append(whole, i, i + 3);
        }
        return grouped + rest;
    }

    private static boolean isNumberChar(char c) {
        return (c >= '0' && c <= '9') || c == '.';
    }
}
