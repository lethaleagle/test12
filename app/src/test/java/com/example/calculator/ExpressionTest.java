package com.example.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ExpressionTest {

    private final Expression expression = new Expression();

    private void type(String keys) {
        for (int i = 0; i < keys.length(); i++) {
            char c = keys.charAt(i);
            if (c >= '0' && c <= '9') {
                expression.appendDigit(c);
            } else if (c == '.') {
                expression.appendDot();
            } else if (c == '%') {
                expression.appendPercent();
            } else {
                expression.appendOperator(c);
            }
        }
    }

    @Test
    void collapsesRepeatedOperators() {
        type("5+×");
        assertEquals("5×", expression.text());
    }

    @Test
    void keepsMinusAfterTimesAsANegativeOperand() {
        type("5×−3");
        assertEquals("5×−3", expression.text());
    }

    @Test
    void allowsOnlyALeadingMinus() {
        type("+");
        assertEquals("", expression.text());
        type("−5");
        assertEquals("−5", expression.text());
    }

    @Test
    void dropsADanglingDecimalPointBeforeAnOperator() {
        type("5.+3");
        assertEquals("5+3", expression.text());
    }

    @Test
    void refusesASecondDecimalPointInTheSameNumber() {
        type("1.5.2");
        assertEquals("1.52", expression.text());
    }

    @Test
    void startsDecimalsWithAZero() {
        type(".5");
        assertEquals("0.5", expression.text());
        expression.clear();
        type("2+.5");
        assertEquals("2+0.5", expression.text());
    }

    @Test
    void refusesLeadingZeros() {
        type("007");
        assertEquals("7", expression.text());
        expression.clear();
        type("10");
        assertEquals("10", expression.text());
    }

    @Test
    void percentOnlyFollowsANumber() {
        type("+");
        expression.appendPercent();
        assertEquals("", expression.text());
        type("50%");
        assertEquals("50%", expression.text());
    }

    @Test
    void multipliesWhenTypingStraightAfterAPercent() {
        type("50%2");
        assertEquals("50%×2", expression.text());
    }

    @Test
    void togglesTheSignOfTheNumberBeingTyped() {
        type("5+3");
        expression.toggleSign();
        assertEquals("5+−3", expression.text());
        expression.toggleSign();
        assertEquals("5+3", expression.text());
    }

    @Test
    void togglesASignOnAnEmptyExpression() {
        expression.toggleSign();
        assertEquals("−", expression.text());
        expression.toggleSign();
        assertEquals("", expression.text());
    }

    @Test
    void doesNotTurnASubtractionIntoASignChange() {
        type("5−3");
        expression.toggleSign();
        assertEquals("5−−3", expression.text());
    }

    @Test
    void backspaceRemovesOneCharacter() {
        type("12+3");
        expression.backspace();
        assertEquals("12+", expression.text());
        expression.backspace();
        assertEquals("12", expression.text());
    }

    @Test
    void previewsOnlyOnceThereIsSomethingToCompute() {
        type("12");
        assertFalse(expression.hasOperation());
        type("+");
        assertTrue(expression.hasOperation());
        expression.clear();
        expression.toggleSign();
        type("5");
        assertEquals("−5", expression.text());
        assertFalse(expression.hasOperation(), "a leading minus is not an operation");
    }

    @Test
    void stopsAcceptingInputAtTheLengthLimit() {
        for (int i = 0; i < 200; i++) {
            expression.appendDigit('9');
        }
        assertEquals(64, expression.text().length());
    }
}
