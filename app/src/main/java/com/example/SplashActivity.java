package com.example;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;

import androidx.appcompat.app.AppCompatActivity;

import com.example.databinding.ActivitySplashBinding;

public class SplashActivity extends AppCompatActivity {

    private ActivitySplashBinding binding;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isNavigated = false;

    private final Runnable navigateRunnable = this::navigateToWelcome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // --- Smooth Premium Fade-In Animation ---
        Animation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(1500); // 1.5 seconds smooth fade-in
        fadeIn.setInterpolator(new DecelerateInterpolator());
        binding.splashContainer.startAnimation(fadeIn);

        // --- Gentle Infinite Breathing Animation on Ambient Glows ---
        Animation breatheGlow = new AlphaAnimation(0.4f, 0.8f);
        breatheGlow.setDuration(2500);
        breatheGlow.setRepeatCount(Animation.INFINITE);
        breatheGlow.setRepeatMode(Animation.REVERSE);
        breatheGlow.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        binding.ambientGlow1.startAnimation(breatheGlow);
        binding.ambientGlow2.startAnimation(breatheGlow);

        // Automatically transition to WelcomeActivity after exactly 1.2 seconds
        handler.postDelayed(navigateRunnable, 1200);
    }

    private synchronized void navigateToWelcome() {
        if (!isNavigated) {
            isNavigated = true;
            handler.removeCallbacks(navigateRunnable);

            Intent intent = new Intent(SplashActivity.this, WelcomeActivity.class);
            startActivity(intent);
            // Apply standard slide transition or fade transition
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(navigateRunnable);
        super.onDestroy();
    }
}
