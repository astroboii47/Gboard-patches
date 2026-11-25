package dev.jason.gboardpatches.extension.calculator;

import android.inputmethodservice.InputMethodService;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.FrameLayout;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

final class GboardShortcodeRuntime {
    private static final String TAG = "GboardGifShortcut";
    private static boolean enabled;
    private static boolean vertical;
    private static boolean gifQueryEnabled;
    private static boolean replacing;
    private static FrameLayout activeHost;

    static void onInputViewStarted(Object receiver, boolean isEnabled, boolean useVertical,
            boolean enableGifQuery) {
        enabled = isEnabled && receiver instanceof InputMethodService
                && !isPassword(((InputMethodService) receiver).getCurrentInputEditorInfo());
        vertical = useVertical;
        gifQueryEnabled = enableGifQuery && receiver instanceof InputMethodService
                && !isPassword(((InputMethodService) receiver).getCurrentInputEditorInfo());
        hide();
    }

    static void onSelectionUpdated(Object receiver, View inputView, int start, int end) {
        if ((!enabled && !gifQueryEnabled) || replacing || start != end
                || !(receiver instanceof InputMethodService service)) {
            hide();
            return;
        }
        InputConnection connection = service.getCurrentInputConnection();
        if (connection == null) { hide(); return; }
        CharSequence beforeSequence = connection.getTextBeforeCursor(80, 0);
        String before = beforeSequence == null ? "" : beforeSequence.toString();
        GifToken gifToken = gifQueryEnabled ? GifToken.from(before) : null;
        if (gifToken != null && openGifSearch(service, gifToken.query)) {
            replace(connection, gifToken.length, "");
            hide();
            return;
        }
        if (!enabled) { hide(); return; }
        Token token = Token.from(before);
        if (token == null) { hide(); return; }
        try {
            GboardShortcodeIndex index = GboardShortcodeIndex.get(service);
            if (token.closed) {
                GboardShortcodeIndex.Entry exact = index.exact(token.name);
                if (exact != null) replace(connection, token.length, exact.value);
                hide();
                return;
            }
            if (token.name.isEmpty()) { hide(); return; }
            List<GboardShortcodeIndex.Entry> entries = index.prefix(token.name, 8);
            GboardCalculatorSuggestionView.Placement placement =
                    GboardCalculatorSuggestionView.resolvePlacement(inputView);
            if (entries.isEmpty() || placement == null) { hide(); return; }
            activeHost = placement.host();
            GboardShortcodeView.show(placement, entries, vertical,
                    entry -> replaceCurrent(service, entry));
        } catch (Throwable failure) { hide(); }
    }

    static void onInputWindowHidden() {
        hide();
        enabled = false;
        gifQueryEnabled = false;
    }

    private static boolean openGifSearch(InputMethodService service, String query) {
        try {
            EditorInfo editorInfo = service.getCurrentInputEditorInfo();
            Class<?> support = Class.forName("jwc");
            Method supported = support.getMethod("n", android.content.Context.class,
                    EditorInfo.class);
            if (!Boolean.TRUE.equals(supported.invoke(null, service, editorInfo))) {
                return false;
            }

            Class<?> gifExtension = Class.forName(
                    "com.google.android.apps.inputmethod.libs.expression.extension.IGifKeyboardExtension");
            Class<?> activationClass = Class.forName("ohr");
            Object activation = activationClass.getField("c").get(null);
            HashMap<String, Object> arguments = new HashMap<>();
            arguments.put("extension_interface", gifExtension);
            arguments.put("activation_source", activation);
            arguments.put("query", query);

            Class<?> qbbClass = Class.forName("qbb");
            Class<?> qbcClass = Class.forName("qbc");
            Constructor<?> qbcConstructor = qbcClass.getConstructor(
                    int.class, qbbClass, Object.class, int.class);
            Object qbc = qbcConstructor.newInstance(-10059, null, arguments, Integer.MAX_VALUE);
            Class<?> oheClass = Class.forName("ohe");
            Object event = oheClass.getMethod("d", qbcClass).invoke(null, qbc);
            service.getClass().getMethod("dG", oheClass).invoke(service, event);
            return true;
        } catch (Throwable failure) {
            Log.w(TAG, "native GIF dispatch failed", failure);
            return false;
        }
    }

    private static void replaceCurrent(InputMethodService service, GboardShortcodeIndex.Entry entry) {
        InputConnection connection = service.getCurrentInputConnection();
        if (connection == null) return;
        CharSequence value = connection.getTextBeforeCursor(80, 0);
        Token token = Token.from(value == null ? "" : value.toString());
        if (token != null) replace(connection, token.length, entry.value);
        hide();
    }

    private static void replace(InputConnection connection, int deleteLength, String value) {
        replacing = true;
        try {
            connection.beginBatchEdit();
            connection.deleteSurroundingText(deleteLength, 0);
            connection.commitText(value, 1);
            connection.endBatchEdit();
        } finally { replacing = false; }
    }

    private static void hide() {
        if (activeHost != null) GboardShortcodeView.remove(activeHost);
        activeHost = null;
    }

    private static boolean isPassword(EditorInfo info) {
        if (info == null) return false;
        int type = info.inputType;
        int cls = type & InputType.TYPE_MASK_CLASS;
        int variation = type & InputType.TYPE_MASK_VARIATION;
        return (cls == InputType.TYPE_CLASS_TEXT && (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD
                || variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                || variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD))
                || (cls == InputType.TYPE_CLASS_NUMBER && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD);
    }

    static final class Token {
        final String name; final int length; final boolean closed;
        Token(String name, int length, boolean closed) { this.name = name; this.length = length; this.closed = closed; }
        static Token from(String text) {
            if (text == null || text.isEmpty()) return null;
            boolean closed = text.endsWith(":");
            int searchEnd = closed ? text.length() - 1 : text.length();
            int colon = text.lastIndexOf(':', searchEnd - 1);
            if (colon < 0 || searchEnd - colon - 1 > 40) return null;
            String name = text.substring(colon + 1, searchEnd);
            if (!name.matches("[A-Za-z0-9_+-]*")) return null;
            return new Token(name, text.length() - colon, closed);
        }
    }

    static final class GifToken {
        final String query;
        final int length;
        GifToken(String query, int length) { this.query = query; this.length = length; }

        static GifToken from(String text) {
            if (text == null || text.length() < 4 || !text.endsWith("?")) return null;
            int opening = text.lastIndexOf('?', text.length() - 2);
            if (opening < 0 || (opening > 0 && !Character.isWhitespace(text.charAt(opening - 1)))) {
                return null;
            }
            String query = text.substring(opening + 1, text.length() - 1).trim();
            if (query.length() < 2 || query.length() > 50 || query.indexOf('\n') >= 0
                    || query.indexOf('\r') >= 0 || query.indexOf('?') >= 0) return null;
            return new GifToken(query, text.length() - opening);
        }
    }
}
