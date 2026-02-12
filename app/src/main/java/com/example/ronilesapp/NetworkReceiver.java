package com.example.ronilesapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class NetworkReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Utils.isConnected(context)) {
            Toast.makeText(context, "אין חיבור לאינטרנט", Toast.LENGTH_SHORT).show();
        }
    }
}
