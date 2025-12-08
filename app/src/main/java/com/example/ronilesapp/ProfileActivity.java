package com.example.ronilesapp;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

public class ProfileActivity extends AppCompatActivity {

    private ImageView profileImageView;
    private TextView tvFirstName, tvLastName, tvEmail;

    private static final String PREFS_NAME = "AppSettingsPrefs";
    private static final String KEY_THEME = "theme";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        // 🔹 מיישם את ה-Theme שנבחר לפני setContentView
        applySelectedTheme();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        profileImageView = findViewById(R.id.imageviewProfile);
        tvFirstName = findViewById(R.id.tvFirstName);
        tvLastName = findViewById(R.id.tvLastName);
        tvEmail = findViewById(R.id.tvEmail);

        loadUserProfile();
    }

    private void applySelectedTheme() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String theme = prefs.getString(KEY_THEME, "pink_brown");

        switch (theme) {
            case "pink_brown":
                setTheme(R.style.Theme_PinkBrown);
                break;
            case "blue_white":
                setTheme(R.style.Theme_BlueWhite);
                break;
            case "green_white":
                setTheme(R.style.Theme_GreenWhite);
                break;
        }
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
