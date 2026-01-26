package com.example.ronilesapp;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import java.util.Calendar;

public class NotificationHelper {

    // יצירת הערוץ (חובה באנדרואיד חדש)
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "TaskReminders";
            String description = "Channel for Task Reminders";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("task_channel", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    // הפונקציה שקובעת את ההתראה
    public static void scheduleNotification(Context context, String taskId, String title, int year, int month, int day, int hour, int minute) {

        // 1. בדיקה: האם המשתמש בכלל רוצה התראות? (מההגדרות)
        SharedPreferences prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        boolean isEnabled = prefs.getBoolean("notifications_enabled", true);
        if (!isEnabled) {
            return; // אם כבוי בהגדרות - לא עושים כלום
        }

        // 2. חישוב הזמן המדויק
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1); // חשוב! חודשים הם 0-11
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        long triggerTime = calendar.getTimeInMillis();

        // --- התיקון החשוב: מניעת התראות מידיות ---
        // אם הזמן שנבחר הוא בעבר (קטן מהזמן הנוכחי), אנחנו לא קובעים התראה.
        if (triggerTime < System.currentTimeMillis()) {
            return;
        }
        // ------------------------------------------

        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("title", title);
        intent.putExtra("taskId", taskId);

        // יצירת מזהה ייחודי להתראה לפי ה-ID של המשימה (כדי שנוכל לבטל אותה אח"כ)
        int uniqueId = taskId.hashCode();

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                uniqueId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager != null) {
            // קביעת ההתראה בדיוק בזמן
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    // פונקציה לביטול התראה (כשמוחקים משימה או מסמנים כבוצעה)
    public static void cancelNotification(Context context, String taskId) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        int uniqueId = taskId.hashCode();

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                uniqueId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }
}