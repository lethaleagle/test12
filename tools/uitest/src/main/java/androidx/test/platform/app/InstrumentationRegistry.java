package androidx.test.platform.app;

import android.app.Instrumentation;
import android.os.Bundle;

/** Minimal stand-in for androidx.test:monitor, which is not on Maven Central. */
public final class InstrumentationRegistry {
    private static volatile Instrumentation instrumentation;
    private static volatile Bundle arguments;

    private InstrumentationRegistry() {}

    public static Instrumentation getInstrumentation() {
        Instrumentation current = instrumentation;
        if (current == null) {
            throw new IllegalStateException("No instrumentation registered");
        }
        return current;
    }

    public static void registerInstance(Instrumentation newInstrumentation, Bundle newArguments) {
        instrumentation = newInstrumentation;
        arguments = newArguments;
    }

    public static Bundle getArguments() {
        return arguments == null ? new Bundle() : arguments;
    }
}
