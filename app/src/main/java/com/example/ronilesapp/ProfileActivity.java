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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

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

    // משתנה לשמירת הקישור הנוכחי לתמונה
    private String currentProfileImageUrl = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        applySelectedTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // חיבור רכיבים
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

        // הגדרת תפריט ניווט
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    ProfileActivity.this.startActivity(new Intent(ProfileActivity.this, TasksActivity.class));
                    ProfileActivity.this.overridePendingTransition(0, 0);
                    return true;
                } else if (id == R.id.nav_settings) {
                    ProfileActivity.this.startActivity(new Intent(ProfileActivity.this, SettingsActivity.class));
                    ProfileActivity.this.overridePendingTransition(0, 0);
                    return true;
                }
                return id == R.id.nav_profile;
            }
        });

        btnEditProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ProfileActivity.this.showEditProfileDialog();
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                ProfileActivity.this.startActivity(intent);
            }
        });

        // האזנה לשינויי Theme ורענון המסך בזמן אמת
        themeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences prefs, @Nullable String key) {
                if (BaseActivity.KEY_THEME.equals(key)) {
                    ProfileActivity.this.recreate();    // ה recreate מרענן את כל ה-Activity - לעומת applyThemeColors
                }
            }
        };
        baseSharedPreferences.registerOnSharedPreferenceChangeListener(themeListener);

        applyThemeColors();

        loadUserProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // לוודא שה־Profile מסומן כשחוזרים למסך
        if (bottomNavigationView.getSelectedItemId() != R.id.nav_profile) {
            bottomNavigationView.setSelectedItemId(R.id.nav_profile);
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        calculateStats(); // רענון נתונים בכל פעם שחוזרים למסך
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // הסרת המאזין
        if (baseSharedPreferences != null && themeListener != null) {
            baseSharedPreferences.unregisterOnSharedPreferenceChangeListener(themeListener);
        }
    }

    private void calculateStats() {
        if (!Utils.isConnected(this)) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }

        Utils.getUserTasksRef().get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
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
                }else {
                    Toast.makeText(ProfileActivity.this, "Failed to load stats", Toast.LENGTH_SHORT).show();
                }
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
        btnEditProfile.setBackgroundColor(buttonColor);
        tvTotalTasks.setTextColor(textColor);
        tvCompletedTasks.setTextColor(textColor);
        tvPendingTasks.setTextColor(textColor);
        tvPercentage.setTextColor(textColor);

        //TODO btnLogout.setBackgroundColor(buttonColor);
        //TODO btnLogout.setTextColor(textColor);
    }

    private void loadUserProfile() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        refUsers.document(uid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot doc = task.getResult();
                    if (doc != null && doc.exists()) {
                        // טעינת הטקסטים
                        tvFirstName.setText(doc.getString("firstName"));
                        tvLastName.setText(doc.getString("lastName"));
                        tvEmail.setText(doc.getString("email"));

                        // תמונה
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

        // לתמונה
        final EditText inputImageUrl = new EditText(this);
        inputImageUrl.setHint("Image URL (leave empty to keep current)");
        inputImageUrl.setText(currentProfileImageUrl); // מציג את הקישור הקיים אם יש
        layout.addView(inputImageUrl);

        builder.setView(layout);
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ProfileActivity.this.updateUserProfile(
                                inputFirstName.getText().toString().trim(),
                                inputLastName.getText().toString().trim(),
                                inputImageUrl.getText().toString().trim() // שולח גם את ה-URL
                        );
                    }
                }
        );
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // הפונקציה שמקבלת גם URL
    private void updateUserProfile(String firstName, String lastName, String imageUrl) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null)
            return;

        refUsers.document(uid)
                .update("firstName", firstName,
                        "lastName", lastName,
                        "profileImageUrl", imageUrl) // מעדכן גם את התמונה ב-Firebase
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        tvFirstName.setText(firstName);
                        tvLastName.setText(lastName);
                        currentProfileImageUrl = imageUrl; // עדכון המשתנה המקומי

                        // רענון התמונה במסך מיד עם Glide
                        if (!imageUrl.isEmpty()) {
                            loadImageFromString(imageUrl);
                        } else {
                            profileImageView.setImageResource(R.drawable.ic_default_profile);
                        }

                        Toast.makeText(ProfileActivity.this, "Profile Updated Successfully!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ProfileActivity.this, "Error updating profile", Toast.LENGTH_SHORT).show();
                    }
                });
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