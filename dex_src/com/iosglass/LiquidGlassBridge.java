package com.iosglass;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

public class LiquidGlassBridge {
    private static final String TAG = "LiquidGlass";
    private Context appContext;
    private Application.ActivityLifecycleCallbacks callbacks;

    public void onLoad(Context ctx, Object unused) {
        Log.i(TAG, "LiquidGlassBridge.onLoad");
        if (!(ctx instanceof Application)) {
            Log.e(TAG, "ctx is not Application: " + ctx);
            return;
        }
        this.appContext = ctx;
        final Application app = (Application) ctx;

        callbacks = new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(Activity a, Bundle b) {}
            @Override public void onActivityStarted(Activity a) {}
            @Override public void onActivityResumed(Activity a) {
                try { Glassifier.attach(a); }
                catch (Throwable t) { Log.e(TAG, "attach fail", t); }
            }
            @Override public void onActivityPaused(Activity a) {
                try { Glassifier.detach(a); }
                catch (Throwable t) { Log.e(TAG, "detach fail", t); }
            }
            @Override public void onActivityStopped(Activity a) {}
            @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
            @Override public void onActivityDestroyed(Activity a) {}
        };
        app.registerActivityLifecycleCallbacks(callbacks);
    }

    public void onUnload() {
        Log.i(TAG, "LiquidGlassBridge.onUnload");
        if (appContext instanceof Application && callbacks != null) {
            ((Application) appContext).unregisterActivityLifecycleCallbacks(callbacks);
            callbacks = null;
        }
        Glassifier.clearAll();
    }
}
