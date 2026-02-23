package com.example.ronilesapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class AddTaskActivity extends BaseActivity {

    private EditText editTaskTitle, editTaskDescription;
    private DatePicker datePicker;
    private TimePicker timePicker;
    private Spinner spinnerCategory;
    private Button btnAddCategory;
    private Button btnCancelTask;
    private Button btnSaveTask;

    private ArrayAdapter<String> categoryAdapter;

    private List<String> categoryList = new ArrayList<>();

    private SharedPreferences.OnSharedPreferenceChangeListener themeListener;

    private String taskIdToEdit = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applySelectedTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        editTaskTitle = findViewById(R.id.editTextTaskTitle);
        editTaskDescription = findViewById(R.id.editTextTaskDescription);
        datePicker = findViewById(R.id.datePickerTask);
        timePicker = findViewById(R.id.timePickerTask);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnAddCategory = findViewById(R.id.btnAddCategory);
        btnCancelTask = findViewById(R.id.buttonCancelTask);
        btnSaveTask = findViewById(R.id.buttonSaveTask);

        timePicker.setIs24HourView(true);

        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryList);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        if (getIntent() != null && getIntent().hasExtra("taskId")) {
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

        btnAddCategory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AddTaskActivity.this.showAddCategoryDialog();
            }
        });

        btnCancelTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AddTaskActivity.this.finish();
            }
        });

        btnSaveTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AddTaskActivity.this.saveTask(v);
            }
        });

        themeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences prefs, @Nullable String key) {
                if (BaseActivity.KEY_THEME.equals(key)) {
                    AddTaskActivity.this.applyThemeColors();    // ה recreate מרענן את כל ה-Activity - לעומת applyThemeColors
                }
            }
        };
        baseSharedPreferences.registerOnSharedPreferenceChangeListener(themeListener);

        applyThemeColors();

        loadCategories();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (themeListener != null && baseSharedPreferences != null) {
            baseSharedPreferences.unregisterOnSharedPreferenceChangeListener(themeListener);
        }
    }

    private void applyThemeColors() {
        String theme = baseSharedPreferences.getString(BaseActivity.KEY_THEME, "pink_brown");
        int backgroundColor, buttonColor, textColor;

        switch (theme) {
            case "pink_brown":
                backgroundColor = ContextCompat.getColor(this, R.color.pink_background);
                buttonColor = ContextCompat.getColor(this, R.color.pink_primary);
                textColor = ContextCompat.getColor(this, R.color.brown);
                break;
            case "blue_white":
                backgroundColor = ContextCompat.getColor(this, R.color.blue_background);
                buttonColor = ContextCompat.getColor(this, R.color.blue_primary);
                textColor = ContextCompat.getColor(this, R.color.black);
                break;
            case "green_white":
                backgroundColor = ContextCompat.getColor(this, R.color.green_background);
                buttonColor = ContextCompat.getColor(this, R.color.green_primary);
                textColor = ContextCompat.getColor(this, R.color.black);
                break;
            default:
                backgroundColor = ContextCompat.getColor(this, R.color.pink_background);
                buttonColor = ContextCompat.getColor(this, R.color.pink_primary);
                textColor = ContextCompat.getColor(this, R.color.brown);
                break;
        }

        View contentView = findViewById(android.R.id.content);
        if (contentView != null) contentView.setBackgroundColor(backgroundColor);

        btnAddCategory.setBackgroundColor(buttonColor);
        btnAddCategory.setTextColor(textColor);
        btnCancelTask.setBackgroundColor(buttonColor);
        btnCancelTask.setTextColor(textColor);

        btnSaveTask.setBackgroundColor(buttonColor);
        btnSaveTask.setTextColor(textColor);

        editTaskTitle.setTextColor(textColor);
        editTaskDescription.setTextColor(textColor);
        spinnerCategory.setPopupBackgroundResource(android.R.color.white);
    }

    private void loadCategories() {

        if (!Utils.isConnected(this)) {
            Toast.makeText(this, "No Internet Connection.", Toast.LENGTH_LONG).show();
            return;
        }

        categoryList.clear();
        Utils.getUserCategoriesRef().get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        String name = doc.getString("name");
                        if (name != null && !name.isEmpty()) {
                            categoryList.add(name);
                        }
                    }
                    categoryAdapter.notifyDataSetChanged();

                    if (taskIdToEdit != null) {
                        if (AddTaskActivity.this.getIntent() != null) {
                            String currentCat = AddTaskActivity.this.getIntent().getStringExtra("category");
                            if (currentCat != null) {
                                int pos = categoryAdapter.getPosition(currentCat);
                                if (pos >= 0) spinnerCategory.setSelection(pos);
                            }
                        }
                    }
                } else {
                    Toast.makeText(AddTaskActivity.this, "Failed Loading Categories", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showAddCategoryDialog() {
        EditText input = new EditText(this);
        input.setHint("Name Of Category");

        new AlertDialog.Builder(this)
                .setTitle("Add Category")
                .setView(input)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newCategory = input.getText().toString().trim();
                        if (!newCategory.isEmpty())
                            AddTaskActivity.this.saveNewCategory(newCategory);
                        else
                            Toast.makeText(AddTaskActivity.this, "Please put a name for the category", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveNewCategory(String categoryName) {
        Category category = new Category(categoryName);
        Utils.getUserCategoriesRef().document(categoryName)
                .set(category)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        categoryList.add(categoryName);
                        categoryAdapter.notifyDataSetChanged();
                        spinnerCategory.setSelection(categoryList.indexOf(categoryName));
                        Toast.makeText(AddTaskActivity.this, "Category Added!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(AddTaskActivity.this, "Failed Adding Category", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void saveTask(View view) {

        if (!Utils.isConnected(this)) {
            Toast.makeText(this, "No Internet Connection.", Toast.LENGTH_LONG).show();
            return;
        }

        String title = editTaskTitle.getText().toString().trim();
        String description = editTaskDescription.getText().toString().trim();

        int day = datePicker.getDayOfMonth();
        int month = datePicker.getMonth() + 1; // 1-12
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
            finalTaskId = Utils.getUserTasksRef().document().getId();
        }

        UserTask newTask = new UserTask(finalTaskId, title, description, day, month, year, hour, minute, category, false);

        Utils.getUserTasksRef().document(finalTaskId).set(newTask)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                        NotificationHelper.scheduleNotification(
                                AddTaskActivity.this,
                                finalTaskId,
                                title,
                                year,
                                month,
                                day,
                                hour,
                                minute
                        );

                        String msg = (taskIdToEdit != null) ? "Task Updated!" : "Task Created!";
                        Toast.makeText(AddTaskActivity.this, msg, Toast.LENGTH_SHORT).show();


                        AddTaskActivity.this.setResult(RESULT_OK);
                        AddTaskActivity.this.finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {

                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(AddTaskActivity.this, "Failed to save task", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}