package com.example.ronilesapp;

import static com.example.ronilesapp.Utils.mAuth;
import static com.example.ronilesapp.Utils.refSharedTasks;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TasksAdapter extends RecyclerView.Adapter<TasksAdapter.TaskViewHolder> {

    private final List<UserTask> taskList;
    private final OnTaskCheckedListener listener;
    private final OnTaskClickListener clickListener;

    public interface OnTaskCheckedListener {
        void onTaskChecked(UserTask userTask, boolean isChecked);
    }
    public interface OnTaskClickListener {
        void onTaskClick(UserTask userTask);
    }

    public TasksAdapter(List<UserTask> taskList, OnTaskCheckedListener listener, OnTaskClickListener clickListener) {
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
        UserTask userTask = taskList.get(position);
        Context context = holder.itemView.getContext();

        holder.tvTitle.setText(userTask.getTitle());
        holder.tvDescription.setText(userTask.getDescription());
        holder.tvCategory.setText(userTask.getCategory());

        String dateString = String.format("%02d/%02d/%04d", userTask.getDay(), userTask.getMonth(), userTask.getYear());
        String timeString = String.format("%02d:%02d", userTask.getHour(), userTask.getMinute());

        holder.tvDay.setText("Date: " + dateString);
        holder.tvHour.setText("Time: " + timeString);

        // טיפול בעיצוב של משימה שהושלמה (Strikethrough)
        if (userTask.isDone()) {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTitle.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));
        } else {
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvTitle.setTextColor(ContextCompat.getColor(context, android.R.color.black));
        }

        holder.checkBoxDone.setOnCheckedChangeListener(null);
        holder.checkBoxDone.setChecked(userTask.isDone());

        holder.checkBoxDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onTaskChecked(userTask, isChecked);
            }
        });

        holder.tvEditButton.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onTaskClick(userTask);
            }
        });

        if (holder.btnShare != null) {
            holder.btnShare.setOnClickListener(v -> showShareDialog(context, userTask));
        }
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    private void showShareDialog(Context context, UserTask userTask) {
        try {
            // קידוד הנתונים לקישור (Deep Link)
            String encodedTitle = java.net.URLEncoder.encode(userTask.getTitle(), "UTF-8");
            String encodedDesc = java.net.URLEncoder.encode(userTask.getDescription() != null ? userTask.getDescription() : "", "UTF-8");

            String deepLink = "ronilesapp://task?title=" + encodedTitle
                    + "&desc=" + encodedDesc
                    + "&day=" + userTask.getDay()
                    + "&month=" + userTask.getMonth()
                    + "&year=" + userTask.getYear();

            String subject = "משימה חדשה שותפה איתך: " + userTask.getTitle();
            String message = "היי! צירפתי לך משימה ב-RonilesApp.\n\n" +
                    "לחץ על הקישור כדי להוסיף אותה:\n" + deepLink;

            // יצירת Intent שפותח ישירות את בחירת אפליקציית השיתוף
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            shareIntent.putExtra(Intent.EXTRA_TEXT, message);

            // זה יפתח למשתמש ישר את ה-Share Sheet של אנדרואיד (וואטסאפ, מייל, וכו')
            context.startActivity(Intent.createChooser(shareIntent, "שתף משימה באמצעות:"));

        } catch (Exception e) {
            Toast.makeText(context, "שגיאה ביצירת הקישור", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareTaskWithUser(Context context, UserTask userTask, String receiverEmail) {
        if (!Utils.isConnected(context)) {
            Toast.makeText(context, "No Internet Connection", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mAuth.getCurrentUser() == null) return;

        String senderEmail = mAuth.getCurrentUser().getEmail();

        if (senderEmail != null && senderEmail.equalsIgnoreCase(receiverEmail)) {
            Toast.makeText(context, "Cannot share a task with yourself!", Toast.LENGTH_SHORT).show();
            return;
        }

        String shareId = refSharedTasks.document().getId();
        SharedTask sharedTask = new SharedTask(shareId, senderEmail, receiverEmail, userTask.getId());

        refSharedTasks.document(shareId)
                .set(sharedTask)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Task shared successfully!", Toast.LENGTH_SHORT).show();
                    openShareMenu(context, userTask);
                })
                .addOnFailureListener(e -> Toast.makeText(context, "Error sharing task: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void openShareMenu(Context context, UserTask userTask) {
        String subject = "Task Shared: " + userTask.getTitle();
        String message = "Hi!\n\nI shared a task with you via RonilesApp.\n\n" +
                "Task: " + userTask.getTitle() + "\n" +
                "Details: " + userTask.getDescription() + "\n" +
                "Due Date: " + userTask.getDay() + "/" + userTask.getMonth() + "/" + userTask.getYear() + "\n\n" +
                "Check it out in the app!";

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        shareIntent.putExtra(Intent.EXTRA_TEXT, message);

        try {
            context.startActivity(Intent.createChooser(shareIntent, "Share Task via:"));
        } catch (Exception e) {
            Toast.makeText(context, "Cannot open share menu", Toast.LENGTH_SHORT).show();
        }
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription, tvDay, tvHour, tvCategory;
        TextView tvEditButton;
        CheckBox checkBoxDone;
        ImageButton btnShare;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvDescription = itemView.findViewById(R.id.tvTaskDescription);
            tvDay = itemView.findViewById(R.id.tvTaskDay);
            tvHour = itemView.findViewById(R.id.tvTaskHour);
            tvCategory = itemView.findViewById(R.id.tvTaskCategory);
            tvEditButton = itemView.findViewById(R.id.tvEditButton);
            checkBoxDone = itemView.findViewById(R.id.checkBoxDone);
            btnShare = itemView.findViewById(R.id.btnShareTask);
        }
    }
}