package dev.jason.gboardpatches.extension.calculator;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ColorDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.PopupWindow;
import java.util.List;

final class GboardShortcodeView {
    private static final String TAG = "gboard-patches-shortcodes";
    private static PopupWindow activePopup;

    static void show(GboardCalculatorSuggestionView.Placement placement,
            List<GboardShortcodeIndex.Entry> entries, boolean vertical, Click click) {
        FrameLayout host = placement.host();
        remove(host);
        Context context = host.getContext();
        boolean dark = (context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        int surface = dark ? 0xff303134 : 0xfff1f3f4;
        int foreground = dark ? Color.WHITE : 0xff202124;
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(vertical ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        row.setGravity(vertical ? Gravity.START : Gravity.CENTER_VERTICAL);
        if (vertical) {
            GradientDrawable card = new GradientDrawable();
            card.setColor(surface);
            card.setCornerRadius(dp(context, 12));
            card.setStroke(dp(context, 1), dark ? 0xff4a4a4a : 0xffdadce0);
            row.setBackground(card);
            row.setPadding(dp(context, 4), dp(context, 4), dp(context, 4), dp(context, 4));
        }
        int visibleCount = vertical ? Math.min(entries.size(), 4) : entries.size();
        int measuredVerticalWidth = 0;
        for (int index = 0; index < visibleCount; index++) {
            GboardShortcodeIndex.Entry entry = entries.get(index);
            TextView chip = new TextView(context);
            chip.setText(entry.value + "  :" + entry.name + ":");
            chip.setTextColor(foreground);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            chip.setGravity(vertical ? Gravity.CENTER_VERTICAL | Gravity.START : Gravity.CENTER);
            int horizontalPadding = dp(context, vertical ? 8 : 14);
            chip.setPadding(horizontalPadding, 0, horizontalPadding, 0);
            chip.setSingleLine(true);
            if (vertical) {
                int contentWidth = (int) Math.ceil(
                        chip.getPaint().measureText(chip.getText().toString()));
                measuredVerticalWidth = Math.max(
                        measuredVerticalWidth,
                        contentWidth + horizontalPadding * 2 + dp(context, 8));
            }
            if (!vertical) {
                GradientDrawable background = new GradientDrawable();
                background.setColor(surface);
                background.setCornerRadius(dp(context, 18));
                chip.setBackground(background);
            }
            chip.setOnClickListener(v -> click.accept(entry));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    vertical ? ViewGroup.LayoutParams.MATCH_PARENT
                            : ViewGroup.LayoutParams.WRAP_CONTENT,
                    dp(context, 40));
            if (!vertical) params.setMarginEnd(dp(context, 6));
            row.addView(chip, params);
        }
        View content;
        if (vertical) {
            content = row;
        } else {
            HorizontalScrollView scroll = new HorizontalScrollView(context);
            scroll.setTag(TAG);
            scroll.setHorizontalScrollBarEnabled(false);
            scroll.setFillViewport(false);
            scroll.addView(row);
            content = scroll;
        }
        content.setTag(TAG);
        int popupHeight = dp(context, vertical ? visibleCount * 40 + 8 : 44);
        int popupWidth = vertical
                ? Math.max(dp(context, 120), Math.min(dp(context, 230), measuredVerticalWidth))
                : ViewGroup.LayoutParams.MATCH_PARENT;
        PopupWindow popup = new PopupWindow(
                content, popupWidth, popupHeight, false);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setOutsideTouchable(false);
        popup.setTouchable(true);
        popup.setClippingEnabled(true);
        popup.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        popup.setElevation(dp(context, 24));
        int popupTop = Math.max(
                0,
                placement.topMargin() - popupHeight - dp(context, 6));
        popup.showAtLocation(host, vertical ? Gravity.TOP | Gravity.START
                        : Gravity.TOP | Gravity.CENTER_HORIZONTAL,
                0, popupTop);
        activePopup = popup;
    }

    static void remove(FrameLayout host) {
        PopupWindow popup = activePopup;
        activePopup = null;
        if (popup != null) {
            try { popup.dismiss(); } catch (RuntimeException ignored) { }
        }
        if (host == null) return;
        for (int i = host.getChildCount() - 1; i >= 0; i--) {
            View child = host.getChildAt(i);
            if (child != null && TAG.equals(child.getTag())) host.removeViewAt(i);
        }
    }

    interface Click { void accept(GboardShortcodeIndex.Entry entry); }
    private static int dp(Context context, int value) {
        return Math.max(1, Math.round(value * context.getResources().getDisplayMetrics().density));
    }
}
