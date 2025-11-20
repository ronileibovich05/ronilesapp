package com.example.ronilesapp;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TasksAdapter extends RecyclerView.Adapter<TasksAdapter.TaskViewHolder> {

    private final List<Task> taskList;
    private final OnTaskCheckedListener listener;
    private final OnStartDragListener dragListener;
    private boolean dragEnabled = true; // אפשרות לגרירה

    public interface OnTaskCheckedListener {
        void onTaskChecked(Task task, boolean isChecked);
    }

    public interface OnStartDragListener {
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    public TasksAdapter(List<Task> taskList, OnTaskCheckedListener listener, OnStartDragListener dragListener) {
        this.taskList = taskList;
        this.listener = listener;
        this.dragListener = dragListener;
    }

    public void setDragEnabled(boolean enabled) {
        this.dragEnabled = enabled;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.single_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);

        holder.tvTitle.setText(task.getTitle());
        holder.tvDescription.setText(task.getDescription());
        holder.tvDay.setText(String.valueOf(task.getDay()));
        holder.tvHour.setText(String.valueOf(task.getHour()));
        holder.tvCategory.setText(task.getCategory());

        holder.checkBoxDone.setOnCheckedChangeListener(null);
        holder.checkBoxDone.setChecked(task.isDone());
        holder.checkBoxDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) listener.onTaskChecked(task, isChecked);
        });

        // גרירה
        if (dragEnabled) {
            holder.dragHandle.setVisibility(View.VISIBLE);
            holder.dragHandle.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN && dragListener != null) {
                    dragListener.onStartDrag(holder);
                }
                return false;
            });
        } else {
            holder.dragHandle.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription, tvDay, tvHour, tvCategory;
        CheckBox checkBoxDone;
        ImageView dragHandle;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvDescription = itemView.findViewById(R.id.tvTaskDescription);
            tvDay = itemView.findViewById(R.id.tvTaskDay);
            tvHour = itemView.findViewById(R.id.tvTaskHour);
            tvCategory = itemView.findViewById(R.id.tvTaskCategory);
            checkBoxDone = itemView.findViewById(R.id.checkBoxDone);
            dragHandle = itemView.findViewById(R.id.dragHandle);
        }
    }
}
