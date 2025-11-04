package com.example.ronilesapp;

public class Task {
    private String title;
    private String description;
    private String day;
    private String category;
    private boolean done;

    public Task() { }

    public Task(String title, String description, String day, String category, boolean done) {
        this.title = title;
        this.description = description;
        this.day = day;
        this.category = category;
        this.done = done;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDay() { return day; }
    public void setDay(String day) { this.day = day; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isDone() { return done; }
    public void setDone(boolean done) { this.done = done; }

    public boolean getHour() {
        return false;
    }
}
