package com.example;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.database.FocusSession;
import com.example.databinding.ActivityAnalyticsBinding;
import com.example.viewmodel.FocusViewModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AnalyticsActivity extends AppCompatActivity {

    private ActivityAnalyticsBinding binding;
    private FocusViewModel viewModel;
    private SessionManager sessionManager;

    // Active session display values
    private int sessionMinutes = 50;
    private int distractionCount = 0;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAnalyticsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize helpers
        sessionManager = new SessionManager(this);
        viewModel = new ViewModelProvider(this).get(FocusViewModel.class);

        // Configure Back Button click listener
        binding.btnBack.setOnClickListener(v -> handleBackNavigation());
        binding.btnDoneAnalytics.setOnClickListener(v -> handleBackNavigation());

        // Configure modern back press callback
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackNavigation();
            }
        });

        // Determine if we are viewing a specific completed session from Intent extras
        if (getIntent().hasExtra("duration_mins")) {
            sessionMinutes = getIntent().getIntExtra("duration_mins", 50);
            distractionCount = getIntent().getIntExtra("distraction_count", 0);
            displayActiveSessionStats();
        } else {
            // Otherwise, we load the most recent session from the database as default active session
            loadLatestSessionStats();
        }

        // Fetch user history to populate the 3-day colored bar graph
        loadThreeDayGraphData();
    }

    @SuppressLint("SetTextI18n")
    private void displayActiveSessionStats() {
        // Display Pill: "Session: X min - Distractions: Y"
        binding.tvSessionDetails.setText("Session: " + sessionMinutes + " min - Distractions: " + distractionCount);

        // Setup Score and Dynamic Messaging/Icons
        // 0 Distractions: Excellent!
        // 1-2 Distractions: Good!
        // 3 Distractions: Better!
        // 4+ Distractions: Bad
        if (distractionCount == 0) {
            binding.ivRatingIcon.setImageResource(R.drawable.ic_lightbulb);
            binding.ivRatingIcon.setColorFilter(Color.parseColor("#4CAF50")); // Success Green
            binding.tvRatingText.setText("Excellent!");
            binding.tvRatingText.setTextColor(Color.parseColor("#4CAF50"));
            binding.tvRatingDescription.setText("Amazing focus! You kept distractions to absolute zero. Perfect session!");
        } else if (distractionCount <= 2) {
            binding.ivRatingIcon.setImageResource(R.drawable.ic_lightbulb);
            binding.ivRatingIcon.setColorFilter(Color.parseColor("#1877F2")); // Blue
            binding.tvRatingText.setText("Good!");
            binding.tvRatingText.setTextColor(Color.parseColor("#1877F2"));
            binding.tvRatingDescription.setText("Great job! Very minimal distractions. You stayed mostly aligned to your study goal.");
        } else if (distractionCount == 3) {
            binding.ivRatingIcon.setImageResource(R.drawable.ic_timer);
            binding.ivRatingIcon.setColorFilter(Color.parseColor("#FFC107")); // Warning Yellow/Orange
            binding.tvRatingText.setText("Better!");
            binding.tvRatingText.setTextColor(Color.parseColor("#FFC107"));
            binding.tvRatingDescription.setText("Nice effort, but you can do better! Try to avoid checking distracting apps.");
        } else {
            binding.ivRatingIcon.setImageResource(R.drawable.ic_bell);
            binding.ivRatingIcon.setColorFilter(Color.parseColor("#E53935")); // Red
            binding.tvRatingText.setText("Bad!");
            binding.tvRatingText.setTextColor(Color.parseColor("#E53935"));
            binding.tvRatingDescription.setText("Highly distracted. Close all social media platforms and focus on your work next time!");
        }
    }

    private void loadLatestSessionStats() {
        String email = sessionManager.getLoggedInEmail();
        if (email == null) email = "test@example.com";

        viewModel.getRecentSessions(email, 1).observe(this, sessions -> {
            if (sessions != null && !sessions.isEmpty()) {
                FocusSession lastSession = sessions.get(0);
                sessionMinutes = (int) (lastSession.getCompletedTime() / 60);
                if (sessionMinutes <= 0) sessionMinutes = 50;
                distractionCount = lastSession.getDistractionCount();
            } else {
                // Default placeholders if database has no records
                sessionMinutes = 50;
                distractionCount = 0;
            }
            displayActiveSessionStats();
        });
    }

    private int countOccurrences(String source, String target) {
        if (source == null || target == null || source.isEmpty() || target.isEmpty()) {
            return 0;
        }
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(target, index)) != -1) {
            count++;
            index += target.length();
        }
        return count;
    }

    @SuppressLint("SetTextI18n")
    private void loadThreeDayGraphData() {
        String email = sessionManager.getLoggedInEmail();
        if (email == null) email = "test@example.com";

        viewModel.getSessionsForUser(email).observe(this, sessions -> {
            int wa = 0;
            int ig = 0;
            int fb = 0;
            int tt = 0;
            int yt = 0;

            boolean hasRealSessions = (sessions != null && !sessions.isEmpty());

            if (hasRealSessions) {
                // Get date format strings for Today, Yesterday, and 2 Days Ago
                Calendar cal = Calendar.getInstance();
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

                String todayStr = sdf.format(cal.getTime());

                cal.add(Calendar.DAY_OF_YEAR, -1);
                String yesterdayStr = sdf.format(cal.getTime());

                cal.add(Calendar.DAY_OF_YEAR, -1);
                String dayBeforeStr = sdf.format(cal.getTime());

                for (FocusSession session : sessions) {
                    String date = session.getDate();
                    if (date != null && (date.equals(todayStr) || date.equals(yesterdayStr) || date.equals(dayBeforeStr))) {
                        String apps = session.getDistractionApps();
                        if (apps != null && !apps.isEmpty()) {
                            String lowerApps = apps.toLowerCase();
                            // Count actual app occurrences in historical string
                            wa += countOccurrences(lowerApps, "whatsapp");
                            ig += countOccurrences(lowerApps, "instagram");
                            fb += countOccurrences(lowerApps, "facebook");
                            tt += countOccurrences(lowerApps, "tiktok");
                            yt += countOccurrences(lowerApps, "youtube");
                        }
                    }
                }
            } else {
                // Fallback preview values ONLY when the user is completely new and has absolutely 0 sessions
                wa = 2; // WhatsApp used 2 times -> green bar goes up
                ig = 1; // Instagram used 1 time -> pink bar goes up
                fb = 0;
                tt = 0;
                yt = 0;
            }

            // Update custom Graph View
            binding.distractionGraphView.setAppCounts(wa, ig, fb, tt, yt);

            // Update counts in list table
            binding.tvCountWa.setText(wa + (wa == 1 ? " time" : " times"));
            binding.tvCountIg.setText(ig + (ig == 1 ? " time" : " times"));
            binding.tvCountFb.setText(fb + (fb == 1 ? " time" : " times"));
            binding.tvCountTt.setText(tt + (tt == 1 ? " time" : " times"));
            binding.tvCountYt.setText(yt + (yt == 1 ? " time" : " times"));
        });
    }

    private void handleBackNavigation() {
        Intent intent = new Intent(AnalyticsActivity.this, WelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
