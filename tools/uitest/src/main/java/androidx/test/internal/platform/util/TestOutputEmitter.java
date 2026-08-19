package androidx.test.internal.platform.util;

public final class TestOutputEmitter {
    private TestOutputEmitter() {}

    public static void dumpThreadStates(String outputFile) {
        // Diagnostics only; nothing to emit outside a real instrumentation run.
    }

    public static void captureWindowHierarchy(String outputFile) {
    }

    public static void takeScreenshot(String outputFile) {
    }
}
