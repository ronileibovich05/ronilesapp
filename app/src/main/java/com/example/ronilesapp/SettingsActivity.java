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
import com.google.firebase.firestore.FirebaseFirestore;

public class SettingsActivity extends BaseActivity {

    private Switch switchNotifications;
    private RadioGroup radioGroupTheme;
    private RadioButton rbPinkBrown, rbBlueWhite, rbGreenWhite;
    private Button btnChangePassword, btnShareApp, btnAdminPanel;
    private TextView tvVersion;
    private BottomNavigationView bottomNavigation;
    private ConstraintLayout rootLayout;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener themeListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);

        // --- חיבור ל-Views ---
        rootLayout = findViewById(R.id.rootLayoutSettings);
        switchNotifications = findViewById(R.id.switchNotifications);
        radioGroupTheme = findViewById(R.id.radioGroupTheme);
        rbPinkBrown = findViewById(R.id.rbPinkBrown);
        rbBlueWhite = findViewById(R.id.rbBlueWhite);
        rbGreenWhite = findViewById(R.id.rbGreenWhite);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnShareApp = findViewById(R.id.btnShareApp);
        tvVersion = findViewById(R.id.tvVersion);
        btnAdminPanel = findViewById(R.id.btnAdminPanel);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // --- בדיקת מנהל ---
        checkIfAdmin();

        // --- ניווט תחתון ---
        setupNavigation();

        // --- טעינת הגדרות והחלת עיצוב ---
        loadSettings();
        applyThemeColors();
        setAppVersion();

        // --- מאזינים ---
        setupListeners();
    }

    private void setupListeners() {
        // התראות
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("notifications_enabled", isChecked).apply();
            Toast.makeText(this, isChecked ? "Notifications Enabled" : "Notifications Disabled", Toast.LENGTH_SHORT).show();
        });

        // שינוי ערכת נושא - כאן הוספתי את ה-recreate()
        radioGroupTheme.setOnCheckedChangeListener((group, checkedId) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            if (checkedId == R.id.rbPinkBrown) editor.putString("theme", "pink_brown");
            else if (checkedId == R.id.rbBlueWhite) editor.putString("theme", "blue_white");
            else if (checkedId == R.id.rbGreenWhite) editor.putString("theme", "green_white");
            editor.apply();

            // רענון המסך מיד כדי לראות את השינוי
            recreate();
        });

        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        btnShareApp.setOnClickListener(v -> shareApp());
        btnAdminPanel.setOnClickListener(v -> startActivity(new Intent(this, AdminDashboardActivity.class)));
    }

    private void checkIfAdmin() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("Users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean isAdmin = documentSnapshot.getBoolean("isAdmin");
                        if (isAdmin != null && isAdmin) {
                            btnAdminPanel.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    private void loadSettings() {
        boolean notificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", true);
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
        int backgroundColor, textColor;

        switch (theme) {
            case "blue_white":
                backgroundColor = getResources().getColor(R.color.blue_background);
                textColor = getResources().getColor(android.R.color.black);
                break;
            case "green_white":
                backgroundColor = getResources().getColor(R.color.green_background);
                textColor = getResources().getColor(android.R.color.black);
                break;
            default:
                backgroundColor = getResources().getColor(R.color.pink_background);
                textColor = getResources().getColor(R.color.brown);
                break;
        }

        if (rootLayout != null) rootLayout.setBackgroundColor(backgroundColor);
        rbPinkBrown.setTextColor(textColor);
        rbBlueWhite.setTextColor(textColor);
        rbGreenWhite.setTextColor(textColor);
    }

    private void setupNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_settings);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, TasksActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return id == R.id.nav_settings;
        });
    }

    private void showChangePasswordDialog() {
        EditText newPasswordInput = new EditText(this);
        newPasswordInput.setHint("Enter new password (min 6 chars)");
        new AlertDialog.Builder(this).setTitle("Change Password").setView(newPasswordInput)
                .setPositiveButton("Update", (dialog, which) -> {
                    String newPass = newPasswordInput.getText().toString();
                    if (newPass.length() >= 6) {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null) {
                            user.updatePassword(newPass).addOnCompleteListener(task -> {
                                if (task.isSuccessful()) Toast.makeText(this, "Password Updated", Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    private void shareApp() {
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Hey! Check out RonilesApp! 📝");
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "Share App via"));
    }

    private void setAppVersion() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            tvVersion.setText("Version " + pInfo.versionName);
        } catch (Exception e) {
            tvVersion.setText("Version 1.0");
        }
    }
}