package com.example.ronilesapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ProfileActivity extends BaseActivity {

    private ImageView profileImageView;
    private TextView tvFirstName, tvLastName, tvEmail;
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        profileImageView = findViewById(R.id.imageviewProfile);
        tvFirstName = findViewById(R.id.tvFirstName);
        tvLastName = findViewById(R.id.tvLastName);
        tvEmail = findViewById(R.id.tvEmail);
        bottomNavigation = findViewById(R.id.bottomNavigation);

        // בוחר את הפריט הנוכחי
        bottomNavigation.setSelectedItemId(R.id.nav_profile);

        // מאזין ללחיצות בפריטים
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {  // TasksActivity
                startActivity(new Intent(ProfileActivity.this, TasksActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_settings) {  // SettingsActivity
                startActivity(new Intent(ProfileActivity.this, SettingsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_profile) {  // Activity הנוכחי
                return true;
            }
            return false;
        });

        loadUserProfile();
    }

    private void loadUserProfile() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FBRef.refUsers.document(uid).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot doc = task.getResult();
                if (doc != null && doc.exists()) {
                    String firstName = doc.getString("firstName");
                    String lastName = doc.getString("lastName");
                    String email = doc.getString("email");
                    String profileImageUrl = doc.getString("profileImageUrl");

                    tvFirstName.setText(firstName != null ? firstName : "");
                    tvLastName.setText(lastName != null ? lastName : "");
                    tvEmail.setText(email != null ? email : "");

                    if (profileImageUrl != null) {
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
}
