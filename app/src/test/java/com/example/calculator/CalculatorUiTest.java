package com.example.calculator;

import static org.junit.Assert.assertEquals;

import android.app.Activity;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

/** Drives the real activity through the real Android framework, on the JVM. */
@RunWith(org.robolectric.RobolectricTestRunner.class)
@Config(sdk = 34, qualifiers = "w411dp-h891dp-xxhdpi")
public class CalculatorUiTest {

    private MainActivity launch() {
        ActivityController<MainActivity> controller =
                Robolectric.buildActivity(MainActivity.class).setup();
        return controller.get();
    }

    private static void press(Activity activity, int id) {
        View key = activity.findViewById(id);
        if (key == null) {
            throw new AssertionError("no key with id " + id);
        }
        key.performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    private static String expression(Activity activity) {
        return ((TextView) activity.findViewById(R.id.expression)).getText().toString();
    }

    private static String result(Activity activity) {
        return ((TextView) activity.findViewById(R.id.result)).getText().toString();
    }

    @Test
    public void startsAtZero() {
        MainActivity activity = launch();
        assertEquals("0", expression(activity));
        assertEquals("", result(activity));
    }

    @Test
    public void computesWithPrecedence() {
        MainActivity activity = launch();
        press(activity, R.id.key_1);
        press(activity, R.id.key_2);
        press(activity, R.id.key_add);
        press(activity, R.id.key_7);
        press(activity, R.id.key_multiply);
        press(activity, R.id.key_3);
        assertEquals("12+7×3", expression(activity));
        assertEquals("33", result(activity));   // live preview
        press(activity, R.id.key_equals);
        assertEquals("33", expression(activity));
        assertEquals("", result(activity));
    }

    @Test
    public void appliesPercentToTheLeftHandSide() {
        MainActivity activity = launch();
        press(activity, R.id.key_5);
        press(activity, R.id.key_0);
        press(activity, R.id.key_add);
        press(activity, R.id.key_1);
        press(activity, R.id.key_0);
        press(activity, R.id.key_percent);
        assertEquals("50+10%", expression(activity));
        press(activity, R.id.key_equals);
        assertEquals("55", expression(activity));
    }

    @Test
    public void reportsDivisionByZero() {
        MainActivity activity = launch();
        press(activity, R.id.key_5);
        press(activity, R.id.key_divide);
        press(activity, R.id.key_0);
        press(activity, R.id.key_equals);
        assertEquals("Can't divide by zero", result(activity));
    }

    @Test
    public void clearsAndTogglesSign() {
        MainActivity activity = launch();
        press(activity, R.id.key_9);
        press(activity, R.id.key_clear);
        assertEquals("0", expression(activity));
        press(activity, R.id.key_9);
        press(activity, R.id.key_sign);
        assertEquals("−9", expression(activity));
    }

    @Test
    public void groupsLongResults() {
        MainActivity activity = launch();
        press(activity, R.id.key_9);
        press(activity, R.id.key_9);
        press(activity, R.id.key_9);
        press(activity, R.id.key_9);
        press(activity, R.id.key_9);
        press(activity, R.id.key_multiply);
        press(activity, R.id.key_9);
        press(activity, R.id.key_equals);
        assertEquals("899,991", expression(activity));
    }
}
