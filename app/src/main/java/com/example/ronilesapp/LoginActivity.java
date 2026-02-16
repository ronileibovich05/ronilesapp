package com.example.ronilesapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

// 1. שינוי: יורש מ-BaseActivity כדי לקבל את הרקע המשתנה
public class LoginActivity extends BaseActivity {

    private FirebaseAuth mAuth;
    private EditText emailEditText, passwordEditText;
    private CheckBox rememberCheckBox;
    private Button btnLogin; // משתנה לכפתור כדי שנוכל לצבוע אותו

    private SharedPreferences themePrefs; // העדפות לערכת נושא

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        // חיבור ל-SharedPreferences של העיצוב (AppPrefs)
        themePrefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);

        // חיבור Views
        emailEditText = findViewById(R.id.edittext_email);
        passwordEditText = findViewById(R.id.edittext_password);
        rememberCheckBox = findViewById(R.id.checkbox_remember);

        // --- חשוב: ---
        // נסי למצוא את הכפתור לפי ה-ID שיש לך ב-XML.
        // אם ב-activity_login.xml אין לכפתור ID, תוסיפי לו: android:id="@+id/btn_login"
        try {
            btnLogin = findViewById(R.id.button_login);
            // אם ה-ID אצלך שונה (למשל buttonLogin), תשני כאן בהתאם!
        } catch (Exception e) {
            e.printStackTrace();
        }

        // לוגיקה קיימת: בדיקה אם נשמרו פרטי התחברות (loginPrefs)
        SharedPreferences loginPrefs = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        boolean remember = loginPrefs.getBoolean("remember", false);

        if (remember) {
            String savedEmail = loginPrefs.getString("email", "");
            String savedPassword = loginPrefs.getString("password", "");
            emailEditText.setText(savedEmail);
            passwordEditText.setText(savedPassword);
            rememberCheckBox.setChecked(true);
        }

        // 2. החלת הצבעים
        applyThemeColors();
    }

    // פונקציה לצביעת הכפתורים וה-CheckBox לפי ה-Theme
    private void applyThemeColors() {
        String theme = themePrefs.getString("theme", "pink_brown");
        int primaryColor;

        // בחירת הצבע הנכון לפי ה-Theme
        switch (theme) {
            case "blue_white":
                primaryColor = getResources().getColor(R.color.blue_primary);
                break;
            case "green_white":
                primaryColor = getResources().getColor(R.color.green_primary);
                break;
            default: // pink_brown
                primaryColor = getResources().getColor(R.color.pink_primary);
                break;
        }

        // שימוש ב-Tint במקום ב-BackgroundColor כדי לשמור על הצורה (הפינות המעוגלות)
        if (btnLogin != null) {
            btnLogin.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primaryColor));
        }

        // נצבע גם את כפתור ה-Register אם מצאת אותו ב-onCreate
        Button btnGoToRegister = findViewById(R.id.button_register); // וודאי שזה ה-ID ב-XML
        if (btnGoToRegister != null) {
            btnGoToRegister.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primaryColor));
        }

        // צביעת ה-CheckBox
        if (rememberCheckBox != null) {
            rememberCheckBox.setButtonTintList(android.content.res.ColorStateList.valueOf(primaryColor));
        }
    }

    public void login(View view) {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_LONG).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // שמירת פרטי התחברות
                        SharedPreferences loginPrefs = getSharedPreferences("loginPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = loginPrefs.edit();
                        if (rememberCheckBox.isChecked()) {
                            editor.putString("email", email);
                            editor.putString("password", password);
                            editor.putBoolean("remember", true);
                        } else {
                            editor.clear();
                        }
                        editor.apply();

                        // מעבר למסך המשימות
                        Intent intent = new Intent(LoginActivity.this, TasksActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this,
                                "Login failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    public void register(View view) {
        startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
    }
}