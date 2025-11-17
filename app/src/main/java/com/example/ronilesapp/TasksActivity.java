package com.example.ronilesapp;

import android.content.Intent;
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

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TasksActivity extends AppCompatActivity {

    private TabLayout tabLayoutCategories;
    private ViewPager2 viewPagerTasks;
    private FloatingActionButton fabAddTask;
    private Button btnAddCategory; // כפתור חדש להוספת קטגוריה

    private ActivityResultLauncher<Intent> addTaskLauncher;

    private List<String> categoryList = new ArrayList<>();
    private List<Fragment> fragments = new ArrayList<>();
    private CategoryPagerAdapter pagerAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks);

        tabLayoutCategories = findViewById(R.id.tabLayoutCategories);
        viewPagerTasks = findViewById(R.id.viewPagerTasks);
        fabAddTask = findViewById(R.id.fabAddTask);
        btnAddCategory = findViewById(R.id.btnAddCategoryTasks); // מחובר ל-XML החדש

        // פתיחת מסך הוספת משימה
        addTaskLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        loadCategoriesAndTasks(); // טוען מחדש
                    }
                }
        );

        fabAddTask.setOnClickListener(v -> {
            Intent intent = new Intent(TasksActivity.this, Item_TaskActivity.class);
            addTaskLauncher.launch(intent);
        });

        // כפתור להוספת קטגוריה ישירות מהעמוד
        btnAddCategory.setOnClickListener(v -> showAddCategoryDialog());

        // טוענים קטגוריות ומשימות
        loadCategoriesAndTasks();
    }

    // טוען קטגוריות ויוצר טאבים ופרגמנטים
    void loadCategoriesAndTasks() {
        FBRef.getUserCategoriesRef().get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                categoryList.clear();
                fragments.clear();

                for (QueryDocumentSnapshot doc : task.getResult()) {
                    String categoryName = doc.getString("name");
                    if (categoryName != null) {
                        categoryList.add(categoryName);
                    }
                }

                // "כל המשימות" בטאב הראשון
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

    // דיאלוג הוספת קטגוריה
    private void showAddCategoryDialog() {
        EditText input = new EditText(this);
        input.setHint("שם קטגוריה");

        new AlertDialog.Builder(this)
                .setTitle("הוסף קטגוריה")
                .setView(input)
                .setPositiveButton("שמור", (dialog, which) -> {
                    String newCategory = input.getText().toString().trim();
                    if (!newCategory.isEmpty()) {
                        saveNewCategory(newCategory);
                    } else {
                        Toast.makeText(this, "יש להזין שם קטגוריה", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    // שמירת קטגוריה חדשה ל-Firestore
    private void saveNewCategory(String categoryName) {
        Category category = new Category(categoryName);

        FBRef.getUserCategoriesRef().document(categoryName)
                .set(category)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "קטגוריה נוספה!", Toast.LENGTH_SHORT).show();
                    loadCategoriesAndTasks(); // טוען מחדש את הטאבים
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "שגיאה בהוספת קטגוריה", Toast.LENGTH_SHORT).show()
                );
    }

    // Adapter ל־ViewPager2
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
