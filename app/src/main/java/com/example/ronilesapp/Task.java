package com.example.ronilesapp;

import java.util.Calendar;

public class Task {
    private String id; // 1. הוספתי את שדה ה-ID שחסר
    private String title;
    private String description;
    private int day;
    private int month;
    private int year;
    private int hour;
    private int minute;
    private String category;
    private boolean done;
    private int position;
    private long creationTime;

    // בנאי ריק (חובה ל-Firebase)
    public Task() { }

    // 2. עדכנתי את הבנאי כדי שיקבל גם את ה-id בהתחלה
    public Task(String id, String title, String description, int day, int month, int year,
                int hour, int minute, String category, boolean done) {
        this.id = id; // שמירת ה-ID
        this.title = title;
        this.description = description;
        this.day = day;
        this.month = month;
        this.year = year;
        this.hour = hour;
        this.minute = minute;
        this.category = category;
        this.done = done;
        this.creationTime = System.currentTimeMillis();
    }

    // גטר וסטרים

    // 3. הוספתי גטר וסטר ל-ID
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getDay() { return day; }
    public void setDay(int day) { this.day = day; }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public int getHour() { return hour; }
    public void setHour(int hour) { this.hour = hour; }

    public int getMinute() { return minute; }
    public void setMinute(int minute) { this.minute = minute; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public long getCreationTime() { return creationTime; }
    public void setCreationTime(long creationTime) { this.creationTime = creationTime; }

    // 4. פונקציה חדשה וחשובה - מחזירה את זמן המשימה במילי-שניות עבור ההתראות
    public long getTimeInMillis() {
        Calendar calendar = Calendar.getInstance();
        // שים לב: ב-Calendar החודשים הם 0-11, אבל אנחנו שומרים 1-12. לכן עושים month - 1
        calendar.set(year, month - 1, day, hour, minute, 0);
        return calendar.getTimeInMillis();
    }
}