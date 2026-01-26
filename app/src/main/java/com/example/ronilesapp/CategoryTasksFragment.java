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
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;

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
    private List<Task> taskList = new ArrayList<>();
    private List<Task> displayedTaskList = new ArrayList<>();
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
        if (getArguments() != null) category = getArguments().getString(ARG_CATEGORY);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        SharedPreferences prefs = getActivity()
                .getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String theme = prefs.getString("theme", "pink_brown");

        int themeResId;
        switch (theme) {
            case "blue_white": themeResId = R.style.Theme_BlueWhite; break;
            case "green_white": themeResId = R.style.Theme_GreenWhite; break;
            default: themeResId = R.style.Theme_PinkBrown; break;
        }

        LayoutInflater themedInflater = inflater.cloneInContext(
                new android.view.ContextThemeWrapper(getContext(), themeResId)
        );

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
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterTasks(s.toString()); }
            @Override public void afterTextChanged(Editable s) { }
        });

        // --- תיקון הלוגיקה: עדכון במקום מחיקה ---
        adapter = new TasksAdapter(displayedTaskList,
                (task, isChecked) -> {
                    if (isChecked) {
                        String docId = task.getId() != null ? task.getId() : task.getTitle();

                        // 1. במקום למחוק, אנחנו מעדכנים ל-Done = true
                        FBRef.getUserTasksRef().document(docId).update("done", true)
                                .addOnSuccessListener(aVoid -> {
                                    NotificationHelper.cancelNotification(getContext(), docId);

                                    // 2. Snackbar עם Undo
                                    Snackbar.make(recyclerView, "Task Completed", 3500)
                                            .setAction("UNDO", v -> {
                                                // 3. אם התחרטנו - מחזירים ל-Done = false
                                                FBRef.getUserTasksRef().document(docId).update("done", false);
                                            })
                                            .setActionTextColor(getResources().getColor(android.R.color.holo_orange_light))
                                            .show();
                                });
                    }
                },
                task -> {
                    android.content.Intent intent = new android.content.Intent(getContext(), Item_TaskActivity.class);
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

        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item,
                sortOptions);
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
        if ("All Tasks".equals(category)) btnDeleteCategory.setVisibility(View.GONE);
        else btnDeleteCategory.setOnClickListener(v -> deleteCategory());

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
            query = FBRef.getUserTasksRef();
        } else {
            query = FBRef.getUserTasksRef().whereEqualTo("category", category);
        }

        firestoreListener = query.addSnapshotListener((value, error) -> {
            if (error != null) {
                Toast.makeText(getContext(), "Error loading tasks", Toast.LENGTH_SHORT).show();
                return;
            }

            if (value != null) {
                taskList.clear();
                for (QueryDocumentSnapshot doc : value) {
                    Task taskObj = doc.toObject(Task.class);
                    taskObj.setId(doc.getId());
                    if (taskObj.getCategory() == null) taskObj.setCategory("No Category");

                    if (!category.equals("All Tasks") && !taskObj.getCategory().equals(category)) continue;

                    // --- השינוי החשוב: לא להציג משימות שכבר בוצעו ---
                    if (taskObj.isDone()) {
                        continue; // מדלגים על המשימה הזו, היא לא תיכנס לרשימה
                    }
                    // --------------------------------------------------

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
                updateEmptyState();
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

        switch (sortOption) {
            case 0: displayedTaskList.sort((t1, t2) -> Long.compare(t1.getCreationTime(), t2.getCreationTime())); break;
            case 1: displayedTaskList.sort((t1, t2) -> Long.compare(t1.getCreationTime(), t2.getCreationTime())); break;
            case 2: displayedTaskList.sort((t1, t2) -> {
                if(t1.getYear() != t2.getYear()) return Integer.compare(t1.getYear(), t2.getYear());
                if(t1.getMonth() != t2.getMonth()) return Integer.compare(t1.getMonth(), t2.getMonth());
                return Integer.compare(t1.getDay(), t2.getDay());
            }); break;
            case 3: displayedTaskList.sort((t1, t2) -> {
                if(t1.getHour() != t2.getHour()) return Integer.compare(t1.getHour(), t2.getHour());
                return Integer.compare(t1.getMinute(), t2.getMinute());
            }); break;
        }

        if (adapter != null) adapter.notifyDataSetChanged();
        updateEmptyState();
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
        updateEmptyState();
    }

    private void deleteCategory() {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Category")
                .setMessage("Are you sure you want to delete '" + category + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    FBRef.getUserTasksRef().whereEqualTo("category", category).get()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    for (QueryDocumentSnapshot doc : task.getResult())
                                        doc.getReference().update("category", "No Category");

                                    FBRef.getUserCategoriesRef().document(category).delete()
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