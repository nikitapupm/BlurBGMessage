package com.iosglass;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

public class Glassifier {
    private static final String TAG = "LiquidGlass";
    private static final Map<View, Drawable> originals = new WeakHashMap<>();
    private static final Map<Activity, ViewTreeObserver.OnGlobalLayoutListener> listeners = new WeakHashMap<>();
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static boolean sweepPending = false;

    public static void attach(Activity activity) {
        if (listeners.containsKey(activity)) return;
        View decor = activity.getWindow().getDecorView();
        ViewTreeObserver.OnGlobalLayoutListener l = () -> scheduleSweep(activity);
        decor.getViewTreeObserver().addOnGlobalLayoutListener(l);
        listeners.put(activity, l);
        scheduleSweep(activity);
    }

    public static void detach(Activity activity) {
        View decor = activity.getWindow().getDecorView();
        ViewTreeObserver.OnGlobalLayoutListener l = listeners.remove(activity);
        if (l != null && decor.getViewTreeObserver().isAlive()) {
            decor.getViewTreeObserver().removeOnGlobalLayoutListener(l);
        }
        restoreRecursive(decor);
    }

    private static void scheduleSweep(final Activity activity) {
        if (sweepPending) return;
        sweepPending = true;
        handler.postDelayed(() -> {
            sweepPending = false;
            try { sweep(activity); }
            catch (Throwable t) { Log.e(TAG, "sweep error", t); }
        }, 200);
    }

    private static void sweep(Activity activity) {
        View decor = activity.getWindow().getDecorView();
        boolean isDark = isDarkTheme(activity);
        float density = activity.getResources().getDisplayMetrics().density;
        walk(decor, isDark, density);
    }

    private static boolean isDarkTheme(Context ctx) {
        int mode = ctx.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }

    private static void walk(View v, boolean isDark, float density) {
        if (v == null) return;
        if (shouldGlassify(v)) {
            try { installGlass(v, isDark, density); }
            catch (Throwable t) { Log.w(TAG, "glass fail " + v.getClass().getSimpleName() + ": " + t); }
        }
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) walk(vg.getChildAt(i), isDark, density);
        }
    }

    private static boolean shouldGlassify(View v) {
        if (v == null) return false;
        if (originals.containsKey(v)) return false;
        if (!v.isClickable()) return false;
        if (v.getBackground() == null) return false;
        if (v.getAlpha() < 0.5f) return false;
        if (v.getVisibility() != View.VISIBLE) return false;
        String cn = v.getClass().getName().toLowerCase();
        if (cn.contains("recycler") || cn.contains("listview") || cn.contains("viewpager")) return false;
        if (cn.contains("edittext")) return false;
        if (cn.contains("chatmessagecell") || cn.endsWith("messagecell")) return false;
        if (cn.contains("imageview") && !cn.contains("button")) return false;
        if (cn.contains("photo") || cn.contains("animatedimage")) return false;
        if (cn.contains("decorview") || cn.endsWith("framelayout")) return false;
        int w = v.getWidth(), h = v.getHeight();
        float density = v.getResources().getDisplayMetrics().density;
        if (w > 0 && h > 0 && (w < 20 * density || h < 18 * density)) return false;
        if (w > 500 * density) return false;
        return true;
    }

    private static void installGlass(View v, boolean isDark, float density) {
        Drawable orig = v.getBackground();
        if (orig == null) return;
        originals.put(v, orig);
        int w = v.getWidth(), h = v.getHeight();
        float radiusPx = Math.min(w, h) / 2f;
        float maxR = 22f * density;
        if (radiusPx > maxR) radiusPx = maxR;
        if (radiusPx < 6f * density) radiusPx = 6f * density;
        GlassDrawable glass = new GlassDrawable(isDark, radiusPx, density);
        PreservePaddingLayerDrawable layered = new PreservePaddingLayerDrawable(orig, glass);
        v.setBackground(layered);
        v.invalidate();
    }

    private static void restoreRecursive(View v) {
        if (v == null) return;
        Drawable orig = originals.remove(v);
        if (orig != null) {
            v.setBackground(orig);
            v.invalidate();
        }
        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) restoreRecursive(vg.getChildAt(i));
        }
    }

    public static void clearAll() {
        for (Iterator<Map.Entry<View, Drawable>> it = originals.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<View, Drawable> e = it.next();
            View v = e.getKey();
            if (v != null) {
                try { v.setBackground(e.getValue()); v.invalidate(); } catch (Throwable ignored) {}
            }
            it.remove();
        }
        listeners.clear();
    }
}
