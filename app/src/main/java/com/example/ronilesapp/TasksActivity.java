package com.example.ronilesapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;
import android.app.AlertDialog;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

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

        // ✅ חדש — סימון שהמסך הנוכחי הוא HOME
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
                // כבר כאן — לא צריך לעשות כלום
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
            if (!NetworkUtil.isConnected(this)) {
                Toast.makeText(this, "אין חיבור לאינטרנט.", Toast.LENGTH_LONG).show();
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

        // החלת צבעים ראשונית
        applyThemeColors();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (themeListener != null && sharedPreferences != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(themeListener);
        }
    }

    private void applyInitialTheme(String themeName) {
        switch (themeName) {
            case "Theme.PinkBrown":
                setTheme(R.style.Theme_PinkBrown);
                break;
            case "Theme.BlueWhite":
                setTheme(R.style.Theme_BlueWhite);
                break;
            case "Theme.GreenWhite":
                setTheme(R.style.Theme_GreenWhite);
                break;
        }
    }

    private void applyThemeColors() {
        String theme = sharedPreferences.getString("theme", "Theme.PinkBrown");
        int backgroundColor, fabColor, buttonColor, tabSelectedColor, tabUnselectedColor;

        switch(theme) {
            case "Theme.PinkBrown":
                backgroundColor = getResources().getColor(R.color.pink_background);
                fabColor = getResources().getColor(R.color.pink_primary);
                buttonColor = getResources().getColor(R.color.pink_primary);
                tabSelectedColor = getResources().getColor(R.color.pink);
                tabUnselectedColor = getResources().getColor(R.color.brown);
                break;
            case "Theme.BlueWhite":
                backgroundColor = getResources().getColor(R.color.blue_background);
                fabColor = getResources().getColor(R.color.blue_primary);
                buttonColor = getResources().getColor(R.color.blue_primary);
                tabSelectedColor = getResources().getColor(R.color.blue);
                tabUnselectedColor = getResources().getColor(R.color.white);
                break;
            case "Theme.GreenWhite":
                backgroundColor = getResources().getColor(R.color.green_background);
                fabColor = getResources().getColor(R.color.green_primary);
                buttonColor = getResources().getColor(R.color.green_primary);
                tabSelectedColor = getResources().getColor(R.color.green);
                tabUnselectedColor = getResources().getColor(R.color.white);
                break;
            default:
                backgroundColor = getResources().getColor(R.color.pink_background);
                fabColor = getResources().getColor(R.color.pink_primary);
                buttonColor = getResources().getColor(R.color.pink_primary);
                tabSelectedColor = getResources().getColor(R.color.pink);
                tabUnselectedColor = getResources().getColor(R.color.brown);
                break;
        }

        findViewById(R.id.viewPagerTasks).setBackgroundColor(backgroundColor);
        fabAddTask.setBackgroundTintList(android.content.res.ColorStateList.valueOf(fabColor));
        btnAddCategory.setBackgroundColor(buttonColor);

        // צבעי TabLayout
        tabLayoutCategories.setSelectedTabIndicatorColor(tabSelectedColor);
        tabLayoutCategories.setTabTextColors(tabUnselectedColor, tabSelectedColor);
    }

    void loadCategoriesAndTasks() {
        if (!NetworkUtil.isConnected(this)) {
            Toast.makeText(this, "אין חיבור לאינטרנט.", Toast.LENGTH_LONG).show();
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

                categoryList.add(0, "כל המשימות");

                for (String cat : categoryList) {
                    fragments.add(CategoryTasksFragment.newInstance(cat));
                }

                pagerAdapter = new CategoryPagerAdapter(this, fragments);
                viewPagerTasks.setAdapter(pagerAdapter);

                new TabLayoutMediator(tabLayoutCategories, viewPagerTasks,
                        (tab, position) -> tab.setText(categoryList.get(position))
                ).attach();

            } else {
                Toast.makeText(this, "שגיאה בטעינת קטגוריות", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddCategoryDialog() {
        EditText input = new EditText(this);
        input.setHint("שם קטגוריה");

        new AlertDialog.Builder(this)
                .setTitle("הוסף קטגוריה")
                .setView(input)
                .setPositiveButton("שמור", (dialog, which) -> {
                    String newCategory = input.getText().toString().trim();
                    if (!newCategory.isEmpty()) saveNewCategory(newCategory);
                    else Toast.makeText(this, "יש להזין שם קטגוריה", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void saveNewCategory(String categoryName) {
        Category category = new Category(categoryName);

        FBRef.getUserCategoriesRef().document(categoryName)
                .set(category)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "קטגוריה נוספה!", Toast.LENGTH_SHORT).show();
                    loadCategoriesAndTasks();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה בהוספת קטגוריה", Toast.LENGTH_SHORT).show());
    }

    private void updateTasksMonthAndTime() {
        if (!NetworkUtil.isConnected(this)) {
            Toast.makeText(this, "אין חיבור לאינטרנט.", Toast.LENGTH_LONG).show();
            return;
        }

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
                        FBRef.getUserTasksRef().document(doc.getId())
                                .set(t)
                                .addOnSuccessListener(aVoid -> System.out.println("Task updated: " + t.getTitle()))
                                .addOnFailureListener(e -> System.out.println("Error updating task: " + t.getTitle()));
                    }
                }
            } else {
                System.out.println("Error fetching tasks for update");
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
