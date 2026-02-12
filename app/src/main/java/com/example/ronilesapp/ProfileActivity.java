package com.example.ronilesapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ProfileActivity extends BaseActivity {

    private ImageView profileImageView;
    private TextView tvFirstName, tvLastName, tvEmail;
    private BottomNavigationView bottomNavigation;
    private Button btnEditProfile;
    private ScrollView scrollProfile;

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

        // חיבור רכיבים
        profileImageView = findViewById(R.id.imageviewProfile);
        tvFirstName = findViewById(R.id.tvFirstName);
        tvLastName = findViewById(R.id.tvLastName);
        tvEmail = findViewById(R.id.tvEmail);
        bottomNavigation = findViewById(R.id.bottomNavigation);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        scrollProfile = findViewById(R.id.scrollProfile);
        progressBarStats = findViewById(R.id.progressBarStats);
        tvPercentage = findViewById(R.id.tvPercentage);
        tvTotalTasks = findViewById(R.id.tvTotalTasks);
        tvCompletedTasks = findViewById(R.id.tvCompletedTasks);
        tvPendingTasks = findViewById(R.id.tvPendingTasks);
        btnLogout = findViewById(R.id.btnLogout);

        // הגדרת תפריט ניווט
        bottomNavigation.setSelectedItemId(R.id.nav_profile);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, TasksActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return id == R.id.nav_profile;
        });

        loadUserProfile();
        calculateStats();

        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // האזנה לשינויי Theme ורענון המסך בזמן אמת
        themeListener = (prefs, key) -> {
            if ("theme".equals(key)) {
                recreate(); // מרענן את כל ה-Activity כדי שהעיצוב יתעדכן מיד
            }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(themeListener);

        applyThemeColors();
    }

    @Override
    protected void onStart() {
        super.onStart();
        calculateStats(); // רענון נתונים בכל פעם שחוזרים למסך
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sharedPreferences != null && themeListener != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(themeListener);
        }
    }

    private void calculateStats() {
        // תיקון: אם אין אינטרנט, תציג הודעה ותעצור
        if (!Utils.isConnected(this)) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        Utils.getUserTasksRef().get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                int total = 0;
                int done = 0;

                for (QueryDocumentSnapshot doc : task.getResult()) {
                    Task t = doc.toObject(Task.class);
                    total++;
                    if (t.isDone()) { // וודאי שבמחלקה Task הפונקציה נקראת isDone()
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
            }
        });
    }

    private void applyThemeColors() {
        String theme = sharedPreferences.getString("theme", "pink_brown");
        int backgroundColor, textColor, buttonColor;

        switch (theme) {
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
            default: // pink_brown
                backgroundColor = getResources().getColor(R.color.pink_background);
                textColor = getResources().getColor(R.color.brown);
                buttonColor = getResources().getColor(R.color.pink_primary);
                break;
        }

        scrollProfile.setBackgroundColor(backgroundColor);
        tvFirstName.setTextColor(textColor);
        tvLastName.setTextColor(textColor);
        tvEmail.setTextColor(textColor);
        btnEditProfile.setBackgroundColor(buttonColor);
        // הטקסטים של הסטטיסטיקה
        tvTotalTasks.setTextColor(textColor);
        tvCompletedTasks.setTextColor(textColor);
        tvPendingTasks.setTextColor(textColor);
        tvPercentage.setTextColor(textColor);
    }

    private void loadUserProfile() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        Utils.refUsers.document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                tvFirstName.setText(doc.getString("firstName"));
                tvLastName.setText(doc.getString("lastName"));
                tvEmail.setText(doc.getString("email"));

                String url = doc.getString("profileImageUrl");
                if (url != null && !url.isEmpty()) {
                    Glide.with(this).load(url).placeholder(R.drawable.ic_default_profile).into(profileImageView);
                }
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
                updateUserProfile(inputFirstName.getText().toString(), inputLastName.getText().toString())
        );
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updateUserProfile(String firstName, String lastName) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        Utils.refUsers.document(uid)
                .update("firstName", firstName, "lastName", lastName)
                .addOnSuccessListener(aVoid -> {
                    tvFirstName.setText(firstName);
                    tvLastName.setText(lastName);
                    Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show();
                });
    }
}