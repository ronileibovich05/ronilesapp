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

import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AddTaskActivity extends BaseActivity { // שינינו את השם כאן

    private EditText editTaskTitle, editTaskDescription;
    private DatePicker datePicker;
    private TimePicker timePicker;
    private Spinner spinnerCategory;
    private Button btnAddCategory;
    private Button btnCancelTask;
    private Button btnSaveTask; // הוספנו משתנה לכפתור השמירה

    private ArrayAdapter<String> categoryAdapter;
    private List<String> categoryList = new ArrayList<>();

    private SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener themeListener;

    private String taskIdToEdit = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // הגדרות Theme לפני הכל
        sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        applyInitialTheme(sharedPreferences.getString("theme", "pink_brown"));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task); // וודאי שזה שם ה-XML שלך

        // חיבור לרכיבים
        editTaskTitle = findViewById(R.id.editTextTaskTitle);
        editTaskDescription = findViewById(R.id.editTextTaskDescription);
        datePicker = findViewById(R.id.datePickerTask);
        timePicker = findViewById(R.id.timePickerTask);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnAddCategory = findViewById(R.id.btnAddCategory);
        btnCancelTask = findViewById(R.id.buttonCancelTask);

        // נניח שיש לך כפתור שמירה ב-XML, צריך למצוא אותו לפי ה-ID
        // אם ה-ID שלו הוא buttonSaveTask:
        btnSaveTask = findViewById(R.id.buttonSaveTask);

        timePicker.setIs24HourView(true);

        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryList);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        // בדיקה האם הגענו לעריכה (Edit) או יצירה חדשה
        if (getIntent().hasExtra("taskId")) {
            taskIdToEdit = getIntent().getStringExtra("taskId");

            editTaskTitle.setText(getIntent().getStringExtra("title"));
            editTaskDescription.setText(getIntent().getStringExtra("desc"));

            int y = getIntent().getIntExtra("year", 2025);
            int m = getIntent().getIntExtra("month", 1);
            int d = getIntent().getIntExtra("day", 1);
            datePicker.updateDate(y, m - 1, d); // חודשים ב-DatePicker הם 0-11

            int h = getIntent().getIntExtra("hour", 12);
            int min = getIntent().getIntExtra("minute", 0);
            timePicker.setHour(h);
            timePicker.setMinute(min);
        }

        loadCategories();

        btnAddCategory.setOnClickListener(v -> showAddCategoryDialog());

        btnCancelTask.setOnClickListener(v -> finish());

        // חיבור כפתור השמירה לפונקציה
        if (btnSaveTask != null) {
            btnSaveTask.setOnClickListener(this::saveTask);
        }

        // האזנה לשינויי צבעים
        themeListener = (prefs, key) -> {
            if ("theme".equals(key)) {
                applyThemeColors();
            }
        };
        sharedPreferences.registerOnSharedPreferenceChangeListener(themeListener);

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
            case "pink_brown": setTheme(R.style.Theme_PinkBrown); break;
            case "blue_white": setTheme(R.style.Theme_BlueWhite); break;
            case "green_white": setTheme(R.style.Theme_GreenWhite); break;
            default: setTheme(R.style.Theme_PinkBrown); break;
        }
    }

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

        View contentView = findViewById(android.R.id.content);
        if (contentView != null) contentView.setBackgroundColor(backgroundColor);

        btnAddCategory.setBackgroundColor(buttonColor);
        btnAddCategory.setTextColor(textColor);
        btnCancelTask.setBackgroundColor(buttonColor);
        btnCancelTask.setTextColor(textColor);

        if (btnSaveTask != null) {
            btnSaveTask.setBackgroundColor(buttonColor);
            btnSaveTask.setTextColor(textColor);
        }

        editTaskTitle.setTextColor(textColor);
        editTaskDescription.setTextColor(textColor);
        spinnerCategory.setPopupBackgroundResource(android.R.color.white);
    }

    private void loadCategories() {
        categoryList.clear();
        // תיקון: שימוש ב-Utils במקום FBRef
        Utils.getUserCategoriesRef().get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    categoryList.add(doc.getString("name"));
                }
                categoryAdapter.notifyDataSetChanged();

                if (taskIdToEdit != null) {
                    String currentCat = getIntent().getStringExtra("category");
                    if (currentCat != null) {
                        int pos = categoryAdapter.getPosition(currentCat);
                        if (pos >= 0) spinnerCategory.setSelection(pos);
                    }
                }
            } else {
                Toast.makeText(this, "Failed Loading Categories", Toast.LENGTH_SHORT).show();
            }
        });
    }

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
        // תיקון: שימוש ב-Utils במקום FBRef
        Utils.getUserCategoriesRef().document(categoryName)
                .set(category)
                .addOnSuccessListener(aVoid -> {
                    categoryList.add(categoryName);
                    categoryAdapter.notifyDataSetChanged();
                    spinnerCategory.setSelection(categoryList.indexOf(categoryName));
                    Toast.makeText(this, "Category Added!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed Adding Category", Toast.LENGTH_SHORT).show());
    }

    public void saveTask(View view) {
        String title = editTaskTitle.getText().toString().trim();
        String description = editTaskDescription.getText().toString().trim();

        int day = datePicker.getDayOfMonth();
        int month = datePicker.getMonth(); // 0-11
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

        String finalTaskId;
        if (taskIdToEdit != null) {
            finalTaskId = taskIdToEdit;
        } else {
            // תיקון: שימוש ב-Utils
            finalTaskId = Utils.getUserTasksRef().document().getId();
        }

        // יצירת משימה חדשה (month+1 כי נשמר 1-12)
        Task newTask = new Task(finalTaskId, title, description, day, month + 1, year, hour, minute, category, false);

        // תיקון: שימוש ב-Utils
        Utils.getUserTasksRef().document(finalTaskId).set(newTask)
                .addOnSuccessListener(aVoid -> {

                    // תזמון התראה
                    NotificationHelper.scheduleNotification(
                            this,
                            finalTaskId,
                            title,
                            year,
                            month + 1,
                            day,
                            hour,
                            minute
                    );

                    String msg = (taskIdToEdit != null) ? "Task Updated!" : "Task Created!";
                    Toast.makeText(AddTaskActivity.this, msg, Toast.LENGTH_SHORT).show();

                    Intent resultIntent = new Intent();
                    setResult(RESULT_OK, resultIntent);
                    finish();
                });
    }
}