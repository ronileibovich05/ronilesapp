package com.example.ronilesapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

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

        // לאפשר פתיחת מסך הוספת משימה
        addTaskLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        // אפשר לטעון מחדש את הקטגוריות והמשימות
                        loadCategoriesAndTasks();
                    }
                }
        );

        fabAddTask.setOnClickListener(v -> {
            Intent intent = new Intent(TasksActivity.this, Item_TaskActivity.class);
            addTaskLauncher.launch(intent);
        });

        // טוענים קטגוריות ומשימות
        loadCategoriesAndTasks();
    }

    private void loadCategoriesAndTasks() {
        // קודם כל טוענים את הקטגוריות של המשתמש
        FBRef.getUserCategoriesRef().get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                categoryList.clear();
                fragments.clear();

                // מוסיפים קטגוריות מה-Firestore
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    String categoryName = doc.getString("name");
                    if (categoryName != null) {
                        categoryList.add(categoryName);
                    }
                }

                // אפשרות "כל המשימות" בכל ההתחלה
                categoryList.add(0, "כל המשימות");

                // יוצרים פרגמנט לכל קטגוריה
                for (String cat : categoryList) {
                    fragments.add(CategoryTasksFragment.newInstance(cat));

                }

                // יוצרים Adapter ל-ViewPager2
                pagerAdapter = new CategoryPagerAdapter(this, fragments);
                viewPagerTasks.setAdapter(pagerAdapter);

                // מחברים את TabLayout עם ViewPager2
                new TabLayoutMediator(tabLayoutCategories, viewPagerTasks,
                        (tab, position) -> tab.setText(categoryList.get(position))
                ).attach();

            } else {
                Toast.makeText(this, "שגיאה בטעינת קטגוריות", Toast.LENGTH_SHORT).show();
            }
        });
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
