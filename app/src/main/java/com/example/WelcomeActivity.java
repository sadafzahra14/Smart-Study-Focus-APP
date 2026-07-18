package com.example;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;

import androidx.appcompat.app.AppCompatActivity;

import com.example.databinding.ActivityWelcomeBinding;

public class WelcomeActivity extends AppCompatActivity {

    private ActivityWelcomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWelcomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // --- 1. Top Bar / Title Animations ---
        AnimationSet barAnim = new AnimationSet(true);
        barAnim.setInterpolator(new DecelerateInterpolator());
        Animation barFade = new AlphaAnimation(0f, 1f);
        barFade.setDuration(800);
        Animation barSlide = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, -0.3f, Animation.RELATIVE_TO_SELF, 0f
        );
        barSlide.setDuration(800);
        barAnim.addAnimation(barFade);
        barAnim.addAnimation(barSlide);
        binding.toolbarWelcome.startAnimation(barAnim);

        // --- 2. Illustration Entrance & Loop Animations ---
        AnimationSet illustrationEntrance = new AnimationSet(true);
        illustrationEntrance.setInterpolator(new OvershootInterpolator(1.2f));
        Animation illFade = new AlphaAnimation(0f, 1f);
        illFade.setDuration(900);
        Animation illScale = new ScaleAnimation(
                0.4f, 1.0f, 0.4f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
        );
        illScale.setDuration(900);
        illustrationEntrance.addAnimation(illFade);
        illustrationEntrance.addAnimation(illScale);
        illustrationEntrance.setStartOffset(150);

        illustrationEntrance.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                // Smooth Floating Animation for Illustration
                TranslateAnimation floatAnim = new TranslateAnimation(
                        Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f,
                        Animation.ABSOLUTE, 0f, Animation.ABSOLUTE, -20f
                );
                floatAnim.setDuration(2400);
                floatAnim.setRepeatCount(Animation.INFINITE);
                floatAnim.setRepeatMode(Animation.REVERSE);
                floatAnim.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
                binding.illustrationContainerWelcome.startAnimation(floatAnim);

                // Breathing glow animation for outer glow/middle rings
                ScaleAnimation breatheAnim = new ScaleAnimation(
                        0.95f, 1.05f, 0.95f, 1.05f,
                        Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
                );
                breatheAnim.setDuration(2000);
                breatheAnim.setRepeatCount(Animation.INFINITE);
                breatheAnim.setRepeatMode(Animation.REVERSE);
                breatheAnim.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
                binding.bulbMiddleGlow.startAnimation(breatheAnim);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        binding.illustrationContainerWelcome.startAnimation(illustrationEntrance);

        // --- 3. Text Layout Entrance ---
        AnimationSet textAnim = new AnimationSet(true);
        textAnim.setInterpolator(new DecelerateInterpolator());
        Animation textFade = new AlphaAnimation(0f, 1f);
        textFade.setDuration(800);
        Animation textSlide = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0.2f, Animation.RELATIVE_TO_SELF, 0f
        );
        textSlide.setDuration(800);
        textAnim.addAnimation(textFade);
        textAnim.addAnimation(textSlide);
        textAnim.setStartOffset(350);
        binding.textLayoutWelcome.startAnimation(textAnim);

        // --- 4. Staggered Badges Entrance ---
        setupStaggeredBadgeAnim(binding.badgeWelcome1, 550);
        setupStaggeredBadgeAnim(binding.badgeWelcome2, 700);
        setupStaggeredBadgeAnim(binding.badgeWelcome3, 850);

        // --- 5. Start Button Entrance ---
        AnimationSet buttonAnim = new AnimationSet(true);
        buttonAnim.setInterpolator(new OvershootInterpolator(1.4f));
        Animation buttonFade = new AlphaAnimation(0f, 1f);
        buttonFade.setDuration(700);
        Animation buttonScale = new ScaleAnimation(
                0.5f, 1.0f, 0.5f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
        );
        buttonScale.setDuration(700);
        buttonAnim.addAnimation(buttonFade);
        buttonAnim.addAnimation(buttonScale);
        buttonAnim.setStartOffset(1000);
        binding.btnWelcomeStart.startAnimation(buttonAnim);

        // --- Toolbar Clicks ---
        binding.btnToolbarProfile.setOnClickListener(v -> {
            SessionManager sm = new SessionManager(WelcomeActivity.this);
            if (sm.isLoggedIn()) {
                Intent intent = new Intent(WelcomeActivity.this, ProfileActivity.class);
                startActivity(intent);
            } else {
                android.widget.Toast.makeText(WelcomeActivity.this, "First Login", android.widget.Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        binding.btnToolbarSearch.setOnClickListener(v -> {
            SessionManager sm = new SessionManager(WelcomeActivity.this);
            if (sm.isLoggedIn()) {
                Intent intent = new Intent(WelcomeActivity.this, SearchActivity.class);
                startActivity(intent);
            } else {
                android.widget.Toast.makeText(WelcomeActivity.this, "First Login", android.widget.Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        // --- Let's Start Bottom Button Click ---
        binding.btnWelcomeStart.setOnClickListener(v -> {
            SessionManager sm = new SessionManager(WelcomeActivity.this);
            if (sm.isLoggedIn()) {
                Intent intent = new Intent(WelcomeActivity.this, FocusSessionActivity.class);
                startActivity(intent);
                finish();
            } else {
                android.widget.Toast.makeText(WelcomeActivity.this, "First Login", android.widget.Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        // --- Badge Shortcuts ---
        binding.badgeWelcome1.setOnClickListener(v -> {
            SessionManager sm = new SessionManager(WelcomeActivity.this);
            Intent intent;
            if (sm.isLoggedIn()) {
                intent = new Intent(WelcomeActivity.this, FocusSessionActivity.class);
            } else {
                intent = new Intent(WelcomeActivity.this, LoginActivity.class);
            }
            startActivity(intent);
        });

        binding.badgeWelcome2.setOnClickListener(v -> {
            SessionManager sm = new SessionManager(WelcomeActivity.this);
            Intent intent;
            if (sm.isLoggedIn()) {
                intent = new Intent(WelcomeActivity.this, FocusSessionActivity.class);
            } else {
                intent = new Intent(WelcomeActivity.this, LoginActivity.class);
            }
            startActivity(intent);
        });

        binding.badgeWelcome3.setOnClickListener(v -> {
            SessionManager sm = new SessionManager(WelcomeActivity.this);
            Intent intent;
            if (sm.isLoggedIn()) {
                intent = new Intent(WelcomeActivity.this, AnalyticsActivity.class);
            } else {
                intent = new Intent(WelcomeActivity.this, LoginActivity.class);
            }
            startActivity(intent);
        });
    }

    private void setupStaggeredBadgeAnim(android.view.View view, long delay) {
        AnimationSet anim = new AnimationSet(true);
        anim.setInterpolator(new DecelerateInterpolator());
        Animation fade = new AlphaAnimation(0f, 1f);
        fade.setDuration(700);
        Animation slide = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0.25f, Animation.RELATIVE_TO_SELF, 0f
        );
        slide.setDuration(700);
        anim.addAnimation(fade);
        anim.addAnimation(slide);
        anim.setStartOffset(delay);
        view.startAnimation(anim);
    }
}
