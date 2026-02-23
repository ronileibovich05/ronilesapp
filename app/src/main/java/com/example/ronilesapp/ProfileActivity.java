package com.example.ronilesapp;

import static com.example.ronilesapp.Utils.refUsers;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class ProfileActivity extends BaseActivity {

    private ImageView profileImageView;
    private TextView tvFirstName, tvLastName, tvEmail;
    private BottomNavigationView bottomNavigationView;
    private Button btnEditProfile;
    private ScrollView scrollProfile;

    private ProgressBar progressBarStats;
    private TextView tvPercentage, tvTotalTasks, tvCompletedTasks, tvPendingTasks;
    private Button btnLogout;

    private SharedPreferences.OnSharedPreferenceChangeListener themeListener;
    private String currentProfileImageUrl = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        applySelectedTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        profileImageView = findViewById(R.id.imageviewProfile);
        tvFirstName = findViewById(R.id.tvFirstName);
        tvLastName = findViewById(R.id.tvLastName);
        tvEmail = findViewById(R.id.tvEmail);
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        scrollProfile = findViewById(R.id.scrollProfile);
        progressBarStats = findViewById(R.id.progressBarStats);
        tvPercentage = findViewById(R.id.tvPercentage);
        tvTotalTasks = findViewById(R.id.tvTotalTasks);
        tvCompletedTasks = findViewById(R.id.tvCompletedTasks);
        tvPendingTasks = findViewById(R.id.tvPendingTasks);
        btnLogout = findViewById(R.id.btnLogout);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(ProfileActivity.this, TasksActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(ProfileActivity.this, SettingsActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return id == R.id.nav_profile;
        });

        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        themeListener = (prefs, key) -> {
            if (BaseActivity.KEY_THEME.equals(key)) {
                recreate();
            }
        };
        baseSharedPreferences.registerOnSharedPreferenceChangeListener(themeListener);

        applyThemeColors();
        loadUserProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNavigationView.getSelectedItemId() != R.id.nav_profile) {
            bottomNavigationView.setSelectedItemId(R.id.nav_profile);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        calculateStats();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (baseSharedPreferences != null && themeListener != null) {
            baseSharedPreferences.unregisterOnSharedPreferenceChangeListener(themeListener);
        }
    }

    private void calculateStats() {
        if (!Utils.isConnected(this)) {
            Toast.makeText(this, "No Internet Connection", Toast.LENGTH_SHORT).show();
            return;
        }

        Utils.getUserTasksRef().get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                int total = 0;
                int done = 0;

                for (QueryDocumentSnapshot doc : task.getResult()) {
                    UserTask t = doc.toObject(UserTask.class);
                    total++;
                    if (t.isDone()) {
                        done++;
                    }
                }

                int pending = total - done;
                int progress = (total == 0) ? 0 : (done * 100 / total);

                tvTotalTasks.setText(String.valueOf(total));
                tvCompletedTasks.setText(String.valueOf(done));
                tvPendingTasks.setText(String.valueOf(pending));
                progressBarStats.setProgress(progress);
                tvPercentage.setText(progress + "%");
            } else {
                Toast.makeText(ProfileActivity.this, "Failed to load stats", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyThemeColors() {
        String theme = baseSharedPreferences.getString(BaseActivity.KEY_THEME, "pink_brown");
        int backgroundColor, textColor, buttonColor;

        switch (theme) {
            case "blue_white":
                backgroundColor = ContextCompat.getColor(this, R.color.blue_background);
                textColor = ContextCompat.getColor(this, R.color.black);
                buttonColor = ContextCompat.getColor(this, R.color.blue_primary);
                break;
            case "green_white":
                backgroundColor = ContextCompat.getColor(this, R.color.green_background);
                textColor = ContextCompat.getColor(this, R.color.black);
                buttonColor = ContextCompat.getColor(this, R.color.green_primary);
                break;
            default: // pink_brown
                backgroundColor = ContextCompat.getColor(this, R.color.pink_background);
                textColor = ContextCompat.getColor(this, R.color.brown);
                buttonColor = ContextCompat.getColor(this, R.color.pink_primary);
                break;
        }

        scrollProfile.setBackgroundColor(backgroundColor);
        tvFirstName.setTextColor(textColor);
        tvLastName.setTextColor(textColor);
        tvEmail.setTextColor(textColor);

        tvTotalTasks.setTextColor(textColor);
        tvCompletedTasks.setTextColor(textColor);
        tvPendingTasks.setTextColor(textColor);
        tvPercentage.setTextColor(textColor);

        // Buttons using Tint instead of setBackgroundColor
        btnEditProfile.setBackgroundTintList(android.content.res.ColorStateList.valueOf(buttonColor));
        btnLogout.setBackgroundTintList(android.content.res.ColorStateList.valueOf(buttonColor));
        btnLogout.setTextColor(ContextCompat.getColor(this, android.R.color.white));
    }

    private void loadUserProfile() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        refUsers.document(uid).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot doc = task.getResult();
                if (doc != null && doc.exists()) {
                    tvFirstName.setText(doc.getString("firstName"));
                    tvLastName.setText(doc.getString("lastName"));
                    tvEmail.setText(doc.getString("email"));

                    String imageString = doc.getString("profileImageUrl");
                    if (imageString != null && !imageString.isEmpty()) {
                        currentProfileImageUrl = imageString;
                        loadImageFromString(imageString);
                    } else {
                        profileImageView.setImageResource(R.drawable.ic_default_profile);
                    }
                }
            } else {
                Toast.makeText(ProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Profile");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText inputFirstName = new EditText(this);
        inputFirstName.setHint("First Name");
        inputFirstName.setText(tvFirstName.getText().toString());
        layout.addView(inputFirstName);

        final EditText inputLastName = new EditText(this);
        inputLastName.setHint("Last Name");
        inputLastName.setText(tvLastName.getText().toString());
        layout.addView(inputLastName);

        builder.setView(layout);
        builder.setPositiveButton("Save", (dialog, which) ->
                updateUserProfile(inputFirstName.getText().toString().trim(), inputLastName.getText().toString().trim())
        );
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updateUserProfile(String firstName, String lastName) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        refUsers.document(uid)
                .update("firstName", firstName, "lastName", lastName)
                .addOnSuccessListener(aVoid -> {
                    tvFirstName.setText(firstName);
                    tvLastName.setText(lastName);
                    Toast.makeText(ProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Error updating profile", Toast.LENGTH_SHORT).show());
    }

    private void loadImageFromString(String imageString) {
        if (imageString.startsWith("http")) {
            Glide.with(this).load(imageString)
                    .placeholder(R.drawable.ic_default_profile).into(profileImageView);
        } else {
            try {
                byte[] decoded = android.util.Base64.decode(imageString, android.util.Base64.DEFAULT);
                profileImageView.setImageBitmap(
                        android.graphics.BitmapFactory.decodeByteArray(decoded, 0, decoded.length));
            } catch (Exception e) {
                profileImageView.setImageResource(R.drawable.ic_default_profile);
            }
        }
    }
}