package com.example.ronilesapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CategoryTasksFragment extends Fragment {

    private static final String ARG_CATEGORY = "category";
    private String category;

    private RecyclerView recyclerView;
    private TasksAdapter adapter;
    private List<Task> taskList = new ArrayList<>();
    private Button btnDeleteCategory;
    private Spinner spinnerSort;
    private int currentSortOption = 0;

    public static CategoryTasksFragment newInstance(String category) {
        CategoryTasksFragment fragment = new CategoryTasksFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CATEGORY, category);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) category = getArguments().getString(ARG_CATEGORY);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_category_tasks, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewCategoryTasks);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // מאזין לגרירה
        TasksAdapter.OnStartDragListener dragListener = viewHolder -> {
            ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                    ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
                @Override
                public boolean onMove(RecyclerView recyclerView,
                                      RecyclerView.ViewHolder viewHolder,
                                      RecyclerView.ViewHolder target) {

                    int fromPosition = viewHolder.getAdapterPosition();
                    int toPosition = target.getAdapterPosition();

                    Task movedTask = taskList.remove(fromPosition);
                    taskList.add(toPosition, movedTask);
                    adapter.notifyItemMoved(fromPosition, toPosition);

                    for (int i = 0; i < taskList.size(); i++) {
                        Task t = taskList.get(i);
                        t.setPosition(i);
                        FBRef.getUserTasksRef().document(t.getTitle()).update("position", i);
                    }
                    return true;
                }

                @Override
                public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) { }
            });
            itemTouchHelper.attachToRecyclerView(recyclerView);
        };

        adapter = new TasksAdapter(taskList, (task, isChecked) -> {
            if (isChecked) {
                FBRef.getUserTasksRef().document(task.getTitle()).delete()
                        .addOnSuccessListener(aVoid -> {
                            int pos = taskList.indexOf(task);
                            if (pos != -1) {
                                taskList.remove(pos);
                                adapter.notifyItemRemoved(pos);
                            }
                            Toast.makeText(getContext(), "✅ משימה הושלמה!", Toast.LENGTH_SHORT).show();
                        });
            }
        }, dragListener);

        recyclerView.setAdapter(adapter);

        // Spinner למיון
        spinnerSort = view.findViewById(R.id.spinnerSort);
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"סדר עצמי", "לפי תאריך", "לפי שעה"});
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSort.setAdapter(sortAdapter);

        spinnerSort.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                currentSortOption = position;
                sortTasks(currentSortOption);
                adapter.setDragEnabled(position == 0); // גרירה רק בסדר עצמי
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });

        btnDeleteCategory = view.findViewById(R.id.btnDeleteCategory);
        if ("כל המשימות".equals(category)) btnDeleteCategory.setVisibility(View.GONE);
        else btnDeleteCategory.setOnClickListener(v -> deleteCategory());

        loadTasksForCategory();
        return view;
    }

    private void sortTasks(int sortOption) {
        switch (sortOption) {
            case 0:
                taskList.sort((t1, t2) -> Integer.compare(t1.getPosition(), t2.getPosition()));
                break;
            case 1:
                taskList.sort((t1, t2) -> Integer.compare(t1.getDay(), t2.getDay()));
                break;
            case 2:
                taskList.sort((t1, t2) -> Integer.compare(t1.getHour(), t2.getHour()));
                break;
        }
        adapter.notifyDataSetChanged();
    }

    private void loadTasksForCategory() {
        FBRef.getUserTasksRef().get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                taskList.clear();
                for (QueryDocumentSnapshot doc : task.getResult()) {
                    String taskCategory = doc.getString("category");
                    if (taskCategory == null) taskCategory = "ללא קטגוריה";

                    if (!category.equals("כל המשימות") && !taskCategory.equals(category)) continue;

                    Task taskObj = new Task();
                    taskObj.setTitle(doc.getString("title"));
                    taskObj.setDescription(doc.getString("description"));
                    taskObj.setCategory(taskCategory);
                    taskObj.setDone(Boolean.TRUE.equals(doc.getBoolean("done")));

                    Object dayObj = doc.get("day");
                    int dayInt = (dayObj instanceof Number) ? ((Number) dayObj).intValue() : 0;
                    taskObj.setDay(dayInt);

                    Object hourObj = doc.get("hour");
                    int hourInt = (hourObj instanceof Number) ? ((Number) hourObj).intValue() : 0;
                    taskObj.setHour(hourInt);

                    Object posObj = doc.get("position");
                    int posInt = (posObj instanceof Number) ? ((Number) posObj).intValue() : taskList.size();
                    taskObj.setPosition(posInt);

                    taskList.add(taskObj);
                }

                if (currentSortOption == 0)
                    taskList.sort((t1, t2) -> Integer.compare(t1.getPosition(), t2.getPosition()));

                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(getContext(), "שגיאה בטעינת המשימות", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteCategory() {
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("מחיקת קטגוריה")
                .setMessage("האם אתה בטוח שברצונך למחוק את הקטגוריה \"" + category + "\"? המשימות שבתוכה יישארו בטאב 'כל המשימות'.")
                .setPositiveButton("מחק", (dialog, which) -> {
                    FBRef.getUserTasksRef().whereEqualTo("category", category)
                            .get().addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    for (QueryDocumentSnapshot doc : task.getResult()) {
                                        doc.getReference().update("category", "ללא קטגוריה");
                                    }
                                    FBRef.getUserCategoriesRef().document(category).delete()
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(getContext(), "קטגוריה נמחקה! המשימות נשמרו ב'כל המשימות'.", Toast.LENGTH_SHORT).show();
                                                if (getActivity() != null)
                                                    ((TasksActivity) getActivity()).loadCategoriesAndTasks();
                                            })
                                            .addOnFailureListener(e -> Toast.makeText(getContext(), "שגיאה במחיקת הקטגוריה", Toast.LENGTH_SHORT).show());
                                }
                            });
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null && resultCode == getActivity().RESULT_OK) {
            String title = data.getStringExtra("newTaskTitle");
            String description = data.getStringExtra("newTaskDescription");
            int day = data.getIntExtra("newTaskDay", 0);
            int hour = data.getIntExtra("newTaskHour", 0);
            String taskCategory = data.getStringExtra("newTaskCategory");
            boolean done = data.getBooleanExtra("newTaskDone", false);

            if (taskCategory == null) taskCategory = "ללא קטגוריה";

            if (category.equals("כל המשימות") || category.equals(taskCategory)) {
                Task newTask = new Task(title, description, day, hour, taskCategory, done);
                newTask.setPosition(taskList.size());
                taskList.add(newTask);

                sortTasks(currentSortOption);
                adapter.notifyDataSetChanged();
            }
        }
    }
}
