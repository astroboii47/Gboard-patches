package dev.jason.gboardpatches.extension.customtheme;

import android.content.Context;
import android.content.SharedPreferences;
import android.app.AlertDialog;
import android.os.Handler;
import android.os.Looper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import dev.jason.gboardpatches.extension.R;
import dev.jason.gboardpatches.extension.settings.GboardPatchesFeatureAvailability;
import dev.jason.gboardpatches.extension.settings.GboardPatchesSettings;
import dev.jason.gboardpatches.extension.settings.GboardPatchesSettingsContract;
import dev.jason.gboardpatches.extension.settings.GboardSettingsText;
import dev.jason.gboardpatches.extension.spacebarlogo.GboardSpacebarLogoDialogs;

/** Settings page that gates entry to the Custom Theme gallery. */
public final class GboardCustomThemeSettingsFeature
        implements GboardPatchesSettingsContract.Feature {
    private static final String PREVIEW_ASSET =
            "settings-previews/customtheme/enabled.png";

    private final String title;
    private final String summary;
    private final GboardCustomThemeGalleryFeature galleryFeature;

    private static final StockTheme[] STOCK_THEMES = {
            new StockTheme("Classic light", "silk:light"),
            new StockTheme("Classic dark", "silk:dark"),
            new StockTheme("Material 3 light", "silk:material3_light"),
            new StockTheme("Material 3 dark", "silk:material3_dark"),
            new StockTheme("Material light", "assets:theme_package_metadata_material_light.binarypb"),
            new StockTheme("Material dark", "assets:theme_package_metadata_material_dark.binarypb"),
            new StockTheme("Google blue light", "assets:theme_package_metadata_google_blue_light.binarypb"),
            new StockTheme("Google blue dark", "assets:theme_package_metadata_google_blue_dark.binarypb"),
            new StockTheme("Red", "assets:theme_package_metadata_color_red.binarypb"),
            new StockTheme("Green", "assets:theme_package_metadata_color_green.binarypb"),
            new StockTheme("Teal", "assets:theme_package_metadata_color_teal.binarypb"),
            new StockTheme("Blue", "assets:theme_package_metadata_color_blue.binarypb"),
            new StockTheme("Cyan", "assets:theme_package_metadata_color_cyan.binarypb"),
            new StockTheme("Deep purple", "assets:theme_package_metadata_color_deep_purple.binarypb"),
            new StockTheme("Pink", "assets:theme_package_metadata_color_pink.binarypb"),
            new StockTheme("Light pink", "assets:theme_package_metadata_color_light_pink.binarypb"),
            new StockTheme("Sand", "assets:theme_package_metadata_color_sand.binarypb"),
            new StockTheme("Brown", "assets:theme_package_metadata_color_brown.binarypb"),
            new StockTheme("Blue grey", "assets:theme_package_metadata_color_blue_grey.binarypb"),
            new StockTheme("Black", "assets:theme_package_metadata_color_black.binarypb"),
            new StockTheme("Holo blue", "assets:theme_package_metadata_holo_blue.binarypb"),
            new StockTheme("Holo white", "assets:theme_package_metadata_holo_white.binarypb"),
    };

    public GboardCustomThemeSettingsFeature(Context context) {
        title = text(context, R.string.gboard_patches_custom_theme_title);
        summary = text(context, R.string.gboard_patches_custom_theme_summary);
        galleryFeature = new GboardCustomThemeGalleryFeature(context);
    }

    @Override public String getEntryTitle() { return title; }
    @Override public String getEntrySummary() { return summary; }

    @Override
    public boolean isAvailable(Context context) {
        return GboardPatchesFeatureAvailability.hasFeature(context,
                GboardPatchesFeatureAvailability.FEATURE_CUSTOM_THEME);
    }

    @Override
    public List<GboardPatchesSettingsContract.Feature> getNavigationChildren() {
        return Collections.singletonList(galleryFeature);
    }

    @Override
    public GboardPatchesSettingsContract.Screen buildScreen(
            GboardPatchesSettingsContract.FeatureHost host) {
        Context context = host.getContext();
        SharedPreferences preferences = GboardPatchesSettings.preferences(context);
        boolean enabled = GboardCustomThemeSettings.readEnabled(preferences);
        boolean followSystem = GboardCustomThemeSettings.followsSystem(preferences);
        String accent = GboardCustomThemeSettings.accent(preferences);
        String enabledTitle = text(context,
                R.string.gboard_patches_custom_theme_enabled_title);
        return new GboardPatchesSettingsContract.Screen(
                title,
                text(context, R.string.gboard_patches_header_badge),
                title,
                summary,
                Collections.emptyList(),
                Collections.singletonList(new GboardPatchesSettingsContract.Section(
                        text(context, R.string.gboard_patches_custom_theme_section),
                        Arrays.asList(
                                new GboardPatchesSettingsContract.ToggleRow(
                                        enabledTitle,
                                        text(context,
                                                R.string.gboard_patches_custom_theme_enabled_summary),
                                        true,
                                        enabled,
                                        value -> {
                                            GboardCustomThemeSettings.writeEnabled(
                                                    preferences, value);
                                            GboardPatchesSettingsContract.refresh(host);
                                        },
                                        new GboardPatchesSettingsContract.PreviewSpec(
                                                enabledTitle,
                                                "",
                                                new GboardPatchesSettingsContract.PreviewImage(
                                                        PREVIEW_ASSET, ""))),
                                new GboardPatchesSettingsContract.ToggleRow(
                                        "Follow system light/dark mode",
                                        "Use a separate built-in Gboard theme for light and dark mode.",
                                        enabled,
                                        followSystem,
                                        value -> {
                                            GboardCustomThemeSettings.writeFollowsSystem(
                                                    preferences, value);
                                            GboardCustomThemeAutoSwitcher.reconcile(context,
                                                    context.getResources().getConfiguration());
                                            GboardPatchesSettingsContract.refresh(host);
                                        }),
                                new GboardPatchesSettingsContract.SelectorRow(
                                        "Light theme", "Built-in Gboard theme",
                                        GboardCustomThemeSettings.assignedName(preferences, false),
                                        enabled && followSystem,
                                        () -> showStockThemeDialog(host, context, preferences,
                                                false)),
                                new GboardPatchesSettingsContract.SelectorRow(
                                        "Dark theme", "Built-in Gboard theme",
                                        GboardCustomThemeSettings.assignedName(preferences, true),
                                        enabled && followSystem,
                                        () -> showStockThemeDialog(host, context, preferences,
                                                true)),
                                new GboardPatchesSettingsContract.ToggleRow(
                                        "Key borders",
                                        "Show outlined keys with the selected Gboard theme.",
                                        enabled && followSystem,
                                        preferences.getBoolean(
                                                GboardCustomThemeSettings.LIGHT_BORDER_KEY, false),
                                        value -> {
                                            preferences.edit()
                                                    .putBoolean(GboardCustomThemeSettings.LIGHT_BORDER_KEY, value)
                                                    .putBoolean(GboardCustomThemeSettings.DARK_BORDER_KEY, value)
                                                    .commit();
                                            GboardCustomThemeAutoSwitcher.reconcile(context,
                                                    context.getResources().getConfiguration());
                                            GboardPatchesSettingsContract.refresh(host);
                                        }),
                                new GboardPatchesSettingsContract.SelectorRow(
                                        "Custom accent colour",
                                        "Changes action keys for built-in and imported themes. Reopen the keyboard after changing it.",
                                        GboardCustomThemeSettings.DEFAULT_ACCENT.equals(accent)
                                                ? "Theme default" : accent,
                                        enabled,
                                        () -> showAccentDialog(host, context, preferences, accent)),
                                new GboardPatchesSettingsContract.NavigationRow(
                                        text(context,
                                                R.string.gboard_patches_custom_theme_browse_title),
                                        text(context,
                                                R.string.gboard_patches_custom_theme_browse_summary),
                                        enabled,
                                        () -> GboardPatchesSettingsContract.openFeature(
                                                host, galleryFeature))))),
                GboardPatchesSettingsContract.RefreshPolicy.none(),
                GboardPatchesSettingsContract.PanelStyle.FLAT);
    }

    private static void showStockThemeDialog(
            GboardPatchesSettingsContract.FeatureHost host, Context context,
            SharedPreferences preferences, boolean dark) {
        String[] labels = new String[STOCK_THEMES.length];
        int checked = -1;
        String current = preferences.getString(dark ? GboardCustomThemeSettings.DARK_THEME_KEY
                : GboardCustomThemeSettings.LIGHT_THEME_KEY, "");
        for (int index = 0; index < STOCK_THEMES.length; index++) {
            labels[index] = STOCK_THEMES[index].label;
            if (STOCK_THEMES[index].value.equals(current)) checked = index;
        }
        final int selected = checked;
        GboardPatchesSettingsContract.showManagedDialog(host, onDismiss -> {
            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setTitle(dark ? "Dark Gboard theme" : "Light Gboard theme")
                    .setSingleChoiceItems(labels, selected, (ignored, which) -> {
                        StockTheme theme = STOCK_THEMES[which];
                        GboardCustomThemeSettings.assign(preferences, dark, theme.value,
                                theme.label, false);
                        GboardCustomThemeAutoSwitcher.reconcile(context,
                                context.getResources().getConfiguration());
                        GboardPatchesSettingsContract.refresh(host);
                        ignored.dismiss();
                    })
                    .setNegativeButton("Cancel", null)
                    .create();
            dialog.setOnDismissListener(ignored -> onDismiss.run());
            dialog.show();
            return true;
        });
    }

    private static final class StockTheme {
        final String label;
        final String value;
        StockTheme(String label, String value) {
            this.label = label;
            this.value = value;
        }
    }

    private static void showAccentDialog(GboardPatchesSettingsContract.FeatureHost host,
            Context context, SharedPreferences preferences, String current) {
        GboardPatchesSettingsContract.showManagedDialog(host, onDismiss -> {
            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setTitle("Custom theme accent")
                    .setItems(new String[] {"Use theme default", "Choose custom colour"},
                            (ignored, which) -> {
                                if (which == 0) {
                                    GboardCustomThemeSettings.writeAccent(preferences,
                                            GboardCustomThemeSettings.DEFAULT_ACCENT);
                                    GboardPatchesSettingsContract.refresh(host);
                                } else {
                                    new Handler(Looper.getMainLooper()).post(() ->
                                            showCustomAccentDialog(host, context, preferences,
                                                    current));
                                }
                            })
                    .setNegativeButton("Cancel", null)
                    .create();
            dialog.setOnDismissListener(ignored -> onDismiss.run());
            dialog.show();
            return true;
        });
    }

    private static void showCustomAccentDialog(GboardPatchesSettingsContract.FeatureHost host,
            Context context, SharedPreferences preferences, String current) {
        String initial = GboardCustomThemeSettings.DEFAULT_ACCENT.equals(current)
                ? "#4285F4" : current;
        GboardPatchesSettingsContract.showManagedDialog(host,
                GboardSpacebarLogoDialogs.colorPickerDialog(context,
                        "Custom theme accent", "Opacity", "Colour code", "#RRGGBB",
                        "Invalid colour code", initial, value -> {
                            GboardCustomThemeSettings.writeAccent(preferences, value);
                            GboardPatchesSettingsContract.refresh(host);
                        }));
    }

    private static String text(Context context, int id) {
        return GboardSettingsText.get(context, id);
    }
}
