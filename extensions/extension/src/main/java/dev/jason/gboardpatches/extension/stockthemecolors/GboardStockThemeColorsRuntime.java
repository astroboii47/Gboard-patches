package dev.jason.gboardpatches.extension.stockthemecolors;

import android.app.AlertDialog;
import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/** Adds stock-theme color controls and persists them in Gboard's generated stylesheet. */
public final class GboardStockThemeColorsRuntime {
    private static final String TAG = "GboardStockColors";
    private static final Map<Object, Colors> COLORS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Object, WeakReference<ViewGroup>> PREVIEWS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<View, ColorStateList> ORIGINAL_TINTS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private GboardStockThemeColorsRuntime() { }

    /**
     * Reliable entry point from the stock theme-builder activity.  Gboard creates
     * its preview page lazily, so the old constructor hooks can run before that
     * page is in the hierarchy.  Keep watching until the page containing both
     * the brightness slider and the final action button is actually visible.
     */
    public static void attachActivity(Object value) {
        try {
            if (!(value instanceof Activity)) return;
            Activity activity = (Activity) value;
            View content = activity.findViewById(android.R.id.content);
            if (!(content instanceof ViewGroup)) return;
            ViewGroup activityRoot = (ViewGroup) content;
            ViewTreeObserver.OnGlobalLayoutListener listener =
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override public void onGlobalLayout() {
                    try {
                        Object builder = activity.getClass().getMethod("u").invoke(activity);
                        if (builder == null) return;
                        loadSavedColors(activity, builder);
                        ViewGroup preview = findPreviewPage(activityRoot);
                        boolean editor = activity.getClass().getName().endsWith("ThemeEditorActivity");
                        if (preview != null) attachToRoot(preview, builder);
                        else if (editor) attachEditorOverlay(activityRoot, builder);
                        View attached = activityRoot.findViewWithTag("morphe-stock-theme-colors");
                        if (attached != null) {
                            ViewTreeObserver observer = activityRoot.getViewTreeObserver();
                            if (observer.isAlive()) observer.removeOnGlobalLayoutListener(this);
                            Log.i(TAG, "controls attached from theme activity");
                        }
                    } catch (Throwable failure) {
                        Log.w(TAG, "activity layout attach failed", failure);
                    }
                }
            };
            activityRoot.getViewTreeObserver().addOnGlobalLayoutListener(listener);
            activityRoot.post(() -> listener.onGlobalLayout());
            Log.i(TAG, "watching stock theme editor");
        } catch (Throwable failure) {
            Log.w(TAG, "attachActivity failed", failure);
        }
    }

    private static ViewGroup findPreviewPage(View root) {
        if (!(root instanceof ViewGroup) || root.getVisibility() != View.VISIBLE) return null;
        ViewGroup group = (ViewGroup) root;
        if (containsVisible(group, SeekBar.class) && containsVisible(group, Button.class)
                && group instanceof LinearLayout) return group;
        for (int i = 0; i < group.getChildCount(); i++) {
            ViewGroup result = findPreviewPage(group.getChildAt(i));
            if (result != null) return result;
        }
        return null;
    }

    private static boolean containsVisible(View root, Class<?> type) {
        if (root.getVisibility() != View.VISIBLE) return false;
        if (type.isInstance(root)) return true;
        if (!(root instanceof ViewGroup)) return false;
        ViewGroup group = (ViewGroup) root;
        for (int i = 0; i < group.getChildCount(); i++) {
            if (containsVisible(group.getChildAt(i), type)) return true;
        }
        return false;
    }

    private static void attachEditorOverlay(ViewGroup root, Object builder) {
        if (root.findViewWithTag("morphe-stock-theme-colors") != null) return;
        Colors colors = COLORS.computeIfAbsent(builder, ignored -> new Colors());
        LinearLayout bar = makeBar(root, builder, colors);
        if (root instanceof FrameLayout) {
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM);
            params.setMargins(dp(root, 8), dp(root, 8), dp(root, 8), dp(root, 72));
            bar.setElevation(dp(root, 8));
            GradientDrawable background = new GradientDrawable();
            background.setColor(0xee303034);
            background.setCornerRadius(dp(root, 16));
            bar.setBackground(background);
            root.addView(bar, params);
        } else {
            root.addView(bar);
        }
    }

    /** Direct hook for the visible brightness/preview page. */
    public static void attachPage(View page, Object builder) {
        try {
            if (!(page instanceof ViewGroup) || builder == null) return;
            attachToRoot((ViewGroup) page, builder);
            Log.i(TAG, "controls attached to preview page");
        } catch (Throwable failure) {
            Log.w(TAG, "attachPage failed", failure);
        }
    }

    public static void attach(Object adapter) {
        try {
            Object builder = read(adapter, "b");
            Object rawViews = read(adapter, "e");
            if (builder == null || !(rawViews instanceof View[])) return;
            View[] views = (View[]) rawViews;
            if (views.length < 2 || !(views[1] instanceof ViewGroup)) return;
            ViewGroup root = (ViewGroup) views[1];
            attachToRoot(root, builder);
        } catch (Throwable failure) {
            Log.w(TAG, "attach failed", failure);
        }
    }

    private static void attachToRoot(ViewGroup root, Object builder) {
            if (root.findViewWithTag("morphe-stock-theme-colors") != null) return;
            Colors colors = COLORS.computeIfAbsent(builder, ignored -> new Colors());
            LinearLayout bar = makeBar(root, builder, colors);
            if (root instanceof LinearLayout) {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                ((LinearLayout) root).addView(bar, Math.max(0, root.getChildCount() - 1), params);
            } else {
                root.addView(bar);
            }
    }

    /** Restores Morphe colours when Gboard opens an already-saved theme. */
    private static void loadSavedColors(Activity activity, Object builder) {
        Colors colors = COLORS.computeIfAbsent(builder, ignored -> new Colors());
        if (colors.loaded) return;
        colors.loaded = true;
        if (!activity.getClass().getName().endsWith("ThemeEditorActivity")) return;
        try {
            Field sourceField = activity.getClass().getField("r");
            Object source = sourceField.get(activity);
            if (!(source instanceof File) || !((File) source).isFile()) return;
            try (ZipFile zip = new ZipFile((File) source)) {
                ZipEntry entry = zip.getEntry("style_sheet.binarypb");
                if (entry == null) return;
                byte[] bytes = readAll(zip.getInputStream(entry));
                Class<?> sheetClass = Class.forName("rrv");
                Object defaultSheet = sheetClass.getField("a").get(null);
                Class<?> protoBase = Class.forName("aaln");
                Object sheet = protoBase.getMethod("t", protoBase, byte[].class)
                        .invoke(null, defaultSheet, bytes);
                boolean marked = restoreMarkers(sheet, colors);
                if (!marked) restoreLegacyRules(sheet, colors);
                Log.i(TAG, "restored saved colors background=" + hex(colors.background)
                        + " key=" + hex(colors.key) + " accent=" + hex(colors.accent)
                        + " toolbar=" + hex(colors.toolbar)
                        + " modifier=" + hex(colors.modifier));
                ColorPreview preview = colors.preview == null ? null : colors.preview.get();
                if (preview != null) preview.invalidate();
            }
        } catch (Throwable failure) {
            Log.w(TAG, "could not restore saved theme colors", failure);
        }
    }

    private static boolean restoreMarkers(Object sheet, Colors colors) throws Exception {
        boolean found = false;
        Object variables = sheet.getClass().getField("c").get(sheet);
        for (Object variable : (Iterable<?>) variables) {
            String name = (String) variable.getClass().getField("d").get(variable);
            if (!name.startsWith("__morphe_") || !name.endsWith("_color")) continue;
            Object value = variable.getClass().getField("e").get(variable);
            long encoded = Math.round(value.getClass().getField("j").getDouble(value));
            Integer color = encoded < 0 ? null : (int) encoded;
            if ("__morphe_background_color".equals(name)) colors.background = color;
            else if ("__morphe_key_color".equals(name)) colors.key = color;
            else if ("__morphe_accent_color".equals(name)) colors.accent = color;
            else if ("__morphe_toolbar_color".equals(name)) colors.toolbar = color;
            else if ("__morphe_modifier_color".equals(name)) colors.modifier = color;
            found = true;
        }
        return found;
    }

    /** Migrates themes produced by v7-v10, before colour markers were stored. */
    private static void restoreLegacyRules(Object sheet, Colors colors) throws Exception {
        Object rules = sheet.getClass().getField("b").get(sheet);
        for (Object rule : (Iterable<?>) rules) {
            int property = rule.getClass().getField("e").getInt(rule);
            if (property != 1) continue; // BACKGROUND_COLOR
            Object selectors = rule.getClass().getField("d").get(rule);
            Object value = rule.getClass().getField("f").get(rule);
            int color = value.getClass().getField("e").getInt(value);
            if (((java.util.List<?>) selectors).contains(".keytop.for-action-key")) {
                colors.accent = color;
            } else if (((java.util.List<?>) selectors).contains(".keytop.for-modifier-key")) {
                colors.modifier = color;
            } else if (((java.util.List<?>) selectors).contains(".keytop")) {
                colors.key = color;
            } else if (((java.util.List<?>) selectors).contains(".keyboard-body-area")) {
                colors.background = color;
            }
        }
    }

    private static LinearLayout makeBar(View root, Object builder, Colors colors) {
        if (root instanceof ViewGroup) PREVIEWS.put(builder, new WeakReference<>((ViewGroup) root));
        LinearLayout bar = new LinearLayout(root.getContext());
        bar.setTag("morphe-stock-theme-colors");
        bar.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(root, 8);
        bar.setPadding(pad, pad / 2, pad, pad / 2);
        ColorPreview preview = new ColorPreview(root.getContext(), colors);
        colors.preview = new WeakReference<>(preview);
        bar.addView(preview, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(root, 76)));
        LinearLayout buttons = new LinearLayout(root.getContext());
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        bar.addView(buttons, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        Button background = button(buttons, "Background");
        Button key = button(buttons, "Keys");
        Button modifier = button(buttons, "Modifiers");
        LinearLayout buttons2 = new LinearLayout(root.getContext());
        buttons2.setOrientation(LinearLayout.HORIZONTAL);
        bar.addView(buttons2, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        Button accent = button(buttons2, "Accent");
        Button toolbar = button(buttons2, "Top strip");
        Button reset = button(buttons2, "Defaults");
        background.setOnClickListener(v -> colorDialog(builder, colors, 0,
                background, key, accent, toolbar, modifier));
        key.setOnClickListener(v -> colorDialog(builder, colors, 1,
                background, key, accent, toolbar, modifier));
        accent.setOnClickListener(v -> colorDialog(builder, colors, 2,
                background, key, accent, toolbar, modifier));
        toolbar.setOnClickListener(v -> colorDialog(builder, colors, 3,
                background, key, accent, toolbar, modifier));
        modifier.setOnClickListener(v -> colorDialog(builder, colors, 4,
                background, key, accent, toolbar, modifier));
        reset.setOnClickListener(v -> {
            colors.background = null;
            colors.key = null;
            colors.accent = null;
            colors.toolbar = null;
            colors.modifier = null;
            updateSwatches(background, key, accent, toolbar, modifier, colors);
            applyPreview(builder, colors);
        });
        updateSwatches(background, key, accent, toolbar, modifier, colors);
        return bar;
    }

    public static boolean postProcess(Object builder, File file, boolean stockSuccess) {
        if (!stockSuccess) return false;
        Colors colors = COLORS.get(builder);
        if (colors == null || (colors.background == null && colors.key == null
                && colors.accent == null && colors.toolbar == null
                && colors.modifier == null)) return true;
        Log.i(TAG, "saving colors background=" + hex(colors.background)
                + " key=" + hex(colors.key) + " accent=" + hex(colors.accent)
                + " toolbar=" + hex(colors.toolbar)
                + " modifier=" + hex(colors.modifier)
                + " file=" + file.getName());
        File temp = new File(file.getParentFile(), file.getName() + ".colors");
        byte[] solidBackground = colors.background == null ? null
                : solidPng(builder, colors.background, false);
        byte[] solidThumbnail = colors.background == null ? null
                : solidPng(builder, colors.background, true);
        try (ZipFile input = new ZipFile(file);
             ZipOutputStream output = new ZipOutputStream(new FileOutputStream(temp))) {
            Enumeration<? extends ZipEntry> entries = input.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                byte[] data = readAll(input.getInputStream(entry));
                if ("style_sheet.binarypb".equals(entry.getName())) {
                    data = styled(data, colors);
                } else if (solidBackground != null && "background".equals(entry.getName())) {
                    data = solidBackground;
                } else if (solidThumbnail != null && "thumbnail".equals(entry.getName())) {
                    data = solidThumbnail;
                }
                ZipEntry replacement = new ZipEntry(entry.getName());
                replacement.setTime(entry.getTime());
                output.putNextEntry(replacement);
                output.write(data);
                output.closeEntry();
            }
        } catch (Throwable failure) {
            Log.w(TAG, "theme color save failed", failure);
            temp.delete();
            return false;
        }
        if (!file.delete() || !temp.renameTo(file)) {
            Log.w(TAG, "could not replace generated theme");
            temp.delete();
            return false;
        }
        Log.i(TAG, "theme colors saved successfully: " + file.getAbsolutePath());
        return true;
    }

    /**
     * Runs immediately before Gboard serializes the theme. A user-image theme
     * normally writes a Material-coloured keyboard-body overlay over its
     * background image. For a user-selected solid background that overlay must
     * be transparent; changing only the image leaves the familiar blue-grey
     * Material colour visible instead of the selected colour.
     */
    public static void prepareSave(Object builder) {
        Colors colors = COLORS.get(builder);
        if (colors == null || colors.background == null) return;
        try {
            Field transparency = builder.getClass().getDeclaredField("h");
            transparency.setAccessible(true);
            transparency.setFloat(builder, 1.0f);
            Log.i(TAG, "disabled stock overlay for background=" + hex(colors.background));
        } catch (Throwable failure) {
            Log.w(TAG, "could not disable stock theme overlay", failure);
        }
    }

    private static byte[] solidPng(Object builder, int color, boolean thumbnail) {
        int width = thumbnail ? 384 : 1080;
        int height = thumbnail ? 216 : 720;
        try {
            Object raw = read(builder, "c");
            if (raw instanceof Rect) {
                Rect crop = (Rect) raw;
                if (crop.width() > 0 && crop.height() > 0) {
                    if (thumbnail) {
                        width = Math.min(384, crop.width());
                        height = Math.max(1,
                                Math.round(width * crop.height() / (float) crop.width()));
                    } else {
                        width = crop.width();
                        height = crop.height();
                    }
                }
            }
        } catch (Throwable failure) {
            Log.w(TAG, "could not read theme crop dimensions", failure);
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(color);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
        bitmap.recycle();
        return output.toByteArray();
    }

    private static String hex(Integer color) {
        return color == null ? "default" : String.format("#%08X", color);
    }

    private static byte[] styled(byte[] source, Colors colors) throws Exception {
        Class<?> sheetClass = Class.forName("rrv");
        Object defaultSheet = sheetClass.getField("a").get(null);
        Class<?> protoBase = Class.forName("aaln");
        Object sheet = protoBase.getMethod("t", protoBase, byte[].class)
                .invoke(null, defaultSheet, source);
        Object sheetBuilder = protoBase.getMethod("l").invoke(sheet);
        Method addRule = sheetBuilder.getClass().getMethod("an", Class.forName("rru"));
        if (colors.background != null) {
            addRule.invoke(sheetBuilder, rule(colors.background,
                    ".keyboard-background", ".keyboard-body-area",
                    ".keyboard-body-area.for-non-prime-body"));
        }
        if (colors.key != null) {
            addRule.invoke(sheetBuilder, rule(colors.key,
                    ".keytop", ".keytop.dark", ".keytop.with-popup",
                    ".keytop:pressed", ".keytop.dark:pressed",
                    ".keytop.semi-rounded", ".keytop.dark.semi-rounded",
                    ".keytop.dark.semi-rounded.pill-shaped.pill-colored",
                    ".keytop.for-bottom-key", ".keytop.dark.for-bottom-key",
                    ".keytop.dark.for-bottom-key.pill-shaped.pill-colored",
                    ".keytop.for-bottom-key.with-popup", ".keytop.dark.for-bottom-key.with-popup",
                    ".keytop.for-space-bar", ".keytop.dark.for-space-bar",
                    ".keytop.for-bottom-key.for-space-bar",
                    ".keytop.for-numpad", ".keytop.dark.for-numpad",
                    ".keytop.dark.for-numpad.pill-shaped",
                    ".keytop.dark.for-numpad.pill-shaped.pill-colored",
                    ".keytop.for-numpad.pill-shaped-if-bordered",
                    ".keytop.dark.for-numpad.pill-shaped-if-bordered",
                    ".keytop.for-capslock-key-holder",
                    ".keytop.square-corner", ".keytop.transparent"));
        }
        if (colors.modifier != null) {
            addRule.invoke(sheetBuilder, rule(colors.modifier,
                    ".keytop.for-modifier-key",
                    ".keytop.for-modifier-key-holder"));
        }
        if (colors.accent != null) {
            addRule.invoke(sheetBuilder, rule(colors.accent,
                    ".keytop.for-action-key",
                    ".keytop.for-action-key-holder",
                    ".keytop.for-action-key-holder.pill-shaped",
                    ".keytop.for-action-key-holder.for-numpad",
                    ".keytop.for-action-key-holder.for-numpad.pill-shaped",
                    ".keytop.for-bottom-key.for-action-key-holder",
                    ".keytop.for-bottom-key.for-action-key-holder.pill-shaped",
                    ".keytop.semi-rounded.for-action-key-holder.pill-shaped"));
            addRule.invoke(sheetBuilder, ruleProperty("COLOR", colors.accent,
                    ".background-icon.for-action-key", ".icon.action-color-bg",
                    ".track.for-gesture"));
        }
        if (colors.toolbar != null) {
            addRule.invoke(sheetBuilder, rule(colors.toolbar,
                    ".keyboard-header-area", ".candidates-area",
                    ".candidates-area.expanded"));
            addRule.invoke(sheetBuilder, numericRule("BACKGROUND_ALPHA", 1.0,
                    ".keyboard-header-area", ".candidates-area",
                    ".candidates-area.expanded"));
        }
        Method addVariable = sheetBuilder.getClass().getMethod("ao", Class.forName("rrw"));
        addVariable.invoke(sheetBuilder, marker("__morphe_background_color", colors.background));
        addVariable.invoke(sheetBuilder, marker("__morphe_key_color", colors.key));
        addVariable.invoke(sheetBuilder, marker("__morphe_accent_color", colors.accent));
        addVariable.invoke(sheetBuilder, marker("__morphe_toolbar_color", colors.toolbar));
        addVariable.invoke(sheetBuilder, marker("__morphe_modifier_color", colors.modifier));
        Object finished = sheetBuilder.getClass().getMethod("q").invoke(sheetBuilder);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Method write = find(finished.getClass(), "f", java.io.OutputStream.class);
        write.invoke(finished, bytes);
        return bytes.toByteArray();
    }

    private static Object rule(int color, String... selectors) throws Exception {
        return ruleProperty("BACKGROUND_COLOR", color, selectors);
    }

    private static Object ruleProperty(String propertyName, int color,
            String... selectors) throws Exception {
        Class<?> utils = Class.forName("rqm");
        Object colorValue = utils.getMethod("b", int.class).invoke(null, color);
        Class<?> property = Class.forName("rrt");
        @SuppressWarnings({"rawtypes", "unchecked"})
        Object background = Enum.valueOf((Class<? extends Enum>) property, propertyName);
        return utils.getMethod("f", property, Class.forName("rrs"), String[].class)
                .invoke(null, background, colorValue, selectors);
    }

    private static Object numericRule(String propertyName, double value,
            String... selectors) throws Exception {
        Class<?> utils = Class.forName("rqm");
        Object numericValue = utils.getMethod("c", double.class).invoke(null, value);
        Class<?> property = Class.forName("rrt");
        @SuppressWarnings({"rawtypes", "unchecked"})
        Object enumValue = Enum.valueOf((Class<? extends Enum>) property, propertyName);
        return utils.getMethod("f", property, Class.forName("rrs"), String[].class)
                .invoke(null, enumValue, numericValue, selectors);
    }

    private static Object marker(String name, Integer color) throws Exception {
        long encoded = color == null ? -1L : Integer.toUnsignedLong(color);
        Class<?> utils = Class.forName("rqm");
        Object numericValue = utils.getMethod("c", double.class)
                .invoke(null, (double) encoded);
        return utils.getMethod("m", String.class, Class.forName("rrs"))
                .invoke(null, name, numericValue);
    }

    private static void colorDialog(Object builder, Colors colors, int target,
            Button backgroundButton, Button keyButton, Button accentButton,
            Button toolbarButton, Button modifierButton) {
        Integer current = target == 0 ? colors.background : target == 1 ? colors.key
                : target == 2 ? colors.accent : target == 3 ? colors.toolbar
                : colors.modifier;
        int initial = current != null ? current
                : (target == 2 ? 0xff4285f4
                : (target == 1 || target == 4) ? 0xff5f6368 : 0xff202124);
        LinearLayout content = new LinearLayout(backgroundButton.getContext());
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(backgroundButton, 20);
        content.setPadding(pad, pad / 2, pad, 0);
        ColorField field = new ColorField(backgroundButton.getContext(), initial);
        content.addView(field, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(backgroundButton, 220)));
        SeekBar hue = new SeekBar(backgroundButton.getContext());
        hue.setMax(359);
        hue.setProgress(Math.round(field.hsv[0]));
        content.addView(hue, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView swatch = new TextView(backgroundButton.getContext());
        swatch.setGravity(Gravity.CENTER);
        swatch.setTextSize(16);
        swatch.setPadding(0, dp(keyButton, 10), 0, dp(keyButton, 10));
        content.addView(swatch, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        Runnable update = () -> {
            int color = field.color();
            swatch.setText(String.format("#%06X", color & 0xffffff));
            swatch.setBackgroundColor(color);
            swatch.setTextColor(luminance(color) > 0.55 ? Color.BLACK : Color.WHITE);
        };
        Runnable commitLive = () -> {
            int picked = field.color();
            setTarget(colors, target, picked);
            updateSwatches(backgroundButton, keyButton, accentButton, toolbarButton,
                    modifierButton, colors);
            applyPreview(builder, colors);
        };
        field.changed = () -> {
            update.run();
            commitLive.run();
        };
        hue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean user) {
                field.setHue(progress);
                update.run();
                if (user) commitLive.run();
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        update.run();
        new AlertDialog.Builder(backgroundButton.getContext())
                .setTitle(target == 0 ? "Keyboard background" : target == 1
                        ? "Key background colour" : target == 2 ? "Accent colour"
                        : target == 3 ? "Top strip colour" : "Modifier key colour")
                .setView(content)
                .setNeutralButton("Theme default", (dialog, which) -> {
                    if (target == 0) colors.background = null;
                    else if (target == 1) colors.key = null;
                    else if (target == 2) colors.accent = null;
                    else if (target == 3) colors.toolbar = null;
                    else colors.modifier = null;
                    updateSwatches(backgroundButton, keyButton, accentButton, toolbarButton,
                            modifierButton, colors);
                    applyPreview(builder, colors);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    setTarget(colors, target, current);
                    updateSwatches(backgroundButton, keyButton, accentButton, toolbarButton,
                            modifierButton, colors);
                    applyPreview(builder, colors);
                })
                .setPositiveButton("Apply", (dialog, which) -> {
                    int picked = field.color();
                    setTarget(colors, target, picked);
                    Log.i(TAG, "picked " + (target == 0 ? "background" : target == 1
                            ? "key" : target == 2 ? "accent" : target == 3 ? "toolbar"
                            : "modifier")
                            + "=" + hex(picked));
                    updateSwatches(backgroundButton, keyButton, accentButton, toolbarButton,
                            modifierButton, colors);
                    applyPreview(builder, colors);
                }).show();
    }

    private static void setTarget(Colors colors, int target, Integer value) {
        if (target == 0) colors.background = value;
        else if (target == 1) colors.key = value;
        else if (target == 2) colors.accent = value;
        else if (target == 3) colors.toolbar = value;
        else colors.modifier = value;
    }

    private static void applyPreview(Object builder, Colors colors) {
        WeakReference<ViewGroup> reference = PREVIEWS.get(builder);
        ViewGroup root = reference == null ? null : reference.get();
        if (root == null) return;
        applyPreviewRecursive(root, colors);
    }

    private static void applyPreviewRecursive(View view, Colors colors) {
        Object rawTag = view.getTag();
        String tag = rawTag == null ? "" : rawTag.toString();
        Integer color = null;
        if (tag.contains("keyboard-background") || tag.contains("keyboard-body-area")) {
            color = colors.background;
        } else if (tag.contains("keytop")) {
            color = tag.contains("for-action-key-holder") ? colors.accent
                    : tag.contains("for-modifier-key") ? colors.modifier : colors.key;
        }
        if (tag.contains("keyboard-background") || tag.contains("keyboard-body-area")
                || tag.contains("keytop")) {
            ORIGINAL_TINTS.putIfAbsent(view, view.getBackgroundTintList());
            view.setBackgroundTintList(color == null ? ORIGINAL_TINTS.get(view)
                    : ColorStateList.valueOf(color));
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyPreviewRecursive(group.getChildAt(i), colors);
            }
        }
    }

    private static final class ColorField extends View {
        final float[] hsv = new float[3];
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Runnable changed;

        ColorField(android.content.Context context, int initial) {
            super(context);
            Color.colorToHSV(initial, hsv);
            setMinimumHeight(dp(this, 180));
        }

        void setHue(float hue) {
            hsv[0] = hue;
            invalidate();
        }

        int color() { return Color.HSVToColor(hsv); }

        @Override protected void onDraw(Canvas canvas) {
            int pure = Color.HSVToColor(new float[] {hsv[0], 1f, 1f});
            Shader saturation = new LinearGradient(0, 0, getWidth(), 0,
                    Color.WHITE, pure, Shader.TileMode.CLAMP);
            Shader value = new LinearGradient(0, 0, 0, getHeight(),
                    Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP);
            paint.setShader(new ComposeShader(saturation, value, PorterDuff.Mode.MULTIPLY));
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(this, 3));
            paint.setColor(Color.WHITE);
            canvas.drawCircle(hsv[1] * getWidth(), (1f - hsv[2]) * getHeight(),
                    dp(this, 9), paint);
            paint.setStyle(Paint.Style.FILL);
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            if (event.getActionMasked() != MotionEvent.ACTION_DOWN
                    && event.getActionMasked() != MotionEvent.ACTION_MOVE) return true;
            hsv[1] = Math.max(0f, Math.min(1f, event.getX() / Math.max(1f, getWidth())));
            hsv[2] = 1f - Math.max(0f, Math.min(1f,
                    event.getY() / Math.max(1f, getHeight())));
            invalidate();
            if (changed != null) changed.run();
            return true;
        }
    }

    private static Button button(LinearLayout parent, String text) {
        Button button = new Button(parent.getContext());
        button.setText(text);
        button.setAllCaps(false);
        parent.addView(button, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return button;
    }

    private static void updateSwatches(Button background, Button key, Button accent,
            Button toolbar, Button modifier, Colors colors) {
        style(background, colors.background);
        style(key, colors.key);
        style(accent, colors.accent);
        style(toolbar, colors.toolbar);
        style(modifier, colors.modifier);
        ColorPreview preview = colors.preview == null ? null : colors.preview.get();
        if (preview != null) preview.invalidate();
    }

    /** Honest colour preview: the same three values that are written to the saved theme. */
    private static final class ColorPreview extends View {
        private final Colors colors;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        ColorPreview(android.content.Context context, Colors colors) {
            super(context);
            this.colors = colors;
        }

        @Override protected void onDraw(Canvas canvas) {
            int background = colors.background == null ? 0xff202124 : colors.background;
            int key = colors.key == null ? 0xff5f6368 : colors.key;
            int accent = colors.accent == null ? 0xff4285f4 : colors.accent;
            int modifier = colors.modifier == null ? key : colors.modifier;
            canvas.drawColor(background);
            paint.setColor(colors.toolbar == null ? 0xff303134 : colors.toolbar);
            canvas.drawRect(0, 0, getWidth(), dp(this, 18), paint);
            float gap = dp(this, 4);
            float top = dp(this, 22);
            float keyHeight = (getHeight() - top - gap * 2f - dp(this, 4)) / 3f;
            float keyWidth = (getWidth() - gap * 8f) / 7f;
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 7; column++) {
                    paint.setColor(row == 2 && column == 6 ? accent
                            : (column == 0 || (row == 2 && column == 5)) ? modifier : key);
                    float left = gap + column * (keyWidth + gap);
                    float y = top + row * (keyHeight + gap);
                    canvas.drawRoundRect(left, y, left + keyWidth, y + keyHeight,
                            dp(this, 5), dp(this, 5), paint);
                }
            }
        }
    }

    private static void style(Button button, Integer color) {
        if (color == null) {
            button.setBackgroundTintList(null);
            return;
        }
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        button.setTextColor(luminance(color) > 0.55 ? Color.BLACK : Color.WHITE);
    }

    private static double luminance(int color) {
        return (0.2126 * Color.red(color) + 0.7152 * Color.green(color)
                + 0.0722 * Color.blue(color)) / 255.0;
    }

    private static int dp(View view, int value) {
        return Math.round(value * view.getResources().getDisplayMetrics().density);
    }

    private static Object read(Object owner, String name) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }

    private static byte[] readAll(InputStream source) throws Exception {
        try (InputStream input = source;
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
    }

    private static Method find(Class<?> type, String name, Class<?> parameter) throws Exception {
        while (type != null) {
            try {
                Method method = type.getDeclaredMethod(name, parameter);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) { type = type.getSuperclass(); }
        }
        throw new NoSuchMethodException(name);
    }

    private static final class Colors {
        boolean loaded;
        Integer background;
        Integer key;
        Integer accent;
        Integer toolbar;
        Integer modifier;
        WeakReference<ColorPreview> preview;
    }
}
