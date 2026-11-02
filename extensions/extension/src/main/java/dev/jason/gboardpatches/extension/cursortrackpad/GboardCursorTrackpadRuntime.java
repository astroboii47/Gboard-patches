package dev.jason.gboardpatches.extension.cursortrackpad;

import android.content.SharedPreferences;
import android.view.MotionEvent;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

import dev.jason.gboardpatches.extension.flagsettings.GboardFlagRuntimeContext;
import dev.jason.gboardpatches.extension.settings.GboardPatchesSettings;

public final class GboardCursorTrackpadRuntime {
    private static final String TAG = "GboardPatchesTrackpad";
    private static final Map<Object, Boolean> TWO_FINGER_SELECTIONS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Object, Boolean> SCRUB_SELECTIONS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Object, View> SCRUB_SELECT_BUTTONS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private GboardCursorTrackpadRuntime() {
    }

    public static boolean applyBooleanPreference(String key, boolean stock) {
        if (!"enable_scrub_move".equals(key) || stock) return stock;
        return isEnabledOrStock(stock);
    }

    public static boolean applyResourceBooleanPreference(int key, boolean stock) {
        if (key != 0x7f14096b || stock) return stock;
        return isEnabledOrStock(stock);
    }

    private static boolean isEnabledOrStock(boolean stock) {
        try {
            SharedPreferences prefs = GboardFlagRuntimeContext.preferencesOrNull();
            return applyPreference(stock, prefs);
        } catch (Throwable ignored) { return stock; }
    }

    static boolean applyPreference(boolean stock, SharedPreferences prefs) {
        return prefs == null ? stock : stock || GboardCursorTrackpadSettings.readEnabled(prefs);
    }

    public static Object applyOverriddenFlagValue(String flagName, Object stockResult) {
        try {
            if (!(GboardCursorTrackpadPolicy.FREE_CURSOR_FLAG.equals(flagName)
                    || GboardCursorTrackpadPolicy.FREE_CURSOR_LOCK_MODE_FLAG.equals(flagName)
                    || GboardCursorTrackpadPolicy.FREE_CURSOR_LOCK_COUNTDOWN_FLAG.equals(flagName))) {
                return stockResult;
            }
            SharedPreferences preferences = GboardFlagRuntimeContext.preferencesOrNull();
            return preferences == null
                    ? stockResult
                    : applyOverriddenFlagValue(flagName, stockResult, preferences);
        } catch (Throwable ignored) {
            return stockResult;
        }
    }

    static Object applyOverriddenFlagValue(
            String flagName,
            Object stockResult,
            SharedPreferences preferences) {
        return GboardCursorTrackpadPolicy.maybeForceFlag(
                flagName,
                stockResult,
                GboardCursorTrackpadSettings.readEnabled(preferences));
    }

    /**
     * Normal Free Cursor has no Select button.  Its locked mode implements that
     * button by sending Shift down/up, so reuse exactly that supported IME path
     * while a second pointer is resting on the normal trackpad.
     */
    public static void updateTwoFingerSelection(Object keyboard, Object event) {
        if (keyboard == null || event == null) return;
        try {
            boolean enabled = isFeatureEnabled(keyboard);
            boolean normalMode = isNormalMode(keyboard);
            MotionEvent motionEvent = motionEvent(event);
            boolean select = enabled && normalMode && motionEvent != null
                    && motionEvent.getPointerCount() >= 2
                    && motionEvent.getActionMasked() != MotionEvent.ACTION_UP
                    && motionEvent.getActionMasked() != MotionEvent.ACTION_CANCEL;
            if (motionEvent != null) {
                Log.i(TAG, "twoFinger enabled=" + enabled + " normal=" + normalMode
                        + " pointers=" + motionEvent.getPointerCount()
                        + " action=" + motionEvent.getActionMasked() + " select=" + select);
            }
            updateSelectionKey(keyboard, select);
        } catch (Throwable failure) {
            Log.w(TAG, "two-finger selection hook failed", failure);
            // Trackpad input must remain fully usable if reflection ever drifts.
        }
    }

