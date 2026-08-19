package androidx.test.espresso;

import android.os.Looper;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public final class IdlingRegistry {
    private static final IdlingRegistry INSTANCE = new IdlingRegistry();

    private final Set<IdlingResource> resources = new CopyOnWriteArraySet<>();
    private final Set<Looper> loopers = new CopyOnWriteArraySet<>();

    private IdlingRegistry() {}

    public static IdlingRegistry getInstance() {
        return INSTANCE;
    }

    public Collection<IdlingResource> getResources() {
        return Collections.unmodifiableSet(resources);
    }

    public Collection<Looper> getLoopers() {
        return Collections.unmodifiableSet(loopers);
    }

    public boolean register(IdlingResource... toAdd) {
        return Collections.addAll(resources, toAdd);
    }

    public boolean unregister(IdlingResource... toRemove) {
        return resources.removeAll(java.util.Arrays.asList(toRemove));
    }
}
