package dev.jason.gboardpatches.extension.calculator;

import android.content.Context;
import android.content.SharedPreferences;

import dev.jason.gboardpatches.extension.settings.GboardPatchesSettings;

public final class GboardCalculatorSettings {
    public static final String PREF_KEY_ENABLED = "pref_simple_calculator_enabled";
    public static final boolean DEFAULT_ENABLED = false;
    public static final String PREF_KEY_SHORTCODES_ENABLED = "pref_emoji_shortcodes_enabled";
    public static final String PREF_KEY_SHORTCODES_VERTICAL = "pref_emoji_shortcodes_vertical";
    public static final String PREF_KEY_GIF_QUERY_SHORTCUT = "pref_gif_query_shortcut_enabled";

    private GboardCalculatorSettings() {
    }

    public static boolean readEnabled(Context context) {
        if (context == null) {
            return false;
        }
        SharedPreferences preferences = GboardPatchesSettings.preferences(context);
        ensureDefault(preferences);
        return readEnabled(preferences);
    }

    static boolean readEnabled(SharedPreferences preferences) {
        if (preferences == null) {
            return false;
        }
        Object raw = preferences.getAll().get(PREF_KEY_ENABLED);
        if (raw instanceof Boolean value) {
            return value.booleanValue();
        }
        if (raw instanceof String value) {
            if ("true".equalsIgnoreCase(value)) {
                return true;
            }
            if ("false".equalsIgnoreCase(value)) {
                return false;
            }
        }
        return DEFAULT_ENABLED;
    }

    static void ensureDefault(SharedPreferences preferences) {
        if (preferences != null && !preferences.contains(PREF_KEY_ENABLED)) {
            preferences.edit().putBoolean(PREF_KEY_ENABLED, DEFAULT_ENABLED).commit();
        }
    }

    public static boolean writeEnabled(Context context, boolean enabled) {
        if (context == null) {
            return false;
        }
        return GboardPatchesSettings.preferences(context).edit()
                .putBoolean(PREF_KEY_ENABLED, enabled)
                .commit();
    }

    public static boolean readShortcodesEnabled(Context context) {
        return GboardPatchesSettings.readRuntimeSetting(context, preferences -> {
            Object raw = preferences.getAll().get(PREF_KEY_SHORTCODES_ENABLED);
            if (raw instanceof Boolean) return (Boolean) raw;
            if (raw instanceof String) return Boolean.parseBoolean((String) raw);
            return true;
        }, true);
    }

    public static boolean writeShortcodesEnabled(Context context, boolean enabled) {
        return GboardPatchesSettings.writeStringSetting(
                context, PREF_KEY_SHORTCODES_ENABLED, Boolean.toString(enabled));
    }

    public static boolean readShortcodesVertical(Context context) {
        return GboardPatchesSettings.readRuntimeSetting(context, preferences -> {
            Object raw = preferences.getAll().get(PREF_KEY_SHORTCODES_VERTICAL);
            if (raw instanceof Boolean) return (Boolean) raw;
            if (raw instanceof String) return Boolean.parseBoolean((String) raw);
            return true;
        }, true);
    }

    public static boolean writeShortcodesVertical(Context context, boolean enabled) {
        return GboardPatchesSettings.writeStringSetting(
                context, PREF_KEY_SHORTCODES_VERTICAL, Boolean.toString(enabled));
    }

    public static boolean readGifQueryShortcutEnabled(Context context) {
        return GboardPatchesSettings.readRuntimeSetting(context, preferences -> {
            Object raw = preferences.getAll().get(PREF_KEY_GIF_QUERY_SHORTCUT);
            if (raw instanceof Boolean) return (Boolean) raw;
            if (raw instanceof String) return Boolean.parseBoolean((String) raw);
            return true;
        }, true);
    }

    public static boolean writeGifQueryShortcutEnabled(Context context, boolean enabled) {
        return GboardPatchesSettings.writeStringSetting(
                context, PREF_KEY_GIF_QUERY_SHORTCUT, Boolean.toString(enabled));
    }
}
