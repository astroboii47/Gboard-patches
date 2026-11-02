package dev.jason.gboardpatches.extension.customtheme;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import dev.jason.gboardpatches.extension.settings.GboardPatchesSettings;

@RunWith(RobolectricTestRunner.class)
public final class GboardCustomThemeAutomationTest {
    @Test
    public void reconcileSelectsConfiguredLightAndDarkArchives() {
        Context context = RuntimeEnvironment.getApplication();
        SharedPreferences settings = GboardPatchesSettings.preferences(context);
        settings.edit().clear().putBoolean(GboardCustomThemeSettings.ENABLED_KEY, true)
                .putBoolean(GboardCustomThemeSettings.FOLLOW_SYSTEM_KEY, true)
                .putString(GboardCustomThemeSettings.LIGHT_THEME_KEY, "silk:light")
                .putString(GboardCustomThemeSettings.DARK_THEME_KEY, "silk:dark")
                .putBoolean(GboardCustomThemeSettings.LIGHT_BORDER_KEY, false)
                .putBoolean(GboardCustomThemeSettings.DARK_BORDER_KEY, true).commit();
        Configuration configuration = new Configuration();
        configuration.uiMode = Configuration.UI_MODE_NIGHT_YES;
        SharedPreferences target = GboardCustomThemeAutoSwitcher.targetPreferences(context);
        target.edit().clear().commit();

        Assert.assertTrue(GboardCustomThemeAutoSwitcher.reconcile(context, configuration));
        Assert.assertEquals("silk:dark",
                target.getString("additional_keyboard_theme", ""));
        Assert.assertTrue(target.getBoolean("enable_key_border", false));

        configuration.uiMode = Configuration.UI_MODE_NIGHT_NO;
        Assert.assertTrue(GboardCustomThemeAutoSwitcher.reconcile(context, configuration));
        Assert.assertEquals("silk:light",
                target.getString("additional_keyboard_theme", ""));
        Assert.assertFalse(target.getBoolean("enable_key_border", true));
    }

    @Test
    public void accentRewritesOnlyStandardActionColour() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        GboardPatchesSettings.preferences(context).edit()
                .putString(GboardCustomThemeSettings.ACCENT_KEY, "#123456").commit();
        File archive = new File(context.getCacheDir(), "accent-test.zip");
        try (ZipOutputStream output = new ZipOutputStream(new FileOutputStream(archive))) {
            output.putNextEntry(new ZipEntry("metadata.json"));
            output.write("{}".getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
            output.putNextEntry(new ZipEntry("style.css"));
            output.write(("@def color_action_default #4285F4FF;\n"
                    + "@def color_label #000000FF;").getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }

        GboardCustomThemeAccent.apply(context, archive);

        try (ZipFile zip = new ZipFile(archive)) {
            String css = new String(zip.getInputStream(zip.getEntry("style.css")).readAllBytes(),
                    StandardCharsets.UTF_8);
            Assert.assertTrue(css.contains("color_action_default #123456FF;"));
            Assert.assertTrue(css.contains("color_label #000000FF;"));
        }
    }
}
