package androidx.test.runner.intent;

public final class IntentStubberRegistry {
    private static volatile IntentStubber instance;

    private IntentStubberRegistry() {}

    public static IntentStubber getInstance() {
        return instance;
    }

    public static boolean isLoaded() {
        return instance != null;
    }

    public static void load(IntentStubber stubber) {
        instance = stubber;
    }
}
