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
    private Button btnAddCategory;

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
        btnAddCategory = findViewById(R.id.btnAddCategoryTasks);

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

        btnAddCategory.setOnClickListener(v -> showAddCategoryDialog());

        // טוענים קטגוריות ומשימות
        loadCategoriesAndTasks();

        // 🔹 מתקן את החודש והשעה של כל המשימות הישנות (רק פעם אחת)
        updateTasksMonthAndTime();
    }

    // טוען קטגוריות ויוצר טאבים ופרגמנטים
    void loadCategoriesAndTasks() {
        FBRef.getUserCategoriesRef().get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                categoryList.clear();
                fragments.clear();

                for (QueryDocumentSnapshot doc : task.getResult()) {
                    String categoryName = doc.getString("name");
                    if (categoryName != null) categoryList.add(categoryName);
                }

                // הוספת טאב "כל המשימות" ראשון
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

    // דיאלוג להוספת קטגוריה
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
                .addOnFailureListener(e ->
                        Toast.makeText(this, "שגיאה בהוספת קטגוריה", Toast.LENGTH_SHORT).show()
                );
    }

    // 🔹 פונקציה לתיקון החודש והשעה של משימות קיימות
    private void updateTasksMonthAndTime() {
        FBRef.getUserTasksRef().get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    Task t = doc.toObject(Task.class);
                    boolean needsUpdate = false;

                    // אם החודש 0 או הדקה 0 – מתקנים לפי זמן יצירת המשימה
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
