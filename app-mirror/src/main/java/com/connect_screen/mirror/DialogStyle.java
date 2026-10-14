package com.connect_screen.mirror;

import android.widget.Button;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.TypedValue;

/**
 * 应用主题中的 android:buttonStyle 把按钮文字颜色设为白色，该样式同时被平台版
 * AlertDialog 的按钮栏采用。弹窗按钮栏没有深色底，白字落在浅色底上不可见。
 * 这里在弹窗显示后统一为按钮指定可见的文字颜色，避免逐个布局遗漏。
 */
public final class DialogStyle {

    private DialogStyle() {
    }

    public static void apply(AlertDialog dialog) {
        dialog.setOnShowListener(ignored -> {
            try {
                recolor(dialog.getButton(AlertDialog.BUTTON_POSITIVE), androidx.appcompat.R.attr.colorPrimary);
                recolor(dialog.getButton(AlertDialog.BUTTON_NEGATIVE), com.google.android.material.R.attr.colorOnSurfaceVariant);
                recolor(dialog.getButton(AlertDialog.BUTTON_NEUTRAL), com.google.android.material.R.attr.colorOnSurfaceVariant);
            } catch (Throwable ignored2) {
                // Cosmetic only: styling must never stop a dialog from showing.
            }
        });
    }

    private static void recolor(Button button, int attrRes) {
        if (button == null) {
            return;
        }
        Integer color = resolve(button, attrRes);
        if (color == null) {
            return;
        }
        button.setTextColor(color);
        // Dialog buttons are text buttons: drop the filled background the app button
        // style would otherwise apply, so text and fill never share a colour.
        button.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
    }

    private static Integer resolve(Button button, int attrRes) {
        try {
            TypedValue value = new TypedValue();
            if (!button.getContext().getTheme().resolveAttribute(attrRes, value, true)) {
                return null;
            }
            if (value.type >= TypedValue.TYPE_FIRST_COLOR_INT && value.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                return value.data;
            }
            if (value.resourceId != 0) {
                return button.getContext().getColor(value.resourceId);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
