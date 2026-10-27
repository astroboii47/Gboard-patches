package dev.jason.gboardpatches.extension.hideaccentpopups;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/** Reflection handles for the Gboard 18.5.4 SoftKeyDef / ActionDef metadata model. */
final class GboardHideAccentPopupsReflectionHandles {
    private final Method exactActionLookup;       // SoftKeyDef.g(qae)
    private final Field actionEntries;             // ActionDef.f : List<qbc>
    private final Field actionPopupLabels;         // ActionDef.p : List<String>
    private final Field actionPopupIcons;          // ActionDef.q : int[]
    private final Field entryKeycode;              // qbc.c : int
    private final Field entryPayload;              // qbc.e : Object
    private final Class<?> entryClass;              // qbc
    private final Constructor<?> actionBuilder;    // qaf (ActionDef builder)
    private final Method actionBuilderCopy;        // qaf.i(ActionDef)
    private final Method actionBuilderSetEntries;  // qaf.n(qbc...)
    private final Method actionBuilderSetLabels;   // qaf.t(String...)
    private final Field actionBuilderIcons;        // qaf.d : int[]
    private final Method actionBuilderBuild;       // qaf.c()
    private final Constructor<?> metadataBuilder;  // qcv (SoftKeyDef builder)
    private final Method metadataBuilderCopy;      // qcv.k(SoftKeyDef)
    private final Method metadataBuilderPut;       // qcv.t(ActionDef)
    private final Field metadataBuilderActions;    // qcv.v : EnumMap<qae, ActionDef>
    private final Method metadataBuilderBuild;     // qcv.d()
    private final Object pressType;
    private final Object longPressType;

    GboardHideAccentPopupsReflectionHandles(ClassLoader loader) throws Throwable {
        Class<?> softKeyDef = Class.forName(
                "com.google.android.libraries.inputmethod.metadata.SoftKeyDef", false, loader);
        Class<?> actionDef = Class.forName(
                "com.google.android.libraries.inputmethod.metadata.ActionDef", false, loader);
        Class<?> actionType = Class.forName("qae", false, loader);
        entryClass = Class.forName("qbc", false, loader);
        Class<?> actionBuilderClass = Class.forName("qaf", false, loader);
        Class<?> metadataBuilderClass = Class.forName("qcv", false, loader);
        Class<?> entryArrayClass = Array.newInstance(entryClass, 0).getClass();

        exactActionLookup = accessible(softKeyDef.getDeclaredMethod("g", actionType));
        actionEntries = accessible(actionDef.getDeclaredField("f"));
        actionPopupLabels = accessible(actionDef.getDeclaredField("p"));
        actionPopupIcons = accessible(actionDef.getDeclaredField("q"));
        entryKeycode = accessible(entryClass.getDeclaredField("c"));
        entryPayload = accessible(entryClass.getDeclaredField("e"));
        actionBuilder = accessible(actionBuilderClass.getDeclaredConstructor());
        actionBuilderCopy = accessible(actionBuilderClass.getDeclaredMethod("i", actionDef));
        actionBuilderSetEntries = accessible(
                actionBuilderClass.getDeclaredMethod("n", entryArrayClass));
        actionBuilderSetLabels = accessible(
                actionBuilderClass.getDeclaredMethod("t", String[].class));
        actionBuilderIcons = accessible(actionBuilderClass.getDeclaredField("d"));
        actionBuilderBuild = accessible(actionBuilderClass.getDeclaredMethod("c"));
        metadataBuilder = accessible(metadataBuilderClass.getDeclaredConstructor());
        metadataBuilderCopy = accessible(metadataBuilderClass.getDeclaredMethod("k", softKeyDef));
        metadataBuilderPut = accessible(metadataBuilderClass.getDeclaredMethod("t", actionDef));
        metadataBuilderActions = accessible(metadataBuilderClass.getDeclaredField("v"));
        metadataBuilderBuild = accessible(metadataBuilderClass.getDeclaredMethod("d"));

        if (!List.class.isAssignableFrom(actionEntries.getType())
                || !List.class.isAssignableFrom(actionPopupLabels.getType())
                || actionPopupIcons.getType() != int[].class
                || !Map.class.isAssignableFrom(metadataBuilderActions.getType())) {
            throw new IllegalStateException("18.5.4 SoftKeyDef/ActionDef shape drift");
        }
        pressType = enumValue(actionType, "PRESS");
        longPressType = enumValue(actionType, "LONG_PRESS");
    }

