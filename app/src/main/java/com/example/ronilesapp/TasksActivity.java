package com.example.ronilesapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;
import android.app.AlertDialog;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TasksActivity extends BaseActivity {

    private TabLayout tabLayoutCategories;
    private ViewPager2 viewPagerTasks;
    private FloatingActionButton fabAddTask;
    private Button btnAddCategory;

    private ActivityResultLauncher<Intent> addTaskLauncher;

    private List<String> categoryList = new ArrayList<>();
    private List<Fragment> fragments = new ArrayList<>();
    private CategoryPagerAdapter pagerAdapter;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener themeListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        applyInitialTheme(sharedPreferences.getString("theme", "Theme.PinkBrown"));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks);

        tabLayoutCategories = findViewById(R.id.tabLayoutCategories);
        viewPagerTasks = findViewById(R.id.viewPagerTasks);
        fabAddTask = findViewById(R.id.fabAddTask);
        btnAddCategory = findViewById(R.id.btnAddCategoryTasks);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);

        // ✅ סימון שהמסך הנוכחי הוא HOME
        bottomNavigationView.setSelectedItemId(R.id.nav_home);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile) {
                startActivity(new Intent(TasksActivity.this, ProfileActivity.class));
                return true;
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(TasksActivity.this, SettingsActivity.class));
                return true;
            } else if (id == R.id.nav_home) {
                return true;
            } else {
                return false;
            }
        });

        addTaskLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        loadCategoriesAndTasks();
                    }
                }
        );

        fabAddTask.setOnClickListener(v -> {
            // בדיקה גם בלחיצה
            if (!NetworkUtil.isConnected(this)) {
                checkInternet(); // שימוש בפונקציה החדשה שמציגה דיאלוג יפה
                return;
            }
            startActivity(new Intent(TasksActivity.this, Item_TaskActivity.class));
        });

        btnAddCategory.setOnClickListener(v -> showAddCategoryDialog());

        loadCategoriesAndTasks();
        updateTasksMonthAndTime();

        // מאזין לשינוי Theme בזמן אמת
        themeListener = (prefs, key) -> {
            if ("theme".equals(key)) {
                applyThemeColors();
            }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(themeListener);

        applyThemeColors();
    }

    // --- הוספנו את onStart כדי לבדוק אינטרנט בכניסה ---
    @Override
    protected void onStart() {
        super.onStart();
        checkInternet();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (themeListener != null && sharedPreferences != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(themeListener);
        }
    }

    // --- הפונקציה החדשה שמציגה הודעה אם אין אינטרנט ---
    private void checkInternet() {
        if (!NetworkUtil.isConnected(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("No Internet Connection")
                    .setMessage("Please check your internet connection to view and manage tasks.")
                    .setPositiveButton("OK", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
    }

    private void applyInitialTheme(String themeName) {
        switch (themeName) {
            case "pink_brown": setTheme(R.style.Theme_PinkBrown); break;
            case "blue_white": setTheme(R.style.Theme_BlueWhite); break;
            case "green_white": setTheme(R.style.Theme_GreenWhite); break;
            default: setTheme(R.style.Theme_PinkBrown); break;
        }
    }

    private void applyThemeColors() {
        String theme = sharedPreferences.getString("theme", "pink_brown");
        int backgroundColor, fabColor, buttonColor, tabSelectedColor, tabUnselectedColor, textColor;

        switch (theme) {
            case "pink_brown":
                backgroundColor = getResources().getColor(R.color.pink_background);
                fabColor = getResources().getColor(R.color.pink_primary);
                buttonColor = getResources().getColor(R.color.pink_primary);
                tabSelectedColor = getResources().getColor(R.color.pink);
                tabUnselectedColor = getResources().getColor(R.color.brown);
                textColor = getResources().getColor(R.color.brown);
                break;
            case "blue_white":
                backgroundColor = getResources().getColor(R.color.blue_background);
                fabColor = getResources().getColor(R.color.blue_primary);
                buttonColor = getResources().getColor(R.color.blue_primary);
                tabSelectedColor = getResources().getColor(R.color.blue);
                tabUnselectedColor = getResources().getColor(R.color.black);
                textColor = getResources().getColor(R.color.black);
                break;
            case "green_white":
                backgroundColor = getResources().getColor(R.color.green_background);
                fabColor = getResources().getColor(R.color.green_primary);
                buttonColor = getResources().getColor(R.color.green_primary);
                tabSelectedColor = getResources().getColor(R.color.green);
                tabUnselectedColor = getResources().getColor(R.color.black);
                textColor = getResources().getColor(R.color.black);
                break;
            default:
                backgroundColor = getResources().getColor(R.color.pink_background);
                fabColor = getResources().getColor(R.color.pink_primary);
                buttonColor = getResources().getColor(R.color.pink_primary);
                tabSelectedColor = getResources().getColor(R.color.pink);
                tabUnselectedColor = getResources().getColor(R.color.brown);
                textColor = getResources().getColor(R.color.brown);
                break;
        }

        findViewById(R.id.viewPagerTasks).setBackgroundColor(backgroundColor);
        fabAddTask.setBackgroundTintList(android.content.res.ColorStateList.valueOf(fabColor));
        btnAddCategory.setBackgroundColor(buttonColor);
        btnAddCategory.setTextColor(textColor);
        tabLayoutCategories.setSelectedTabIndicatorColor(tabSelectedColor);
        tabLayoutCategories.setTabTextColors(tabUnselectedColor, tabSelectedColor);
    }

    void loadCategoriesAndTasks() {
        if (!NetworkUtil.isConnected(this)) {
            // כבר יש בדיקה ב-onStart, אבל ליתר ביטחון נשאיר כאן Toast
            Toast.makeText(this, "No Internet Connection.", Toast.LENGTH_LONG).show();
            return;
        }

        FBRef.getUserCategoriesRef().get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                categoryList.clear();
                fragments.clear();

                for (QueryDocumentSnapshot doc : task.getResult()) {
                    String categoryName = doc.getString("name");
                    if (categoryName != null) categoryList.add(categoryName);
                }

                categoryList.add(0, "All Tasks");

                for (String cat : categoryList) {
                    fragments.add(CategoryTasksFragment.newInstance(cat));
                }

                pagerAdapter = new CategoryPagerAdapter(this, fragments);
                viewPagerTasks.setAdapter(pagerAdapter);

                new TabLayoutMediator(tabLayoutCategories, viewPagerTasks,
                        (tab, position) -> tab.setText(categoryList.get(position))
                ).attach();

            } else {
                Toast.makeText(this, "Failed Loading Categories", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddCategoryDialog() {
        EditText input = new EditText(this);
        input.setHint("Category's Name");

        new AlertDialog.Builder(this)
                .setTitle("Add Category")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newCategory = input.getText().toString().trim();
                    if (!newCategory.isEmpty()) saveNewCategory(newCategory);
                    else Toast.makeText(this, "Put A name For The Category", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveNewCategory(String categoryName) {
        if (!NetworkUtil.isConnected(this)) {
            checkInternet();
            return;
        }

        Category category = new Category(categoryName);
        FBRef.getUserCategoriesRef().document(categoryName)
                .set(category)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Category Added!", Toast.LENGTH_SHORT).show();
                    loadCategoriesAndTasks();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed Adding Category", Toast.LENGTH_SHORT).show());
    }

    private void updateTasksMonthAndTime() {
        if (!NetworkUtil.isConnected(this)) return;

        FBRef.getUserTasksRef().get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    Task t = doc.toObject(Task.class);
                    boolean needsUpdate = false;

                    if (t.getMonth() == 0 || t.getMinute() == 0) {
                        java.util.Calendar cal = java.util.Calendar.getInstance();
                        cal.setTimeInMillis(t.getCreationTime());

                        t.setMonth(cal.get(java.util.Calendar.MONTH) + 1);
                        t.setDay(cal.get(java.util.Calendar.DAY_OF_MONTH));
                        t.setHour(cal.get(java.util.Calendar.HOUR_OF_DAY));
                        t.setMinute(cal.get(java.util.Calendar.MINUTE));

                        needsUpdate = true;
                    }

                    if (needsUpdate) {
                        FBRef.getUserTasksRef().document(doc.getId()).set(t);
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