package dev.jason.gboardpatches.extension.backspaceswipe;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import dev.jason.gboardpatches.extension.settings.GboardPatchesSettings;

/** FUTO-like fixed-distance backspace staging, while retaining Gboard's commit/cancel flow. */
public final class GboardBackspaceSwipeRuntime {
    private static final String TAG = "GboardSmoothDelete";
    private static final Map<Object, float[]> ORIGINAL_THRESHOLDS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private GboardBackspaceSwipeRuntime() { }

    public static void configure(Object handler) {
        try {
            int mode = mode();
            Object config = field(handler, "g");
            if (config == null) return;
            Field thresholds = config.getClass().getDeclaredField("h");
            thresholds.setAccessible(true);
            float[] current = (float[]) thresholds.get(config);
            ORIGINAL_THRESHOLDS.putIfAbsent(handler, current == null ? null : current.clone());
            if (mode == GboardBackspaceSwipeSettings.OFF) {
                float[] original = ORIGINAL_THRESHOLDS.get(handler);
                if (original != null) thresholds.set(config, original.clone());
                return;
            }
            Context context = context(handler);
            float density = context == null ? 1f : context.getResources().getDisplayMetrics().density;
            float stepDp = mode == GboardBackspaceSwipeSettings.CHARACTERS ? 6f : 40f;
            thresholds.set(config, new float[] { stepDp * density });
        } catch (Throwable failure) {
            Log.w(TAG, "configure failed", failure);
        }
    }

    /** Returns true when character-mode selection replaced ozv.e(int). */
    public static boolean handleCharacterStep(Object scrubHandler, int requested) {
        if (mode() != GboardBackspaceSwipeSettings.CHARACTERS) return false;
        try {
            int next = Math.min(0, requested);
            Field currentField = scrubHandler.getClass().getDeclaredField("h");
            currentField.setAccessible(true);
            int current = currentField.getInt(scrubHandler);
            Object editor = field(scrubHandler, "d");
            if (editor == null) return false;
            invoke(editor, "b");
            if (current == 0 && next != 0) invoke(editor, "j");
            int delta = next - current;
            if (delta != 0) invoke(editor, "n", new Class<?>[] { int.class, int.class }, delta, 0);
            if (current != 0 && next == 0) {
                int start = intField(scrubHandler, "i");
                int end = intField(scrubHandler, "j");
                if (start > 0 || end > 0) {
                    Method restore = find(editor.getClass(), "s", 3);
                    restore.invoke(editor, start, end, null);
                }
            }
            invoke(editor, "i");
            currentField.setInt(scrubHandler, next);
            return true;
        } catch (Throwable failure) {
            Log.w(TAG, "character step failed; using stock", failure);
            return false;
        }
    }

    private static int mode() {
        try {
            Application app = (Application) Class.forName("android.app.ActivityThread")
                    .getMethod("currentApplication").invoke(null);
            if (app == null) return GboardBackspaceSwipeSettings.OFF;
            return GboardPatchesSettings.readRuntimeSetting(
                    app,
                    GboardBackspaceSwipeSettings::read,
                    GboardBackspaceSwipeSettings.OFF);
        } catch (Throwable ignored) {
            return GboardBackspaceSwipeSettings.OFF;
        }
    }

    private static Context context(Object value) throws Exception {
        Class<?> type = value.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField("o");
                field.setAccessible(true);
                Object result = field.get(value);
                return result instanceof Context ? (Context) result : null;
            } catch (NoSuchFieldException ignored) { type = type.getSuperclass(); }
        }
        return null;
    }

    private static Object field(Object owner, String name) throws Exception {
        Class<?> type = owner.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(owner);
            } catch (NoSuchFieldException ignored) { type = type.getSuperclass(); }
        }
        return null;
    }

    private static int intField(Object owner, String name) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(owner);
    }

    private static Method find(Class<?> type, String name, int parameters) throws Exception {
        while (type != null) {
            for (Method method : type.getDeclaredMethods()) {
                if (method.getName().equals(name) && method.getParameterTypes().length == parameters) {
                    method.setAccessible(true);
                    return method;
                }
            }
            type = type.getSuperclass();
        }
        throw new NoSuchMethodException(name);
    }

    private static Object invoke(Object owner, String name) throws Exception {
        return find(owner.getClass(), name, 0).invoke(owner);
    }

    private static Object invoke(Object owner, String name, Class<?>[] ignored, Object... args)
            throws Exception {
        return find(owner.getClass(), name, args.length).invoke(owner, args);
    }
}
