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
import java.util.Calendar;

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

        // Check if we are in Edit Mode
        if (getIntent().hasExtra("taskId")) {
            String taskIdToEdit = getIntent().getStringExtra("taskId");
            String oldTitle = getIntent().getStringExtra("title");
            String oldDesc = getIntent().getStringExtra("desc");
            // ... get other extras ...

            // Set text to fields
            editTaskTitle.setText(oldTitle);
            editTaskDescription.setText(oldDesc);
            // ... update date/time pickers ...
        }

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

        // נתונים מהפיקרים
        int day = datePicker.getDayOfMonth();
        int month = datePicker.getMonth(); // שים לב: ב-Calendar חודשים הם 0-11
        int year = datePicker.getYear();
        int hour = timePicker.getHour();
        int minute = timePicker.getMinute();

        String category = spinnerCategory.getSelectedItem() != null
                ? spinnerCategory.getSelectedItem().toString()
                : "No Category";

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a task title", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. חישוב הזמן המדויק במילי-שניות עבור ההתראה
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day, hour, minute, 0);
        long taskTimeInMillis = calendar.getTimeInMillis();

        // בדיקה שהזמן לא עבר כבר (אופציונלי - כדי לא לקבל התראה מיידית על משימה בעבר)
        if (taskTimeInMillis < System.currentTimeMillis()) {
            // אם בחר זמן עבר, נוסיף לו דקה כדי שלא יצעק מיד, או פשוט נתעלם
            // כאן נשאיר כרגיל
        }

        // 2. יצירת מזהה ייחודי (ID) למשימה
        // חשוב מאוד! לא להשתמש ב-Title כ-ID כי אם תשנה שם למשימה זה ייצור חדשה
        String taskId = FBRef.getUserTasksRef().document().getId();

        // יצירת האובייקט (הוספתי את ה-taskTimeInMillis לאובייקט אם תרצה לשמור אותו גם)
        // שים לב: אני שולח month + 1 לתצוגה, אבל לחישוב הזמן השתמשתי ב-month המקורי
        Task newTask = new Task(taskId, title, description, day, month + 1, year, hour, minute, category, false);

        // אופציונלי: אם הוספת שדה timeInMillis למחלקה Task, תוסיף:
        // newTask.setTimeInMillis(taskTimeInMillis);
        String finalTaskId;
        if (getIntent().hasExtra("taskId")) {
            // EDIT MODE: Use existing ID
            finalTaskId = getIntent().getStringExtra("taskId");
        } else {
            // CREATE MODE: Generate new ID
            finalTaskId = FBRef.getUserTasksRef().document().getId();
        }
        FBRef.getUserTasksRef().document(taskId).set(newTask)
                .addOnSuccessListener(aVoid -> {

                    // Schedule the notification
                    NotificationHelper.scheduleNotification(this, taskTimeInMillis, title, taskId);

                    // Print to Logcat for debugging
                    System.out.println("DEBUG: Alarm set for task: " + title);

                    Toast.makeText(Item_TaskActivity.this, "Task Added Successfully!", Toast.LENGTH_SHORT).show();

                    Intent resultIntent = new Intent();
                    setResult(RESULT_OK, resultIntent);
                    finish();
                });
    }
}
