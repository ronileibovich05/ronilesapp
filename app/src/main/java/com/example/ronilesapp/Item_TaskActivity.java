package com.example.ronilesapp;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class Item_TaskActivity extends AppCompatActivity {

    private EditText editTaskTitle, editTaskDescription;
    private DatePicker datePicker;
    private TimePicker timePicker;
    private Spinner spinnerCategory;
    private Button btnAddCategory;
    private Button btnCancelTask; // כפתור ביטול

    private ArrayAdapter<String> categoryAdapter;
    private List<String> categoryList = new ArrayList<>();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_task);

        editTaskTitle = findViewById(R.id.editTextTaskTitle);
        editTaskDescription = findViewById(R.id.editTextTaskDescription);
        datePicker = findViewById(R.id.datePickerTask);
        timePicker = findViewById(R.id.timePickerTask);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        btnAddCategory = findViewById(R.id.btnAddCategory);
        btnCancelTask = findViewById(R.id.buttonCancelTask); // כפתור ביטול

        // הגדרת רשימת הקטגוריות ל־Spinner
        categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryList);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        // טוען קטגוריות קיימות מה-Firestore
        loadCategories();

        // מאזין לכפתור הוספת קטגוריה
        btnAddCategory.setOnClickListener(v -> showAddCategoryDialog());

        // מאזין לכפתור ביטול/חזרה
        btnCancelTask.setOnClickListener(v -> {
            if (!editTaskTitle.getText().toString().isEmpty() ||
                    !editTaskDescription.getText().toString().isEmpty()) {

                // מציג אזהרה לפני החזרה
                new AlertDialog.Builder(this)
                        .setTitle("חזרה")
                        .setMessage("המשימה לא נשמרה. האם אתה בטוח שברצונך לחזור?")
                        .setPositiveButton("כן", (dialog, which) -> finish())
                        .setNegativeButton("לא", null)
                        .show();
            } else {
                // אם אין שום תוכן בשדות, סוגר ישר
                finish();
            }
        });
    }

    private void loadCategories() {
        categoryList.clear();
        FBRef.getUserCategoriesRef().get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    categoryList.add(doc.getString("name"));
                }
                categoryAdapter.notifyDataSetChanged();
            } else {
                Toast.makeText(this, "שגיאה בטעינת קטגוריות", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddCategoryDialog() {
        EditText input = new EditText(this);
        input.setHint(getString(R.string.new_category));

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.add_category))
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

    private void saveNewCategory(String categoryName) {
        Category category = new Category(categoryName);
        FBRef.getUserCategoriesRef().document(categoryName)
                .set(category)
                .addOnSuccessListener(aVoid -> {
                    categoryList.add(categoryName);
                    categoryAdapter.notifyDataSetChanged();
                    spinnerCategory.setSelection(categoryList.indexOf(categoryName));
                    Toast.makeText(this, "קטגוריה נוספה!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה בהוספת קטגוריה", Toast.LENGTH_SHORT).show());
    }

    public void saveTask(View view) {
        String title = editTaskTitle.getText().toString().trim();
        String description = editTaskDescription.getText().toString().trim();
        String day = datePicker.getDayOfMonth() + "/" + (datePicker.getMonth() + 1) + "/" + datePicker.getYear();
        String hour = timePicker.getHour() + ":" + timePicker.getMinute();
        String category = spinnerCategory.getSelectedItem() != null
                ? spinnerCategory.getSelectedItem().toString()
                : "ללא קטגוריה";

        if (title.isEmpty()) {
            Toast.makeText(this, "נא למלא כותרת למשימה", Toast.LENGTH_SHORT).show();
            return;
        }

        Task newTask = new Task(title, description, day, hour, false);
        newTask.setCategory(category);

        FBRef.getUserTasksRef().document(title).set(newTask)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(Item_TaskActivity.this, "משימה נוספה!", Toast.LENGTH_SHORT).show();

                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("newTaskTitle", title);
                    resultIntent.putExtra("newTaskDescription", description);
                    resultIntent.putExtra("newTaskDay", day);
                    resultIntent.putExtra("newTaskHour", hour);
                    resultIntent.putExtra("newTaskCategory", category);
                    resultIntent.putExtra("newTaskDone", false);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(Item_TaskActivity.this, "שגיאה בשמירה: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