    String extractPressText(Object metadata) throws Throwable {
        Object[] entries = entries(exactActionLookup.invoke(metadata, pressType));
        Object payload = entries.length == 0 || entries[0] == null
                ? null : entryPayload.get(entries[0]);
        return payload instanceof CharSequence ? payload.toString() : null;
    }

    /**
     * Returns a copy of {@code metadata} without the accented long-press entries, or {@code null}
     * when the key has nothing to hide.
     */
    Object withoutAccentedLongPressEntries(Object metadata, String pressText) throws Throwable {
        Object longPress = exactActionLookup.invoke(metadata, longPressType);
        Object[] entries = entries(longPress);
        int[] keycodes = new int[entries.length];
        Object[] payloads = new Object[entries.length];
        for (int index = 0; index < entries.length; index++) {
            if (entries[index] != null) {
                keycodes[index] = entryKeycode.getInt(entries[index]);
                payloads[index] = entryPayload.get(entries[index]);
            }
        }
        boolean[] keep = GboardHideAccentPopupsPolicy.planKeepMask(pressText, keycodes, payloads);
        if (keep == null) {
            return null;
        }

        Object builder = metadataBuilder.newInstance();
        metadataBuilderCopy.invoke(builder, metadata);
        Object filteredEntries = filterEntries(entries, keep);
        if (Array.getLength(filteredEntries) == 0) {
            // Nothing but accents: drop LONG_PRESS so the key behaves like one without a popup.
            ((Map<?, ?>) metadataBuilderActions.get(builder)).remove(longPressType);
        } else {
            Object action = actionBuilder.newInstance();
            actionBuilderCopy.invoke(action, longPress);
            actionBuilderSetEntries.invoke(action, filteredEntries);

            // Popup labels/icons are per-entry only when their size matches the entry count.
            Object labelsValue = actionPopupLabels.get(longPress);
            if (labelsValue instanceof List<?> labels && labels.size() == entries.length) {
                actionBuilderSetLabels.invoke(action, (Object) filterLabels(labels, keep));
            }
            Object iconsValue = actionPopupIcons.get(longPress);
            if (iconsValue instanceof int[] icons && icons.length == entries.length) {
                actionBuilderIcons.set(action, filterIcons(icons, keep));
            }
            Object patchedAction = actionBuilderBuild.invoke(action);
            if (patchedAction == null) {
                return null;
            }
            metadataBuilderPut.invoke(builder, patchedAction);
        }
        return metadataBuilderBuild.invoke(builder);
    }

    private Object[] entries(Object action) throws IllegalAccessException {
        Object value = action == null ? null : actionEntries.get(action);
        return value instanceof List<?> ? ((List<?>) value).toArray() : new Object[0];
    }

    private Object filterEntries(Object[] entries, boolean[] keep) {
        Object result = Array.newInstance(entryClass, keptCount(keep));
        for (int index = 0, target = 0; index < keep.length; index++) {
            if (keep[index]) {
                Array.set(result, target++, entries[index]);
            }
        }
        return result;
    }

    private static String[] filterLabels(List<?> labels, boolean[] keep) {
        String[] result = new String[keptCount(keep)];
        for (int index = 0, target = 0; index < keep.length; index++) {
            if (keep[index]) {
                Object label = labels.get(index);
                result[target++] = label == null ? null : label.toString();
            }
        }
        return result;
    }

    private static int[] filterIcons(int[] icons, boolean[] keep) {
        int[] result = new int[keptCount(keep)];
        for (int index = 0, target = 0; index < keep.length; index++) {
            if (keep[index]) {
                result[target++] = icons[index];
            }
        }
        return result;
    }

    private static int keptCount(boolean[] keep) {
        int count = 0;
        for (boolean kept : keep) {
            count += kept ? 1 : 0;
        }
        return count;
    }

    private static <T extends AccessibleObject> T accessible(T member) {
        member.setAccessible(true);
        return member;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object enumValue(Class<?> enumClass, String name) {
        return Enum.valueOf((Class<? extends Enum>) enumClass.asSubclass(Enum.class), name);
    }
}
