package com.example.ronilesapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class SettingsActivity extends BaseActivity {

    private Switch switchNotifications;
    private RadioGroup radioGroupTheme;
    private RadioButton rbPinkBrown, rbBlueWhite, rbGreenWhite;
    private Button btnChangePassword, btnShareApp, btnAdminPanel;
    private TextView tvVersion;
    private BottomNavigationView bottomNavigation;
    private ConstraintLayout rootLayout;

    private SharedPreferences.OnSharedPreferenceChangeListener themeListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        applySelectedTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

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

        btnAdminPanel.setVisibility(View.GONE);

        // --- ניווט תחתון ---
        setupNavigation();

        // --- בדיקת מנהל ---
        checkIfAdmin();

        // --- טעינת הגדרות והחלת עיצוב ---
        loadSettings();
        //Todo applyThemeColors not needed because we have recreate
        setAppVersion();


        themeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences prefs, @Nullable String key) {
                if (BaseActivity.KEY_THEME.equals(key)) {
                    SettingsActivity.this.recreate();   // TODO ה recreate מרענן את כל ה-Activity - לעומת applyThemeColors
                }
            }
        };
        baseSharedPreferences.registerOnSharedPreferenceChangeListener(themeListener);

        // --- מאזינים ---
        setupListeners();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (baseSharedPreferences != null && themeListener != null) {
            baseSharedPreferences.unregisterOnSharedPreferenceChangeListener(themeListener);
        }
    }


    private void setupListeners() {
        // התראות
        switchNotifications.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                baseSharedPreferences.edit().putBoolean("notifications_enabled", isChecked).apply();
                Toast.makeText(SettingsActivity.this, isChecked ? "Notifications Enabled" : "Notifications Disabled", Toast.LENGTH_SHORT).show();
            }
        });

        // שינוי ערכת נושא
        radioGroupTheme.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                SharedPreferences.Editor editor = baseSharedPreferences.edit();
                if (checkedId == R.id.rbPinkBrown) editor.putString(BaseActivity.KEY_THEME, "pink_brown");
                else if (checkedId == R.id.rbBlueWhite) editor.putString(BaseActivity.KEY_THEME, "blue_white");
                else if (checkedId == R.id.rbGreenWhite) editor.putString(BaseActivity.KEY_THEME, "green_white");
                editor.apply();

                // recreate() מטופל על ידי המאזין
                // SettingsActivity.this.recreate();
            }
        });

        btnChangePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SettingsActivity.this.showChangePasswordDialog();
            }
        });
        btnShareApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SettingsActivity.this.shareApp();
            }
        });
        btnAdminPanel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SettingsActivity.this.startActivity(new Intent(SettingsActivity.this, AdminDashboardActivity.class));
            }
        });
    }

    private void checkIfAdmin() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();
        FirebaseFirestore.getInstance().collection("Users").document(uid).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            Boolean isAdmin = documentSnapshot.getBoolean("isAdmin");
                            if (isAdmin != null && isAdmin) {
                                btnAdminPanel.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                });
    }

    private void loadSettings() {
        boolean notificationsEnabled = baseSharedPreferences.getBoolean("notifications_enabled", true);
        switchNotifications.setChecked(notificationsEnabled);

        String theme = baseSharedPreferences.getString(BaseActivity.KEY_THEME, "pink_brown");
        switch (theme) {
            case "pink_brown": rbPinkBrown.setChecked(true); break;
            case "blue_white": rbBlueWhite.setChecked(true); break;
            case "green_white": rbGreenWhite.setChecked(true); break;
        }
    }

    private void applyThemeColors() {
        String theme = baseSharedPreferences.getString(BaseActivity.KEY_THEME, "pink_brown");
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
        bottomNavigation.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    SettingsActivity.this.startActivity(new Intent(SettingsActivity.this, TasksActivity.class));
                    SettingsActivity.this.overridePendingTransition(0, 0);
                    SettingsActivity.this.finish();
                    return true;
                } else if (id == R.id.nav_profile) {
                    SettingsActivity.this.startActivity(new Intent(SettingsActivity.this, ProfileActivity.class));
                    SettingsActivity.this.overridePendingTransition(0, 0);
                    SettingsActivity.this.finish();
                    return true;
                }
                return id == R.id.nav_settings;
            }
        });
    }

    private void showChangePasswordDialog() {
        EditText newPasswordInput = new EditText(this);
        newPasswordInput.setHint("Enter new password (min 6 chars)");
        new AlertDialog.Builder(this).setTitle("Change Password").setView(newPasswordInput)
                .setPositiveButton("Update", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newPass = newPasswordInput.getText().toString();
                        if (newPass.length() >= 6) {
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            if (user != null) {
                                user.updatePassword(newPass).addOnCompleteListener(task -> {
                                    if (task.isSuccessful())
                                        Toast.makeText(SettingsActivity.this, "Password Updated", Toast.LENGTH_SHORT).show();
                                });
                            }
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