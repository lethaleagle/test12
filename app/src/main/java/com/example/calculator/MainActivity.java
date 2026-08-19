package com.example.calculator;

import android.app.Activity;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

/** The calculator screen: a display, a keypad, and the glue between them. */
public class MainActivity extends Activity implements View.OnClickListener,
        View.OnLongClickListener {

    private static final String STATE_EXPRESSION = "expression";
    private static final String STATE_RESULT = "result";

    private final Expression expression = new Expression();

    private TextView expressionView;
    private TextView resultView;
    private HorizontalScrollView expressionScroll;

    /**
     * The value '=' produced, kept so the display can show its prettiest form
     * (including exponents) while the expression itself holds a re-usable one.
     */
    private Double committedResult;

    /** Set when the last '=' failed; cleared by the next keypress. */
    private String errorMessage;

    private int colorSecondary;
    private int colorError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        expressionView = findViewById(R.id.expression);
        resultView = findViewById(R.id.result);
        expressionScroll = findViewById(R.id.expression_scroll);
        colorSecondary = getResources().getColor(R.color.text_secondary);
        colorError = getResources().getColor(R.color.error);

        attachKeypadListeners((ViewGroup) findViewById(R.id.keypad));
        findViewById(R.id.key_delete).setOnLongClickListener(this);

        if (savedInstanceState != null) {
            expression.set(savedInstanceState.getString(STATE_EXPRESSION, ""));
            if (savedInstanceState.containsKey(STATE_RESULT)) {
                committedResult = savedInstanceState.getDouble(STATE_RESULT);
            }
        }
        render();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_EXPRESSION, expression.text());
        if (committedResult != null) {
            outState.putDouble(STATE_RESULT, committedResult);
        }
    }

    /** Every key carries its meaning in android:tag, so ids stay out of the logic. */
    private void attachKeypadListeners(ViewGroup keypad) {
        for (int i = 0; i < keypad.getChildCount(); i++) {
            View child = keypad.getChildAt(i);
            if (child.getTag() != null) {
                child.setOnClickListener(this);
            }
        }
    }

    @Override
    public void onClick(View view) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        handleKey(String.valueOf(view.getTag()));
        render();
    }

    @Override
    public boolean onLongClick(View view) {
        // Holding backspace clears everything, the way a hardware calculator does.
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        handleKey("clear");
        render();
        return true;
    }

    private void handleKey(String key) {
        errorMessage = null;
        switch (key) {
            case "clear":
                expression.clear();
                committedResult = null;
                return;
            case "delete":
                startEditing();
                expression.backspace();
                return;
            case "equals":
                evaluate();
                return;
            case "sign":
                startEditing();
                expression.toggleSign();
                return;
            case "percent":
                startEditing();
                expression.appendPercent();
                return;
            case ".":
                startFreshIfJustEvaluated();
                expression.appendDot();
                return;
            case "+":
            case "−":
            case "×":
            case "÷":
                startEditing();
                expression.appendOperator(key.charAt(0));
                return;
            default:
                startFreshIfJustEvaluated();
                expression.appendDigit(key.charAt(0));
        }
    }

    /** Continuing with an operator keeps the result as the left-hand side. */
    private void startEditing() {
        committedResult = null;
    }

    /** Typing a number after '=' starts a new calculation instead of appending. */
    private void startFreshIfJustEvaluated() {
        if (committedResult != null) {
            expression.clear();
            committedResult = null;
        }
    }

    private void evaluate() {
        if (expression.isEmpty()) {
            return;
        }
        try {
            double value = Evaluator.eval(expression.text());
            committedResult = value;
            expression.set(Numbers.plain(value));
        } catch (Evaluator.MathException e) {
            errorMessage = e.getMessage();
            committedResult = null;
        } catch (Evaluator.SyntaxException e) {
            errorMessage = getString(R.string.error_incomplete);
            committedResult = null;
        }
    }

    private void render() {
        String shown = committedResult != null
                ? Numbers.format(committedResult)
                : expression.text();
        expressionView.setText(shown.isEmpty() ? "0" : Numbers.group(shown));
        scrollExpressionToEnd();

        if (errorMessage != null) {
            resultView.setTextColor(colorError);
            resultView.setText(errorMessage);
            return;
        }
        if (committedResult != null) {
            resultView.setText("");
            return;
        }
        showPreview();
    }

    /** Shows the running answer under the expression, but only once it means something. */
    private void showPreview() {
        resultView.setTextColor(colorSecondary);
        if (!expression.hasOperation()) {
            resultView.setText("");
            return;
        }
        try {
            double value = Evaluator.eval(expression.text());
            resultView.setText(Numbers.group(Numbers.format(value)));
        } catch (RuntimeException e) {
            resultView.setText(""); // half-typed expressions simply have no preview yet
        }
    }

    private void scrollExpressionToEnd() {
        expressionScroll.post(new Runnable() {
            @Override
            public void run() {
                expressionScroll.fullScroll(View.FOCUS_RIGHT);
            }
        });
    }
}
