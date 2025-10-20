package com.example.ronilesapp;

import static com.example.ronilesapp.FBRef.*;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
            if (user == null) {
                Toast.makeText(this, "אין משתמש מחובר", Toast.LENGTH_SHORT).show();
                return;
            }

            // נביא את נתוני המשתמש הקיימים מה־Firebase
            userRef.child(user.getUid()).get().addOnSuccessListener(snapshot -> {
                if (!snapshot.exists()) {
                    Toast.makeText(this, "לא נמצאו נתונים למשתמש הזה", Toast.LENGTH_SHORT).show();
                    return;
                }

                // נשלוף את הנתונים הקיימים
                String currentName = snapshot.child("name").getValue(String.class);
                String currentEmail = snapshot.child("email").getValue(String.class);
                Long currentAgeLong = snapshot.child("age").getValue(Long.class);
                int currentAge = currentAgeLong != null ? currentAgeLong.intValue() : 0;

                // ניצור את תצוגת הדיאלוג
                View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_user, null);

                // נקשר את השדות
                EditText nameInput = dialogView.findViewById(R.id.nameInput);
                EditText emailInput = dialogView.findViewById(R.id.emailInput);
                EditText ageInput = dialogView.findViewById(R.id.ageInput);


                // נמלא נתונים קיימים
                nameInput.setText(currentName != null ? currentName : "");
                emailInput.setText(currentEmail != null ? currentEmail : "");
                ageInput.setText(currentAge > 0 ? String.valueOf(currentAge) : "");

                // נציג את הדיאלוג
                new androidx.appcompat.app.AlertDialog.Builder(WelcomeActivity.this)
                        .setTitle("עריכת פרטים")
                        .setView(dialogView)
                        .setPositiveButton("שמור", (dialog, which) -> {
                            String name = nameInput.getText().toString().trim();
                            String email = emailInput.getText().toString().trim();
                            String ageStr = ageInput.getText().toString().trim();

                            if (name.isEmpty() || email.isEmpty() || ageStr.isEmpty()) {
                                Toast.makeText(this, "נא למלא את כל השדות", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            int age;
                            try {
                                age = Integer.parseInt(ageStr);
                            } catch (NumberFormatException e) {
                                Toast.makeText(this, "הגיל חייב להיות מספר", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // יצירת אובייקט User חדש
                            User updatedUser = new User(name, email, "3",true,"4"); //TODO

                            // שמירה למסד הנתונים
                            userRef.child(user.getUid()).setValue(updatedUser)
                                    .addOnSuccessListener(a -> Toast.makeText(this, "הפרטים עודכנו בהצלחה!", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e -> Toast.makeText(this, "שגיאה: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        })
                        .setNegativeButton("ביטול", (dialog, which) -> dialog.dismiss())
                        .show();

            }).addOnFailureListener(e -> {
                Toast.makeText(this, "שגיאה בטעינת הנתונים: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
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
