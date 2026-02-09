package com.example.ronilesapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminDashboardActivity extends AppCompatActivity {

    private ConstraintLayout rootLayout;
    private TextView tvTitle, tvSubtitle;
    private RecyclerView rvUsers;
    private InternalUserAdapter userAdapter; // שימוש באדפטר הפנימי
    private List<User> userList;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // 1. חיבור לרכיבים
        rootLayout = findViewById(R.id.rootLayoutAdmin);
        tvTitle = findViewById(R.id.tvAdminTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        rvUsers = findViewById(R.id.rvUsers);

        // 2. הגדרת ה-RecyclerView
        rvUsers.setHasFixedSize(true);
        rvUsers.setLayoutManager(new LinearLayoutManager(this));

        userList = new ArrayList<>();
        // כאן אנחנו יוצרים את האדפטר הפנימי
        userAdapter = new InternalUserAdapter(this, userList);
        rvUsers.setAdapter(userAdapter);

        // 3. טעינת המשתמשים מ-Firestore
        loadUsersFromFirestore();

        // 4. עיצוב
        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String theme = sharedPreferences.getString("theme", "pink_brown");
        applyThemeColors(theme);
    }

    private void loadUsersFromFirestore() {
        FirebaseFirestore.getInstance().collection("Users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userList.clear();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        User user = document.toObject(User.class);
                        if (user != null) {
                            user.setUid(document.getId());
                            // תיקון קטן: אם אין תמונה, נמנע קריסה אח"כ
                            if (user.getProfileImageUrl() == null) user.setProfileImageUrl("");
                            userList.add(user);
                        }
                    }
                    userAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "שגיאה בטעינת משתמשים", Toast.LENGTH_SHORT).show();
                });
    }

    private void applyThemeColors(String theme) {
        // ... (אותו קוד צבעים כמו מקודם)
        switch (theme) {
            case "blue_white":
                rootLayout.setBackgroundColor(0xFFE3F2FD);
                tvTitle.setTextColor(0xFF1565C0);
                tvSubtitle.setTextColor(0xFF424242);
                break;
            case "green_white":
                rootLayout.setBackgroundColor(0xFFE8F5E9);
                tvTitle.setTextColor(0xFF2E7D32);
                tvSubtitle.setTextColor(0xFF424242);
                break;
            case "pink_brown":
            default:
                rootLayout.setBackgroundColor(0xFFFBEFF1);
                tvTitle.setTextColor(0xFFD32F2F);
                tvSubtitle.setTextColor(0xFF5D4037);
                break;
        }
    }

    // =================================================================
    // כאן מתחיל האדפטר הפנימי (Inner Class) - במקום קובץ נפרד!
    // =================================================================

    public class InternalUserAdapter extends RecyclerView.Adapter<InternalUserAdapter.UserViewHolder> {

        private Context context;
        private List<User> list;

        public InternalUserAdapter(Context context, List<User> list) {
            this.context = context;
            this.list = list;
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // עדיין משתמשים ב-item_user.xml שיצרת
            View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            User user = list.get(position);

            String fullName = user.getFirstName() + " " + user.getLastName();
            holder.tvName.setText(fullName);
            holder.tvEmail.setText(user.getEmail());

            // לחיצה על כפתור מחיקה
            holder.btnDelete.setOnClickListener(v -> {
                // קריאה לפונקציה שנמצאת בתוך ה-Activity הראשית
                deleteUser(user, position);
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        // ה-ViewHolder הפנימי
        public class UserViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvEmail;
            ImageView imgIcon;
            ImageButton btnDelete;

            public UserViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvUserName);
                tvEmail = itemView.findViewById(R.id.tvUserEmail);
                imgIcon = itemView.findViewById(R.id.imgUserIcon);
                btnDelete = itemView.findViewById(R.id.btnDeleteUser);
            }
        }
    }

    // פונקציית המחיקה (עכשיו היא חלק מה-Activity, אז קל לגשת אליה)
    private void deleteUser(User user, int position) {
        if (user.isAdmin()) {
            Toast.makeText(this, "לא ניתן למחוק מנהל מערכת!", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore.getInstance().collection("Users")
                .document(user.getUid())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    userList.remove(position);
                    userAdapter.notifyItemRemoved(position);
                    userAdapter.notifyItemRangeChanged(position, userList.size());
                    Toast.makeText(this, "המשתמש נמחק", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}