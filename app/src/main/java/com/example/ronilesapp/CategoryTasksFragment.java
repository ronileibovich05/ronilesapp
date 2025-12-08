package com.example.ronilesapp;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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
    private List<Task> displayedTaskList = new ArrayList<>();
    private Button btnDeleteCategory;
    private Spinner spinnerSort;
    private EditText searchEditText;

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

        // 🔹 מיישמים את ה-Theme הנבחר
        android.content.SharedPreferences prefs = getActivity()
                .getSharedPreferences("AppSettingsPrefs", android.content.Context.MODE_PRIVATE);
        String theme = prefs.getString("theme", "pink_brown");
        int themeResId;
        switch (theme) {
            case "blue_white": themeResId = R.style.Theme_BlueWhite; break;
            case "green_white": themeResId = R.style.Theme_GreenWhite; break;
            default: themeResId = R.style.Theme_PinkBrown; break;
        }

        // Wrap Context עם Theme
        LayoutInflater themedInflater = inflater.cloneInContext(
                new android.view.ContextThemeWrapper(getContext(), themeResId)
        );

        // Inflate עם ה-Context החדש
        View view = themedInflater.inflate(R.layout.fragment_category_tasks, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewCategoryTasks);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        searchEditText = view.findViewById(R.id.editTextSearch);
        searchEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterTasks(s.toString()); }
            @Override public void afterTextChanged(android.text.Editable s) { }
        });

        // גרירה
        TasksAdapter.OnStartDragListener dragListener = viewHolder -> {
            ItemTouchHelper itemTouchHelper = new ItemTouchHelper(
                    new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
                        @Override
                        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                              RecyclerView.ViewHolder target) {
                            int from = viewHolder.getAdapterPosition();
                            int to = target.getAdapterPosition();

                            Task moved = displayedTaskList.remove(from);
                            displayedTaskList.add(to, moved);
                            adapter.notifyItemMoved(from, to);

                            for (int i = 0; i < displayedTaskList.size(); i++) {
                                Task t = displayedTaskList.get(i);
                                t.setPosition(i);
                                FBRef.getUserTasksRef().document(t.getTitle()).update("position", i);
                            }
                            return true;
                        }

                        @Override public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) { }
                    });
            itemTouchHelper.attachToRecyclerView(recyclerView);
        };

        adapter = new TasksAdapter(displayedTaskList, (task, isChecked) -> {
            if (isChecked) {
                FBRef.getUserTasksRef().document(task.getTitle()).delete()
                        .addOnSuccessListener(aVoid -> {
                            int pos = displayedTaskList.indexOf(task);
                            if (pos != -1) {
                                displayedTaskList.remove(pos);
                                adapter.notifyItemRemoved(pos);
                            }
                            Toast.makeText(getContext(), "✅ משימה הושלמה!", Toast.LENGTH_SHORT).show();
                        });
            }
        }, dragListener);

        recyclerView.setAdapter(adapter);

        spinnerSort = view.findViewById(R.id.spinnerSort);
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"סדר הוספה", "סדר עצמי", "לפי תאריך", "לפי שעה"});
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSort.setAdapter(sortAdapter);

        spinnerSort.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View v, int position, long id) {
                currentSortOption = position;
                sortTasks(position);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });

        btnDeleteCategory = view.findViewById(R.id.btnDeleteCategory);
        if ("כל המשימות".equals(category)) btnDeleteCategory.setVisibility(View.GONE);
        else btnDeleteCategory.setOnClickListener(v -> deleteCategory());

        loadTasksForCategory();

        return view;
    }


    private void sortTasks(int sortOption) {
        switch (sortOption) {
            case 0: displayedTaskList.sort((t1,t2) -> Long.compare(t1.getCreationTime(), t2.getCreationTime())); break;
            case 1: displayedTaskList.sort((t1,t2) -> Integer.compare(t1.getPosition(), t2.getPosition())); break;
            case 2: displayedTaskList.sort((t1,t2) -> {
                if(t1.getYear() != t2.getYear()) return Integer.compare(t1.getYear(), t2.getYear());
                if(t1.getMonth() != t2.getMonth()) return Integer.compare(t1.getMonth(), t2.getMonth());
                return Integer.compare(t1.getDay(), t2.getDay());
            }); break;
            case 3: displayedTaskList.sort((t1,t2) -> {
                if(t1.getHour() != t2.getHour()) return Integer.compare(t1.getHour(), t2.getHour());
                return Integer.compare(t1.getMinute(), t2.getMinute());
            }); break;
        }
        adapter.notifyDataSetChanged();
    }

    private void filterTasks(String query) {
        displayedTaskList.clear();
        if (query.isEmpty()) displayedTaskList.addAll(taskList);
        else {
            for (Task t : taskList) {
                if (t.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                        t.getDescription().toLowerCase().contains(query.toLowerCase())) {
                    displayedTaskList.add(t);
                }
            }
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
                    taskObj.setDay(dayObj instanceof Number ? ((Number) dayObj).intValue() : 0);

                    Object monthObj = doc.get("month");
                    taskObj.setMonth(monthObj instanceof Number ? ((Number) monthObj).intValue() : 0);

                    Object yearObj = doc.get("year");
                    taskObj.setYear(yearObj instanceof Number ? ((Number) yearObj).intValue() : 2025);

                    Object hourObj = doc.get("hour");
                    taskObj.setHour(hourObj instanceof Number ? ((Number) hourObj).intValue() : 0);

                    Object minuteObj = doc.get("minute");
                    taskObj.setMinute(minuteObj instanceof Number ? ((Number) minuteObj).intValue() : 0);

                    Object posObj = doc.get("position");
                    taskObj.setPosition(posObj instanceof Number ? ((Number) posObj).intValue() : taskList.size());

                    Object creationObj = doc.get("creationTime");
                    taskObj.setCreationTime(creationObj instanceof Number ? ((Number) creationObj).longValue() : System.currentTimeMillis());

                    taskList.add(taskObj);
                }
                displayedTaskList.clear();
                displayedTaskList.addAll(taskList);
                sortTasks(currentSortOption);
            } else {
                Toast.makeText(getContext(), "שגיאה בטעינת המשימות", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteCategory() {
        new AlertDialog.Builder(getContext())
                .setTitle("מחיקת קטגוריה")
                .setMessage("האם למחוק את הקטגוריה \"" + category + "\"? המשימות יועברו ל'כל המשימות'.")
                .setPositiveButton("מחק", (dialog, which) -> {
                    FBRef.getUserTasksRef().whereEqualTo("category", category).get()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    for (QueryDocumentSnapshot doc : task.getResult())
                                        doc.getReference().update("category", "ללא קטגוריה");

                                    FBRef.getUserCategoriesRef().document(category).delete()
                                            .addOnSuccessListener(aVoid ->
                                                    Toast.makeText(getContext(), "קטגוריה נמחקה", Toast.LENGTH_SHORT).show())
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(getContext(), "שגיאה במחיקה", Toast.LENGTH_SHORT).show());
                                }
                            });
                })
                .setNegativeButton("ביטול", null)
                .show();
    }
}
