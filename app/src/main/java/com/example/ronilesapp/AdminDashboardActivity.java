package com.example.ronilesapp;

import android.content.Context;
import android.content.Intent;
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
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminDashboardActivity extends BaseActivity { // שונה ל-BaseActivity כדי לקבל את ה-Theme

    private ConstraintLayout rootLayout;
    private TextView tvTitle, tvSubtitle;
    private RecyclerView rvUsers;
    private InternalUserAdapter userAdapter;
    private List<User> userList;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener themeListener; // המאזין לשינויים

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
        userAdapter = new InternalUserAdapter(this, userList);
        rvUsers.setAdapter(userAdapter);

        // 3. טעינת המשתמשים
        loadUsersFromFirestore();

        // 4. ניהול עיצוב וצבעים
        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);

        // הוספת המאזין שגורם למסך להתרענן מיד כשמשנים צבע בהגדרות
        themeListener = (prefs, key) -> {
            if ("theme".equals(key)) {
                recreate(); // פקודת הקסם שמרעננת את המסך
            }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(themeListener);

        applyThemeColors();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // הסרת המאזין כדי למנוע נזילות זיכרון
        if (sharedPreferences != null && themeListener != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(themeListener);
        }
    }

    private void loadUsersFromFirestore() {
        Utils.refUsers.get() // שימוש ב-Utils המאוחד שלנו!
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userList.clear();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        User user = document.toObject(User.class);
                        if (user != null) {
                            user.setUid(document.getId());
                            if (user.getProfileImageUrl() == null) user.setProfileImageUrl("");
                            userList.add(user);
                        }
                    }
                    userAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error loading users", Toast.LENGTH_SHORT).show());
    }

    private void applyThemeColors() {
        String theme = sharedPreferences.getString("theme", "pink_brown");
        int backgroundColor, titleColor, subtitleColor;

        switch (theme) {
            case "blue_white":
                backgroundColor = getResources().getColor(R.color.blue_background);
                titleColor = getResources().getColor(R.color.blue_primary);
                subtitleColor = getResources().getColor(android.R.color.black);
                break;
            case "green_white":
                backgroundColor = getResources().getColor(R.color.green_background);
                titleColor = getResources().getColor(R.color.green_primary);
                subtitleColor = getResources().getColor(android.R.color.black);
                break;
            default: // pink_brown
                backgroundColor = getResources().getColor(R.color.pink_background);
                titleColor = getResources().getColor(R.color.pink_primary);
                subtitleColor = getResources().getColor(R.color.brown);
                break;
        }

        rootLayout.setBackgroundColor(backgroundColor);
        tvTitle.setTextColor(titleColor);
        tvSubtitle.setTextColor(subtitleColor);
    }

    // --- האדפטר הפנימי נשאר דומה, רק וידאתי שהוא משתמש ב-Utils למחיקה ---
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
            View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            User user = list.get(position);
            holder.tvName.setText(user.getFirstName() + " " + user.getLastName());
            holder.tvEmail.setText(user.getEmail());
            holder.btnDelete.setOnClickListener(v -> deleteUser(user, position));
        }

        @Override
        public int getItemCount() { return list.size(); }

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

    private void deleteUser(User user, int position) {
        if (user.isAdmin()) {
            Toast.makeText(this, "Cannot delete an Admin!", Toast.LENGTH_SHORT).show();
            return;
        }

        Utils.refUsers.document(user.getUid()).delete()
                .addOnSuccessListener(aVoid -> {
                    userList.remove(position);
                    userAdapter.notifyItemRemoved(position);
                    Toast.makeText(this, "User Deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}