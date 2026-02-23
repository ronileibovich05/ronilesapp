package com.example.ronilesapp;

import static com.example.ronilesapp.Utils.mAuth;
import static com.example.ronilesapp.Utils.refUsers;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseUser;

public class UserActivity extends BaseActivity { // שינוי ל-BaseActivity

    private TextView tvName, tvEmail;
    private ImageView profileImageView;
    private Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_user);

        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        profileImageView = findViewById(R.id.imageview_profile);
        btnLogout = findViewById(R.id.btnLogout);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        FirebaseUser current = mAuth.getCurrentUser();
        if (current == null) {
            Toast.makeText(this, "אין משתמש מחובר", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String uid = current.getUid();

        // שליפת פרטי המשתמש ממסד הנתונים
        refUsers.document(uid).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                com.google.firebase.firestore.DocumentSnapshot snapshot = task.getResult();
                if (snapshot != null && snapshot.exists()) {
                    User user = snapshot.toObject(User.class);
                    if (user != null) {
                        tvName.setText(user.getFirstName() + " " + user.getLastName());
                        tvEmail.setText(user.getEmail());

                        String imageString = user.getProfileImageUrl();

                        // בדיקה וטעינת התמונה (קישור רגיל או Base64)
                        if (imageString != null && !imageString.isEmpty()) {
                            if (imageString.startsWith("http")) {
                                Glide.with(UserActivity.this)
                                        .load(imageString)
                                        .placeholder(R.drawable.ic_default_profile)
                                        .error(R.drawable.ic_default_profile)
                                        .into(profileImageView);
                            } else {
                                try {
                                    byte[] decodedString = android.util.Base64.decode(imageString, android.util.Base64.DEFAULT);
                                    android.graphics.Bitmap decodedByte = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                    profileImageView.setImageBitmap(decodedByte);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    profileImageView.setImageResource(R.drawable.ic_default_profile);
                                }
                            }
                        } else {
                            profileImageView.setImageResource(R.drawable.ic_default_profile);
                        }
                    }
                } else {
                    tvName.setText("משתמש חדש");
                    tvEmail.setText(current.getEmail());
                    profileImageView.setImageResource(R.drawable.ic_default_profile);
                }
            } else {
                Toast.makeText(UserActivity.this, "שגיאה בטעינת נתונים", Toast.LENGTH_LONG).show();
            }
        });

        // כפתור התנתקות מסודר
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(UserActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}