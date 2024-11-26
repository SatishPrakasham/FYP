package com.example.smartdiet;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class Splash_screen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        // Delay of 1.5 seconds (1500 milliseconds)
        new Handler().postDelayed(() -> {
            // Start LoginScreen activity
            Intent intent = new Intent(Splash_screen.this, MainActivity.class);
            startActivity(intent);
            // Finish SplashScreen activity so the user can't go back to it
            finish();
        }, 1500);

    }
}