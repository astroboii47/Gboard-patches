package dev.jason.gboardpatches.extension.cursortrackpad;

public final class GboardCursorTrackpadPolicy {
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
            return Double.valueOf(0.0d);
        }
        return stockResult;
    }

}
