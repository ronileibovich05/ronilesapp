package com.example.ronilesapp;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class Item_TaskActivity extends BaseActivity {

    private EditText editTaskTitle, editTaskDescription;
    private DatePicker datePicker;
    private TimePicker timePicker;
    private Spinner spinnerCategory;
    private Button btnAddCategory;
    private Button btnCancelTask;

    private ArrayAdapter<String> categoryAdapter;
    private List<String> categoryList = new ArrayList<>();

    // 🔹 Theme
    private SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener themeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // 🔹 SharedPreferences ו־Theme ראשוני
        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        applyInitialTheme(sharedPreferences.getString("theme", "pink_brown"));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_task);

        // 🔹 findViewById אחרי setContentView
        editTaskTitle = findViewById(R.id.editTextTaskTitle);
        editTaskDescription = findViewById(R.id.editTextTaskDescription);
        datePicker = findViewById(R.id.datePickerTask);
        timePicker = findViewById(R.id.timePickerTask);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnAddCategory = findViewById(R.id.btnAddCategory);
        btnCancelTask = findViewById(R.id.buttonCancelTask);

        // 🔹 Spinner setup
        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryList);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        // 🔹 load categories
        loadCategories();

        // 🔹 Add Category button
        btnAddCategory.setOnClickListener(v -> showAddCategoryDialog());

        // 🔹 Cancel Task button
        btnCancelTask.setOnClickListener(v -> {
            if (!editTaskTitle.getText().toString().isEmpty() || !editTaskDescription.getText().toString().isEmpty()) {
                new AlertDialog.Builder(this)
                        .setTitle("Return")
                        .setMessage("Task is not saved. Sure you want to return?")
                        .setPositiveButton("Yes", (dialog, which) -> finish())
                        .setNegativeButton("No", null)
                        .show();
            } else {
                finish();
            }
        });

        // 🔹 Theme listener בזמן אמת
        themeListener = (prefs, key) -> {
            if ("theme".equals(key)) {
                applyThemeColors();
            }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(themeListener);

        // 🔹 החלת צבעים ראשונית
        applyThemeColors();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (themeListener != null && sharedPreferences != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(themeListener);
        }
    }

    // 🔹 Theme ראשוני
    private void applyInitialTheme(String themeName) {
        switch (themeName) {
            case "pink_brown":
                setTheme(R.style.Theme_PinkBrown);
                break;
            case "blue_white":
                setTheme(R.style.Theme_BlueWhite);
                break;
            case "green_white":
                setTheme(R.style.Theme_GreenWhite);
                break;
            default:
                setTheme(R.style.Theme_PinkBrown);
                break;
        }
    }

    // 🔹 החלפת צבעים לפי Theme
    private void applyThemeColors() {
        String theme = sharedPreferences.getString("theme", "pink_brown");
        int backgroundColor, buttonColor, textColor;

        switch (theme) {
            case "pink_brown":
                backgroundColor = getResources().getColor(R.color.pink_background);
                buttonColor = getResources().getColor(R.color.pink_primary);
                textColor = getResources().getColor(R.color.brown);
                break;
            case "blue_white":
                backgroundColor = getResources().getColor(R.color.blue_background);
                buttonColor = getResources().getColor(R.color.blue_primary);
                textColor = getResources().getColor(R.color.black);
                break;
            case "green_white":
                backgroundColor = getResources().getColor(R.color.green_background);
                buttonColor = getResources().getColor(R.color.green_primary);
                textColor = getResources().getColor(R.color.black);
                break;
            default:
                backgroundColor = getResources().getColor(R.color.pink_background);
                buttonColor = getResources().getColor(R.color.pink_primary);
                textColor = getResources().getColor(R.color.brown);
                break;
        }

        // רקע כללי
        findViewById(android.R.id.content).setBackgroundColor(backgroundColor);

        // כפתורים
        btnAddCategory.setBackgroundColor(buttonColor);
        btnAddCategory.setTextColor(textColor);
        btnCancelTask.setBackgroundColor(buttonColor);
        btnCancelTask.setTextColor(textColor);

        // EditText ו-Spinner
        editTaskTitle.setTextColor(textColor);
        editTaskDescription.setTextColor(textColor);
        spinnerCategory.setPopupBackgroundResource(android.R.color.white);
    }

    // 🔹 Load categories
    private void loadCategories() {
        categoryList.clear();
        FBRef.getUserCategoriesRef().get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    categoryList.add(doc.getString("name"));
                }
                categoryAdapter.notifyDataSetChanged();
            } else {
                Toast.makeText(this, "Failed Loading Categories", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 🔹 Add category dialog
    private void showAddCategoryDialog() {
        EditText input = new EditText(this);
        input.setHint("Name Of Category");

        new AlertDialog.Builder(this)
                .setTitle("Add Category")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newCategory = input.getText().toString().trim();
                    if (!newCategory.isEmpty()) saveNewCategory(newCategory);
                    else Toast.makeText(this, "Please put a name for the category", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveNewCategory(String categoryName) {
        Category category = new Category(categoryName);
        FBRef.getUserCategoriesRef().document(categoryName)
                .set(category)
                .addOnSuccessListener(aVoid -> {
                    categoryList.add(categoryName);
                    categoryAdapter.notifyDataSetChanged();
                    spinnerCategory.setSelection(categoryList.indexOf(categoryName));
                    Toast.makeText(this, "Category Added!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed Adding Category", Toast.LENGTH_SHORT).show());
    }

    // 🔹 Save task
    public void saveTask(View view) {
        String title = editTaskTitle.getText().toString().trim();
        String description = editTaskDescription.getText().toString().trim();

        int day = datePicker.getDayOfMonth();
        int month = datePicker.getMonth() + 1;
        int year = datePicker.getYear();
        int hour = timePicker.getHour();
        int minute = timePicker.getMinute();

        String category = spinnerCategory.getSelectedItem() != null
                ? spinnerCategory.getSelectedItem().toString()
                : "ללא קטגוריה";

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a task title", Toast.LENGTH_SHORT).show();
            return;
        }

        Task newTask = new Task(title, description, day, month, year, hour, minute, category, false);

        FBRef.getUserTasksRef().document(title).set(newTask)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(Item_TaskActivity.this, "Task Added!", Toast.LENGTH_SHORT).show();

                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("newTaskTitle", title);
                    resultIntent.putExtra("newTaskDescription", description);
                    resultIntent.putExtra("newTaskDay", day);
                    resultIntent.putExtra("newTaskMonth", month);
                    resultIntent.putExtra("newTaskYear", year);
                    resultIntent.putExtra("newTaskHour", hour);
                    resultIntent.putExtra("newTaskMinute", minute);
                    resultIntent.putExtra("newTaskCategory", category);
                    resultIntent.putExtra("newTaskDone", false);
                    setResult(RESULT_OK, resultIntent);

                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(Item_TaskActivity.this, "Failed Saving: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
