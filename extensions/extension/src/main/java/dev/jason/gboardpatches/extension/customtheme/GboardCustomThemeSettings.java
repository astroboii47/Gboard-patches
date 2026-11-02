package dev.jason.gboardpatches.extension.customtheme;

import android.content.SharedPreferences;

/** Runtime setting for the Custom Theme feature. */
public final class GboardCustomThemeSettings {
    static final String ENABLED_KEY = "pref_custom_theme_enabled";
    static final boolean DEFAULT_ENABLED = false;
    static final String FOLLOW_SYSTEM_KEY = "pref_custom_theme_follow_system";
    // Values are native Gboard theme identifiers (silk:/assets:), not imported files.
    // Keep the old preference names so existing installations migrate harmlessly: a legacy
    // filename has no supported prefix and is simply ignored until the user chooses a slot.
    static final String LIGHT_THEME_KEY = "pref_custom_theme_light_file";
    static final String DARK_THEME_KEY = "pref_custom_theme_dark_file";
    static final String LIGHT_BORDER_KEY = "pref_custom_theme_light_border";
    static final String DARK_BORDER_KEY = "pref_custom_theme_dark_border";
    static final String LIGHT_NAME_KEY = "pref_custom_theme_light_name";
    static final String DARK_NAME_KEY = "pref_custom_theme_dark_name";
    static final String ACCENT_KEY = "pref_custom_theme_accent";
    static final String DEFAULT_ACCENT = "auto";

    private GboardCustomThemeSettings() {
    }

    static boolean readEnabled(SharedPreferences preferences) {
        if (preferences == null) {
            return DEFAULT_ENABLED;
        }
        try {
            Object raw = preferences.getAll().get(ENABLED_KEY);
            if (raw instanceof Boolean value) {
                return value;
            }
            if (raw instanceof String value) {
                return Boolean.parseBoolean(value.trim());
            }
        } catch (Throwable ignored) {
            // Invalid or unavailable preferences fail closed.
        }
        return DEFAULT_ENABLED;
    }

    static boolean writeEnabled(SharedPreferences preferences, boolean enabled) {
        return preferences != null && preferences.edit().putBoolean(ENABLED_KEY, enabled).commit();
    }

    static boolean followsSystem(SharedPreferences preferences) {
        return preferences != null && preferences.getBoolean(FOLLOW_SYSTEM_KEY, false);
    }

    static boolean writeFollowsSystem(SharedPreferences preferences, boolean enabled) {
        return preferences != null && preferences.edit().putBoolean(FOLLOW_SYSTEM_KEY, enabled)
                .commit();
    }

    static String accent(SharedPreferences preferences) {
        return preferences == null ? DEFAULT_ACCENT
                : preferences.getString(ACCENT_KEY, DEFAULT_ACCENT);
    }

    static boolean writeAccent(SharedPreferences preferences, String accent) {
        return preferences != null && preferences.edit().putString(ACCENT_KEY,
                accent == null || accent.isEmpty() ? DEFAULT_ACCENT : accent).commit();
    }

    static String assignedName(SharedPreferences preferences, boolean dark) {
        if (preferences == null) return "Not selected";
        return preferences.getString(dark ? DARK_NAME_KEY : LIGHT_NAME_KEY, "Not selected");
    }

    static boolean assign(SharedPreferences preferences, boolean dark, String themeValue,
            String displayName, boolean keyBorder) {
        if (preferences == null || themeValue == null || themeValue.isEmpty()) return false;
        return preferences.edit()
                .putString(dark ? DARK_THEME_KEY : LIGHT_THEME_KEY, themeValue)
                .putString(dark ? DARK_NAME_KEY : LIGHT_NAME_KEY, displayName)
                .putBoolean(dark ? DARK_BORDER_KEY : LIGHT_BORDER_KEY, keyBorder)
                .commit();
    }
}
