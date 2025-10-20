package com.example.ronilesapp;

import static com.example.ronilesapp.FBRef.*;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;


public class RegisterActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 100;

    EditText firstNameEditText, lastNameEditText, emailEditText, passwordEditText;
    CheckBox notificationsCheckBox;
    ImageView profileImageView;
    Uri imageUri;

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
    }

    // בוחרים תמונה מהגלריה
    public void chooseImage(View view) {
        Intent gallery = new Intent(Intent.ACTION_GET_CONTENT);
        gallery.setType("image/*");
        startActivityForResult(gallery, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.getData();
            profileImageView.setImageURI(imageUri);
        }
    }

    public void registerUser(View view) {
        String firstName = firstNameEditText.getText().toString().trim();
        String lastName = lastNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        boolean notifications = notificationsCheckBox.isChecked();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_LONG).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {

                            String uid = mAuth.getCurrentUser().getUid();

                            if (imageUri != null) {
                                try {
                                    String documentId = "documentId";
                                    // קבלת InputStream מה-URI
                                    InputStream stream = getContentResolver().openInputStream(imageUri);
                                    Bitmap imageBitmap = BitmapFactory.decodeStream(stream);

                                    // ההמרה ל-byte array עם Compress / Resize
                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                                    byte[] imageBytes = baos.toByteArray();

                                    // אופציה 1: Compress (Degradation)
                                    int quality = 100;
                                    while (imageBytes.length > 1048500 && quality > 5) { // פחות מ-1MB
                                        baos.reset();
                                        quality -= 5;
                                        imageBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
                                        imageBytes = baos.toByteArray();
                                    }

                                    // אופציה 2: Resize
                                    while (imageBytes.length > 1048500) {
                                        imageBitmap = Bitmap.createScaledBitmap(
                                                imageBitmap,
                                                (int) (imageBitmap.getWidth() * 0.9),
                                                (int) (imageBitmap.getHeight() * 0.9),
                                                true
                                        );
                                        baos.reset();
                                        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                                        imageBytes = baos.toByteArray();
                                    }

                                    // יצירת Map לאחסון הנתונים ב-Firestore
                                    Map<String, Object> imageMap = new HashMap<>();
                                    imageMap.put("imageName", documentId);
                                    imageMap.put("imageData", imageBytes);

                                    // העלאה ל-Firestore


                                    refImages.document(documentId)
                                            .set(imageMap)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Toast.makeText(getApplicationContext(),
                                                            "Upload successful", Toast.LENGTH_SHORT).show();
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Toast.makeText(getApplicationContext(),
                                                            "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                            });

                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Toast.makeText(getApplicationContext(),
                                            "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                User user = new User(firstName, lastName, email, notifications, null);
                                userRef.child(uid).setValue(user);
                                Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(RegisterActivity.this, WelcomeActivity.class));
                                finish();
                            }

                        } else {
                            Toast.makeText(RegisterActivity.this, "Register failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}
