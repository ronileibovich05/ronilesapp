package com.example.ronilesapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ProfileActivity extends BaseActivity {

    private ImageView profileImageView;
    private TextView tvFirstName, tvLastName, tvEmail;
    private BottomNavigationView bottomNavigation;
    private Button btnEditProfile;
    private ScrollView scrollProfile;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener themeListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);

        profileImageView = findViewById(R.id.imageviewProfile);
        tvFirstName = findViewById(R.id.tvFirstName);
        tvLastName = findViewById(R.id.tvLastName);
        tvEmail = findViewById(R.id.tvEmail);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        scrollProfile = findViewById(R.id.scrollProfile);

        bottomNavigation.setSelectedItemId(R.id.nav_profile);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(ProfileActivity.this, TasksActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(ProfileActivity.this, SettingsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_profile) {
                return true;
            }
            return false;
        });

        loadUserProfile();
        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());

        applyThemeColors();

        themeListener = (prefs, key) -> {
            if ("theme".equals(key)) {
                applyThemeColors();
            }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(themeListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sharedPreferences != null && themeListener != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(themeListener);
        }
    }

    private void applyThemeColors() {
        String theme = sharedPreferences.getString("theme", "pink_brown");

        int backgroundColor, textColor, buttonColor;

        switch (theme) {
            case "pink_brown":
                backgroundColor = getResources().getColor(R.color.pink_background);
                textColor = getResources().getColor(R.color.brown);
                buttonColor = getResources().getColor(R.color.pink_primary);
                break;
            case "blue_white":
                backgroundColor = getResources().getColor(R.color.blue_background);
                textColor = getResources().getColor(R.color.black);
                buttonColor = getResources().getColor(R.color.blue_primary);
                break;
            case "green_white":
                backgroundColor = getResources().getColor(R.color.green_background);
                textColor = getResources().getColor(R.color.black);
                buttonColor = getResources().getColor(R.color.green_primary);
                break;
            default:
                backgroundColor = getResources().getColor(R.color.pink_background);
                textColor = getResources().getColor(R.color.brown);
                buttonColor = getResources().getColor(R.color.pink_primary);
        }

        scrollProfile.setBackgroundColor(backgroundColor);
        tvFirstName.setTextColor(textColor);
        tvLastName.setTextColor(textColor);
        tvEmail.setTextColor(textColor);
        btnEditProfile.setBackgroundColor(buttonColor);
    }


    private void loadUserProfile() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "אין משתמש מחובר", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FBRef.refUsers.document(uid).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot doc = task.getResult();
                if (doc != null && doc.exists()) {
                    tvFirstName.setText(doc.getString("firstName"));
                    tvLastName.setText(doc.getString("lastName"));
                    tvEmail.setText(doc.getString("email"));

                    String profileImageUrl = doc.getString("profileImageUrl");
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Glide.with(this)
                                .load(Uri.parse(profileImageUrl))
                                .placeholder(R.drawable.ic_default_profile)
                                .into(profileImageView);
                    } else {
                        profileImageView.setImageResource(R.drawable.ic_default_profile);
                    }
                } else {
                    Toast.makeText(this, "לא נמצאו נתוני משתמש", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "שגיאה בטעינת פרופיל", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("עריכת פרופיל");

        EditText inputFirstName = new EditText(this);
        inputFirstName.setHint("שם פרטי");
        inputFirstName.setText(tvFirstName.getText().toString());

        EditText inputLastName = new EditText(this);
        inputLastName.setHint("שם משפחה");
        inputLastName.setText(tvLastName.getText().toString());

        EditText inputProfileUrl = new EditText(this);
        inputProfileUrl.setHint("כתובת URL תמונת פרופיל");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);
        layout.addView(inputFirstName);
        layout.addView(inputLastName);
        layout.addView(inputProfileUrl);

        builder.setView(layout);

        builder.setPositiveButton("שמור", (dialog, which) ->
                updateUserProfile(
                        inputFirstName.getText().toString().trim(),
                        inputLastName.getText().toString().trim(),
                        inputProfileUrl.getText().toString().trim()
                ));

        builder.setNegativeButton("ביטול", null);
        builder.show();
    }

    private void updateUserProfile(String firstName, String lastName, String profileUrl) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FBRef.refUsers.document(uid)
                .update("firstName", firstName, "lastName", lastName, "profileImageUrl", profileUrl)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "פרטי המשתמש עודכנו בהצלחה", Toast.LENGTH_SHORT).show();
                    tvFirstName.setText(firstName);
                    tvLastName.setText(lastName);
                    if (!profileUrl.isEmpty()) {
                        Glide.with(this)
                                .load(Uri.parse(profileUrl))
                                .placeholder(R.drawable.ic_default_profile)
                                .into(profileImageView);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "שגיאה בעדכון פרטי משתמש", Toast.LENGTH_SHORT).show());
    }
}
