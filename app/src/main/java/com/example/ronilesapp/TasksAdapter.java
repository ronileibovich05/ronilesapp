package com.example.ronilesapp;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TasksAdapter extends RecyclerView.Adapter<TasksAdapter.TaskViewHolder> {

    private final List<Task> taskList;
    private final OnTaskCheckedListener listener;
    private final OnTaskClickListener clickListener;

    // ממשקים
    public interface OnTaskCheckedListener { void onTaskChecked(Task task, boolean isChecked); }
    public interface OnTaskClickListener { void onTaskClick(Task task); }

    // בנאי
    public TasksAdapter(List<Task> taskList, OnTaskCheckedListener listener, OnTaskClickListener clickListener) {
        this.taskList = taskList;
        this.listener = listener;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // וודאי ששם הקובץ כאן הוא אכן item_task או single_task (לפי מה שקראת לו)
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.single_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = taskList.get(position);
        Context context = holder.itemView.getContext(); // השגת ה-Context לצורך חלוניות

        holder.tvTitle.setText(task.getTitle());
        holder.tvDescription.setText(task.getDescription());
        holder.tvCategory.setText(task.getCategory());

        String dateString = String.format("%02d/%02d/%04d", task.getDay(), task.getMonth(), task.getYear());
        String timeString = String.format("%02d:%02d", task.getHour(), task.getMinute());

        holder.tvDay.setText("Date: " + dateString);
        holder.tvHour.setText("Hour: " + timeString);

        // טיפול ב-CheckBox
        holder.checkBoxDone.setOnCheckedChangeListener(null);
        holder.checkBoxDone.setChecked(task.isDone()); // שים לב: בדקי אם בפרויקט שלך זה isDone() או isCompleted()
        holder.checkBoxDone.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) listener.onTaskChecked(task, isChecked);
        });

        // לחיצה על כפתור עריכה (EDIT)
        holder.tvEditButton.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onTaskClick(task);
            }
        });

        // ==========================================
        // חדש: לחיצה על כפתור השיתוף (SHARE)
        // ==========================================
        if (holder.btnShare != null) {
            holder.btnShare.setOnClickListener(v -> showShareDialog(context, task));
        }
    }

    @Override
    public int getItemCount() { return taskList.size(); }

    // ==========================================
    // פונקציות עזר לשיתוף משימות (הלוגיקה החדשה)
    // ==========================================

    private void showShareDialog(Context context, Task task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Share Task");
        builder.setMessage("Enter user email to share with:");

        final EditText input = new EditText(context);
        input.setHint("email@example.com");
        builder.setView(input);

        builder.setPositiveButton("Share", (dialog, which) -> {
            String email = input.getText().toString().trim();
            if (!email.isEmpty()) {
                shareTaskWithUser(context, task, email);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void shareTaskWithUser(Context context, Task task, String receiverEmail) {
        // בדיקה שמשתמש מחובר
        if (Utils.mAuth.getCurrentUser() == null) return;

        String senderEmail = Utils.mAuth.getCurrentUser().getEmail();

        if (senderEmail != null && senderEmail.equals(receiverEmail)) {
            Toast.makeText(context, "Cannot share with yourself!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. שמירה ב-Firebase (כמו שעשינו קודם)
        String shareId = Utils.FBFS.collection("SharedTasks").document().getId();
        SharedTask sharedTask = new SharedTask(shareId, senderEmail, receiverEmail, task.getId());

        Utils.FBFS.collection("SharedTasks").document(shareId)
                .set(sharedTask)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Task Shared in Database!", Toast.LENGTH_SHORT).show();

                    // 2. שליחת אימייל אמיתי (החלק החדש!)
                    sendRealEmail(context, receiverEmail, task);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Error sharing task", Toast.LENGTH_SHORT).show());
    }

    // פונקציה חדשה שפותחת את ה-Gmail
    private void sendRealEmail(Context context, String receiverEmail, Task task) {
        String subject = "New Task Shared With You: " + task.getTitle();
        String message = "Hi!\n\nI shared a new task with you in RonilesApp.\n\n" +
                "Task: " + task.getTitle() + "\n" +
                "Description: " + task.getDescription() + "\n" +
                "Date: " + task.getDay() + "/" + task.getMonth() + "/" + task.getYear() + "\n\n" +
                "Good luck!";

        // שינוי קטן: במקום ACTION_SENDTO נשתמש ב-ACTION_SEND
        // זה מאפשר לשתף גם בוואטסאפ, הודעות, או להעתיק ללוח
        android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("text/plain"); // טקסט רגיל
        shareIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{receiverEmail});
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, message);

        try {
            // ניסיון לפתוח את תפריט השיתוף
            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Task via..."));
        } catch (Exception e) {
            // אם עדיין יש שגיאה - נציג הודעה במקום שהאפליקציה תיתקע
            Toast.makeText(context, "Cannot open share menu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // ==========================================
    // ViewHolder
    // ==========================================

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDescription, tvDay, tvHour, tvCategory;
        TextView tvEditButton;
        CheckBox checkBoxDone;
        ImageButton btnShare; // הכפתור החדש

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTaskTitle);
            tvDescription = itemView.findViewById(R.id.tvTaskDescription);
            tvDay = itemView.findViewById(R.id.tvTaskDay);
            tvHour = itemView.findViewById(R.id.tvTaskHour);
            tvCategory = itemView.findViewById(R.id.tvTaskCategory);
            tvEditButton = itemView.findViewById(R.id.tvEditButton);
            checkBoxDone = itemView.findViewById(R.id.checkBoxDone);

            // חיבור הכפתור החדש מה-XML
            btnShare = itemView.findViewById(R.id.btnShareTask);
        }
    }
}