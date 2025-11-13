package com.example.ronilesapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_category_tasks, container, false);
        recyclerView = view.findViewById(R.id.recyclerViewCategoryTasks);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TasksAdapter(taskList, (task, isChecked) -> {
            if (isChecked) {
                FBRef.getUserTasksRef().document(task.getTitle()).delete()
                        .addOnSuccessListener(aVoid -> {
                            int position = taskList.indexOf(task);
                            if (position != -1) {
                                taskList.remove(position);
                                adapter.notifyItemRemoved(position);
                            }
                            Toast.makeText(getContext(), "✅ משימה הושלמה!", Toast.LENGTH_SHORT).show();
                        });
            }
        });
        recyclerView.setAdapter(adapter);

        loadTasksForCategory();
        return view;
    }

    private void loadTasksForCategory() {
        if ("כל המשימות".equals(category)) {  // אם זה טאב 'כל המשימות'
            FBRef.getUserTasksRef()
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            taskList.clear();
                            for (QueryDocumentSnapshot doc : task.getResult()) {
                                taskList.add(doc.toObject(Task.class));
                            }
                            adapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(getContext(), "שגיאה בטעינת המשימות", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {  // סינון רגיל לפי קטגוריה
            FBRef.getUserTasksRef()
                    .whereEqualTo("category", category)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            taskList.clear();
                            for (QueryDocumentSnapshot doc : task.getResult()) {
                                taskList.add(doc.toObject(Task.class));
                            }
                            adapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(getContext(), "שגיאה בטעינת המשימות", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

}
