package com.example;

import android.annotation.SuppressLint;
import android.app.AppOpsManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.database.FocusSession;
import com.example.databinding.ActivityFocusSessionBinding;
import com.example.viewmodel.FocusViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FocusSessionActivity extends AppCompatActivity {

    private ActivityFocusSessionBinding binding;
    private FocusViewModel viewModel;
    private SessionManager sessionManager;

    private final int[] timeChoices = {15, 25, 30, 45, 50, 60, 90, 120};
    private int currentChoiceIndex = 4; // default index is 4 (50 mins)
    private int selectedMinutes = 50;
    private long totalSecondsRemaining;
    private long totalDurationSeconds;
    private CountDownTimer countDownTimer;
    private boolean isTimerRunning = false;

    // Distraction tracking
    private int distractionCount = 0;
    private final ArrayList<String> clickedAppsList = new ArrayList<>();
    private String startTimeFormatted;

    // Background polling for real distraction detection
    private Thread distractionCheckThread;
    private volatile boolean isCheckingDistractions = false;
    private long lastDistractionAlertTime = 0;
    private static final long COOLDOWN_MS = 25000; // 25s cooldown if user chooses "Continue Anyway"

    private AlertDialog permissionDialog;
    private AlertDialog distractionDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFocusSessionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(FocusViewModel.class);
        sessionManager = new SessionManager(this);

        // Check if we came from a completed session notification
        Intent startIntent = getIntent();
        if (startIntent != null && startIntent.getBooleanExtra("session_completed", false)) {
            int distCount = startIntent.getIntExtra("completed_distraction_count", 0);
            int durationMins = startIntent.getIntExtra("completed_duration_mins", 50);
            String distApps = startIntent.getStringExtra("completed_distraction_apps");
            showCompletionDialog(true, distCount, distApps, durationMins);
        }

        updateControllerUI();

        // Plus / Minus adjustments
        binding.btnMinus.setOnClickListener(v -> {
            if (!isTimerRunning && currentChoiceIndex > 0) {
                currentChoiceIndex--;
                selectedMinutes = timeChoices[currentChoiceIndex];
                updateControllerUI();
            }
        });

        binding.btnPlus.setOnClickListener(v -> {
            if (!isTimerRunning && currentChoiceIndex < timeChoices.length - 1) {
                currentChoiceIndex++;
                selectedMinutes = timeChoices[currentChoiceIndex];
                updateControllerUI();
            }
        });

        // Start / Pause Focus
        binding.btnStartFocus.setOnClickListener(v -> {
            if (isTimerRunning) {
                pauseFocusSession();
            } else {
                startFocusSession();
            }
        });

        // Top Actions
        binding.btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(FocusSessionActivity.this, WelcomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        binding.btnStats.setOnClickListener(v -> {
            Intent intent = new Intent(FocusSessionActivity.this, AnalyticsActivity.class);
            startActivity(intent);
        });

        // Helpful guide to open the real apps for background distraction test
        binding.layoutInstagram.setOnClickListener(v -> Toast.makeText(this, "Open the real Instagram app to test background monitoring!", Toast.LENGTH_SHORT).show());
        binding.layoutFacebook.setOnClickListener(v -> Toast.makeText(this, "Open the real Facebook app to test background monitoring!", Toast.LENGTH_SHORT).show());
        binding.layoutWhatsApp.setOnClickListener(v -> Toast.makeText(this, "Open the real WhatsApp app to test background monitoring!", Toast.LENGTH_SHORT).show());
        binding.layoutYouTube.setOnClickListener(v -> Toast.makeText(this, "Open the real YouTube app to test background monitoring!", Toast.LENGTH_SHORT).show());
        binding.layoutTikTok.setOnClickListener(v -> Toast.makeText(this, "Open the real TikTok app to test background monitoring!", Toast.LENGTH_SHORT).show());

        // Modern back press callback
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Intent intent = new Intent(FocusSessionActivity.this, WelcomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null) {
            if (intent.getBooleanExtra("session_completed", false)) {
                int distCount = intent.getIntExtra("completed_distraction_count", 0);
                int durationMins = intent.getIntExtra("completed_duration_mins", 50);
                String distApps = intent.getStringExtra("completed_distraction_apps");
                showCompletionDialog(true, distCount, distApps, durationMins);
            } else if (intent.getBooleanExtra("show_distraction_alert", false)) {
                String distractedApp = intent.getStringExtra("distracted_app");
                if (distractedApp != null) {
                    FocusService service = FocusService.getInstance();
                    if (service == null || !service.isOverlayShowing()) {
                        showDistractionAlertDialog(distractedApp);
                    }
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!areAllPermissionsGranted()) {
            checkAndRequestPermissions();
        } else {
            FocusService.FocusServiceListener activeListener = new FocusService.FocusServiceListener() {
                @Override
                public void onTick(long secondsRemaining) {
                    runOnUiThread(() -> {
                        totalSecondsRemaining = secondsRemaining;
                        updateCountdownDisplay();
                    });
                }

                @Override
                public void onFinish() {
                    runOnUiThread(() -> {
                        completeFocusSession();
                    });
                }

                @Override
                public void onDistractionUpdated(int currentDistractionCount, String lastDistraction) {
                    runOnUiThread(() -> {
                        distractionCount = currentDistractionCount;
                        binding.tvDistractionBadge.setText(String.valueOf(distractionCount));
                        binding.tvLastDetectedDistraction.setText("Opened Distraction: " + lastDistraction);
                    });
                }

                @Override
                public void onStateChanged(boolean isRunning) {
                    runOnUiThread(() -> {
                        isTimerRunning = isRunning;
                        updateTimerControlsUI();
                    });
                }
            };

            FocusService.setStaticListener(activeListener);

            FocusService service = FocusService.getInstance();
            if (service != null) {
                // Sync UI with running service
                isTimerRunning = service.isTimerRunning();
                totalSecondsRemaining = service.totalSecondsRemaining();
                totalDurationSeconds = service.totalDurationSeconds();
                distractionCount = service.distractionCount();
                clickedAppsList.clear();
                clickedAppsList.addAll(service.clickedAppsList());
                startTimeFormatted = service.startTimeFormatted();
                selectedMinutes = service.selectedMinutes();

                binding.tvMinutesSelected.setText(String.valueOf(selectedMinutes));
                binding.tvDistractionBadge.setText(String.valueOf(distractionCount));
                if (clickedAppsList.size() > 0) {
                    binding.tvLastDetectedDistraction.setText("Opened Distraction: " + clickedAppsList.get(clickedAppsList.size() - 1));
                } else {
                    binding.tvLastDetectedDistraction.setText("Opened Distraction: None");
                }
                updateCountdownDisplay();
                updateTimerControlsUI();
            } else {
                isTimerRunning = false;
                updateTimerControlsUI();
                updateControllerUI();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        FocusService.setStaticListener(null);
    }

    private boolean areAllPermissionsGranted() {
        boolean notificationGranted = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationGranted = ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return notificationGranted && checkUsageStatsPermission() && checkOverlayPermission();
    }

    private boolean checkUsageStatsPermission() {
        try {
            AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOps.noteOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(), getPackageName());
            return mode == AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true;
    }

    private void checkAndRequestPermissions() {
        if (permissionDialog != null && permissionDialog.isShowing()) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permissions Required");
        builder.setMessage("Smart Study Focus requires these permissions to detect distracting apps and help you stay focused:\n\n" +
                "• Notifications: To alert you to stay focused.\n" +
                "• Usage Access: To detect when you open a distracting app.\n" +
                "• Display Over Other Apps: To show a full screen alert when you get distracted.");
        builder.setCancelable(false);
        builder.setPositiveButton("Grant Permissions", (dialog, which) -> {
            dialog.dismiss();
            requestPermissionsSequence();
        });
        builder.setNegativeButton("Go Back", (dialog, which) -> {
            dialog.dismiss();
            Intent intent = new Intent(FocusSessionActivity.this, WelcomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
        permissionDialog = builder.create();
        permissionDialog.show();
    }

    private void requestPermissionsSequence() {
        // 1. Notification Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 201);
                return;
            }
        }

        // 2. Usage Access Permission
        if (!checkUsageStatsPermission()) {
            Toast.makeText(this, "Please enable 'Usage Access' for Smart Study Focus", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivity(intent);
            return;
        }

        // 3. Display Over Other Apps
        if (!checkOverlayPermission()) {
            Toast.makeText(this, "Please enable 'Display Over Other Apps' for Smart Study Focus", Toast.LENGTH_LONG).show();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
            return;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 201) {
            requestPermissionsSequence(); // Continue check sequence
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateControllerUI() {
        binding.tvMinutesSelected.setText(String.valueOf(selectedMinutes));
        if (!isTimerRunning) {
            binding.tvTimerValue.setText(String.format(Locale.getDefault(), "%02d:00", selectedMinutes));
        }
    }

    private void startFocusSession() {
        isTimerRunning = true;
        totalDurationSeconds = selectedMinutes * 60L;
        if (totalSecondsRemaining <= 0) {
            totalSecondsRemaining = totalDurationSeconds;
        }

        FocusService.FocusServiceListener activeListener = new FocusService.FocusServiceListener() {
            @Override
            public void onTick(long secondsRemaining) {
                runOnUiThread(() -> {
                    totalSecondsRemaining = secondsRemaining;
                    updateCountdownDisplay();
                });
            }

            @Override
            public void onFinish() {
                runOnUiThread(() -> {
                    completeFocusSession();
                });
            }

            @Override
            public void onDistractionUpdated(int currentDistractionCount, String lastDistraction) {
                runOnUiThread(() -> {
                    distractionCount = currentDistractionCount;
                    binding.tvDistractionBadge.setText(String.valueOf(distractionCount));
                    binding.tvLastDetectedDistraction.setText("Opened Distraction: " + lastDistraction);
                });
            }

            @Override
            public void onStateChanged(boolean isRunning) {
                runOnUiThread(() -> {
                    isTimerRunning = isRunning;
                    updateTimerControlsUI();
                });
            }
        };

        FocusService.setStaticListener(activeListener);

        Intent serviceIntent = new Intent(this, FocusService.class);
        serviceIntent.putExtra("selected_minutes", selectedMinutes);
        ContextCompat.startForegroundService(this, serviceIntent);

        updateTimerControlsUI();
    }

    private void pauseFocusSession() {
        FocusService service = FocusService.getInstance();
        if (service != null) {
            service.pauseTimer();
        }
        isTimerRunning = false;
        updateTimerControlsUI();
    }

    private void resetFocusSession() {
        Intent serviceIntent = new Intent(this, FocusService.class);
        stopService(serviceIntent);

        isTimerRunning = false;
        distractionCount = 0;
        clickedAppsList.clear();
        binding.tvDistractionBadge.setText("0");
        binding.tvLastDetectedDistraction.setText("Opened Distraction: None");
        selectedMinutes = 50;
        currentChoiceIndex = 4;
        totalSecondsRemaining = 0;
        updateControllerUI();

        binding.btnStartFocus.setText("Start Focus");
        binding.btnStartFocus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play, 0, 0, 0);

        binding.btnMinus.setEnabled(true);
        binding.btnPlus.setEnabled(true);
        binding.btnMinus.setAlpha(1.0f);
        binding.btnPlus.setAlpha(1.0f);

        binding.tvTimerStatus.setText("Ready to focus");
        binding.progressBarTimer.setProgress(100);
    }

    @SuppressLint("SetTextI18n")
    private void updateCountdownDisplay() {
        long minutes = totalSecondsRemaining / 60;
        long seconds = totalSecondsRemaining % 60;
        binding.tvTimerValue.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));

        if (totalDurationSeconds > 0) {
            int progress = (int) ((totalSecondsRemaining * 100) / totalDurationSeconds);
            binding.progressBarTimer.setProgress(progress);
        }
    }

    private void completeFocusSession() {
        isTimerRunning = false;
        updateTimerControlsUI();
        runOnUiThread(() -> showCompletionDialog(true, distractionCount, getClickedAppsString(), selectedMinutes));
    }

    private synchronized void showDistractionAlertDialog(String appName) {
        if (distractionDialog != null && distractionDialog.isShowing()) {
            return;
        }

        try {
            binding.getRoot().performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        } catch (Exception e) {}

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_distraction_alert, null);
        builder.setView(dialogView);
        builder.setCancelable(false);

        distractionDialog = builder.create();
        if (distractionDialog.getWindow() != null) {
            distractionDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTitle = dialogView.findViewById(R.id.tvAlertTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvAlertMessage);
        TextView tvDistractedAppName = dialogView.findViewById(R.id.tvDistractedAppName);
        androidx.appcompat.widget.AppCompatButton btnContinueAnyway = dialogView.findViewById(R.id.btnContinueAnyway);
        androidx.appcompat.widget.AppCompatButton btnBackToStudy = dialogView.findViewById(R.id.btnBackToStudy);

        if (tvTitle != null) tvTitle.setText("Stay Focused!");
        if (tvDistractedAppName != null) tvDistractedAppName.setText(appName);
        if (tvMessage != null) tvMessage.setText("You opened " + appName + " while your focus session is active. Please return to your study session.");

        if (btnBackToStudy != null) {
            btnBackToStudy.setText("Back to Study");
            btnBackToStudy.setOnClickListener(v -> {
                FocusService service = FocusService.getInstance();
                if (service != null) {
                    service.resetCooldownFromActivity();
                }
                distractionDialog.dismiss();
            });
        }

        if (btnContinueAnyway != null) {
            btnContinueAnyway.setText("Back to App");
            btnContinueAnyway.setOnClickListener(v -> {
                FocusService service = FocusService.getInstance();
                if (service != null) {
                    service.startCooldownFromActivity();
                }
                distractionDialog.dismiss();
            });
        }

        distractionDialog.show();
    }

    private void showCompletionDialog(boolean alreadySaved, int distCount, String distApps, int durationMins) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_session_completed, null);
        builder.setView(dialogView);
        builder.setCancelable(false);

        final AlertDialog completionDialog = builder.create();
        if (completionDialog.getWindow() != null) {
            completionDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        androidx.appcompat.widget.AppCompatButton btnViewReport = dialogView.findViewById(R.id.btnViewReport);
        androidx.appcompat.widget.AppCompatButton btnStartAnother = dialogView.findViewById(R.id.btnStartAnother);

        if (!alreadySaved) {
            saveSessionToDatabase();
        }

        btnViewReport.setOnClickListener(v -> {
            completionDialog.dismiss();

            int finalDistCount = alreadySaved ? distCount : distractionCount;
            String finalDistApps = alreadySaved ? distApps : getClickedAppsString();
            int finalDurationMins = alreadySaved ? durationMins : selectedMinutes;
            double pct = Math.max(0, 100 - (finalDistCount * 8.0));

            Intent intent = new Intent(FocusSessionActivity.this, AnalyticsActivity.class);
            intent.putExtra("focus_percent", pct);
            intent.putExtra("distraction_count", finalDistCount);
            intent.putExtra("distraction_apps", finalDistApps);
            intent.putExtra("duration_mins", finalDurationMins);
            startActivity(intent);
            finish();
        });

        btnStartAnother.setOnClickListener(v -> {
            completionDialog.dismiss();
            resetFocusSession();
        });

        completionDialog.show();
    }

    private void saveSessionToDatabase() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        String dateStr = dateFormat.format(new Date());

        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String endTimeFormatted = timeFormat.format(new Date());

        long elapsedFocusSeconds = totalDurationSeconds;
        double pct = Math.max(0, 100 - (distractionCount * 8.0));
        String appsStr = getClickedAppsString();

        String status = "Completed";
        String userEmail = sessionManager.getLoggedInEmail();
        if (userEmail == null) {
            userEmail = "test@example.com";
        }

        FocusSession session = new FocusSession(
                userEmail,
                dateStr,
                startTimeFormatted != null ? startTimeFormatted : timeFormat.format(new Date(System.currentTimeMillis() - totalDurationSeconds * 1000)),
                endTimeFormatted,
                elapsedFocusSeconds,
                totalDurationSeconds,
                distractionCount,
                appsStr,
                pct,
                status
        );

        viewModel.insertSession(session);
        sessionManager.incrementFocusStreak();
    }

    private String getClickedAppsString() {
        StringBuilder appsBuilder = new StringBuilder();
        for (int i = 0; i < clickedAppsList.size(); i++) {
            appsBuilder.append(clickedAppsList.get(i));
            if (i < clickedAppsList.size() - 1) {
                appsBuilder.append(", ");
            }
        }
        return appsBuilder.toString();
    }

    private void updateTimerControlsUI() {
        if (isTimerRunning) {
            binding.btnStartFocus.setText("Pause Focus");
            binding.btnStartFocus.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);

            binding.btnMinus.setEnabled(false);
            binding.btnPlus.setEnabled(false);
            binding.btnMinus.setAlpha(0.3f);
            binding.btnPlus.setAlpha(0.3f);

            binding.tvTimerStatus.setText("Studying intensely...");
        } else {
            if (totalSecondsRemaining > 0 && totalSecondsRemaining < totalDurationSeconds) {
                binding.btnStartFocus.setText("Resume Focus");
                binding.btnStartFocus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play, 0, 0, 0);
                binding.tvTimerStatus.setText("Focus paused");
            } else {
                binding.btnStartFocus.setText("Start Focus");
                binding.btnStartFocus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play, 0, 0, 0);
                binding.tvTimerStatus.setText("Ready to focus");
            }

            binding.btnMinus.setEnabled(true);
            binding.btnPlus.setEnabled(true);
            binding.btnMinus.setAlpha(1.0f);
            binding.btnPlus.setAlpha(1.0f);
        }
    }

    @Override
    protected void onDestroy() {
        FocusService service = FocusService.getInstance();
        if (service != null) {
            service.setListener(null);
        }
        super.onDestroy();
    }
}
