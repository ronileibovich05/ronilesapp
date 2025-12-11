package com.example.ronilesapp;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AppSettingsPrefs";
    private static final String KEY_THEME = "theme";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applySelectedTheme(); // מיישם את הצבע לפני שנטען ה-Layout
        super.onCreate(savedInstanceState);
    }

    private void applySelectedTheme() {
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
