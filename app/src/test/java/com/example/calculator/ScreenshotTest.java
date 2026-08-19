package com.example.calculator;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.View;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;

/** Renders the running activity to a PNG so the layout can be looked at. */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ScreenshotTest {

    private static final String OUT_DIR = System.getProperty("screenshot.dir", "build/screenshots");

    private MainActivity launch() {
        return Robolectric.buildActivity(MainActivity.class).setup().get();
    }

    private static void press(Activity activity, int id) {
        activity.findViewById(id).performClick();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    private static void capture(Activity activity, String name) throws Exception {
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        View decor = activity.getWindow().getDecorView();
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;

        decor.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        decor.layout(0, 0, width, height);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        decor.draw(new Canvas(bitmap));

        File dir = new File(OUT_DIR);
        dir.mkdirs();
        File file = new File(dir, name + ".png");
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        System.out.println("SCREENSHOT " + file.getAbsolutePath()
                + " " + width + "x" + height);
    }

    @Test
    @Config(sdk = 34, qualifiers = "w411dp-h891dp-xxhdpi")
    public void portraitWithALiveResult() throws Exception {
        MainActivity activity = launch();
        press(activity, R.id.key_1);
        press(activity, R.id.key_2);
        press(activity, R.id.key_add);
        press(activity, R.id.key_7);
        press(activity, R.id.key_multiply);
        press(activity, R.id.key_3);
        capture(activity, "portrait");
    }

    @Test
    @Config(sdk = 34, qualifiers = "w411dp-h891dp-xxhdpi")
    public void portraitAfterEquals() throws Exception {
        MainActivity activity = launch();
        press(activity, R.id.key_5);
        press(activity, R.id.key_0);
        press(activity, R.id.key_add);
        press(activity, R.id.key_1);
        press(activity, R.id.key_0);
        press(activity, R.id.key_percent);
        press(activity, R.id.key_equals);
        capture(activity, "portrait-result");
    }

    @Test
    @Config(sdk = 34, qualifiers = "w891dp-h411dp-land-xxhdpi")
    public void landscape() throws Exception {
        MainActivity activity = launch();
        press(activity, R.id.key_9);
        press(activity, R.id.key_divide);
        press(activity, R.id.key_4);
        capture(activity, "landscape");
    }
}