    /** Normal spacebar scrub uses a separate MotionEventHandler, so it needs its own bridge. */
    public static void updateScrubSelection(Object handler, MotionEvent event) {
        if (handler == null || event == null) return;
        try {
            if (!isFeatureEnabledFromHandler(handler)) return;
            if (isScrubActive(handler)) ensureSelectButton(handler);
            if (event.getActionMasked() == MotionEvent.ACTION_UP
                    || event.getActionMasked() == MotionEvent.ACTION_CANCEL) removeSelectButton(handler);
        } catch (Throwable failure) {
            Log.w(TAG, "scrub selection hook failed", failure);
        }
    }

    /** Makes normal mode expose Gboard's existing locked-mode Select control. */
    public static void showNormalModeSelect(Object keyboard, Object mode) {
        try {
            if (keyboard == null || mode == null
                    || !"defpackage.ont".equals(mode.getClass().getName())) return;
            Field controls = keyboard.getClass().getDeclaredField("K");
            controls.setAccessible(true);
            Object controlsView = controls.get(keyboard);
            Field select = keyboard.getClass().getDeclaredField("N");
            select.setAccessible(true);
            Object selectView = select.get(keyboard);

            // Stock normal mode hides the entire locked-trackpad controls host (K),
            // so revealing N alone has no visible effect. Keep the host present and
            // expose only its native Select control. N retains Gboard's own listener,
            // styling, and press-and-hold selection behavior.
            if (controlsView instanceof View) ((View) controlsView).setVisibility(View.VISIBLE);
            if (selectView instanceof View) ((View) selectView).setVisibility(View.VISIBLE);

            // Do not expose the other locked-mode controls in normal mode.
            for (String fieldName : new String[] { "O", "P", "Q", "R", "S" }) {
                Field field = keyboard.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                Object value = field.get(keyboard);
                if (value instanceof View) ((View) value).setVisibility(View.GONE);
            }
            Log.i(TAG, "normal Select visible host=" + (controlsView instanceof View)
                    + " control=" + (selectView instanceof View));
        } catch (Throwable failure) {
            Log.w(TAG, "normal Select control hook failed", failure);
        }
    }

    private static boolean isScrubActive(Object handler) {
        Object active = findFieldValue(handler, "q");
        return active instanceof Boolean && ((Boolean) active).booleanValue();
    }

