package com.example.ronilesapp;

public class SharedTask {
    private String id;            // המזהה של השיתוף עצמו
    private String senderEmail;   // המייל של מי ששולח
    private String receiverEmail; // המייל של מי שמקבל
    private String taskId;        // המזהה של המשימה שמשתפים

    // בנאי ריק (חובה לפיירבייס!)
    public SharedTask() {
    }

    public SharedTask(String id, String senderEmail, String receiverEmail, String taskId) {
        this.id = id;
        this.senderEmail = senderEmail;
        this.receiverEmail = receiverEmail;
        this.taskId = taskId;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSenderEmail() { return senderEmail; }
    public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }

    public String getReceiverEmail() { return receiverEmail; }
    public void setReceiverEmail(String receiverEmail) { this.receiverEmail = receiverEmail; }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
}