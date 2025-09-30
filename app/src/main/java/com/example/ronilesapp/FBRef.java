package com.example.ronilesapp;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FBRef {
    public static FirebaseAuth mAuth = FirebaseAuth.getInstance();


    public static FirebaseDatabase database = FirebaseDatabase.getInstance();
    public static DatabaseReference myRef = database.getReference();
    public static DatabaseReference userRef = myRef.child("Users");
    public static DatabaseReference refItems = myRef.child("item");
}
