package com.example.calculator;

/**
 * The expression being typed, plus the editing rules that keep it well-formed.
 *
 * <p>Keeping this separate from the activity means the keypad only has to say
 * <em>what</em> was pressed; every "you can't put two operators in a row" style
 * decision lives here.
 */
public final class Expression {

    /** Long enough for any realistic sum, short enough to stay renderable. */
    private static final int MAX_LENGTH = 64;

    private final StringBuilder text = new StringBuilder();

    public String text() {
        return text.toString();
    }

    public boolean isEmpty() {
        return text.length() == 0;
    }

    public void clear() {
        text.setLength(0);
    }

    /** Replaces everything with {@code value}, as '=' does with its result. */
    public void set(String value) {
        text.setLength(0);
        text.append(value);
    }

    public void appendDigit(char digit) {
        if (isFull()) {
            return;
        }
        if (last() == '%') {
            text.append('×'); // 50% 5 only makes sense as a product
        }
        String token = trailingNumber();
        if (token.equals("0")) {
            text.deleteCharAt(text.length() - 1); // no leading zeros
        }
        text.append(digit);
    }

    public void appendDot() {
        if (isFull()) {
            return;
        }
        if (trailingNumber().contains(".")) {
            return;
        }
        if (last() == '%') {
            text.append('×');
        }
        if (trailingNumber().isEmpty()) {
            text.append('0'); // ".5" is harder to read than "0.5"
        }
        text.append('.');
    }

    /** @param op one of {@code + − × ÷} */
    public void appendOperator(char op) {
        char last = last();
        if (last == '\0') {
            if (op == '−') {
                text.append(op); // a leading minus starts a negative number
            }
            return;
        }
        if (last == '.') {
            text.deleteCharAt(text.length() - 1); // "5." + "+" -> "5+"
            last = last();
        }
        if (isOperator(last)) {
            // "5×" then "−" is a negative operand; anything else swaps the operator.
            if (op == '−' && (last == '×' || last == '÷')) {
                text.append(op);
            } else {
                text.setCharAt(text.length() - 1, op);
            }
            return;
        }
        if (isFull()) {
            return;
        }
        text.append(op);
    }

    public void appendPercent() {
        char last = last();
        if (isFull() || !(isDigit(last) || last == ')')) {
            return;
        }
        text.append('%');
    }

    /** Flips the sign of the number currently being typed. */
    public void toggleSign() {
        int start = trailingNumberStart();
        if (start == text.length()) {
            // Nothing to flip: toggle a standalone unary minus instead.
            if (last() == '−' && isUnaryMinusAt(text.length() - 1)) {
                text.deleteCharAt(text.length() - 1);
            } else if (!isFull()) {
                text.append('−');
            }
            return;
        }
        if (start > 0 && text.charAt(start - 1) == '−' && isUnaryMinusAt(start - 1)) {
            text.deleteCharAt(start - 1);
        } else if (!isFull()) {
            text.insert(start, '−');
        }
    }

    public void backspace() {
        if (text.length() > 0) {
            text.deleteCharAt(text.length() - 1);
        }
    }

    /** True once the expression is worth previewing a result for. */
    public boolean hasOperation() {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '%' || (isOperator(c) && !isUnaryMinusAt(i))) {
                return true;
            }
        }
        return false;
    }

    /** The digits of the number currently being typed, empty if there isn't one. */
    private String trailingNumber() {
        return text.substring(trailingNumberStart());
    }

    private int trailingNumberStart() {
        int i = text.length();
        while (i > 0 && isDigitOrDot(text.charAt(i - 1))) {
            i--;
        }
        return i;
    }

    /** A minus is unary when it opens the expression or follows another operator. */
    private boolean isUnaryMinusAt(int index) {
        if (index < 0 || index >= text.length() || text.charAt(index) != '−') {
            return false;
        }
        if (index == 0) {
            return true;
        }
        char before = text.charAt(index - 1);
        return isOperator(before) || before == '(';
    }

    private char last() {
        return text.length() == 0 ? '\0' : text.charAt(text.length() - 1);
    }

    private boolean isFull() {
        return text.length() >= MAX_LENGTH;
    }

    public static boolean isOperator(char c) {
        return c == '+' || c == '−' || c == '×' || c == '÷';
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static boolean isDigitOrDot(char c) {
        return isDigit(c) || c == '.';
    }
}
