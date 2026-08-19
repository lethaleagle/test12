package com.example.calculator;

/**
 * Recursive-descent evaluator for the expressions the keypad can produce.
 *
 * <pre>
 *   expr    := term (('+' | '-') term)*
 *   term    := unary (('*' | '/') unary)*
 *   unary   := ('+' | '-')* postfix
 *   postfix := primary '%'*
 *   primary := NUMBER | '(' expr ')'
 * </pre>
 *
 * Percent follows the convention people expect from a pocket calculator rather
 * than a plain divide-by-100: in an addition or subtraction the percentage is
 * taken <em>of the left-hand side</em>, so {@code 50+10%} is 55, while
 * {@code 50*10%} is the literal 5. Only the outermost percent of an operand
 * carries that meaning; once a percent value takes part in a multiplication it
 * has already collapsed to a plain number.
 */
public final class Evaluator {

    /** The expression is incomplete or malformed. */
    public static class SyntaxException extends RuntimeException {
        SyntaxException(String message) {
            super(message);
        }
    }

    /** The expression is well-formed but has no finite value. */
    public static class MathException extends RuntimeException {
        MathException(String message) {
            super(message);
        }
    }

    /** An operand plus a note on whether it was written as a percentage. */
    private static final class Val {
        final double raw;
        final boolean percent;

        Val(double raw, boolean percent) {
            this.raw = raw;
            this.percent = percent;
        }

        /** The operand's value when nothing else claims the percent. */
        double plain() {
            return percent ? raw / 100.0 : raw;
        }
    }

    private final String src;
    private int pos;

    private Evaluator(String src) {
        this.src = src;
    }

    /**
     * Evaluates {@code expression}, which may use either the display glyphs
     * (× ÷ −) or their ASCII equivalents (* / -).
     *
     * @throws SyntaxException if the expression is empty, incomplete or malformed
     * @throws MathException   if the result is not a finite number
     */
    public static double eval(String expression) {
        Evaluator parser = new Evaluator(expression);
        parser.skipSpace();
        if (parser.pos >= parser.src.length()) {
            throw new SyntaxException("Empty expression");
        }
        double value = parser.expr().plain();
        parser.skipSpace();
        if (parser.pos < parser.src.length()) {
            throw new SyntaxException("Unexpected '" + parser.src.charAt(parser.pos) + "'");
        }
        if (Double.isNaN(value)) {
            throw new MathException("Not a number");
        }
        if (Double.isInfinite(value)) {
            throw new MathException("Number too large");
        }
        return value;
    }

    private Val expr() {
        Val first = term();
        if (!isAddOp(peek())) {
            return first;
        }
        double acc = first.plain();
        while (isAddOp(peek())) {
            char op = next();
            Val right = term();
            // "a + b%" means b percent of a, not b/100.
            double delta = right.percent ? acc * right.raw / 100.0 : right.plain();
            acc = (op == '+') ? acc + delta : acc - delta;
        }
        return new Val(acc, false);
    }

    private Val term() {
        Val first = unary();
        if (!isMulOp(peek())) {
            return first;
        }
        double acc = first.plain();
        while (isMulOp(peek())) {
            char op = next();
            double right = unary().plain();
            if (op == '*') { // next() has already normalised × and ÷
                acc *= right;
            } else {
                if (right == 0.0) {
                    throw new MathException("Can't divide by zero");
                }
                acc /= right;
            }
        }
        return new Val(acc, false);
    }

    private Val unary() {
        boolean negate = false;
        while (true) {
            char c = peek();
            if (c == '-' || c == '−') {
                negate = !negate;
                pos++;
            } else if (c == '+') {
                pos++;
            } else {
                break;
            }
        }
        Val v = postfix();
        return negate ? new Val(-v.raw, v.percent) : v;
    }

    private Val postfix() {
        double value = primary();
        boolean percent = false;
        while (peek() == '%') {
            pos++;
            if (percent) {
                value /= 100.0; // each extra '%' is another plain division
            }
            percent = true;
        }
        return new Val(value, percent);
    }

    private double primary() {
        skipSpace();
        if (pos >= src.length()) {
            throw new SyntaxException("Expression is incomplete");
        }
        char c = src.charAt(pos);
        if (c == '(') {
            pos++;
            double value = expr().plain();
            skipSpace();
            if (pos >= src.length() || src.charAt(pos) != ')') {
                throw new SyntaxException("Missing ')'");
            }
            pos++;
            return value;
        }
        return number();
    }

    private double number() {
        int start = pos;
        boolean seenDot = false;
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (c >= '0' && c <= '9') {
                pos++;
            } else if (c == '.' && !seenDot) {
                seenDot = true;
                pos++;
            } else {
                break;
            }
        }
        if (pos == start || (seenDot && pos == start + 1)) {
            throw new SyntaxException("Expected a number");
        }
        try {
            return Double.parseDouble(src.substring(start, pos));
        } catch (NumberFormatException e) {
            throw new SyntaxException("Bad number");
        }
    }

    private void skipSpace() {
        while (pos < src.length() && src.charAt(pos) == ' ') {
            pos++;
        }
    }

    /** The next significant character, or {@code '\0'} at the end of input. */
    private char peek() {
        skipSpace();
        return pos < src.length() ? src.charAt(pos) : '\0';
    }

    private char next() {
        char c = peek();
        pos++;
        // Normalise the display glyphs so callers only ever see ASCII operators.
        if (c == '−') {
            return '-';
        }
        if (c == '×') {
            return '*';
        }
        if (c == '÷') {
            return '/';
        }
        return c;
    }

    private static boolean isAddOp(char c) {
        return c == '+' || c == '-' || c == '−';
    }

    private static boolean isMulOp(char c) {
        return c == '*' || c == '/' || c == '×' || c == '÷';
    }
}
