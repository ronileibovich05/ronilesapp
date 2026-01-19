package com.example.ronilesapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class NotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Get data from intent
        String taskTitle = intent.getStringExtra("title");
        int notificationId = intent.getIntExtra("id", 0);

        // Intent to open the app when notification is clicked
        Intent mainIntent = new Intent(context, TasksActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Critical: This ID must be unique and consistent
        String channelId = "task_channel_id";

        // Create Channel (Required for Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Task Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for task deadlines");
            notificationManager.createNotificationChannel(channel);
        }

        // Build the notification (English Text)
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // שימוש באייקון מערכת ברירת מחדל לבדיקה
                .setContentTitle("Task Reminder") // כותרת באנגלית
                .setContentText("It's time for: " + taskTitle) // טקסט באנגלית
                .setContentIntent(contentIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        // Show the notification
        notificationManager.notify(notificationId, builder.build());
    }
}