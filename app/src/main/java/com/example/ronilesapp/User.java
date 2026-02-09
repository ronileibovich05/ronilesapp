package com.example.ronilesapp;

public class User {
    private String uid;          // הוספנו: מזהה ייחודי
    private String firstName;
    private String lastName;
    private String email;
    private boolean notifications;
    private String profileImageUrl;
    private boolean isAdmin;     // הוספנו: בדיקה האם מנהל

    // קונסטרקטור ריק דרוש ל-Firebase
    public User() { }

    // קונסטרקטור מעודכן עם כל השדות
    public User(String uid, String firstName, String lastName, String email, boolean notifications, String profileImageUrl, boolean isAdmin) {
        this.uid = uid;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.notifications = notifications;
        this.profileImageUrl = profileImageUrl;
        this.isAdmin = isAdmin;
    }

    // --- Getters & Setters ---

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public boolean isAdmin() { return isAdmin; }
    public void setAdmin(boolean admin) { isAdmin = admin; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isNotifications() { return notifications; }
    public void setNotifications(boolean notifications) { this.notifications = notifications; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
}