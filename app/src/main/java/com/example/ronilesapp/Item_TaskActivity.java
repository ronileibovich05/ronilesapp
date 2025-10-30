package com.example.ronilesapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.DatePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class Item_TaskActivity extends AppCompatActivity {

    private EditText editTaskTitle, editTaskDescription;
    private DatePicker datePicker;
    private TimePicker timePicker;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_task);

        editTaskTitle = findViewById(R.id.editTextTaskTitle);
        editTaskDescription = findViewById(R.id.editTextTaskDescription);
        datePicker = findViewById(R.id.datePickerTask);
        timePicker = findViewById(R.id.timePickerTask);
    }

    public void saveTask(View view) {
        String title = editTaskTitle.getText().toString().trim();
        String description = editTaskDescription.getText().toString().trim();
        String day = datePicker.getDayOfMonth() + "/" + (datePicker.getMonth()+1) + "/" + datePicker.getYear();
        String hour = timePicker.getHour() + ":" + timePicker.getMinute();

        if(title.isEmpty()) {
            Toast.makeText(this, "נא למלא כותרת למשימה", Toast.LENGTH_SHORT).show();
            return;
        }

        Task newTask = new Task(title, description, day, hour, false);

        // שומרים ל-Firestore
        FBRef.refTasks.document(title).set(newTask)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(Item_TaskActivity.this, "משימה נוספה!", Toast.LENGTH_SHORT).show();
                    // מחזירים את המשימה ל-TasksActivity
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("newTask", (CharSequence) newTask);
                    setResult(RESULT_OK, resultIntent);
                    finish(); // סוגרים את המסך
                })
                .addOnFailureListener(e -> Toast.makeText(Item_TaskActivity.this, "שגיאה בשמירה: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
