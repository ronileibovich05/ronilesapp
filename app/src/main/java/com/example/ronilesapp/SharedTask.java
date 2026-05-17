package com.example.ronilesapp;

// אובייקט שמייצג משימה ששותפה בין שני משתמשים דרך Firebase
public class SharedTask {

    private String id;            // המזהה של השיתוף עצמו
    private String senderEmail;   // המייל של מי ששולח
    private String receiverEmail; // המייל של מי שמקבל

    // פרטי המשימה המלאים (מועתקים מ-UserTask של השולח)
    private String title;
    private String description;
    private int day;
    private int month;
    private int year;
    private int hour;
    private int minute;
    private String category;

    // בנאי ריק (חובה לפיירבייס!)
    public SharedTask() {
    }

    public SharedTask(String id, String senderEmail, String receiverEmail,
                      String title, String description,
                      int day, int month, int year, int hour, int minute, String category) {
        this.id = id;
        this.senderEmail = senderEmail;
        this.receiverEmail = receiverEmail;
        this.title = title;
        this.description = description;
        this.day = day;
        this.month = month;
        this.year = year;
        this.hour = hour;
        this.minute = minute;
        this.category = category;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSenderEmail() { return senderEmail; }
    public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }

    public String getReceiverEmail() { return receiverEmail; }
    public void setReceiverEmail(String receiverEmail) { this.receiverEmail = receiverEmail; }

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
}
