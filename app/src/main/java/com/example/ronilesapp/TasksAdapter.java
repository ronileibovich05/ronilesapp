package com.example.ronilesapp;

import static com.example.ronilesapp.Utils.mAuth;
import static com.example.ronilesapp.Utils.refSharedTasks;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.List;

public class TasksAdapter extends RecyclerView.Adapter<TasksAdapter.TaskViewHolder> {

    private final List<UserTask> taskList;
    private final OnTaskCheckedListener listener;
    private final OnTaskClickListener clickListener;

    // ממשקים
    public interface OnTaskCheckedListener { void onTaskChecked(UserTask userTask, boolean isChecked); }
    public interface OnTaskClickListener { void onTaskClick(UserTask userTask); }

    // בנאי
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
        Context context = holder.itemView.getContext(); // השגת ה-Context לצורך חלוניות

        holder.tvTitle.setText(userTask.getTitle());
        holder.tvDescription.setText(userTask.getDescription());
        holder.tvCategory.setText(userTask.getCategory());

        String dateString = String.format("%02d/%02d/%04d", userTask.getDay(), userTask.getMonth(), userTask.getYear());
        String timeString = String.format("%02d:%02d", userTask.getHour(), userTask.getMinute());

        holder.tvDay.setText("Date: " + dateString);
        holder.tvHour.setText("Hour: " + timeString);

        // טיפול ב-CheckBox
        holder.checkBoxDone.setOnCheckedChangeListener(null);
        holder.checkBoxDone.setChecked(userTask.isDone());
        holder.checkBoxDone.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (listener != null)
                    listener.onTaskChecked(userTask, isChecked);
            }
        });

        // לחיצה על כפתור עריכה (EDIT)
        holder.tvEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clickListener != null) {
                    clickListener.onTaskClick(userTask);
                }
            }
        });

        // לחיצה על כפתור השיתוף (SHARE)
        if (holder.btnShare != null) {
            holder.btnShare.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TasksAdapter.this.showShareDialog(context, userTask);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    // פונקציות עזר לשיתוף משימות
    private void showShareDialog(Context context, UserTask userTask) {

        if (!Utils.isConnected(context)) {
            Toast.makeText(context, "No Internet Connection", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Share Task");
        builder.setMessage("Enter user email to share with:");

        final EditText input = new EditText(context);
        input.setHint("email@example.com");
        builder.setView(input);

        builder.setPositiveButton("Share", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String email = input.getText().toString().trim();
                if (!email.isEmpty()) {
                    TasksAdapter.this.shareTaskWithUser(context, userTask, email);
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    private void shareTaskWithUser(Context context, UserTask userTask, String receiverEmail) {

        if (!Utils.isConnected(context)) {
            Toast.makeText(context, "No Internet Connection", Toast.LENGTH_SHORT).show();
            return;
        }

        // בדיקה שמשתמש מחובר
        if (mAuth.getCurrentUser() == null)
            return;

        String senderEmail = mAuth.getCurrentUser().getEmail();

        if (senderEmail != null && senderEmail.equals(receiverEmail)) {
            Toast.makeText(context, "Cannot share with yourself!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. שמירה ב-Firebase (כמו שעשינו קודם)
        String shareId = refSharedTasks.document().getId();
        SharedTask sharedTask = new SharedTask(shareId, senderEmail, receiverEmail, userTask.getId());

        refSharedTasks.document(shareId)
                .set(sharedTask)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(context, "Task Shared in Database!", Toast.LENGTH_SHORT).show();

                        // 2. שליחת אימייל אמיתי
                        TasksAdapter.this.sendRealEmail(context, receiverEmail, userTask);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(context, "Error sharing task", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // פונקציה חדשה שפותחת את ה-Gmail
    private void sendRealEmail(Context context, String receiverEmail, UserTask userTask) {
        String subject = "New Task Shared With You: " + userTask.getTitle();
        String message = "Hi!\n\nI shared a new task with you in RonilesApp.\n\n" +
                "Task: " + userTask.getTitle() + "\n" +
                "Description: " + userTask.getDescription() + "\n" +
                "Date: " + userTask.getDay() + "/" + userTask.getMonth() + "/" + userTask.getYear() + "\n\n" +
                "Good luck!";

        // שינוי קטן: במקום ACTION_SENDTO נשתמש ב-ACTION_SEND
        // זה מאפשר לשתף גם בוואטסאפ, הודעות, או להעתיק ללוח
        android.content.Intent shareIntent = new android.content.Intent(Intent.ACTION_SEND);
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

    // ViewHolder
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