package dev.jason.gboardpatches.extension.cursortrackpad;

public final class GboardCursorTrackpadPolicy {
    // A literal zero lets the lock coroutine complete inside the same MotionEvent dispatch,
    // after which the caller can overwrite the locked state. 50 ms still feels immediate,
    // while guaranteeing that the state transition happens after the current event returns.
    static final double FAST_LOCK_COUNTDOWN_MILLIS = 50.0d;
    public static final String FREE_CURSOR_FLAG = "free_cursor";
    public static final String FREE_CURSOR_LOCK_MODE_FLAG = "free_cursor_lock_mode";
    public static final String FREE_CURSOR_LOCK_COUNTDOWN_FLAG =
            "free_cursor_lock_detection_countdown_millisec";
    public static final String FREE_CURSOR_HORIZONTAL_FIRST_STEP_FLAG =
            "free_cursor_horizontal_distance_mm_for_first_step";
    public static final String FREE_CURSOR_HORIZONTAL_STEP_FLAG =
            "free_cursor_horizontal_distance_mm_per_step";
    public static final String FREE_CURSOR_MAX_SPEED_FACTOR_FLAG =
            "free_cursor_advanced_max_speed_factor";

    // Stock uses 1.4 mm per character and can accelerate to 3x. A slightly larger
    // step and lower acceleration ceiling make scrubbing calmer without replacing
    // Gboard's native gesture or touching its lock and Select state machines.
    static final double SMOOTH_HORIZONTAL_STEP_MM = 1.8d;
    static final double SMOOTH_MAX_SPEED_FACTOR = 2.0d;

    private GboardCursorTrackpadPolicy() {
    }

    public static Object maybeForceFlag(
            String flagName,
            Object stockResult,
            boolean enabled) {
        if (!enabled) {
            return stockResult;
        }
        if ((FREE_CURSOR_FLAG.equals(flagName) || FREE_CURSOR_LOCK_MODE_FLAG.equals(flagName))
                && Boolean.FALSE.equals(stockResult)) {
            return Boolean.TRUE;
        }
        if (FREE_CURSOR_LOCK_COUNTDOWN_FLAG.equals(flagName) && stockResult instanceof Double) {
            return Double.valueOf(FAST_LOCK_COUNTDOWN_MILLIS);
        }
        if ((FREE_CURSOR_HORIZONTAL_FIRST_STEP_FLAG.equals(flagName)
                || FREE_CURSOR_HORIZONTAL_STEP_FLAG.equals(flagName))
                && stockResult instanceof Double) {
            return Double.valueOf(SMOOTH_HORIZONTAL_STEP_MM);
        }
        if (FREE_CURSOR_MAX_SPEED_FACTOR_FLAG.equals(flagName) && stockResult instanceof Double) {
            return Double.valueOf(SMOOTH_MAX_SPEED_FACTOR);
        }
        return stockResult;
    }

}
