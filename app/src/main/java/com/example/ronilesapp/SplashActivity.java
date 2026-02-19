package com.example.ronilesapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {

    private CountDownTimer countDownTimer;
    private TextView tvCountDown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // אם המשתמש כבר מחובר — נדלג על Login
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            startActivity(new Intent(SplashActivity.this, TasksActivity.class));
            finish();
            return;
        }

        // חיבור ל-TextView של המספר
        tvCountDown = findViewById(R.id.tvCountDown);

        // --- טיימר ל-4 שניות, יציג 4 עד 1 ---
        countDownTimer = new CountDownTimer(4000, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                // חישוב השניות שנותרו
                // חילוק ב-1000 הופך מילישניות לשניות
                // נעגל למעלה ולא נגיע ל-0
                int secondsRemaining = (int) Math.ceil(millisUntilFinished / 1000.0);

                // עדכון הטקסט במסך
                tvCountDown.setText(String.valueOf(secondsRemaining));
            }

            @Override
            public void onFinish() {
                // כשהזמן נגמר - עוברים למסך הבא
                Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}