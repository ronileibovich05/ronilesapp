package com.example.ronilesapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TasksActivity extends AppCompatActivity {

    private RecyclerView recyclerViewTasks;
    private FloatingActionButton fabAddTask;
    private TasksAdapter adapter;
    private List<Task> taskList;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private ActivityResultLauncher<Intent> addTaskLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tasks);

        recyclerViewTasks = findViewById(R.id.recyclerViewTasks);
        fabAddTask = findViewById(R.id.fabAddTask);

        taskList = new ArrayList<>();
        adapter = new TasksAdapter(taskList, (task, isChecked) -> {
            task.setDone(isChecked);
            db.collection("tasks").document(task.getTitle()).update("done", isChecked);
        });

        recyclerViewTasks.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewTasks.setAdapter(adapter);

        // Launcher לקבלת תוצאה מה-Item_TaskActivity
        addTaskLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        Task newTask = new Task(
                                data.getStringExtra("newTaskTitle"),
                                data.getStringExtra("newTaskDescription"),
                                data.getStringExtra("newTaskDay"),
                                data.getStringExtra("newTaskHour"),
                                data.getBooleanExtra("newTaskDone", false)
                        );
                        taskList.add(newTask);
                        adapter.notifyItemInserted(taskList.size() - 1);
                    }
                }
        );

        fabAddTask.setOnClickListener(v -> {
            Intent intent = new Intent(TasksActivity.this, Item_TaskActivity.class);
            addTaskLauncher.launch(intent);
        });

        loadTasksFromFirestore();
    }

    private void loadTasksFromFirestore() {
        db.collection("tasks").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                taskList.clear();
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    Task t = doc.toObject(Task.class);
                    taskList.add(t);
                }
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(this, "Failed to load tasks", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
