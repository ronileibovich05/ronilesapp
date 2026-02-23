package com.example.ronilesapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class TasksActivity extends BaseActivity {

    private BottomNavigationView bottomNavigationView;
    private TabLayoutMediator tabMediator;
    private TabLayout tabLayoutCategories;
    private ViewPager2 viewPagerTasks;
    private FloatingActionButton fabAddTask;
    private Button btnAddCategory;

    private ActivityResultLauncher<Intent> addTaskLauncher;

    private List<String> categoryList = new ArrayList<>();
    private List<Fragment> fragments = new ArrayList<>();

    private CategoryPagerAdapter pagerAdapter;
    private SharedPreferences.OnSharedPreferenceChangeListener themeListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        applySelectedTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks);

        tabLayoutCategories = findViewById(R.id.tabLayoutCategories);
        viewPagerTasks = findViewById(R.id.viewPagerTasks);
        fabAddTask = findViewById(R.id.fabAddTask);
        btnAddCategory = findViewById(R.id.btnAddCategoryTasks);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        addTaskLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        TasksActivity.this.loadCategoriesAndTasks();
                    }
                }
        );

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile) {
                startActivity(new Intent(TasksActivity.this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(TasksActivity.this, SettingsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return (id == R.id.nav_home);
        });

        fabAddTask.setOnClickListener(v -> {
            if (!Utils.isConnected(TasksActivity.this)) {
                checkInternet();
                return;
            }
            addTaskLauncher.launch(new Intent(TasksActivity.this, AddTaskActivity.class));
        });

        btnAddCategory.setOnClickListener(v -> showAddCategoryDialog());

        // Listener for real-time Theme change
        themeListener = (prefs, key) -> {
            if (BaseActivity.KEY_THEME.equals(key)) {
                recreate();
            }
        };
        baseSharedPreferences.registerOnSharedPreferenceChangeListener(themeListener);

        applyThemeColors();

        loadCategoriesAndTasks();
        updateTasksMonthAndTime();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNavigationView.getSelectedItemId() != R.id.nav_home) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkInternet();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tabMediator != null) {
            tabMediator.detach();
            tabMediator = null;
        }
        if (themeListener != null && baseSharedPreferences != null) {
            baseSharedPreferences.unregisterOnSharedPreferenceChangeListener(themeListener);
        }
    }

    private void checkInternet() {
        if (!Utils.isConnected(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("No Internet Connection")
                    .setMessage("Please check your internet connection to view and manage tasks.")
                    .setPositiveButton("OK", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
    }

    private void applyThemeColors() {
        String theme = baseSharedPreferences.getString(BaseActivity.KEY_THEME, "pink_brown");
        int backgroundColor, fabColor, buttonColor, tabSelectedColor, tabUnselectedColor, textColor;

        switch (theme) {
            case "blue_white":
                backgroundColor = ContextCompat.getColor(this, R.color.blue_background);
                fabColor = ContextCompat.getColor(this, R.color.blue_primary);
                buttonColor = ContextCompat.getColor(this, R.color.blue_primary);
                tabSelectedColor = ContextCompat.getColor(this, R.color.blue);
                tabUnselectedColor = ContextCompat.getColor(this, R.color.black);
                textColor = ContextCompat.getColor(this, R.color.black);
                break;
            case "green_white":
                backgroundColor = ContextCompat.getColor(this, R.color.green_background);
                fabColor = ContextCompat.getColor(this, R.color.green_primary);
                buttonColor = ContextCompat.getColor(this, R.color.green_primary);
                tabSelectedColor = ContextCompat.getColor(this, R.color.green);
                tabUnselectedColor = ContextCompat.getColor(this, R.color.black);
                textColor = ContextCompat.getColor(this, R.color.black);
                break;
            default: // pink_brown
                backgroundColor = ContextCompat.getColor(this, R.color.pink_background);
                fabColor = ContextCompat.getColor(this, R.color.pink_primary);
                buttonColor = ContextCompat.getColor(this, R.color.pink_primary);
                tabSelectedColor = ContextCompat.getColor(this, R.color.pink);
                tabUnselectedColor = ContextCompat.getColor(this, R.color.brown);
                textColor = ContextCompat.getColor(this, R.color.brown);
                break;
        }

        // Full screen background color
        findViewById(android.R.id.content).setBackgroundColor(backgroundColor);
        viewPagerTasks.setBackgroundColor(backgroundColor);
        tabLayoutCategories.setBackgroundColor(backgroundColor);

        // Buttons using Tint
        fabAddTask.setBackgroundTintList(android.content.res.ColorStateList.valueOf(fabColor));
        btnAddCategory.setBackgroundTintList(android.content.res.ColorStateList.valueOf(buttonColor));
        btnAddCategory.setTextColor(ContextCompat.getColor(this, android.R.color.white));

        tabLayoutCategories.setSelectedTabIndicatorColor(tabSelectedColor);
        tabLayoutCategories.setTabTextColors(tabUnselectedColor, tabSelectedColor);
    }

    void loadCategoriesAndTasks() {
        if (!Utils.isConnected(this)) {
            Toast.makeText(this, "No Internet Connection", Toast.LENGTH_LONG).show();
            return;
        }

        Utils.getUserCategoriesRef().get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                categoryList.clear();
                fragments.clear();

                for (QueryDocumentSnapshot doc : task.getResult()) {
                    String categoryName = doc.getString("name");
                    if (categoryName != null) {
                        categoryList.add(categoryName);
                    }
                }

                // Back to English
                categoryList.add(0, "All Tasks");

                for (String cat : categoryList) {
                    fragments.add(CategoryTasksFragment.newInstance(cat));
                }

                pagerAdapter = new CategoryPagerAdapter(TasksActivity.this, fragments);
                viewPagerTasks.setAdapter(pagerAdapter);

                if (tabMediator != null) {
                    tabMediator.detach();
                }
                tabMediator = new TabLayoutMediator(tabLayoutCategories, viewPagerTasks,
                        (tab, position) -> tab.setText(categoryList.get(position))
                );
                tabMediator.attach();
            } else {
                Toast.makeText(TasksActivity.this, "Failed Loading Categories", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddCategoryDialog() {
        EditText input = new EditText(this);
        input.setHint("Category Name");

        new AlertDialog.Builder(this)
                .setTitle("Add New Category")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newCategory = input.getText().toString().trim();
                    if (!newCategory.isEmpty()) {
                        saveNewCategory(newCategory);
                    } else {
                        Toast.makeText(TasksActivity.this, "Please enter a category name", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveNewCategory(String categoryName) {
        if (!Utils.isConnected(this)) {
            checkInternet();
            return;
        }

        Category category = new Category(categoryName);
        Utils.getUserCategoriesRef().document(categoryName)
                .set(category)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(TasksActivity.this, "Category Added Successfully!", Toast.LENGTH_SHORT).show();
                    loadCategoriesAndTasks();
                })
                .addOnFailureListener(e -> Toast.makeText(TasksActivity.this, "Error adding category", Toast.LENGTH_SHORT).show());
    }

    private void updateTasksMonthAndTime() {
        if (!Utils.isConnected(this)) return;

        Utils.getUserTasksRef().get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    UserTask t = doc.toObject(UserTask.class);
                    boolean needsUpdate = false;

                    if (t.getMonth() == 0 && t.getCreationTime() > 0) {
                        java.util.Calendar cal = java.util.Calendar.getInstance();
                        cal.setTimeInMillis(t.getCreationTime());

                        t.setMonth(cal.get(java.util.Calendar.MONTH) + 1);
                        t.setDay(cal.get(java.util.Calendar.DAY_OF_MONTH));
                        t.setHour(cal.get(java.util.Calendar.HOUR_OF_DAY));
                        t.setMinute(cal.get(java.util.Calendar.MINUTE));

                        needsUpdate = true;
                    }

                    if (needsUpdate) {
                        Utils.getUserTasksRef().document(doc.getId()).set(t);
                    }
                }
            }
        });
    }

    private static class CategoryPagerAdapter extends FragmentStateAdapter {
        private final List<Fragment> fragments;

        public CategoryPagerAdapter(AppCompatActivity activity, List<Fragment> fragments) {
            super(activity);
            this.fragments = fragments;
        }

        @Override
        public Fragment createFragment(int position) {
            return fragments.get(position);
        }

        @Override
        public int getItemCount() {
            return fragments.size();
        }
    }
}