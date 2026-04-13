package com.example.ronilesapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CategoryTasksFragment extends Fragment {

    private static final String ARG_CATEGORY = "category";
    private String category;

    // רכיבי UI - רשימה ראשית
    private RecyclerView recyclerView;
    private LinearLayout emptyStateLayout;
    private TasksAdapter adapter;
    private List<UserTask> taskList = new ArrayList<>();
    private List<UserTask> displayedTaskList = new ArrayList<>();

    // רכיבי UI - היסטוריה (Bottom Sheet)
    private RecyclerView recyclerViewHistoryTasks;
    private TasksAdapter historyAdapter;
    private List<UserTask> historyTaskList = new ArrayList<>();
    private List<UserTask> displayedHistoryTaskList = new ArrayList<>();
    private BottomSheetBehavior<LinearLayout> bottomSheetBehavior;

    private Button btnDeleteCategory;
    private Spinner spinnerSort;
    private EditText searchEditText;

    private int currentSortOption = 0;
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
        if (getArguments() != null) {
            category = getArguments().getString(ARG_CATEGORY);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String theme = prefs.getString(BaseActivity.KEY_THEME, "pink_brown");

        int themeResId;
        switch (theme) {
            case "blue_white": themeResId = R.style.Theme_BlueWhite; break;
            case "green_white": themeResId = R.style.Theme_GreenWhite; break;
            default: themeResId = R.style.Theme_PinkBrown; break;
        }

        LayoutInflater themedInflater = inflater.cloneInContext(new android.view.ContextThemeWrapper(getContext(), themeResId));
        View view = themedInflater.inflate(R.layout.fragment_category_tasks, container, false);

        int backgroundColor;
        switch (theme) {
            case "blue_white": backgroundColor = getResources().getColor(R.color.blue_background); break;
            case "green_white": backgroundColor = getResources().getColor(R.color.green_background); break;
            default: backgroundColor = getResources().getColor(R.color.pink_background); break;
        }
        view.setBackgroundColor(backgroundColor);

        // --- הגדרות רשימה ראשית ---
        recyclerView = view.findViewById(R.id.recyclerViewCategoryTasks);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);

        // --- הגדרות רשימת היסטוריה (Bottom Sheet) ---
        recyclerViewHistoryTasks = view.findViewById(R.id.recyclerViewHistoryTasks);
        recyclerViewHistoryTasks.setLayoutManager(new LinearLayoutManager(getContext()));

        LinearLayout bottomSheetLayout = view.findViewById(R.id.bottomSheetHistory);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED); // מתחיל סגור

        LinearLayout historyHeaderLayout = view.findViewById(R.id.historyHeaderLayout);
        historyHeaderLayout.setOnClickListener(v -> {
            if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            } else {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
        searchEditText = view.findViewById(R.id.editTextSearch);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterTasks(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        // --- חיבור האדפטרים לפונקציות החדשות שלנו ---
        adapter = new TasksAdapter(displayedTaskList,
                (task, isChecked) -> handleTaskCheckChange(task, isChecked),
                task -> handleTaskClick(task)
        );
        recyclerView.setAdapter(adapter);

        historyAdapter = new TasksAdapter(displayedHistoryTaskList,
                (task, isChecked) -> handleTaskCheckChange(task, isChecked),
                task -> handleTaskClick(task)
        );
        recyclerViewHistoryTasks.setAdapter(historyAdapter);

        // --- הגדרות מיון ---
        spinnerSort = view.findViewById(R.id.spinnerSort);
        String[] sortOptions = {"Sort By...", "Date Added", "By Date"};

        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, sortOptions);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSort.setAdapter(sortAdapter);

        spinnerSort.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View v, int position, long id) {
                currentSortOption = position;
                sortTasks(position);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // --- כפתור מחיקת קטגוריה ---
        btnDeleteCategory = view.findViewById(R.id.btnDeleteCategory);
        if ("All Tasks".equals(category)) {
            btnDeleteCategory.setVisibility(View.GONE);
        } else {
            btnDeleteCategory.setOnClickListener(v -> deleteCategory());
        }

        return view;
    }

    // פונקציה לטיפול בסימון משימה כ"בוצעה" או "לא בוצעה"
    private void handleTaskCheckChange(UserTask task, boolean isChecked) {
        String docId = task.getId() != null ? task.getId() : task.getTitle();
        Utils.getUserTasksRef().document(docId).update("done", isChecked)
                .addOnSuccessListener(aVoid -> {
                    if (isChecked) {
                        NotificationHelper.cancelNotification(requireContext(), docId);
                        Snackbar.make(requireView(), "Task moved to History", 3000)
                                .setAction("UNDO", v -> Utils.getUserTasksRef().document(docId).update("done", false))
                                .setActionTextColor(getResources().getColor(android.R.color.holo_orange_light))
                                .show();
                    } else {
                        Snackbar.make(requireView(), "Task moved back to active", 2000).show();
                    }
                });
    }

    // פונקציה לטיפול בלחיצה על משימה (לעריכה או מחיקה מהיסטוריה)
    private void handleTaskClick(UserTask task) {
        if (task.isDone()) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Delete Task")
                    .setMessage("Do you want to permanently delete this completed task?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        String docId = task.getId() != null ? task.getId() : task.getTitle();
                        Utils.getUserTasksRef().document(docId).delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getContext(), "Task deleted permanently", Toast.LENGTH_SHORT).show();
                                });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            android.content.Intent intent = new android.content.Intent(getContext(), AddTaskActivity.class);
            intent.putExtra("taskId", task.getId());
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
    }

    @Override
    public void onStart() {
        super.onStart();
        startListeningForTasks();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (firestoreListener != null) firestoreListener.remove();
    }

    private void startListeningForTasks() {
        Query query;
        if (category == null || category.equals("All Tasks")) {
            query = Utils.getUserTasksRef();
        } else {
            query = Utils.getUserTasksRef().whereEqualTo("category", category);
        }

        firestoreListener = query.addSnapshotListener((value, error) -> {
            if (error != null) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Error loading tasks", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            if (value != null) {
                taskList.clear();
                historyTaskList.clear();

                for (QueryDocumentSnapshot doc : value) {
                    UserTask taskObj = doc.toObject(UserTask.class);
                    taskObj.setId(doc.getId());
                    if (taskObj.getCategory() == null) taskObj.setCategory("No Category");

                    if (!category.equals("All Tasks") && !taskObj.getCategory().equals(category)) {
                        continue;
                    }

                    if (taskObj.isDone()) {
                        historyTaskList.add(taskObj);
                    } else {
                        taskList.add(taskObj);
                    }
                }

                String currentSearch = searchEditText.getText().toString();
                filterTasks(currentSearch);
            }
        });
    }

    private void updateEmptyState() {
        if (displayedTaskList.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void sortTasks(int sortOption) {
        if (displayedTaskList != null && !displayedTaskList.isEmpty()) {
            displayedTaskList.sort((t1, t2) -> {
                switch (sortOption) {
                    case 1:
                        return Long.compare(t1.getCreationTime(), t2.getCreationTime());
                    case 2:
                        if(t1.getYear() != t2.getYear()) return Integer.compare(t1.getYear(), t2.getYear());
                        if(t1.getMonth() != t2.getMonth()) return Integer.compare(t1.getMonth(), t2.getMonth());
                        if(t1.getDay() != t2.getDay()) return Integer.compare(t1.getDay(), t2.getDay());
                        if(t1.getHour() != t2.getHour()) return Integer.compare(t1.getHour(), t2.getHour());
                        return Integer.compare(t1.getMinute(), t2.getMinute());
                    default:
                        return Long.compare(t1.getCreationTime(), t2.getCreationTime());
                }
            });
        }

        if (displayedHistoryTaskList != null && !displayedHistoryTaskList.isEmpty()) {
            displayedHistoryTaskList.sort((t1, t2) -> Long.compare(t2.getCreationTime(), t1.getCreationTime()));
        }

        if (adapter != null) adapter.notifyDataSetChanged();
        if (historyAdapter != null) historyAdapter.notifyDataSetChanged();

        updateEmptyState();
    }

    private void filterTasks(String query) {
        displayedTaskList.clear();
        displayedHistoryTaskList.clear();

        if (query.isEmpty()) {
            displayedTaskList.addAll(taskList);
            displayedHistoryTaskList.addAll(historyTaskList);
        } else {
            String lowerQuery = query.toLowerCase();

            for (UserTask t : taskList) {
                if ((t.getTitle() != null && t.getTitle().toLowerCase().contains(lowerQuery)) ||
                        (t.getDescription() != null && t.getDescription().toLowerCase().contains(lowerQuery))) {
                    displayedTaskList.add(t);
                }
            }

            for (UserTask t : historyTaskList) {
                if ((t.getTitle() != null && t.getTitle().toLowerCase().contains(lowerQuery)) ||
                        (t.getDescription() != null && t.getDescription().toLowerCase().contains(lowerQuery))) {
                    displayedHistoryTaskList.add(t);
                }
            }
        }
        sortTasks(currentSortOption);
    }

    private void deleteCategory() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Category")
                .setMessage("Are you sure you want to delete '" + category + "'? All tasks in this category will be moved to 'No Category'.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    Utils.getUserTasksRef().whereEqualTo("category", category).get()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    for (QueryDocumentSnapshot doc : task.getResult()) {
                                        doc.getReference().update("category", "No Category");
                                    }

                                    Utils.getUserCategoriesRef().document(category).delete()
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(getContext(), "Category Removed", Toast.LENGTH_SHORT).show();
                                                if (getActivity() instanceof TasksActivity) {
                                                    ((TasksActivity) getActivity()).loadCategoriesAndTasks();
                                                }
                                            });
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}