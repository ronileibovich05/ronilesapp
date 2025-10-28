package com.example.ronilesapp;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class FBRef {

    // Firebase Auth
    public static FirebaseAuth mAuth = FirebaseAuth.getInstance();

    // Firestore
    public static FirebaseFirestore FBFS = FirebaseFirestore.getInstance();
    public static CollectionReference refUsers = FBFS.collection("Users");
    public static CollectionReference refImages = FBFS.collection("Images");

    // Firebase Storage
    public static StorageReference storageRef = FirebaseStorage.getInstance().getReference();
}
