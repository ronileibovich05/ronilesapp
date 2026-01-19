package com.example.ronilesapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
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

    // משתנה לשמירת המאזין כדי שנוכל לבטל אותו בסגירה
    private ListenerRegistration firestoreListener;

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

        // Theme Handling
        SharedPreferences prefs = getActivity()
                .getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String theme = prefs.getString("theme", "pink_brown");
        int themeResId;
        switch (theme) {
            case "blue_white": themeResId = R.style.Theme_BlueWhite; break;
            case "green_white": themeResId = R.style.Theme_GreenWhite; break;
            default: themeResId = R.style.Theme_PinkBrown; break;
        }

        // Apply Theme Context
        LayoutInflater themedInflater = inflater.cloneInContext(
                new android.view.ContextThemeWrapper(getContext(), themeResId)
        );

        View view = themedInflater.inflate(R.layout.fragment_category_tasks, container, false);

        // RecyclerView Setup
        recyclerView = view.findViewById(R.id.recyclerViewCategoryTasks);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Search Setup
        searchEditText = view.findViewById(R.id.editTextSearch);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterTasks(s.toString()); }
            @Override public void afterTextChanged(Editable s) { }
        });

        // Drag & Drop
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

                            // Update positions in Firebase
                            for (int i = 0; i < displayedTaskList.size(); i++) {
                                Task t = displayedTaskList.get(i);
                                t.setPosition(i);
                                if (t.getId() != null) {
                                    FBRef.getUserTasksRef().document(t.getId()).update("position", i);
                                }
                            }
                            return true;
                        }

                        @Override public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) { }
                    });
            itemTouchHelper.attachToRecyclerView(recyclerView);
        };

        adapter = new TasksAdapter(displayedTaskList,
                // 1. Checkbox Listener
                (task, isChecked) -> {
                    // ... (הקוד הקיים שלך למחיקה/סיום משימה)
                    if (isChecked) {
                        String docId = task.getId() != null ? task.getId() : task.getTitle();
                        FBRef.getUserTasksRef().document(docId).delete();
                        // ... וכו'
                    }
                },
                // 2. Drag Listener
                dragListener,

                // 3. **NEW** Edit Listener (Click on task)
                task -> {
                    android.content.Intent intent = new android.content.Intent(getContext(), Item_TaskActivity.class);

                    // Pass all task data to the activity
                    intent.putExtra("taskId", task.getId()); // MUST HAVE ID
                    intent.putExtra("title", task.getTitle());
                    intent.putExtra("desc", task.getDescription());
                    intent.putExtra("category", task.getCategory());
                    intent.putExtra("day", task.getDay());
                    intent.putExtra("month", task.getMonth());
                    intent.putExtra("year", task.getYear());
                    intent.putExtra("hour", task.getHour());
                    intent.putExtra("minute", task.getMinute());

                    startActivity(intent);
                }
        );

        recyclerView.setAdapter(adapter);

        spinnerSort = view.findViewById(R.id.spinnerSort);

// 1. הוספנו את "Sort By..." כפריט הראשון
        String[] sortOptions = {"Sort By...", "Date Added", "Custom Order", "By Date", "By Time"};

        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item,
                sortOptions);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSort.setAdapter(sortAdapter);

