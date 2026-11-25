package dev.jason.gboardpatches.extension.calculator;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Iterator;

final class GboardShortcodeIndex {
    static final class Entry {
        final String name;
        final String value;
        Entry(String name, String value) { this.name = name; this.value = value; }
    }

    private static volatile GboardShortcodeIndex instance;
    private final Map<String, String> values;

    private GboardShortcodeIndex(Map<String, String> values) { this.values = values; }

    static GboardShortcodeIndex get(Context context) throws Exception {
        GboardShortcodeIndex current = instance;
        if (current != null) return current;
        synchronized (GboardShortcodeIndex.class) {
            if (instance == null) instance = load(context);
            return instance;
        }
    }

    Entry exact(String name) {
        String normalized = normalize(name);
        String value = values.get(normalized);
        return value == null ? null : new Entry(normalized, value);
    }

    List<Entry> prefix(String prefix, int limit) {
        String normalized = normalize(prefix);
        if (normalized.isEmpty()) return Collections.emptyList();
        List<Entry> matches = new ArrayList<>();
        for (Map.Entry<String, String> item : values.entrySet()) {
            if (item.getKey().startsWith(normalized)) {
                matches.add(new Entry(item.getKey(), item.getValue()));
            }
        }
        matches.sort(Comparator.comparingInt((Entry e) -> e.name.length()).thenComparing(e -> e.name));
        return matches.size() <= limit ? matches : new ArrayList<>(matches.subList(0, limit));
    }

    private static GboardShortcodeIndex load(Context context) throws Exception {
        Map<String, String> result = new HashMap<>();
        String emojiText = GboardShortcodeData.emojiJson();
        JSONArray emojis = new JSONArray(emojiText);
        for (int i = 0; i < emojis.length(); i++) {
            JSONObject emoji = emojis.optJSONObject(i);
            if (emoji == null) continue;
            String value = fromUnified(emoji.optString("unified", ""));
            if (value.isEmpty()) continue;
            LinkedHashSet<String> names = new LinkedHashSet<>();
            names.add(emoji.optString("short_name", ""));
            JSONArray aliases = emoji.optJSONArray("short_names");
            if (aliases != null) for (int j = 0; j < aliases.length(); j++) names.add(aliases.optString(j, ""));
            for (String name : names) if (!normalize(name).isEmpty()) result.putIfAbsent(normalize(name), value);
        }
        JSONObject symbols = new JSONObject(GboardShortcodeData.symbolJson());
        Iterator<String> symbolNames = symbols.keys();
        while (symbolNames.hasNext()) {
            String name = symbolNames.next();
            result.put(normalize(name), symbols.optString(name, ""));
        }
        return new GboardShortcodeIndex(result);
    }

    private static String fromUnified(String unified) {
        if (unified == null || unified.isEmpty()) return "";
        StringBuilder value = new StringBuilder();
        try {
            for (String part : unified.split("-")) value.appendCodePoint(Integer.parseInt(part, 16));
        } catch (RuntimeException ignored) { return ""; }
        return value.toString();
    }

    static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace('-', '_');
    }
}
