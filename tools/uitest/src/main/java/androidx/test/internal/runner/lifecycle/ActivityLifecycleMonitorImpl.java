package androidx.test.internal.runner.lifecycle;

import android.app.Activity;

import androidx.test.runner.lifecycle.ActivityLifecycleCallback;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitor;
import androidx.test.runner.lifecycle.Stage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ActivityLifecycleMonitorImpl implements ActivityLifecycleMonitor {
    private final List<ActivityLifecycleCallback> callbacks = new CopyOnWriteArrayList<>();
    private final Map<Activity, Stage> stages = new WeakHashMap<>();

    @Override
    public void addLifecycleCallback(ActivityLifecycleCallback callback) {
        callbacks.add(callback);
    }

    @Override
    public void removeLifecycleCallback(ActivityLifecycleCallback callback) {
        callbacks.remove(callback);
    }

    @Override
    public Stage getLifecycleStageOf(Activity activity) {
        Stage stage = stages.get(activity);
        return stage == null ? Stage.PRE_ON_CREATE : stage;
    }

    @Override
    public Collection<Activity> getActivitiesInStage(Stage stage) {
        List<Activity> matching = new ArrayList<>();
        for (Map.Entry<Activity, Stage> entry : stages.entrySet()) {
            if (entry.getValue() == stage) {
                matching.add(entry.getKey());
            }
        }
        return matching;
    }

    public void signalLifecycleChange(Stage stage, Activity activity) {
        stages.put(activity, stage);
        for (ActivityLifecycleCallback callback : callbacks) {
            callback.onActivityLifecycleChanged(activity, stage);
        }
    }
}
