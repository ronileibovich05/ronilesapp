package com.example.ronilesapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SettingsActivity extends BaseActivity {

    private Switch switchNotifications;
    private RadioGroup radioGroupTheme;
    private RadioButton rbPinkBrown, rbBlueWhite, rbGreenWhite;
    private Button btnLogout;
    private BottomNavigationView bottomNavigation;
    private LinearLayout rootLayout;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener themeListener;

    private static final String PREFS_NAME = "AppPrefs";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // חיבור ל־UI
        rootLayout = findViewById(R.id.rootLayoutSettings);
        switchNotifications = findViewById(R.id.switchNotifications);
        radioGroupTheme = findViewById(R.id.radioGroupTheme);
        rbPinkBrown = findViewById(R.id.rbPinkBrown);
        rbBlueWhite = findViewById(R.id.rbBlueWhite);
        rbGreenWhite = findViewById(R.id.rbGreenWhite);
        btnLogout = findViewById(R.id.btnLogout);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // מסמן את הפריט הנוכחי
        bottomNavigation.setSelectedItemId(R.id.nav_settings);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(SettingsActivity.this, TasksActivity.class));
                overridePendingTransition(0,0);
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(SettingsActivity.this, ProfileActivity.class));
                overridePendingTransition(0,0);
                return true;
            } else if (id == R.id.nav_settings) {
                return true;
            }
            return false;
        });

        // טעינת הגדרות קיימות
        loadSettings();

        // החלת צבעים בפעם הראשונה
        applyThemeColors();

        // מאזין ל־Theme בזמן אמת
        themeListener = (prefs, key) -> {
            if ("theme".equals(key)) {
                applyThemeColors();
            }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(themeListener);

        // שינוי מצב התראות
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("notifications", isChecked);
            editor.apply();
            Toast.makeText(this, isChecked ? "התראות מופעלות" : "התראות כבויות", Toast.LENGTH_SHORT).show();
        });

        // שינוי Theme
        radioGroupTheme.setOnCheckedChangeListener((group, checkedId) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            if (checkedId == R.id.rbPinkBrown) editor.putString("theme", "pink_brown");
            else if (checkedId == R.id.rbBlueWhite) editor.putString("theme", "blue_white");
            else if (checkedId == R.id.rbGreenWhite) editor.putString("theme", "green_white");
            editor.apply();
        });

        // התנתקות
        btnLogout.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, LoginActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        });
    }

    private void loadSettings() {
        boolean notificationsEnabled = sharedPreferences.getBoolean("notifications", true);
        switchNotifications.setChecked(notificationsEnabled);

        String theme = sharedPreferences.getString("theme", "pink_brown");
        switch (theme) {
            case "pink_brown": rbPinkBrown.setChecked(true); break;
            case "blue_white": rbBlueWhite.setChecked(true); break;
            case "green_white": rbGreenWhite.setChecked(true); break;
        }
    }

    private void applyThemeColors() {
        String theme = sharedPreferences.getString("theme", "pink_brown");
        int backgroundColor, buttonColor;

        switch(theme) {
            case "pink_brown":
                backgroundColor = getResources().getColor(R.color.pink_background);
                buttonColor = getResources().getColor(R.color.pink_primary);
                break;
            case "blue_white":
                backgroundColor = getResources().getColor(R.color.blue_background);
                buttonColor = getResources().getColor(R.color.blue_primary);
                break;
            case "green_white":
                backgroundColor = getResources().getColor(R.color.green_background);
                buttonColor = getResources().getColor(R.color.green_primary);
                break;
            default:
                backgroundColor = getResources().getColor(R.color.pink_background);
                buttonColor = getResources().getColor(R.color.pink_primary);
                break;
        }

        rootLayout.setBackgroundColor(backgroundColor);
        btnLogout.setBackgroundColor(buttonColor);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sharedPreferences != null && themeListener != null)
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(themeListener);
    }
}
