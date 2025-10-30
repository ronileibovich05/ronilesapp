package com.example.ronilesapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TasksAdapter extends RecyclerView.Adapter<TasksAdapter.TaskViewHolder> {

    private List<Task> taskList;
    private OnTaskCheckedListener listener;

    public interface OnTaskCheckedListener {
        void onTaskChecked(Task task, boolean isChecked);
    }

    public TasksAdapter(List<Task> taskList, OnTaskCheckedListener listener) {
        this.taskList = taskList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        holder.tvTitle.setText(task.getTitle());
        holder.tvDescription.setText(task.getDescription());
        holder.tvDay.setText(task.getDay());
        holder.tvCategory.setText(task.getCategory());
        holder.checkBoxDone.setChecked(task.isDone());

        holder.checkBoxDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) listener.onTaskChecked(task, isChecked);
        });
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription, tvDay, tvCategory;
        CheckBox checkBoxDone;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvDescription = itemView.findViewById(R.id.tvTaskDescription);
            tvDay = itemView.findViewById(R.id.tvTaskDay);
            tvCategory = itemView.findViewById(R.id.tvTaskCategory);
            checkBoxDone = itemView.findViewById(R.id.checkBoxDone);
        }
    }
}
