package com.example.ronilesapp;

// Firebase Auth
import com.google.firebase.auth.FirebaseAuth;

// Firebase Realtime Database
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

// Firebase Firestore
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class FBRef {
    // Firebase Auth
    public static FirebaseAuth mAuth = FirebaseAuth.getInstance();

    // Realtime Database
    public static FirebaseDatabase database = FirebaseDatabase.getInstance();
    public static DatabaseReference myRef = database.getReference();
    public static DatabaseReference userRef = myRef.child("Users");
    public static DatabaseReference refItems = myRef.child("item");

    // Firestore
    public static FirebaseFirestore FBFS = FirebaseFirestore.getInstance();
    public static CollectionReference refImages = FBFS.collection("Images");
}
