package dev.jason.gboardpatches.extension.cursortrackpad;

import org.junit.Assert;
import org.junit.Test;

public final class GboardCursorTrackpadPolicyTest {
    @Test
    public void enabledForcesBothFlags() {
        Assert.assertEquals(Boolean.TRUE, GboardCursorTrackpadPolicy.maybeForceFlag(
                "free_cursor", Boolean.FALSE, true));
        Assert.assertEquals(Boolean.TRUE, GboardCursorTrackpadPolicy.maybeForceFlag(
                "free_cursor_lock_mode", Boolean.FALSE, true));
        Assert.assertEquals(Double.valueOf(0.0d), GboardCursorTrackpadPolicy.maybeForceFlag(
                "free_cursor_lock_detection_countdown_millisec", Double.valueOf(2000.0d), true));
    }

    @Test
    public void disabledOrUnrelatedPreservesStock() {
        Assert.assertSame(Boolean.FALSE, GboardCursorTrackpadPolicy.maybeForceFlag(
                "free_cursor", Boolean.FALSE, false));
        Double countdown = Double.valueOf(2000.0d);
        Assert.assertSame(countdown, GboardCursorTrackpadPolicy.maybeForceFlag(
                "free_cursor_lock_detection_countdown_millisec", countdown, false));
        Assert.assertEquals("wrong type", GboardCursorTrackpadPolicy.maybeForceFlag(
                "free_cursor_lock_detection_countdown_millisec", "wrong type", true));
        Assert.assertEquals(Double.valueOf(2000.0d), GboardCursorTrackpadPolicy.maybeForceFlag(
                "unrelated", Double.valueOf(2000.0d), true));
    }
}
