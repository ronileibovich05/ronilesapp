package com.example.ronilesapp;

import static com.example.ronilesapp.Utils.mAuth;
import static com.example.ronilesapp.Utils.refUsers;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class WelcomeActivity extends BaseActivity {

    private TextView welcomeText;
    private ImageView profileImageView;
    private Button btnEdit, btnDelete;

    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applySelectedTheme(); // החלת העיצוב
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

        // משיכת נתוני המשתמש
        refUsers.document(uid).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                currentUser = snapshot.toObject(User.class);
                if (currentUser != null) {
                    String first = currentUser.getFirstName();
                    String last = currentUser.getLastName();

                    if (first == null) first = "";
                    if (last == null) last = "";

                    String name = (first + " " + last).trim();
                    if (name.isEmpty()) {
                        name = user.getEmail().split("@")[0];
                    }
                    welcomeText.setText("ברוך הבא, " + name);

                    // טעינת תמונת פרופיל (תומך גם ב-Base64)
                    loadImageFromString(currentUser.getProfileImageUrl(), profileImageView);
                }
            } else {
                String fallback = user.getEmail() != null ? user.getEmail().split("@")[0] : "משתמש";
                welcomeText.setText("ברוך הבא, " + fallback);
                profileImageView.setImageResource(R.drawable.ic_default_profile);
            }
        });

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

        editFirstName.setText(currentUser.getFirstName());
        editLastName.setText(currentUser.getLastName());
        editEmail.setText(currentUser.getEmail());
        editNotifications.setChecked(currentUser.isNotifications());

        // טעינת התמונה גם לתוך החלונית
        loadImageFromString(currentUser.getProfileImageUrl(), editProfileImage);

        AlertDialog dialog = builder.create();

        btnSaveUser.setOnClickListener(v -> {
            String newFirstName = editFirstName.getText().toString().trim();
            String newLastName = editLastName.getText().toString().trim();
            String newEmail = editEmail.getText().toString().trim();
            boolean newNotifications = editNotifications.isChecked();

            if (newFirstName.isEmpty() || newLastName.isEmpty() || newEmail.isEmpty()) {
                Toast.makeText(WelcomeActivity.this, "נא למלא את כל השדות", Toast.LENGTH_SHORT).show();
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

            refUsers.document(uid).update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(WelcomeActivity.this, "המשתמש עודכן בהצלחה!", Toast.LENGTH_SHORT).show();
                        currentUser.setFirstName(newFirstName);
                        currentUser.setLastName(newLastName);
                        currentUser.setEmail(newEmail);
                        currentUser.setNotifications(newNotifications);
                        welcomeText.setText("ברוך הבא, " + (newFirstName + " " + newLastName).trim());
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> Toast.makeText(WelcomeActivity.this, "שגיאה בעדכון: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });

        dialog.show();
    }

    private void deleteUser() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();

        // קודם מוחקים ממסד הנתונים, ואז מ-Authentication
        refUsers.document(uid).delete()
                .addOnSuccessListener(aVoid -> user.delete()
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(WelcomeActivity.this, "המשתמש נמחק", Toast.LENGTH_SHORT).show();
                            goToLogin();
                        })
                        .addOnFailureListener(e -> Toast.makeText(WelcomeActivity.this, "שגיאה במחיקת משתמש (Auth): " + e.getMessage(), Toast.LENGTH_LONG).show()))
                .addOnFailureListener(e -> Toast.makeText(WelcomeActivity.this, "שגיאה במחיקת נתונים: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    public void logout(android.view.View view) {
        mAuth.signOut();
        goToLogin();
    }

    private void goToLogin() {
        startActivity(new android.content.Intent(this, LoginActivity.class));
        finish();
    }

    // פונקציית עזר לטעינת תמונה (תומכת בקישור רגיל וב-Base64)
    private void loadImageFromString(String imageString, ImageView imageView) {
        if (imageString == null || imageString.isEmpty()) {
            imageView.setImageResource(R.drawable.ic_default_profile);
        } else if (imageString.startsWith("http")) {
            Glide.with(this)
                    .load(imageString)
                    .placeholder(R.drawable.ic_default_profile)
                    .error(R.drawable.ic_default_profile)
                    .into(imageView);
        } else {
            try {
                byte[] decoded = android.util.Base64.decode(imageString, android.util.Base64.DEFAULT);
                imageView.setImageBitmap(android.graphics.BitmapFactory.decodeByteArray(decoded, 0, decoded.length));
            } catch (Exception e) {
                e.printStackTrace();
                imageView.setImageResource(R.drawable.ic_default_profile);
            }
        }
    }
}