package com.example.ronilesapp;

import static com.example.ronilesapp.Utils.mAuth;
import static com.example.ronilesapp.Utils.refSharedTasks;

import android.app.AlertDialog;
import android.content.Context;
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

    // מציג דיאלוג שבו המשתמש מזין את המייל של מי שיקבל את המשימה
    private void showShareDialog(Context context, UserTask userTask) {
        EditText inputEmail = new EditText(context);
        inputEmail.setHint("Enter receiver's email");
        inputEmail.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        new AlertDialog.Builder(context)
                .setTitle("Share Task")
                .setMessage("Enter the email address of the user you want to share this task with:")
                .setView(inputEmail)
                .setPositiveButton("Share", (dialog, which) -> {
                    String receiverEmail = inputEmail.getText().toString().trim();
                    if (!receiverEmail.isEmpty()) {
                        shareTaskWithUser(context, userTask, receiverEmail);
                    } else {
                        Toast.makeText(context, "Please enter an email address", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // כותב SharedTask ל-Firebase — המקבל יקבל את המשימה אוטומטית דרך ה-Listener שלו
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

        // יוצרים מזהה ייחודי למסמך ה-SharedTask
        String shareId = refSharedTasks.document().getId();

        // יוצרים אובייקט SharedTask עם כל פרטי המשימה
        SharedTask sharedTask = new SharedTask(
                shareId,
                senderEmail,
                receiverEmail,
                userTask.getTitle(),
                userTask.getDescription(),
                userTask.getDay(),
                userTask.getMonth(),
                userTask.getYear(),
                userTask.getHour(),
                userTask.getMinute(),
                userTask.getCategory()
        );

        // כותבים את אובייקט SharedTask לבסיס הנתונים — זה כל מה שצריך לעשות
        refSharedTasks.document(shareId)
                .set(sharedTask)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(context, "Task shared! The recipient will see it automatically.", Toast.LENGTH_LONG).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Error sharing task: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
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