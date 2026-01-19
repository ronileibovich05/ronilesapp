package com.example.ronilesapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class NotificationHelper {

    // פונקציה לקביעת התראה
    public static void scheduleNotification(Context context, long timeInMillis, String taskTitle, String taskId) {

        // 1. בדיקה האם המשתמש אישר התראות בהגדרות
        SharedPreferences prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        boolean areNotificationsEnabled = prefs.getBoolean("notifications", true); // ברירת מחדל דלוק

        if (!areNotificationsEnabled) {
            return; // אם ההתראות כבויות - יוצאים מהפונקציה
        }

        // 2. הגדרת ה-Intent
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra("title", taskTitle);
        // המרת ה-ID של המשימה למספר ייחודי (כדי שלא נתבלבל בין התראות)
        int uniqueId = taskId.hashCode();
        intent.putExtra("id", uniqueId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                uniqueId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 3. קביעת הזמן ב-AlarmManager
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            // setExact מבטיח דיוק בזמן
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
        }
    }

    // פונקציה לביטול התראה (אם מוחקים משימה)
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