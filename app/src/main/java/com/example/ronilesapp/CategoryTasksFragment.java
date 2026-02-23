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

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class CategoryTasksFragment extends Fragment {

    private static final String ARG_CATEGORY = "category";
    private String category;

    private RecyclerView recyclerView;
    private LinearLayout emptyStateLayout;

    private TasksAdapter adapter;
    private List<UserTask> taskList = new ArrayList<>();
    private List<UserTask> displayedTaskList = new ArrayList<>();
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

        recyclerView = view.findViewById(R.id.recyclerViewCategoryTasks);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        searchEditText = view.findViewById(R.id.editTextSearch);

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterTasks(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        adapter = new TasksAdapter(displayedTaskList,
                (task, isChecked) -> {
                    String docId = task.getId() != null ? task.getId() : task.getTitle();

                    // Update task status
                    Utils.getUserTasksRef().document(docId).update("done", isChecked)
                            .addOnSuccessListener(aVoid -> {
                                if (isChecked) {
                                    NotificationHelper.cancelNotification(requireContext(), docId);
                                    Snackbar.make(recyclerView, "Task Completed", 3500)
                                            .setAction("UNDO", v -> Utils.getUserTasksRef().document(docId).update("done", false))
                                            .setActionTextColor(getResources().getColor(android.R.color.holo_orange_light))
                                            .show();
                                }
                            });
                },
                task -> {
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
        );

        recyclerView.setAdapter(adapter);

        spinnerSort = view.findViewById(R.id.spinnerSort);
        String[] sortOptions = {"Sort By...", "Date Added", "By Date", "By Time"};

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

        btnDeleteCategory = view.findViewById(R.id.btnDeleteCategory);
        if ("All Tasks".equals(category)) {
            btnDeleteCategory.setVisibility(View.GONE);
        } else {
            btnDeleteCategory.setOnClickListener(v -> deleteCategory());
        }

        return view;
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
                for (QueryDocumentSnapshot doc : value) {
                    UserTask taskObj = doc.toObject(UserTask.class);
                    taskObj.setId(doc.getId());
                    if (taskObj.getCategory() == null) taskObj.setCategory("No Category");

                    if (!category.equals("All Tasks") && !taskObj.getCategory().equals(category)) {
                        continue;
                    }

                    // התיקון לבקשת המורה: כבר לא מדלגים על משימות שהושלמו!
                    // הן נכנסות לרשימה ויוצגו, אבל אנחנו נסדר אותן בסוף הרשימה ב-sortTasks.
                    taskList.add(taskObj);
                }

                String currentSearch = searchEditText.getText().toString();
                if (currentSearch.isEmpty()) {
                    displayedTaskList.clear();
                    displayedTaskList.addAll(taskList);
                } else {
                    filterTasks(currentSearch);
                }

                sortTasks(currentSortOption);
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
        if (displayedTaskList == null || displayedTaskList.isEmpty()) {
            updateEmptyState();
            return;
        }

        displayedTaskList.sort((t1, t2) -> {
            // קודם כל, משימות שלא הושלמו מופיעות למעלה, משימות שהושלמו למטה (עונה להערת המורה)
            if (t1.isDone() && !t2.isDone()) return 1;
            if (!t1.isDone() && t2.isDone()) return -1;

            // אחר כך, מיון רגיל לפי בחירת המשתמש
            switch (sortOption) {
                case 1: // Date Added
                    return Long.compare(t1.getCreationTime(), t2.getCreationTime());
                case 2: // By Date (Year, Month, Day)
                    if(t1.getYear() != t2.getYear()) return Integer.compare(t1.getYear(), t2.getYear());
                    if(t1.getMonth() != t2.getMonth()) return Integer.compare(t1.getMonth(), t2.getMonth());
                    return Integer.compare(t1.getDay(), t2.getDay());
                case 3: // By Time (Hour, Minute)
                    if(t1.getHour() != t2.getHour()) return Integer.compare(t1.getHour(), t2.getHour());
                    return Integer.compare(t1.getMinute(), t2.getMinute());
                default: // Default sort (Date Added)
                    return Long.compare(t1.getCreationTime(), t2.getCreationTime());
            }
        });

        if (adapter != null) adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void filterTasks(String query) {
        displayedTaskList.clear();
        if (query.isEmpty()) {
            displayedTaskList.addAll(taskList);
        } else {
            String lowerQuery = query.toLowerCase();
            for (UserTask t : taskList) {
                if ((t.getTitle() != null && t.getTitle().toLowerCase().contains(lowerQuery)) ||
                        (t.getDescription() != null && t.getDescription().toLowerCase().contains(lowerQuery))) {
                    displayedTaskList.add(t);
                }
            }
        }
        sortTasks(currentSortOption); // שומר על מיון נכון גם בזמן חיפוש
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