package dev.jason.gboardpatches.extension.customtheme;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import dev.jason.gboardpatches.extension.settings.GboardPatchesSettings;
import dev.jason.gboardpatches.extension.spacebarlogo.GboardSpacebarLogoSettings;

/** Applies the optional action-key accent to an imported theme before validation. */
final class GboardCustomThemeAccent {
    private static final Pattern ACTION_COLOR = Pattern.compile(
            "(?m)(@def\\s+color_action_default\\s+)#[0-9a-fA-F]{8}(\\s*;)");

    private GboardCustomThemeAccent() { }

    static File apply(Context context, File archive) throws Exception {
        SharedPreferences preferences = GboardPatchesSettings.preferences(context);
        String selected = GboardCustomThemeSettings.accent(preferences);
        if (GboardCustomThemeSettings.DEFAULT_ACCENT.equals(selected)) return archive;
        String normalized = GboardSpacebarLogoSettings.tryNormalizeColorHex(selected);
        if (normalized == null) return archive;
        String cssColor = toCssRgba(normalized);
        File output = new File(archive.getParentFile(), archive.getName() + ".accent.part");
        byte[] buffer = new byte[16 * 1024];
        try (ZipInputStream input = new ZipInputStream(new FileInputStream(archive));
             ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(output))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                ZipEntry copied = new ZipEntry(entry.getName());
                copied.setTime(entry.getTime());
                zip.putNextEntry(copied);
                if (!entry.isDirectory() && entry.getName().toLowerCase(Locale.ROOT).endsWith(".css")) {
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    int read;
                    while ((read = input.read(buffer)) >= 0) if (read > 0) bytes.write(buffer, 0, read);
                    String css = bytes.toString(StandardCharsets.UTF_8.name());
                    css = ACTION_COLOR.matcher(css).replaceAll("$1" + cssColor + "$2");
                    zip.write(css.getBytes(StandardCharsets.UTF_8));
                } else {
                    int read;
                    while ((read = input.read(buffer)) >= 0) if (read > 0) zip.write(buffer, 0, read);
                }
                zip.closeEntry();
            }
        } catch (Throwable failure) {
            output.delete();
            throw failure;
        }
        if (!archive.delete() || !output.renameTo(archive)) {
            output.delete();
            throw new java.io.IOException("Could not commit accent theme archive");
        }
        return archive;
    }

    private static String toCssRgba(String androidColor) {
        String hex = androidColor.substring(1);
        return hex.length() == 6 ? "#" + hex + "FF"
                : "#" + hex.substring(2) + hex.substring(0, 2);
    }
}
