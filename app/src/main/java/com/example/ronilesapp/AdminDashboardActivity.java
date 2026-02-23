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
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminDashboardActivity extends BaseActivity {

    private ConstraintLayout rootLayout;
    private TextView tvTitle, tvSubtitle;
    private RecyclerView rvUsers;
    private InternalUserAdapter userAdapter;
    private List<User> userList;
    private SharedPreferences.OnSharedPreferenceChangeListener themeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applySelectedTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        rootLayout = findViewById(R.id.rootLayoutAdmin);
        tvTitle = findViewById(R.id.tvAdminTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        rvUsers = findViewById(R.id.rvUsers);

        rvUsers.setHasFixedSize(true);
        rvUsers.setLayoutManager(new LinearLayoutManager(this));

        userList = new ArrayList<>();
        userAdapter = new InternalUserAdapter(this, userList);
        rvUsers.setAdapter(userAdapter);

        themeListener = (prefs, key) -> {
            if (BaseActivity.KEY_THEME.equals(key)) {
                recreate();
            }
        };
        baseSharedPreferences.registerOnSharedPreferenceChangeListener(themeListener);

        applyThemeColors();
        loadUsersFromFirestore();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (baseSharedPreferences != null && themeListener != null) {
            baseSharedPreferences.unregisterOnSharedPreferenceChangeListener(themeListener);
        }
    }

    private void loadUsersFromFirestore() {
        Utils.refUsers.get()
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
                .addOnFailureListener(e -> Toast.makeText(AdminDashboardActivity.this, "Error loading users", Toast.LENGTH_SHORT).show());
    }

    private void applyThemeColors() {
        String theme = baseSharedPreferences.getString(BaseActivity.KEY_THEME, "pink_brown");
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
            default:
                backgroundColor = getResources().getColor(R.color.pink_background);
                titleColor = getResources().getColor(R.color.pink_primary);
                subtitleColor = getResources().getColor(R.color.brown);
                break;
        }

        rootLayout.setBackgroundColor(backgroundColor);
        tvTitle.setTextColor(titleColor);
        tvSubtitle.setTextColor(subtitleColor);
    }

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

            holder.btnDelete.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    deleteUser(user);
                }
            });
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

    private void deleteUser(User user) {
        if (user.isAdmin()) {
            Toast.makeText(this, "Cannot delete an Admin!", Toast.LENGTH_SHORT).show();
            return;
        }

        Utils.refUsers.document(user.getUid()).delete()
                .addOnSuccessListener(aVoid -> {
                    int indexToRemove = -1;
                    for (int i = 0; i < userList.size(); i++) {
                        if (userList.get(i).getUid().equals(user.getUid())) {
                            indexToRemove = i;
                            break;
                        }
                    }
                    if (indexToRemove != -1) {
                        userList.remove(indexToRemove);
                        userAdapter.notifyItemRemoved(indexToRemove);
                        userAdapter.notifyItemRangeChanged(indexToRemove, userList.size());
                    }
                    Toast.makeText(AdminDashboardActivity.this, "User deleted successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(AdminDashboardActivity.this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}