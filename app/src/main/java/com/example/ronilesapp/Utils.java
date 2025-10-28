package com.example.ronilesapp;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;

public class Utils {

    public static Uri createImageUri(Context context) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "TempImage");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera");
        return context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }
}
