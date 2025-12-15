package com.example.ronilesapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import android.content.SharedPreferences;

public class SettingsActivity extends BaseActivity {

    private Switch switchNotifications;
    private RadioGroup radioGroupTheme;
    private RadioButton rbPinkBrown, rbBlueWhite, rbGreenWhite;
    private Button btnLogout;
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // חיבור ל־UI
        switchNotifications = findViewById(R.id.switchNotifications);
        radioGroupTheme = findViewById(R.id.radioGroupTheme);
        rbPinkBrown = findViewById(R.id.rbPinkBrown);
        rbBlueWhite = findViewById(R.id.rbBlueWhite);
        rbGreenWhite = findViewById(R.id.rbGreenWhite);
        btnLogout = findViewById(R.id.btnLogout);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // מסמן את הפריט הנוכחי
        bottomNavigation.setSelectedItemId(R.id.nav_settings);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {  // TasksActivity
                startActivity(new Intent(SettingsActivity.this, TasksActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_profile) {  // ProfileActivity
                startActivity(new Intent(SettingsActivity.this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_settings) {  // Activity הנוכחי
                return true;
            }
            return false;
        });

        // SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        loadSettings(sharedPreferences);

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
            if (checkedId == R.id.rbPinkBrown) {
                editor.putString("theme", "pink_brown");
            } else if (checkedId == R.id.rbBlueWhite) {
                editor.putString("theme", "blue_white");
            } else if (checkedId == R.id.rbGreenWhite) {
                editor.putString("theme", "green_white");
            }
            editor.apply();
            // הפעלה מחדש של Activity כדי להחיל את Theme החדש
            recreate();
        });

        // התנתקות
        btnLogout.setOnClickListener(v -> {
            // כאן אפשר לנקות את המשתמש הנוכחי אם יש FirebaseAuth
            startActivity(new Intent(SettingsActivity.this, LoginActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        });
    }

    private void loadSettings(SharedPreferences sharedPreferences) {
        boolean notificationsEnabled = sharedPreferences.getBoolean("notifications", true);
        switchNotifications.setChecked(notificationsEnabled);

        String theme = sharedPreferences.getString("theme", "pink_brown");
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
}
