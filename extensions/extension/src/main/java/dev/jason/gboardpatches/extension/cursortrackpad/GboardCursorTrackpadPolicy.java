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
        return stockResult;
    }

}
