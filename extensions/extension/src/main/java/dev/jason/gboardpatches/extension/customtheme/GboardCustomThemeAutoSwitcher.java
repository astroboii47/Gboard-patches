package dev.jason.gboardpatches.extension.customtheme;

import android.content.ComponentCallbacks;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import dev.jason.gboardpatches.extension.settings.GboardPatchesSettings;

/** Keeps Gboard's active stock-theme preference aligned with Android night mode. */
public final class GboardCustomThemeAutoSwitcher {
    private static boolean registered;

    private GboardCustomThemeAutoSwitcher() { }

    public static synchronized void register(Context context) {
        if (registered || context == null) return;
        Context app = context.getApplicationContext() == null
                ? context : context.getApplicationContext();
        registered = true;
        reconcile(app, app.getResources().getConfiguration());
        app.registerComponentCallbacks(new ComponentCallbacks() {
            @Override public void onConfigurationChanged(Configuration configuration) {
                reconcile(app, configuration);
            }
            @Override public void onLowMemory() { }
        });
    }

    static boolean reconcile(Context context, Configuration configuration) {
        try {
            ThemeSelection selection = GboardPatchesSettings.readRuntimeSetting(
                    context,
                    settings -> readSelection(settings, configuration),
                    null);
            if (selection == null) return false;
            String theme = selection.theme;
            if (!isStockThemeValue(theme)) return false;
            boolean border = selection.border;
            SharedPreferences target = targetPreferences(context);
            if (theme.equals(target.getString("additional_keyboard_theme", ""))
                    && border == target.getBoolean("enable_key_border", false)) return false;
            return target.edit().putString("additional_keyboard_theme", theme)
                    .putBoolean("enable_key_border", border).commit();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static ThemeSelection readSelection(
            SharedPreferences settings, Configuration configuration) {
        if (!GboardCustomThemeSettings.readEnabled(settings)
                || !GboardCustomThemeSettings.followsSystem(settings)) return null;
        boolean dark = (configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        return new ThemeSelection(
                settings.getString(dark ? GboardCustomThemeSettings.DARK_THEME_KEY
                        : GboardCustomThemeSettings.LIGHT_THEME_KEY, ""),
                settings.getBoolean(dark ? GboardCustomThemeSettings.DARK_BORDER_KEY
                        : GboardCustomThemeSettings.LIGHT_BORDER_KEY, false));
    }

    private static final class ThemeSelection {
        final String theme;
        final boolean border;

        ThemeSelection(String theme, boolean border) {
            this.theme = theme;
            this.border = border;
        }
    }

    static boolean isStockThemeValue(String value) {
        return value != null && (value.startsWith("silk:")
                || value.startsWith("assets:theme_package_metadata_"));
    }

    static SharedPreferences targetPreferences(Context context) {
        Context storage = context;
        try {
            Context device = context.createDeviceProtectedStorageContext();
            if (device != null) storage = device;
        } catch (Throwable ignored) {
            // Unit-test/legacy environments may not expose device-protected storage.
        }
        return storage.getSharedPreferences(
                context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
    }
}
