package com.example.ronilesapp;

import android.content.ContentValues;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.MediaStore;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class Utils {

    // ==========================================
    // חלק 1: Firebase References (לשעבר FBRef)
    // ==========================================

    // Firebase Objects
    public static FirebaseAuth mAuth = FirebaseAuth.getInstance();
    public static FirebaseFirestore FBFS = FirebaseFirestore.getInstance();

    // קולקציות ראשיות
    public static CollectionReference refUsers = FBFS.collection("Users");
    public static CollectionReference refImages = FBFS.collection("Images");
    public static CollectionReference refTasks = FBFS.collection("tasks");
    public static CollectionReference refSharedTasks = FBFS.collection("SharedTasks");

    // קולקציות פרטיות של המשתמש (משימות)
    public static CollectionReference getUserTasksRef() {
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            return FBFS.collection("Users").document(uid).collection("Tasks");
        }
        else
            throw new IllegalStateException("User not logged in");
    }

    // קולקציות פרטיות של המשתמש (קטגוריות)
    public static CollectionReference getUserCategoriesRef() {
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            return FBFS.collection("Users").document(uid).collection("Categories");
        }
        else
            throw new IllegalStateException("User not logged in");
    }

    // ==========================================
    // חלק 2: בדיקת אינטרנט (לשעבר NetworkUtil)
    // ==========================================

    public static boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }

    // ==========================================
    // חלק 3: עזרים למצלמה ותמונות (המקורי)
    // ==========================================

    public static Uri createImageUri(Context context) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "TempImage");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera");
        return context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }
}