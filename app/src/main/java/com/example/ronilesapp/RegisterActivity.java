package com.example.ronilesapp;

import static com.example.ronilesapp.FBRef.*;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AppSettingsPrefs";
    private static final String KEY_THEME = "theme";

    private EditText firstNameEditText, lastNameEditText, emailEditText, passwordEditText;
    private CheckBox notificationsCheckBox;
    private ImageView profileImageView;

    private Uri selectedImageUri;  // תמונה שנבחרה מהגלריה או מצלמה
    private Uri cameraImageUri;    // URI זמני למצלמה

    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<String[]> requestPermissionsLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // 🔹 מיישם את ה-Theme שנבחר לפני setContentView
        applySelectedTheme();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        firstNameEditText = findViewById(R.id.edittext_first_name);
        lastNameEditText = findViewById(R.id.edittext_last_name);
        emailEditText = findViewById(R.id.edittext_email);
        passwordEditText = findViewById(R.id.edittext_password);
        notificationsCheckBox = findViewById(R.id.checkbox_notifications);
        profileImageView = findViewById(R.id.imageview_profile);

        setupImagePickers();
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


    private void setupImagePickers() {
        // גלריה
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        profileImageView.setImageURI(selectedImageUri);
                    }
                }
        );

        // מצלמה
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                result -> {
                    if (result && cameraImageUri != null) {
                        selectedImageUri = cameraImageUri;
                        profileImageView.setImageURI(selectedImageUri);
                    }
                }
        );

        // בקשת הרשאות CAMERA בלבד
        requestPermissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    if (result.getOrDefault(Manifest.permission.CAMERA, false)) {
                        openCamera();
                    } else {
                        Toast.makeText(this, "הרשאות דרושות כדי לצלם תמונה", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    // כפתור גלריה
    public void chooseImage(View view) {
        pickImageLauncher.launch("image/*");
    }

    // כפתור מצלמה
    public void takePhoto(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean cameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;

            if (!cameraGranted) {
                requestPermissionsLauncher.launch(new String[]{Manifest.permission.CAMERA});
                return;
            }
        }
        openCamera();
    }

    private void openCamera() {
        try {
            File imageFile = createImageFile();
            if (imageFile != null) {
                cameraImageUri = FileProvider.getUriForFile(
                        this,
                        getApplicationContext().getPackageName() + ".provider",
                        imageFile
                );
                cameraLauncher.launch(cameraImageUri);
            } else {
                Toast.makeText(this, "שגיאה ביצירת קובץ תמונה", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String fileName = "IMG_" + System.currentTimeMillis();
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (!storageDir.exists()) storageDir.mkdirs(); // לוודא שהתקייה קיימת
        return File.createTempFile(fileName, ".jpg", storageDir);
    }

    // הרשמה
    public void registerUser(View view) {
        String firstName = firstNameEditText.getText().toString().trim();
        String lastName = lastNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        boolean notifications = notificationsCheckBox.isChecked();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "נא למלא את כל השדות", Toast.LENGTH_LONG).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();
                        if (selectedImageUri != null) {
                            uploadImageAndSaveUser(uid, firstName, lastName, email, notifications, selectedImageUri);
                        } else {
                            saveUserToFirestore(uid, firstName, lastName, email, notifications, null);
                        }
                    } else {
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            new AlertDialog.Builder(RegisterActivity.this)
                                    .setTitle("המייל כבר קיים")
                                    .setMessage("המייל כבר רשום. רוצה להיכנס במקום להירשם?")
                                    .setPositiveButton("כניסה", (dialog, which) -> {
                                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                        intent.putExtra("email", email);
                                        startActivity(intent);
                                    })
                                    .setNegativeButton("בטל", null)
                                    .show();
                        } else {
                            Toast.makeText(RegisterActivity.this,
                                    "ההרשמה נכשלה: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void uploadImageAndSaveUser(String uid, String firstName, String lastName,
                                        String email, boolean notifications, Uri imageUri) {
        if (imageUri == null) {
            saveUserToFirestore(uid, firstName, lastName, email, notifications, null);
            return;
        }

        StorageReference imageRef = storageRef.child("profileImages/" + uid + ".jpg");

        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> saveUserToFirestore(uid, firstName, lastName, email, notifications, uri.toString())))
                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterActivity.this,
                            "טעינת התמונה נכשלה: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    saveUserToFirestore(uid, firstName, lastName, email, notifications, null);
                });
    }

    private void saveUserToFirestore(String uid, String firstName, String lastName,
                                     String email, boolean notifications, String imageUrl) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("firstName", firstName);
        userMap.put("lastName", lastName);
        userMap.put("email", email);
        userMap.put("notifications", notifications);
        if (imageUrl != null) userMap.put("profileImageUrl", imageUrl);

        refUsers.document(uid)
                .set(userMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegisterActivity.this, "הרשמה הצליחה!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(RegisterActivity.this, TasksActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(RegisterActivity.this,
                        "שמירת המשתמש נכשלה: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    public void goToLogin(View view) {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
