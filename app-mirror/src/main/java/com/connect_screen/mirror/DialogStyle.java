package com.connect_screen.mirror;

import android.widget.Button;

import android.app.AlertDialog;

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
            recolor(dialog.getButton(AlertDialog.BUTTON_POSITIVE), R.color.cs_primary_dark);
            recolor(dialog.getButton(AlertDialog.BUTTON_NEGATIVE), R.color.cs_text_secondary);
            recolor(dialog.getButton(AlertDialog.BUTTON_NEUTRAL), R.color.cs_text_secondary);
        });
    }

    private static void recolor(Button button, int colorRes) {
        if (button != null) {
            button.setTextColor(button.getContext().getColor(colorRes));
        }
    }
}
