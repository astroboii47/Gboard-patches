package dev.jason.gboardpatches.extension.customtheme;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.util.Log;

import dev.jason.gboardpatches.extension.settings.GboardPatchesSettings;
import dev.jason.gboardpatches.extension.spacebarlogo.GboardSpacebarLogoSettings;

/** Supplies a user-selected action-key colour without replacing the selected Gboard theme. */
public final class GboardCustomThemeAccentRuntime {
    private static final String TAG = "GboardPatchesAccent";
    private GboardCustomThemeAccentRuntime() { }

    public static ColorStateList overrideActionKeyColor(
            Object inflater, String selector, ColorStateList stock) {
        try {
            if (!".keytop.for-action-key".equals(selector)
                    || !(inflater instanceof LayoutInflater)) return stock;
            SharedPreferences preferences = GboardPatchesSettings.preferences(
                    ((LayoutInflater) inflater).getContext());
            if (!GboardCustomThemeSettings.readEnabled(preferences)) return stock;
            String value = GboardCustomThemeSettings.accent(preferences);
            if (GboardCustomThemeSettings.DEFAULT_ACCENT.equals(value)) return stock;
            String normalized = GboardSpacebarLogoSettings.tryNormalizeColorHex(value);
            Log.i(TAG, "accent=" + value + " normalized=" + normalized);
            return normalized == null ? stock : ColorStateList.valueOf(Color.parseColor(normalized));
        } catch (Throwable failure) {
            Log.w(TAG, "accent hook failed", failure);
            return stock;
        }
    }
}
