package com.example.ronilesapp;

public class Task {
    private String title;
    private String description;
    private int day;
    private int hour;
    private String category;
    private boolean done;
    private int position; // מיקום בסדר עצמי

    public Task() { }

    public Task(String title, String description, int day, int hour, String category, boolean done) {
        this.title = title;
        this.description = description;
        this.day = day;
        this.hour = hour;
        this.category = category;
        this.done = done;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getDay() { return day; }
    public void setDay(int day) { this.day = day; }

    public int getHour() { return hour; }
    public void setHour(int hour) { this.hour = hour; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
}
