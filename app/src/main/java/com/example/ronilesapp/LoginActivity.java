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

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

// 1. שינוי: יורש מ-BaseActivity כדי לקבל את הרקע המשתנה
public class LoginActivity extends BaseActivity {

    private FirebaseAuth mAuth;
    private EditText emailEditText, passwordEditText;
    private CheckBox rememberCheckBox;
    private Button btnLogin;
    private Button btnGoToRegister;

    SharedPreferences loginPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        // חיבור Views
        emailEditText = findViewById(R.id.edittext_email);
        passwordEditText = findViewById(R.id.edittext_password);
        rememberCheckBox = findViewById(R.id.checkbox_remember);
        btnLogin = findViewById(R.id.button_login);
        btnGoToRegister = findViewById(R.id.btnBackToRegister);

        //מאזינים
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });
        btnGoToRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                register();
            }
        });

        // לוגיקה קיימת: בדיקה אם נשמרו פרטי התחברות
        loginPrefs = getSharedPreferences("loginPrefs", MODE_PRIVATE);
        boolean remember = loginPrefs.getBoolean("remember", false);

        if (remember) {
            String savedEmail = loginPrefs.getString("email", "");
            emailEditText.setText(savedEmail);
            rememberCheckBox.setChecked(true);
        }

        // 2. החלת הצבעים
        applyThemeColors();
    }

    // פונקציה לצביעת הכפתורים וה-CheckBox לפי ה-Theme
    private void applyThemeColors() {
        String theme = baseSharedPreferences.getString("theme", "pink_brown");
        int primaryColor;

        // בחירת הצבע הנכון לפי ה-Theme
        switch (theme) {
            case "blue_white":
                primaryColor = ContextCompat.getColor(this, R.color.blue_primary);
                break;
            case "green_white":
                primaryColor = ContextCompat.getColor(this, R.color.green_primary);
                break;
            default: // pink_brown
                primaryColor = ContextCompat.getColor(this, R.color.pink_primary);
                break;
        }

        // שימוש ב-Tint במקום ב-BackgroundColor כדי לשמור על הצורה (הפינות המעוגלות)
        if (btnLogin != null) {
            btnLogin.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
        }

        // נצבע גם את כפתור ה-Register אם מצאת אותו ב-onCreate
        if (btnGoToRegister != null) {
            btnGoToRegister.setBackgroundTintList(ColorStateList.valueOf(primaryColor));
        }

        // צביעת ה-CheckBox
        if (rememberCheckBox != null) {
            rememberCheckBox.setButtonTintList(ColorStateList.valueOf(primaryColor));
        }
    }

    public void login() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_LONG).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // שמירת פרטי התחברות
                            SharedPreferences.Editor editor = loginPrefs.edit();
                            if (rememberCheckBox.isChecked()) {
                                editor.putString("email", email);
                                editor.putBoolean("remember", true);
                            } else {
                                editor.clear();
                            }
                            editor.apply();

                            // מעבר למסך המשימות
                            Intent intent = new Intent(LoginActivity.this, TasksActivity.class);
                            LoginActivity.this.startActivity(intent);
                            LoginActivity.this.finish();
                        } else {
                            String errorMsg = "Unknown error";
                            if (task.getException() != null)
                                errorMsg = task.getException().getMessage();
                            Toast.makeText(LoginActivity.this,
                                    "Login failed: " + errorMsg,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    public void register() {
        startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
    }
}