package com.example.ronilesapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_welcome);

        // התאמת Edge-to-Edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // קבלת המשתמש המחובר
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        TextView welcomeText = findViewById(R.id.textView2);

        if (user != null) {
            String email = user.getEmail();
            String name = email != null ? email.split("@")[0] : "User"; // החלק לפני ה-@
            welcomeText.setText("Welcome, " + name);
        } else {
            welcomeText.setText("Welcome");
        }
    }

    public void logout(View view) {
        // ניתוק המשתמש
        FirebaseAuth.getInstance().signOut();

        // הודעת Toast
        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();

        // מעבר למסך ההתחברות
        startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
        finish(); // סוגר את המסך הנוכחי
    }
}