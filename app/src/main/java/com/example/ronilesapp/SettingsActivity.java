package com.example.ronilesapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsActivity extends BaseActivity {

    private Switch switchNotifications;
    private RadioGroup radioGroupTheme;
    private RadioButton rbPinkBrown, rbBlueWhite, rbGreenWhite;

    // כפתורים חדשים
    private Button btnChangePassword, btnShareApp;
    private TextView tvVersion;

    private BottomNavigationView bottomNavigation;
    private ConstraintLayout rootLayout; // שמרתי על זה מהקוד שלך

    private SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener themeListener;

    private static final String PREFS_NAME = "AppPrefs";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE); // שים לב לשם הקבוע

        // --- חיבור ל-Views ---
        // rootLayout = findViewById(R.id.rootLayoutSettings); // אם יש לך משתנה כזה במחלקה
        Switch switchNotifications = findViewById(R.id.switchNotifications); // עדיף משתנה מקומי אם לא בשימוש בחוץ

        RadioGroup radioGroupTheme = findViewById(R.id.radioGroupTheme);
        RadioButton rbPinkBrown = findViewById(R.id.rbPinkBrown);
        RadioButton rbBlueWhite = findViewById(R.id.rbBlueWhite);
        RadioButton rbGreenWhite = findViewById(R.id.rbGreenWhite);

        Button btnChangePassword = findViewById(R.id.btnChangePassword);
        Button btnShareApp = findViewById(R.id.btnShareApp);
        TextView tvVersion = findViewById(R.id.tvVersion);

        // --- הוספה חדשה: כפתור מנהלים ---
        Button btnAdminPanel = findViewById(R.id.btnAdminPanel);

        com.google.android.material.bottomnavigation.BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);

        // --- בדיקת מנהל עם הודעות דיבאג ---
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();

        // הודעה 1: נראה שה-ID תקין
        // Toast.makeText(this, "Checking UID: " + uid, Toast.LENGTH_LONG).show();

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("Users")
                .document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // המסמך קיים, בואו נבדוק את השדה
                        Boolean isAdmin = documentSnapshot.getBoolean("isAdmin");

                        // הודעה 2: מה הערך שהתקבל?
                        Toast.makeText(this, "isAdmin value: " + isAdmin, Toast.LENGTH_LONG).show();

                        if (isAdmin != null && isAdmin) {
                            btnAdminPanel.setVisibility(View.VISIBLE);
                        }
                    } else {
                        // הודעה 3: המסמך לא נמצא!
                        Toast.makeText(this, "User document missing in Firestore!", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });

        // לחיצה על כפתור המנהלים
        btnAdminPanel.setOnClickListener(v -> {
            startActivity(new Intent(SettingsActivity.this, AdminDashboardActivity.class));
        });
        // ----------------------------------------------------

        // --- ניווט תחתון ---
        bottomNavigation.setSelectedItemId(R.id.nav_settings);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(SettingsActivity.this, TasksActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(SettingsActivity.this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_settings) {
                return true;
            }
            return false;
        });

        // --- טעינת הגדרות קיימות ---
        // loadSettings(); // וודאי שיש לך את הפונקציה הזו למטה
        // applyThemeColors(); // וודאי שיש לך את הפונקציה הזו למטה

        // מאזין לשינוי Theme
        SharedPreferences.OnSharedPreferenceChangeListener themeListener = (prefs, key) -> {
            if ("theme".equals(key)) {
                // applyThemeColors(); // קריאה לפונקציה שצובעת מחדש
                recreate(); // אופציה: לרענן את המסך כדי שהצבעים יתפסו מיד
            }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(themeListener);

        // --- לוגיקה: התראות ---
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("notifications_enabled", isChecked);
            editor.apply();
            Toast.makeText(this, isChecked ? "Notifications Enabled" : "Notifications Disabled", Toast.LENGTH_SHORT).show();
        });

        // --- לוגיקה: ערכת נושא ---
        radioGroupTheme.setOnCheckedChangeListener((group, checkedId) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            if (checkedId == R.id.rbPinkBrown) editor.putString("theme", "pink_brown");
            else if (checkedId == R.id.rbBlueWhite) editor.putString("theme", "blue_white");
            else if (checkedId == R.id.rbGreenWhite) editor.putString("theme", "green_white");
            editor.apply();
        });

        // --- כפתורים נוספים ---
        // btnChangePassword.setOnClickListener(v -> showChangePasswordDialog()); // וודאי שיש לך את הפונקציה
        // btnShareApp.setOnClickListener(v -> shareApp()); // וודאי שיש לך את הפונקציה

        tvVersion.setText("Version 1.0");
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
        int backgroundColor, buttonColor, textColor;

        switch (theme) {
            case "blue_white":
                backgroundColor = getResources().getColor(R.color.blue_background);
                buttonColor = getResources().getColor(R.color.blue_primary);
                textColor = getResources().getColor(R.color.black);
                break;
            case "green_white":
                backgroundColor = getResources().getColor(R.color.green_background);
                buttonColor = getResources().getColor(R.color.green_primary);
                textColor = getResources().getColor(R.color.black);
                break;
            default: // pink_brown
                backgroundColor = getResources().getColor(R.color.pink_background);
                buttonColor = getResources().getColor(R.color.pink_primary);
                textColor = getResources().getColor(R.color.brown);
                break;
        }

        if (rootLayout != null) rootLayout.setBackgroundColor(backgroundColor);

        // צביעת כפתורים וכיתובים
        rbPinkBrown.setTextColor(textColor);
        rbBlueWhite.setTextColor(textColor);
        rbGreenWhite.setTextColor(textColor);

        // אם רוצים לצבוע גם את הכפתורים החדשים לפי התמה:
        // btnChangePassword.setBackgroundColor(buttonColor); // אופציונלי
        // btnShareApp.setBackgroundColor(buttonColor); // אופציונלי
    }

    // --- פונקציות העזר החדשות ---

    private void showChangePasswordDialog() {
        EditText newPasswordInput = new EditText(this);
        newPasswordInput.setHint("Enter new password (min 6 chars)");

        new AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setView(newPasswordInput)
                .setPositiveButton("Update", (dialog, which) -> {
                    String newPass = newPasswordInput.getText().toString();
                    if (newPass.length() >= 6) {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null) {
                            user.updatePassword(newPass).addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(this, "Password Updated Successfully", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    } else {
                        Toast.makeText(this, "Password too short", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void shareApp() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Hey! Check out this amazing To-Do app called RonilesApp! 📝");
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "Share App via"));
    }

    private void setAppVersion() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            tvVersion.setText("Version " + pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            tvVersion.setText("Version 1.0");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sharedPreferences != null && themeListener != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(themeListener);
        }
    }
}