    private static void ensureSelectButton(Object handler) {
        if (SCRUB_SELECT_BUTTONS.containsKey(handler)) return;
        Object view = findFieldValue(handler, "d");
        if (!(view instanceof ViewGroup)) return;
        ViewGroup root = (ViewGroup) view;
        Button button = new Button(root.getContext());
        button.setText("Select");
        button.setAllCaps(false);
        button.setOnTouchListener((ignored, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) updateDelegateShiftQuietly(handler, true);
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                updateDelegateShiftQuietly(handler, false);
            }
            return true;
        });
        int size = (int) (56 * root.getResources().getDisplayMetrics().density + 0.5f);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size,
                android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL);
        params.rightMargin = (int) (12 * root.getResources().getDisplayMetrics().density + 0.5f);
        root.addView(button, params);
        SCRUB_SELECT_BUTTONS.put(handler, button);
    }

    private static void removeSelectButton(Object handler) {
        updateDelegateShiftQuietly(handler, false);
        View button = SCRUB_SELECT_BUTTONS.remove(handler);
        if (button != null && button.getParent() instanceof ViewGroup) {
            ((ViewGroup) button.getParent()).removeView(button);
        }
    }

    private static void updateDelegateShiftQuietly(Object handler, boolean select) {
        try { updateDelegateShift(handler, select); } catch (Throwable failure) {
            Log.w(TAG, "select button dispatch failed", failure);
        }
    }

    private static boolean isFeatureEnabledFromHandler(Object handler) {
        Object context = findFieldValue(handler, "o");
        if (context instanceof android.content.Context) {
            return GboardCursorTrackpadSettings.readEnabled(
                    GboardPatchesSettings.preferences((android.content.Context) context));
        }
        return false;
    }

    private static void updateDelegateShift(Object handler, boolean select) throws Exception {
        Boolean current = SCRUB_SELECTIONS.get(handler);
        if (current == null && !select) return;
        if (current != null && current.booleanValue() == select) return;
        Object delegate = findDelegate(handler, 0);
        if (delegate == null) return;
        Method dispatch = delegate.getClass().getMethod("D", KeyEvent.class);
        long now = android.os.SystemClock.uptimeMillis();
        dispatch.invoke(delegate, new KeyEvent(now, now, select ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP,
                KeyEvent.KEYCODE_SHIFT_LEFT, 0, 0, -1, 0, 6, 257));
        if (select) SCRUB_SELECTIONS.put(handler, Boolean.TRUE);
        else SCRUB_SELECTIONS.remove(handler);
    }

    private static Object findDelegate(Object value, int depth) {
        if (value == null || depth > 5) return null;
        for (Class<?> type = value.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object candidate = field.get(value);
                    if (candidate == null || candidate == value) continue;
                    try { candidate.getClass().getMethod("D", KeyEvent.class); return candidate; }
                    catch (NoSuchMethodException ignored) { }
                    Object nested = findDelegate(candidate, depth + 1);
                    if (nested != null) return nested;
                } catch (Throwable ignored) { }
            }
        }
        return null;
    }

    private static Object findFieldValue(Object value, String name) {
        for (Class<?> type = value.getClass(); type != null; type = type.getSuperclass()) {
            try { Field field = type.getDeclaredField(name); field.setAccessible(true); return field.get(value); }
            catch (Throwable ignored) { }
        }
        return null;
    }

    private static MotionEvent motionEvent(Object event) throws Exception {
        Method method = event.getClass().getMethod("h", Class.class, int.class);
        Object value = method.invoke(event, MotionEvent.class, -10239);
        return value instanceof MotionEvent ? (MotionEvent) value : null;
    }

    private static boolean isFeatureEnabled(Object keyboard) {
        try {
            Class<?> type = keyboard.getClass();
            while (type != null) {
                try {
                    Field contextField = type.getDeclaredField("z");
                    contextField.setAccessible(true);
                    Object context = contextField.get(keyboard);
                    if (context instanceof android.content.Context) {
                        SharedPreferences preferences = GboardPatchesSettings.preferences(
                                (android.content.Context) context);
                        return GboardCursorTrackpadSettings.readEnabled(preferences);
                    }
                } catch (NoSuchFieldException ignored) {
                    type = type.getSuperclass();
                    continue;
                }
                break;
            }
        } catch (Throwable ignored) {
            // Keep the existing flag-context route as a compatibility fallback.
        }
        SharedPreferences preferences = GboardFlagRuntimeContext.preferencesOrNull();
        return preferences != null && GboardCursorTrackpadSettings.readEnabled(preferences);
    }

    private static boolean isNormalMode(Object keyboard) throws Exception {
        Field field = keyboard.getClass().getField("i");
        Object mode = field.get(keyboard);
        return mode != null && "defpackage.ont".equals(mode.getClass().getName());
    }

    private static void updateSelectionKey(Object keyboard, boolean select) throws Exception {
        Boolean current = TWO_FINGER_SELECTIONS.get(keyboard);
        // Never send M() unless this runtime previously sent K().  Locked mode owns its
        // Select button and uses those same methods, so touching its events must be a no-op.
        if (current == null && !select) return;
        if (current != null && current.booleanValue() == select) return;
        Method method = keyboard.getClass().getMethod(select ? "K" : "M");
        method.invoke(keyboard);
        if (select) {
            TWO_FINGER_SELECTIONS.put(keyboard, Boolean.TRUE);
        } else {
            TWO_FINGER_SELECTIONS.remove(keyboard);
        }
    }

}
