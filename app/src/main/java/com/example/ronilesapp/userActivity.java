package com.example.ronilesapp;

import static com.example.ronilesapp.FBRef.*;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

public class userActivity extends AppCompatActivity {

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

        // Edge-to-edge padding
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

        // קבלת נתונים מ-Firestore
        refUsers.document(uid).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot snapshot = task.getResult();
                if (snapshot != null && snapshot.exists()) {
                    User user = snapshot.toObject(User.class);
                    if (user != null) {
                        tvName.setText(user.getFirstName());
                        tvEmail.setText(user.getEmail());

                        // הצגת תמונה אם קיימת
                        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                            Uri imageUri = Uri.parse(user.getProfileImageUrl());
                            profileImageView.setImageURI(imageUri);
                        }
                    }
                } else {
                    tvName.setText("משתמש חדש");
                    tvEmail.setText(current.getEmail());
                }
            } else {
                Toast.makeText(userActivity.this, "שגיאה בטעינת נתונים: " +
                        task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            finish();
        });
    }
}