// 2. הגדרת המאזין ללחיצה
        spinnerSort.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View v, int position, long id) {
                // שומרים את הבחירה
                currentSortOption = position;

                // קוראים לפונקציית המיון
                sortTasks(position);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });

        // Delete Category Button
        btnDeleteCategory = view.findViewById(R.id.btnDeleteCategory);
        if ("All Tasks".equals(category)) btnDeleteCategory.setVisibility(View.GONE);
        else btnDeleteCategory.setOnClickListener(v -> deleteCategory());

        return view;
    }

    // --- חשוב מאוד: מתחילים להאזין כשהמסך מופיע ---
    @Override
    public void onStart() {
        super.onStart();
        startListeningForTasks();
    }

    // --- ומפסיקים להאזין כשהמסך נעלם (חוסך סוללה ומונע קריסות) ---
    @Override
    public void onStop() {
        super.onStop();
        if (firestoreListener != null) {
            firestoreListener.remove();
        }
    }

    private void startListeningForTasks() {
        Query query;
        if (category == null || category.equals("All Tasks")) {
            query = FBRef.getUserTasksRef();
        } else {
            query = FBRef.getUserTasksRef().whereEqualTo("category", category);
        }

        // שימוש ב-addSnapshotListener לעדכון בזמן אמת!
        firestoreListener = query.addSnapshotListener((value, error) -> {
            if (error != null) {
                Toast.makeText(getContext(), "Error loading tasks", Toast.LENGTH_SHORT).show();
                return;
            }

            if (value != null) {
                taskList.clear();
                for (QueryDocumentSnapshot doc : value) {
                    // המרה חכמה ובטוחה של הנתונים
                    Task taskObj = doc.toObject(Task.class);

                    // ודא שה-ID נשמר (למקרה שהוא לא חלק מהאובייקט ב-Firebase)
                    taskObj.setId(doc.getId());

                    // תיקונים ידניים אם ה-toObject לא עבד מושלם
                    if (taskObj.getCategory() == null) taskObj.setCategory("No Category");

                    // פילטור נוסף בצד הלקוח ליתר ביטחון
                    if (!category.equals("All Tasks") && !taskObj.getCategory().equals(category)) {
                        continue;
                    }

                    taskList.add(taskObj);
                }

                // רענון התצוגה
                // שומרים את החיפוש הנוכחי אם יש
                String currentSearch = searchEditText.getText().toString();
                if (currentSearch.isEmpty()) {
                    displayedTaskList.clear();
                    displayedTaskList.addAll(taskList);
                } else {
                    filterTasks(currentSearch);
                }

                sortTasks(currentSortOption);
                // לא צריך notifyDataSetChanged כאן כי ה-sort עושה את זה
            }
        });
    }

    private void sortTasks(int sortOption) {
        if (displayedTaskList == null || displayedTaskList.isEmpty()) return;

        switch (sortOption) {
            case 0:
                // המצב של "Sort By..." - ברירת מחדל (למשל לפי זמן יצירה)
                // זה חשוב כדי שהרשימה תהיה מסודרת גם כשנכנסים לאפליקציה
                displayedTaskList.sort((t1, t2) -> Long.compare(t1.getCreationTime(), t2.getCreationTime()));
                break;

            case 1: // Date Added (היה קודם 0)
                displayedTaskList.sort((t1, t2) -> Long.compare(t1.getCreationTime(), t2.getCreationTime()));
                break;

            case 2: // Custom Order (היה קודם 1)
                displayedTaskList.sort((t1, t2) -> Integer.compare(t1.getPosition(), t2.getPosition()));
                break;

            case 3: // By Date (היה קודם 2)
                displayedTaskList.sort((t1, t2) -> {
                    if (t1.getYear() != t2.getYear()) return Integer.compare(t1.getYear(), t2.getYear());
                    if (t1.getMonth() != t2.getMonth()) return Integer.compare(t1.getMonth(), t2.getMonth());
                    return Integer.compare(t1.getDay(), t2.getDay());
                });
                break;

            case 4: // By Time (היה קודם 3)
                displayedTaskList.sort((t1, t2) -> {
                    if (t1.getHour() != t2.getHour()) return Integer.compare(t1.getHour(), t2.getHour());
                    return Integer.compare(t1.getMinute(), t2.getMinute());
                });
                break;
        }

        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void filterTasks(String query) {
        displayedTaskList.clear();
        if (query.isEmpty()) displayedTaskList.addAll(taskList);
        else {
            for (Task t : taskList) {
                if ((t.getTitle() != null && t.getTitle().toLowerCase().contains(query.toLowerCase())) ||
                        (t.getDescription() != null && t.getDescription().toLowerCase().contains(query.toLowerCase()))) {
                    displayedTaskList.add(t);
                }
            }
        }
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void deleteCategory() {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Category")
                .setMessage("Are you sure you want to delete '" + category + "'? All tasks will move to 'All Tasks'.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    FBRef.getUserTasksRef().whereEqualTo("category", category).get()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    for (QueryDocumentSnapshot doc : task.getResult())
                                        doc.getReference().update("category", "No Category");

                                    FBRef.getUserCategoriesRef().document(category).delete()
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(getContext(), "Category Removed", Toast.LENGTH_SHORT).show();
                                                // כאן צריך לרענן את הטאבים ב-Activity הראשי
                                                if (getActivity() instanceof TasksActivity) {
                                                    ((TasksActivity) getActivity()).loadCategoriesAndTasks();
                                                }
                                            })
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(getContext(), "Failed Deleting Category", Toast.LENGTH_SHORT).show());
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}