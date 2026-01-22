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

    private final List<Task> taskList;
    private final OnTaskCheckedListener listener;
    private final OnTaskClickListener clickListener; // מאזין לעריכה

    // ממשקים (הורדנו את ממשק הגרירה)
    public interface OnTaskCheckedListener { void onTaskChecked(Task task, boolean isChecked); }
    public interface OnTaskClickListener { void onTaskClick(Task task); }

    // בנאי (Constructor) - הורדנו את ה-dragListener מהפרמטרים
    public TasksAdapter(List<Task> taskList, OnTaskCheckedListener listener, OnTaskClickListener clickListener) {
        this.taskList = taskList;
        this.listener = listener;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.single_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);

        holder.tvTitle.setText(task.getTitle());
        holder.tvDescription.setText(task.getDescription());
        holder.tvCategory.setText(task.getCategory());

        String dateString = String.format("%02d/%02d/%04d", task.getDay(), task.getMonth(), task.getYear());
        String timeString = String.format("%02d:%02d", task.getHour(), task.getMinute());

        holder.tvDay.setText("Date: " + dateString);
        holder.tvHour.setText("Hour: " + timeString);

        holder.checkBoxDone.setOnCheckedChangeListener(null);
        holder.checkBoxDone.setChecked(task.isDone());
        holder.checkBoxDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) listener.onTaskChecked(task, isChecked);
        });

        // לחיצה על כפתור ה-EDIT
        holder.tvEditButton.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onTaskClick(task);
            }
        });
    }

    @Override
    public int getItemCount() { return taskList.size(); }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription, tvDay, tvHour, tvCategory;
        TextView tvEditButton;
        CheckBox checkBoxDone;
        // מחקנו את ה-ImageView של ה-DragHandle מכאן

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvDescription = itemView.findViewById(R.id.tvTaskDescription);
            tvDay = itemView.findViewById(R.id.tvTaskDay);
            tvHour = itemView.findViewById(R.id.tvTaskHour);
            tvCategory = itemView.findViewById(R.id.tvTaskCategory);
            tvEditButton = itemView.findViewById(R.id.tvEditButton);
            checkBoxDone = itemView.findViewById(R.id.checkBoxDone);
        }
    }
}