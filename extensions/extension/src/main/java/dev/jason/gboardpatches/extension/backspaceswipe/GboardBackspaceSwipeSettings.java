package dev.jason.gboardpatches.extension.backspaceswipe;

import android.content.SharedPreferences;

public final class GboardBackspaceSwipeSettings {
    public static final String KEY = "pref_smooth_backspace_swipe_mode";
    public static final int OFF = 0;
    public static final int CHARACTERS = 1;
    public static final int WORDS = 2;

    private GboardBackspaceSwipeSettings() { }

    public static int read(SharedPreferences preferences) {
        return Math.max(OFF, Math.min(WORDS, preferences.getInt(KEY, OFF)));
    }

    public static void write(SharedPreferences preferences, int mode) {
        preferences.edit().putInt(KEY, Math.max(OFF, Math.min(WORDS, mode))).apply();
    }
}
