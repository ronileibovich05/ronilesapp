package com.example.ronilesapp;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {

    protected static final String PREFS_NAME = "AppSettingsPrefs";
    protected static final String KEY_THEME = "theme";

    // 🔹 זוכר איזה theme היה בפעם האחרונה
    private String lastTheme;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        applySelectedTheme();
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        lastTheme = prefs.getString(KEY_THEME, "pink_brown");
    }


    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String currentTheme = prefs.getString(KEY_THEME, "pink_brown");

        if (lastTheme != null && !lastTheme.equals(currentTheme)) {
            lastTheme = currentTheme; // חשוב לעדכן לפני
            recreate();
            return;
        }

        lastTheme = currentTheme;
    }


    protected void applySelectedTheme() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String theme = prefs.getString(KEY_THEME, "pink_brown");

        switch (theme) {
            case "pink_brown":
                setTheme(R.style.Theme_PinkBrown);
                break;
            case "blue_white":
                setTheme(R.style.Theme_BlueWhite);
                break;
            case "green_white":
                setTheme(R.style.Theme_GreenWhite);
                break;
        }
    }
}
