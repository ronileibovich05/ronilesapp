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
    private final OnTaskClickListener clickListener; // מאזין ללחיצה על עריכה
    private final OnStartDragListener dragListener;
    private boolean dragEnabled = true;

    // ממשקים (Interfaces)
    public interface OnTaskCheckedListener { void onTaskChecked(Task task, boolean isChecked); }
    public interface OnStartDragListener { void onStartDrag(RecyclerView.ViewHolder viewHolder); }
    public interface OnTaskClickListener { void onTaskClick(Task task); }

    public TasksAdapter(List<Task> taskList, OnTaskCheckedListener listener, OnStartDragListener dragListener, OnTaskClickListener clickListener) {
        this.taskList = taskList;
        this.listener = listener;
        this.dragListener = dragListener;
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

        // גרירה
        if (dragEnabled) {
            holder.dragHandle.setVisibility(View.VISIBLE);
            holder.dragHandle.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN && dragListener != null) {
                    dragListener.onStartDrag(holder);
                }
                return false;
            });
        } else holder.dragHandle.setVisibility(View.GONE);

        // --- כאן השינוי החשוב! ---
        // הגדרת הלחיצה על כפתור ה-EDIT (הטקסט הכחול שהוספת)
        holder.tvEditButton.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onTaskClick(task);
            }
        });

        // אופציונלי: אפשר להשאיר גם לחיצה על כל הכרטיס אם רוצים, אבל ביקשת ספציפית על ה-EDIT
        // holder.itemView.setOnClickListener(v -> ... );
    }

    @Override
    public int getItemCount() { return taskList.size(); }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription, tvDay, tvHour, tvCategory;

        // משתנה חדש לכפתור העריכה
        TextView tvEditButton;

        CheckBox checkBoxDone;
        ImageView dragHandle;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvDescription = itemView.findViewById(R.id.tvTaskDescription);
            tvDay = itemView.findViewById(R.id.tvTaskDay);
            tvHour = itemView.findViewById(R.id.tvTaskHour);
            tvCategory = itemView.findViewById(R.id.tvTaskCategory);

            // מציאת כפתור העריכה לפי ה-ID שהגדרנו ב-XML
            tvEditButton = itemView.findViewById(R.id.tvEditButton);

            checkBoxDone = itemView.findViewById(R.id.checkBoxDone);
            dragHandle = itemView.findViewById(R.id.dragHandle);
        }
    }
}