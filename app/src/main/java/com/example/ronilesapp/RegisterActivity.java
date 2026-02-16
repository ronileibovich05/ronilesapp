package com.example.ronilesapp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class RegisterActivity extends BaseActivity {

    private EditText firstNameEditText, lastNameEditText, emailEditText, passwordEditText;
    private CheckBox notificationsCheckBox;
    private ImageView profileImageView;
    private Button btnRegister;

    private Uri selectedImageUri;
    private Uri cameraImageUri;

    // משתני Firebase (רק Auth ו-Firestore, בלי Storage!)
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<String[]> requestPermissionsLauncher;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // אתחול Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);

        // אתחול רכיבי UI
        firstNameEditText = findViewById(R.id.edittext_first_name);
        lastNameEditText = findViewById(R.id.edittext_last_name);
        emailEditText = findViewById(R.id.edittext_email);
        passwordEditText = findViewById(R.id.edittext_password);
        notificationsCheckBox = findViewById(R.id.checkbox_notifications);
        profileImageView = findViewById(R.id.imageview_profile);

        // נסי למצוא את הכפתור לצביעה (אם יש לו ID ב-XML)
        // btnRegister = findViewById(R.id.btn_register);

        setupImagePickers();
        applyThemeColors();
    }

    private void applyThemeColors() {
        String theme = sharedPreferences.getString("theme", "pink_brown");
        int buttonColor;
        switch (theme) {
            case "blue_white": buttonColor = getResources().getColor(R.color.blue_primary); break;
            case "green_white": buttonColor = getResources().getColor(R.color.green_primary); break;
            default: buttonColor = getResources().getColor(R.color.pink_primary); break;
        }
        if (btnRegister != null) btnRegister.setBackgroundColor(buttonColor);
        notificationsCheckBox.setButtonTintList(android.content.res.ColorStateList.valueOf(buttonColor));
    }

    private void setupImagePickers() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedImageUri = uri;
                        profileImageView.setImageURI(selectedImageUri);
                    }
                }
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                result -> {
                    if (result) {
                        selectedImageUri = cameraImageUri;
                        profileImageView.setImageURI(selectedImageUri);
                    }
                }
        );

        requestPermissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    if (Boolean.TRUE.equals(result.getOrDefault(Manifest.permission.CAMERA, false))) {
                        openCamera();
                    } else {
                        Toast.makeText(this, "Camera permission needed", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    public void chooseImage(View view) { pickImageLauncher.launch("image/*"); }

    public void takePhoto(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionsLauncher.launch(new String[]{Manifest.permission.CAMERA});
            } else {
                openCamera();
            }
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        try {
            File photoFile = createImageFile();
            if (photoFile != null) {
                cameraImageUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", photoFile);
                cameraLauncher.launch(cameraImageUri);
            }
        } catch (IOException ex) {
            Toast.makeText(this, "Error creating file", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String imageFileName = "JPEG_" + System.currentTimeMillis() + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    // --- הלב של השיטה החדשה: המרה לטקסט ---
    private String encodeImageToBase64(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            // הקטנת התמונה (חובה! כדי לא לתקוע את Firestore)
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 400, 400, true);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream); // איכות 50%
            byte[] byteArray = outputStream.toByteArray();

            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void registerUser(View view) {
        String firstName = firstNameEditText.getText().toString().trim();
        String lastName = lastNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        boolean notifications = notificationsCheckBox.isChecked();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String uid = user.getUid();
                            String imageString = "";

                            // אם יש תמונה - ממירים אותה לטקסט
                            if (selectedImageUri != null) {
                                imageString = encodeImageToBase64(selectedImageUri);
                                if (imageString == null) imageString = ""; // אם נכשל, נשמור ריק
                            }

                            // שומרים ישירות ב-Firestore
                            saveUserData(uid, firstName, lastName, email, notifications, imageString);
                        }
                    } else {
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            Toast.makeText(this, "Email already exists", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void saveUserData(String uid, String firstName, String lastName, String email, boolean notifications, String imageString) {
        // בנאי: uid, firstName, lastName, email, notifications, imageString, isAdmin
        User newUser = new User(uid, firstName, lastName, email, notifications, imageString, false);

        db.collection("Users").document(uid)
                .set(newUser)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Registered successfully!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterActivity.this, TasksActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    public void goToLogin(View view) {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}