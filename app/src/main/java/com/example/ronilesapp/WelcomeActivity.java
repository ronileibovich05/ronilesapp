package com.example.ronilesapp;

import static com.example.ronilesapp.Utils.*;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.Map;

public class WelcomeActivity extends AppCompatActivity {

    private TextView welcomeText;
    private ImageView profileImageView;
    private Button btnEdit, btnDelete;

    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        welcomeText = findViewById(R.id.textView2);
        profileImageView = findViewById(R.id.imageview_profile);
        btnEdit = findViewById(R.id.button_edit);
        btnDelete = findViewById(R.id.button_delete);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            goToLogin();
            return;
        }

        String uid = user.getUid();

        // קבלת פרטי המשתמש מ-Firestore
        refUsers.document(uid).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                currentUser = snapshot.toObject(User.class);
                if (currentUser != null) {
                    String name = currentUser.getFirstName();
                    if (name == null || name.isEmpty()) {
                        name = user.getEmail().split("@")[0];
                    }
                    welcomeText.setText("Welcome, " + name);

                    // הצגת תמונת פרופיל דרך URI בלבד
                    if (currentUser.getProfileImageUrl() != null && !currentUser.getProfileImageUrl().isEmpty()) {
                        Uri imageUri = Uri.parse(currentUser.getProfileImageUrl());
                        profileImageView.setImageURI(imageUri);
                    }
                }
            }
        });

        // כפתור Edit פותח חלון עריכה
        btnEdit.setOnClickListener(v -> openEditDialog());

        btnDelete.setOnClickListener(v -> deleteUser());
    }

    private void openEditDialog() {
        if (currentUser == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.activity_dialog_edit_user, null);
        builder.setView(dialogView);

        ImageView editProfileImage = dialogView.findViewById(R.id.editProfileImage);
        EditText editFirstName = dialogView.findViewById(R.id.editFirstName);
        EditText editLastName = dialogView.findViewById(R.id.editLastName);
        EditText editEmail = dialogView.findViewById(R.id.editEmail);
        Switch editNotifications = dialogView.findViewById(R.id.editNotifications);
        Button btnSaveUser = dialogView.findViewById(R.id.btnSaveUser);

        // ממלאים את השדות
        editFirstName.setText(currentUser.getFirstName());
        editLastName.setText(currentUser.getLastName());
        editEmail.setText(currentUser.getEmail());
        editNotifications.setChecked(currentUser.isNotifications());

        if (currentUser.getProfileImageUrl() != null && !currentUser.getProfileImageUrl().isEmpty()) {
            Uri imageUri = Uri.parse(currentUser.getProfileImageUrl());
            editProfileImage.setImageURI(imageUri);
        }

        AlertDialog dialog = builder.create();

        btnSaveUser.setOnClickListener(v -> {
            String newFirstName = editFirstName.getText().toString().trim();
            String newLastName = editLastName.getText().toString().trim();
            String newEmail = editEmail.getText().toString().trim();
            boolean newNotifications = editNotifications.isChecked();

            if (newFirstName.isEmpty() || newLastName.isEmpty() || newEmail.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null) return;
            String uid = user.getUid();

            Map<String, Object> updates = new HashMap<>();
            updates.put("firstName", newFirstName);
            updates.put("lastName", newLastName);
            updates.put("email", newEmail);
            updates.put("notifications", newNotifications);

            refUsers.document(uid).update(updates).addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "User updated!", Toast.LENGTH_SHORT).show();
                currentUser.setFirstName(newFirstName);
                currentUser.setLastName(newLastName);
                currentUser.setEmail(newEmail);
                currentUser.setNotifications(newNotifications);
                welcomeText.setText("Welcome, " + newFirstName);
                dialog.dismiss();
            }).addOnFailureListener(e -> Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });

        dialog.show();
    }

    private void deleteUser() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();

        refUsers.document(uid).delete();

        user.delete().addOnSuccessListener(unused -> {
            Toast.makeText(this, "User deleted", Toast.LENGTH_SHORT).show();
            goToLogin();
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
        );
    }

    public void logout(android.view.View view) {
        mAuth.signOut();
        goToLogin();
    }

    private void goToLogin() {
        startActivity(new android.content.Intent(this, LoginActivity.class));
        finish();
    }
}
