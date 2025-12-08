package com.example.ronilesapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private Switch switchNotifications;
    private RadioGroup radioGroupTheme;
    private RadioButton rbPinkBrown, rbBlueWhite, rbGreenWhite;
    private Button btnLogout;

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "AppSettingsPrefs";
    private static final String KEY_NOTIFICATIONS = "notifications";
    private static final String KEY_THEME = "theme";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🔹 מיישם את ה-Theme לפי הבחירה של המשתמש
        applySelectedTheme();

        setContentView(R.layout.activity_settings);

        // חיבור ל־UI
        switchNotifications = findViewById(R.id.switchNotifications);
        radioGroupTheme = findViewById(R.id.radioGroupTheme);
        rbPinkBrown = findViewById(R.id.rbPinkBrown);
        rbBlueWhite = findViewById(R.id.rbBlueWhite);
        rbGreenWhite = findViewById(R.id.rbGreenWhite);
        btnLogout = findViewById(R.id.btnLogout);

        // SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        loadSettings();

        // 🔹 שינוי מצב התראות
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(KEY_NOTIFICATIONS, isChecked);
            editor.apply();
            Toast.makeText(this, isChecked ? "התראות מופעלות" : "התראות כבויות", Toast.LENGTH_SHORT).show();
        });

        // 🔹 שינוי Theme
        radioGroupTheme.setOnCheckedChangeListener((group, checkedId) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            if (checkedId == R.id.rbPinkBrown) {
                editor.putString(KEY_THEME, "pink_brown");
            } else if (checkedId == R.id.rbBlueWhite) {
                editor.putString(KEY_THEME, "blue_white");
            } else if (checkedId == R.id.rbGreenWhite) {
                editor.putString(KEY_THEME, "green_white");
            }
            editor.apply();

            // הפעלה מחדש של ה-Activity כדי להחיל את ה-Theme החדש
            recreate();
        });

        // 🔹 התנתקות
        btnLogout.setOnClickListener(v -> {
            // כאן אפשר לנקות את המשתמש הנוכחי אם יש FirebaseAuth
            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadSettings() {
        boolean notificationsEnabled = sharedPreferences.getBoolean(KEY_NOTIFICATIONS, true);
        switchNotifications.setChecked(notificationsEnabled);

        String theme = sharedPreferences.getString(KEY_THEME, "pink_brown");
        switch (theme) {
            case "pink_brown":
                rbPinkBrown.setChecked(true);
                break;
            case "blue_white":
                rbBlueWhite.setChecked(true);
                break;
            case "green_white":
                rbGreenWhite.setChecked(true);
                break;
        }
    }

    // 🔹 פונקציה שמיישמת את ה-Theme לפי הבחירה
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
