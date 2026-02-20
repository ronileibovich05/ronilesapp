package com.example.ronilesapp;

import static com.example.ronilesapp.Utils.mAuth;
import static com.example.ronilesapp.Utils.refUsers;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

public class UserActivity extends AppCompatActivity {
    //TODO BaseActivity

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
        refUsers.document(uid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot snapshot = task.getResult();
                    if (snapshot != null && snapshot.exists()) {
                        User user = snapshot.toObject(User.class);
                        if (user != null) {
                            tvName.setText(user.getFirstName() + " " + user.getLastName());
                            tvEmail.setText(user.getEmail());

                            // הצגת תמונה אם קיימת

                            String url = user.getProfileImageUrl();

                            Glide.with(UserActivity.this)
                                    .load(url)
                                    .placeholder(R.drawable.ic_default_profile)
                                    .error(R.drawable.ic_default_profile)
                                    .into(profileImageView);

                        }
                    } else {
                        tvName.setText("משתמש חדש");
                        tvEmail.setText(current.getEmail());
                        profileImageView.setImageResource(R.drawable.ic_default_profile);
                    }
                } else {
                    String errorMsg = "Unknown error";
                    Exception e = task.getException();
                    if (e != null)
                        errorMsg = e.getMessage();
                    Toast.makeText(UserActivity.this, "שגיאה בטעינת נתונים: " +
                            errorMsg, Toast.LENGTH_LONG).show();
                }
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                UserActivity.this.finish();
            }
        });
    }
}
