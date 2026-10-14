package com.connect_screen.mirror;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

/** Persists and applies the user-facing appearance controls for every screen. */
public final class UiTheme {
    private static final String PREFS = "ui_appearance";
    public static final String COLOR = "color_scheme";
    public static final String MODE = "theme_mode";
    public static final String COLOR_AZURE = "azure";
    public static final String COLOR_TEAL = "teal";
    public static final String COLOR_VIOLET = "violet";
    public static final String COLOR_SYSTEM = "system";
    public static final String MODE_LIGHT = "light";
    public static final String MODE_DARK = "dark";
    public static final String MODE_OLED = "oled";
    public static final String MODE_SYSTEM = "system";

    private UiTheme() { }

    public static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static void apply(Activity activity) {
        SharedPreferences prefs = prefs(activity);
        String mode = prefs.getString(MODE, MODE_LIGHT);
        AppCompatDelegate.setDefaultNightMode(nightMode(mode));
        if (MODE_OLED.equals(mode)) {
            activity.setTheme(R.style.AppTheme_Oled);
        } else {
            activity.setTheme(R.style.AppTheme);
        }
    }

    /** The accent scheme is fixed to Azure blue; only the night mode stays user selectable. */
    public static void saveMode(Context context, String mode) {
        prefs(context).edit().putString(MODE, mode).apply();
        AppCompatDelegate.setDefaultNightMode(nightMode(mode));
    }

    public static void save(Context context, String color, String mode) {
        prefs(context).edit().putString(COLOR, color).putString(MODE, mode).apply();
        AppCompatDelegate.setDefaultNightMode(nightMode(mode));
    }

    private static int nightMode(String mode) {
        if (MODE_SYSTEM.equals(mode)) return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        return MODE_LIGHT.equals(mode) ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES;
    }
}
