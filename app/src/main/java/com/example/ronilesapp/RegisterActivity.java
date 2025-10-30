package com.example.ronilesapp;

import static com.example.ronilesapp.FBRef.*;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.content.Intent;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText firstNameEditText, lastNameEditText, emailEditText, passwordEditText;
    private CheckBox notificationsCheckBox;
    private ImageView profileImageView;

    private Uri selectedImageUri;  // תמונה שנבחרה
    private Uri cameraImageUri;    // URI זמני למצלמה

    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
                    if (result) {
                        selectedImageUri = cameraImageUri;
                        profileImageView.setImageURI(selectedImageUri);
                    }
                }
        );

        // בקשת הרשאת מצלמה
        requestCameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) openCamera();
                    else Toast.makeText(this, "הרשאת מצלמה דרושה כדי לצלם תמונה", Toast.LENGTH_SHORT).show();
                }
        );
    }

    // בוחרים תמונה מהגלריה
    public void chooseImage(View view) {
        pickImageLauncher.launch("image/*");
    }

    // מצלמים תמונה
    public void takePhoto(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        cameraImageUri = createImageUri();
        if (cameraImageUri != null) cameraLauncher.launch(cameraImageUri);
        else Toast.makeText(this, "שגיאה ביצירת קובץ תמונה", Toast.LENGTH_SHORT).show();
    }

    private Uri createImageUri() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera");
        return getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
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
                        Toast.makeText(RegisterActivity.this,
                                "ההרשמה נכשלה: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void uploadImageAndSaveUser(String uid, String firstName, String lastName,
                                        String email, boolean notifications, Uri imageUri) {
        StorageReference imageRef = storageRef.child("profileImages/" + uid + ".jpg");

        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> imageRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> {
                            saveUserToFirestore(uid, firstName, lastName, email, notifications, uri.toString());
                        }))
                .addOnFailureListener(e -> Toast.makeText(RegisterActivity.this,
                        "טעינת התמונה נכשלה: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
                    // מעבר אוטומטי למסך המשימות
                    startActivity(new Intent(RegisterActivity.this, TasksActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(RegisterActivity.this,
                        "הרשמה נכשלה: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    // מעבר למסך התחברות
    public void goToLogin(View view) {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
