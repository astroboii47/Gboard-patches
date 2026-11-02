package dev.jason.gboardpatches.extension.backspaceswipe;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;

import dev.jason.gboardpatches.extension.settings.GboardPatchesSettings;
import dev.jason.gboardpatches.extension.settings.GboardPatchesSettingsContract;

public final class GboardBackspaceSwipeSettingsFeature
        implements GboardPatchesSettingsContract.Feature {
    @Override public String getEntryTitle() { return "Smooth backspace swipe"; }
    @Override public String getEntrySummary() { return "Stock, character steps, or word steps"; }
    @Override public boolean isAvailable(Context context) { return true; }

    @Override
    public GboardPatchesSettingsContract.Screen buildScreen(
            GboardPatchesSettingsContract.FeatureHost host) {
        Context context = host.getContext();
        SharedPreferences preferences = GboardPatchesSettings.preferences(context);
        int mode = GboardBackspaceSwipeSettings.read(preferences);
        String summary = mode == 1 ? "Smooth character deletion"
                : mode == 2 ? "Smooth word deletion" : "Stock Gboard behavior";
        GboardPatchesSettingsContract.SelectorRow row =
                new GboardPatchesSettingsContract.SelectorRow("Mode", summary, summary, true,
                        () -> GboardPatchesSettingsContract.showChoiceDialog(
                                host, "Backspace swipe mode",
                                new String[] { "Off (stock Gboard)", "Smooth characters", "Smooth words" },
                                new String[] { "0", "1", "2" },
                                String.valueOf(GboardBackspaceSwipeSettings.read(preferences)),
                                null, null, value -> {
                                    GboardBackspaceSwipeSettings.write(preferences,
                                            Integer.parseInt(value));
                                    GboardPatchesSettingsContract.refresh(host);
                                }));
        return new GboardPatchesSettingsContract.Screen(
                "Smooth backspace swipe", "GBOARD", "Backspace swipe",
                "Swipe left from Backspace, move right to retract, then lift to delete.",
                Collections.singletonList(row));
    }
}
