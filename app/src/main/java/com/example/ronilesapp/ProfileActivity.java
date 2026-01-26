package com.example.ronilesapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar; // חדש
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot; // חדש
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ProfileActivity extends BaseActivity {

    // משתנים קיימים
    private ImageView profileImageView;
    private TextView tvFirstName, tvLastName, tvEmail;
    private BottomNavigationView bottomNavigation;
    private Button btnEditProfile;
    private ScrollView scrollProfile;

    // --- משתנים חדשים לסטטיסטיקה ול-Logout ---
    private ProgressBar progressBarStats;
    private TextView tvPercentage, tvTotalTasks, tvCompletedTasks, tvPendingTasks;
    private Button btnLogout;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener themeListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);

        // חיבור ה-Views הקיימים
        profileImageView = findViewById(R.id.imageviewProfile);
        tvFirstName = findViewById(R.id.tvFirstName);
        tvLastName = findViewById(R.id.tvLastName);
        tvEmail = findViewById(R.id.tvEmail);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        scrollProfile = findViewById(R.id.scrollProfile);

        // --- חיבור ה-Views החדשים (סטטיסטיקה) ---
        progressBarStats = findViewById(R.id.progressBarStats);
        tvPercentage = findViewById(R.id.tvPercentage);
        tvTotalTasks = findViewById(R.id.tvTotalTasks);
        tvCompletedTasks = findViewById(R.id.tvCompletedTasks);
        tvPendingTasks = findViewById(R.id.tvPendingTasks);
        btnLogout = findViewById(R.id.btnLogout);

        // הגדרת הניווט התחתון
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

        // טעינת פרופיל משתמש
        loadUserProfile();

        // --- חישוב סטטיסטיקות ---
        calculateStats();

        // מאזינים לכפתורים
        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());

        // כפתור התנתקות
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // החלת ערכת נושא
        applyThemeColors();

        themeListener = (prefs, key) -> {
            if ("theme".equals(key)) {
                applyThemeColors();
            }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(themeListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // רענון הסטטיסטיקה בכל פעם שחוזרים למסך (למשל אם מחקו משימה וחזרו)
        calculateStats();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sharedPreferences != null && themeListener != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(themeListener);
        }
    }

    private void calculateStats() {
        if (!NetworkUtil.isConnected(this)) return;

        FBRef.getUserTasksRef().get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                int total = 0;
                int done = 0;

                // 1. קובעים את נקודת ההתחלה של "היום" (00:00 בבוקר)
                java.util.Calendar calendar = java.util.Calendar.getInstance();
                calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
                calendar.set(java.util.Calendar.MINUTE, 0);
                calendar.set(java.util.Calendar.SECOND, 0);
                calendar.set(java.util.Calendar.MILLISECOND, 0);

                long startOfDay = calendar.getTimeInMillis();

                for (QueryDocumentSnapshot doc : task.getResult()) {
                    Task t = doc.toObject(Task.class);

                    // 2. בדיקה אמינה יותר: האם המשימה נוצרה היום?
                    // (בודק לפי זמן יצירה - creationTime)
                    if (t.getCreationTime() >= startOfDay) {
                        total++;
                        if (t.isDone()) {
                            done++;
                        }
                    }
                }

                int pending = total - done;
                int progress = (total == 0) ? 0 : (done * 100 / total);

                // עדכון המסך
                tvTotalTasks.setText(String.valueOf(total));
                tvCompletedTasks.setText(String.valueOf(done));
                tvPendingTasks.setText(String.valueOf(pending));

                progressBarStats.setProgress(progress);
                tvPercentage.setText(progress + "%");

            } else {
                Toast.makeText(this, "Failed to load stats", Toast.LENGTH_SHORT).show();
            }
        });
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
        // כפתור ההתנתקות נשאר אדום בדרך כלל, אבל אם תרצי לשנות גם אותו:
        // btnLogout.setBackgroundColor(buttonColor);
    }

    private void loadUserProfile() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "No User Connected", Toast.LENGTH_SHORT).show();
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
                }
            }
        });
    }

    private void showEditProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Profile");

        EditText inputFirstName = new EditText(this);
        inputFirstName.setHint("First Name");
        inputFirstName.setText(tvFirstName.getText().toString());

        EditText inputLastName = new EditText(this);
        inputLastName.setHint("Last Name");
        inputLastName.setText(tvLastName.getText().toString());

        EditText inputProfileUrl = new EditText(this);
        inputProfileUrl.setHint("Profile Image URL");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);
        layout.addView(inputFirstName);
        layout.addView(inputLastName);
        layout.addView(inputProfileUrl);

        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) ->
                updateUserProfile(
                        inputFirstName.getText().toString().trim(),
                        inputLastName.getText().toString().trim(),
                        inputProfileUrl.getText().toString().trim()
                ));

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updateUserProfile(String firstName, String lastName, String profileUrl) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FBRef.refUsers.document(uid)
                .update("firstName", firstName, "lastName", lastName, "profileImageUrl", profileUrl)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "User profile updated successfully", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(this, "Error updating user profile", Toast.LENGTH_SHORT).show());
    }
}