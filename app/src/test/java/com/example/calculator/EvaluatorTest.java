package com.example.calculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class EvaluatorTest {

    private static final double EPSILON = 1e-9;

    private static void assertEval(double expected, String expression) {
        assertEquals(expected, Evaluator.eval(expression), EPSILON, expression);
    }

    @Test
    void evaluatesBasicArithmetic() {
        assertEval(7, "3+4");
        assertEval(-1, "3-4");
        assertEval(12, "3*4");
        assertEval(0.75, "3/4");
    }

    @Test
    void honoursPrecedence() {
        assertEval(14, "2+3*4");
        assertEval(10, "2*3+4");
        assertEval(2.5, "10/4");
        assertEval(6, "2+2*2");
    }

    @Test
    void acceptsDisplayGlyphs() {
        assertEval(12, "3×4");
        assertEval(0.75, "3÷4");
        assertEval(-1, "3−4");
        assertEval(-7, "−3−4");
    }

    @Test
    void handlesUnaryMinus() {
        assertEval(-5, "-5");
        assertEval(5, "--5");
        assertEval(8, "5--3");
        assertEval(-15, "5*-3");
    }

    @Test
    void handlesDecimals() {
        assertEval(0.3, "0.1+0.2");
        assertEval(2.5, "2.5");
        assertEval(1.5, "0.5+1");
    }

    @Test
    void handlesParentheses() {
        assertEval(20, "(2+3)*4");
        assertEval(14, "2+(3*4)");
        assertEval(-20, "-(2+3)*4");
    }

    @Test
    void percentOfLeftHandSideInAdditions() {
        // The pocket-calculator convention: the percentage is taken of what came before.
        assertEval(55, "50+10%");
        assertEval(45, "50-10%");
        assertEval(220, "200+10%");
    }

    @Test
    void percentIsPlainDivisionElsewhere() {
        assertEval(0.1, "10%");
        assertEval(20, "200*10%");
        assertEval(2000, "200/10%");
        assertEval(0.001, "10%%");
    }

    @Test
    void rejectsDivisionByZero() {
        Evaluator.MathException e =
                assertThrows(Evaluator.MathException.class, () -> Evaluator.eval("1/0"));
        assertEquals("Can't divide by zero", e.getMessage());
        assertThrows(Evaluator.MathException.class, () -> Evaluator.eval("5/(3-3)"));
    }

    @Test
    void rejectsIncompleteExpressions() {
        assertThrows(Evaluator.SyntaxException.class, () -> Evaluator.eval(""));
        assertThrows(Evaluator.SyntaxException.class, () -> Evaluator.eval("5+"));
        assertThrows(Evaluator.SyntaxException.class, () -> Evaluator.eval("5+*3"));
        assertThrows(Evaluator.SyntaxException.class, () -> Evaluator.eval("(2+3"));
        assertThrows(Evaluator.SyntaxException.class, () -> Evaluator.eval("."));
    }

    @Test
    void rejectsResultsThatAreNotFinite() {
        assertThrows(Evaluator.MathException.class,
                () -> Evaluator.eval("9999999999999999999999999999999999999999999999999999"
                        + "9999999999999999999999999999999999999999999999999999"
                        + "9999999999999999999999999999999999999999999999999999"
                        + "9999999999999999999999999999999999999999999999999999"
                        + "9999999999999999999999999999999999999999999999999999"
                        + "9999999999999999999999999999999999999999999999999999"
                        + "999999999999*99999999999999999999999999999999999999"));
    }
}
