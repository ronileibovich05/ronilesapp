package com.example.ronilesapp;

import static com.example.ronilesapp.FBRef.*;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;

public class WelcomeActivity extends AppCompatActivity {

    Button btnEdit, btnDelete, btnAdd;
    TextView welcomeText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_welcome);

        // Edge-to-Edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // התחברות ל-Views
        welcomeText = findViewById(R.id.textView2);
        btnEdit = findViewById(R.id.button_edit);
        btnDelete = findViewById(R.id.button_delete);
        btnAdd = findViewById(R.id.button_AddItem);

        // קבלת המשתמש המחובר
        FirebaseUser user = mAuth.getCurrentUser();

        // כפתור עריכה
        btnEdit.setOnClickListener(v -> {
            User updatedUser = new User("name1", "abc@gmail.com", 44);
            if (user != null) {
                userRef.child(user.getUid()).setValue(updatedUser);
                Toast.makeText(WelcomeActivity.this, "הפרטים עודכנו", Toast.LENGTH_SHORT).show();
            }
        });

        // כפתור מחיקה
        btnDelete.setOnClickListener(v -> {
            if (user != null) {
                userRef.child(user.getUid()).removeValue()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(WelcomeActivity.this, "נתוני המשתמש נמחקו", Toast.LENGTH_SHORT).show();
                                welcomeText.setText("Welcome");
                            } else {
                                Toast.makeText(WelcomeActivity.this, "שגיאה במחיקה: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });

        // כפתור הוספת מוצר
        btnAdd.setOnClickListener(this::onClick);

        // ברכת Welcome
        if (user != null) {
            String email = user.getEmail();
            String name = email != null ? email.split("@")[0] : "User";
            welcomeText.setText("Welcome, " + name);
        } else {
            welcomeText.setText("Welcome");
        }
    }

    public void logout(View view) {
        FirebaseAuth.getInstance().signOut();
        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
        finish();
    }

    private void onClick(View v) {
        // יוצרים אובייקט Item לדוגמה
        com.example.ronilesapp.Item item = new com.example.ronilesapp.Item("Milk", 1.0, 8.3);

        // יצירת מפתח ייחודי
        String keyID = refItems.push().getKey();
        item.setKeyID(keyID);

        // שמירה ל-Firebase
        refItems.child(item.getKeyID()).setValue(item)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(WelcomeActivity.this, "המוצר נוסף בהצלחה!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(WelcomeActivity.this, "שגיאה בהוספה: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
