package com.example.ronilesapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private TextView tvCountDown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // חיבור ל-TextView של המספר
        tvCountDown = findViewById(R.id.tvCountDown);

        // --- טיימר ל-4 שניות (כדי שיראו את ה-3 כמו שצריך) ---
        // שמנו 4000 כדי שהספירה תתחיל יפה מ-3 ותרד ל-1
        new CountDownTimer(3500, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                // חישוב השניות שנותרו
                // חילוק ב-1000 הופך מילישניות לשניות
                int secondsRemaining = (int) (millisUntilFinished / 1000);

                // עדכון הטקסט במסך
                // הוספתי הגנה קטנה שלא יציג 0
                if (secondsRemaining > 0) {
                    tvCountDown.setText(String.valueOf(secondsRemaining));
                } else {
                    tvCountDown.setText("1");
                }
            }

            @Override
            public void onFinish() {
                // כשהזמן נגמר - עוברים למסך הבא
                tvCountDown.setText("0"); // אופציונלי

                Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        }.start();
    }
}