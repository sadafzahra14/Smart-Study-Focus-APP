package com.example;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.database.AppDatabase;
import com.example.database.FocusSession;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FocusService extends Service {

    private static final String CHANNEL_ID = "focus_session_channel";
    private static final int NOTIFICATION_ID = 101;
    private static final long COOLDOWN_MS = 25000; // 25 seconds cooldown if user presses Continue Anyway

    private static FocusService instance = null;
    private static FocusServiceListener staticListener = null;

    public static void setStaticListener(FocusServiceListener listener) {
        staticListener = listener;
        if (instance != null) {
            instance.setListener(listener);
        }
    }

    public interface FocusServiceListener {
        void onTick(long secondsRemaining);
        void onFinish();
        void onDistractionUpdated(int distractionCount, String lastDistraction);
        void onStateChanged(boolean isRunning);
    }

    private FocusServiceListener listener = null;

    // Session states
    private int selectedMinutes = 50;
    private long totalDurationSeconds;
    private long totalSecondsRemaining;
    private boolean isTimerRunning = false;
    private int distractionCount = 0;
    private final ArrayList<String> clickedAppsList = new ArrayList<>();
    private String startTimeFormatted;
    private long currentSessionId = -1;

    private CountDownTimer countDownTimer;

    // Distraction detection background thread
    private Thread distractionCheckThread;
    private volatile boolean isCheckingDistractions = false;
    private long lastDistractionAlertTime = 0;

    // Overlay state
    private boolean isOverlayShowing = false;
    private FrameLayout overlayContainer = null;

    public static FocusService getInstance() {
        return instance;
    }

    public void setListener(FocusServiceListener listener) {
        this.listener = listener;
    }

    public boolean isTimerRunning() {
        return isTimerRunning;
    }

    public long totalSecondsRemaining() {
        return totalSecondsRemaining;
    }

    public long totalDurationSeconds() {
        return totalDurationSeconds;
    }

    public int distractionCount() {
        return distractionCount;
    }

    public ArrayList<String> clickedAppsList() {
        return clickedAppsList;
    }

    public String startTimeFormatted() {
        return startTimeFormatted;
    }

    public int selectedMinutes() {
        return selectedMinutes;
    }

    public boolean isOverlayShowing() {
        return isOverlayShowing;
    }

    public void recordDistractionFromActivity(String appName) {
        recordDistraction(appName);
    }

    public void resetCooldownFromActivity() {
        lastDistractionAlertTime = 0;
    }

    public void startCooldownFromActivity() {
        lastDistractionAlertTime = System.currentTimeMillis();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        if (staticListener != null) {
            this.listener = staticListener;
        }
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("selected_minutes") && !isTimerRunning) {
            selectedMinutes = intent.getIntExtra("selected_minutes", 50);
            totalDurationSeconds = selectedMinutes * 60L;
            totalSecondsRemaining = totalDurationSeconds;
            distractionCount = 0;
            clickedAppsList.clear();
            startTimeFormatted = null;
        }

        // Always show the notification immediately on start
        showForegroundNotification();

        // Auto start the timer if not already running
        if (!isTimerRunning) {
            startTimer();
        } else {
            startDistractionChecking();
        }

        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Focus Session Active Timer",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows ongoing focus session remaining time");
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void showForegroundNotification() {
        long minutes = totalSecondsRemaining / 60;
        long seconds = totalSecondsRemaining % 60;
        String timeStr = String.format(Locale.getDefault(), "%02d:%02d remaining", minutes, seconds);

        Intent notificationIntent = new Intent(this, FocusSessionActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle("Focus Session Active")
                .setContentText(timeStr)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setOngoing(true);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, builder.build(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIFICATION_ID, builder.build());
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                startForeground(NOTIFICATION_ID, builder.build());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void updateServiceNotification() {
        long minutes = totalSecondsRemaining / 60;
        long seconds = totalSecondsRemaining % 60;
        String timeStr = String.format(Locale.getDefault(), "%02d:%02d remaining", minutes, seconds);

        Intent notificationIntent = new Intent(this, FocusSessionActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle("Focus Session Active")
                .setContentText(timeStr)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setOngoing(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    public synchronized void startTimer() {
        if (isTimerRunning) return;
        isTimerRunning = true;

        if (distractionCount == 0 && startTimeFormatted == null) {
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            startTimeFormatted = timeFormat.format(new Date());
        }

        if (totalSecondsRemaining <= 0) {
            totalSecondsRemaining = totalDurationSeconds;
        }

        if (currentSessionId == -1) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            String dateStr = dateFormat.format(new Date());

            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            String startTime = startTimeFormatted != null ? startTimeFormatted : timeFormat.format(new Date());

            SessionManager sm = new SessionManager(this);
            String userEmail = sm.getLoggedInEmail();
            if (userEmail == null) {
                userEmail = "test@example.com";
            }

            FocusSession session = new FocusSession(
                    userEmail,
                    dateStr,
                    startTime,
                    "", // End time empty initially
                    0,  // Focus time starts at 0
                    totalDurationSeconds,
                    distractionCount,
                    "", // Apps empty initially
                    100.0,
                    "Active"
            );

            AppDatabase database = AppDatabase.getDatabase(this);
            AppDatabase.databaseWriteExecutor.execute(() -> {
                currentSessionId = database.focusSessionDao().insertSession(session);
            });
        }

        countDownTimer = new CountDownTimer(totalSecondsRemaining * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                totalSecondsRemaining = millisUntilFinished / 1000;
                if (listener != null) {
                    listener.onTick(totalSecondsRemaining);
                }
                updateServiceNotification();
            }

            @Override
            public void onFinish() {
                isTimerRunning = false;
                totalSecondsRemaining = 0;
                if (listener != null) {
                    listener.onFinish();
                }
                completeSession();
            }
        }.start();

        startDistractionChecking();

        if (listener != null) {
            listener.onStateChanged(true);
        }
    }

    public synchronized void pauseTimer() {
        if (!isTimerRunning) return;
        isTimerRunning = false;
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        stopDistractionChecking();
        dismissOverlay();

        if (listener != null) {
            listener.onStateChanged(false);
        }
    }

    private void completeSession() {
        // Complete the current session in the database
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String endTimeFormatted = timeFormat.format(new Date());

        long elapsedFocusSeconds = totalDurationSeconds - totalSecondsRemaining;
        if (elapsedFocusSeconds <= 0) {
            elapsedFocusSeconds = totalDurationSeconds;
        }
        double pct = Math.max(0, 100 - (distractionCount * 8.0));

        final long sessionId = currentSessionId;
        final String endTime = endTimeFormatted;
        final long focusSecs = elapsedFocusSeconds;
        final double finalPct = pct;

        StringBuilder appsBuilderForDb = new StringBuilder();
        for (int i = 0; i < clickedAppsList.size(); i++) {
            appsBuilderForDb.append(clickedAppsList.get(i));
            if (i < clickedAppsList.size() - 1) {
                appsBuilderForDb.append(", ");
            }
        }
        final String appsStrForDb = appsBuilderForDb.toString();

        AppDatabase database = AppDatabase.getDatabase(this);
        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (sessionId != -1) {
                database.focusSessionDao().completeSession(sessionId, endTime, focusSecs, finalPct, "Completed");
            } else {
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                String dateStr = dateFormat.format(new Date());
                SessionManager sm = new SessionManager(this);
                String userEmail = sm.getLoggedInEmail();
                if (userEmail == null) {
                    userEmail = "test@example.com";
                }
                FocusSession fallbackSession = new FocusSession(
                        userEmail,
                        dateStr,
                        startTimeFormatted != null ? startTimeFormatted : timeFormat.format(new Date(System.currentTimeMillis() - focusSecs * 1000)),
                        endTime,
                        focusSecs,
                        totalDurationSeconds,
                        distractionCount,
                        appsStrForDb,
                        finalPct,
                        "Completed"
                );
                database.focusSessionDao().insertSession(fallbackSession);
            }
        });

        SessionManager sm = new SessionManager(this);
        sm.incrementFocusStreak();

        // Reset currentSessionId for next sessions
        currentSessionId = -1;

        // Bring activity to foreground to show completed dialog
        Intent intent = new Intent(this, FocusSessionActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra("session_completed", true);
        intent.putExtra("completed_distraction_count", distractionCount);
        intent.putExtra("completed_duration_mins", selectedMinutes);

        StringBuilder appsBuilder = new StringBuilder();
        for (int i = 0; i < clickedAppsList.size(); i++) {
            appsBuilder.append(clickedAppsList.get(i));
            if (i < clickedAppsList.size() - 1) {
                appsBuilder.append(", ");
            }
        }
        intent.putExtra("completed_distraction_apps", appsBuilder.toString());
        startActivity(intent);

        stopSelf();
    }

    private void startDistractionChecking() {
        if (isCheckingDistractions) return;
        isCheckingDistractions = true;
        distractionCheckThread = new Thread(() -> {
            while (isCheckingDistractions) {
                try {
                    Thread.sleep(1500); // Poll every 1.5 seconds
                    if (!isTimerRunning) continue;

                    String pkg = getForegroundPackage();
                    if (pkg != null && isDistractingPackage(pkg)) {
                        if (System.currentTimeMillis() - lastDistractionAlertTime > COOLDOWN_MS) {
                            lastDistractionAlertTime = System.currentTimeMillis(); // Start cooldown IMMEDIATELY to prevent double alerts

                            new android.os.Handler(Looper.getMainLooper()).post(() -> {
                                String appName = getAppNameFromPackage(pkg);

                                // 1. Record the distraction immediately
                                recordDistraction(appName);

                                // 2. Show overlay
                                showOverlay(appName);

                                // 3. Also bring our app to the foreground and show the alert dialog
                                Intent intent = new Intent(FocusService.this, FocusSessionActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                                intent.putExtra("show_distraction_alert", true);
                                intent.putExtra("distracted_app", appName);
                                startActivity(intent);
                            });
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        distractionCheckThread.start();
    }

    private void stopDistractionChecking() {
        isCheckingDistractions = false;
        if (distractionCheckThread != null) {
            distractionCheckThread.interrupt();
            distractionCheckThread = null;
        }
    }

    private String getForegroundPackage() {
        String foregroundProcess = "";
        UsageStatsManager mUsageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        if (mUsageStatsManager != null) {
            long time = System.currentTimeMillis();

            // 1. Try real-time UsageEvents with a chronologically ordered sequence (last 15 minutes)
            try {
                android.app.usage.UsageEvents usageEvents = mUsageStatsManager.queryEvents(time - 1000L * 900, time + 1000L * 60);
                if (usageEvents != null) {
                    android.app.usage.UsageEvents.Event event = new android.app.usage.UsageEvents.Event();
                    long latestEventTime = 0;
                    String latestPkg = "";
                    while (usageEvents.hasNextEvent()) {
                        usageEvents.getNextEvent(event);
                        int eventType = event.getEventType();
                        if (eventType == 1 || eventType == 19) { // MOVE_TO_FOREGROUND (1) or ACTIVITY_RESUMED (19)
                            if (event.getTimeStamp() > latestEventTime) {
                                latestPkg = event.getPackageName();
                                latestEventTime = event.getTimeStamp();
                            }
                        }
                    }
                    if (!latestPkg.isEmpty()) {
                        foregroundProcess = latestPkg;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // 2. Fallback to queryUsageStats with 24-hour daily bucket window and 5-minute freshness check
            if (foregroundProcess == null || foregroundProcess.isEmpty()) {
                try {
                    List<UsageStats> stats = mUsageStatsManager.queryUsageStats(
                            UsageStatsManager.INTERVAL_DAILY,
                            time - 1000L * 86400, // Spans 24 hours
                            time + 1000L * 60
                    );
                    if (stats != null && !stats.isEmpty()) {
                        long lastUsedTime = 0;
                        String bestProcess = "";
                        for (UsageStats usageStats : stats) {
                            if (usageStats.getLastTimeUsed() > lastUsedTime) {
                                bestProcess = usageStats.getPackageName();
                                lastUsedTime = usageStats.getLastTimeUsed();
                            }
                        }
                        // Accept if this app was actively used within the last 5 minutes to accommodate system stats write latency
                        if (System.currentTimeMillis() - lastUsedTime <= 300000) {
                            foregroundProcess = bestProcess;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return foregroundProcess;
    }

    private boolean isDistractingPackage(String pkg) {
        if (pkg == null || pkg.isEmpty() || pkg.equals(getPackageName())) return false;

        String lowerPkg = pkg.toLowerCase();
        return lowerPkg.contains("whatsapp") ||
                lowerPkg.contains("instagram") ||
                lowerPkg.contains("facebook") ||
                lowerPkg.contains("tiktok") ||
                lowerPkg.contains("musically") ||
                lowerPkg.contains("snapchat") ||
                lowerPkg.contains("youtube") ||
                lowerPkg.contains("aweme");
    }

    private String getAppNameFromPackage(String pkg) {
        if (pkg == null) return "Distracting App";
        String lower = pkg.toLowerCase();
        if (lower.contains("whatsapp")) return "WhatsApp";
        if (lower.contains("instagram")) return "Instagram";
        if (lower.contains("facebook")) return "Facebook";
        if (lower.contains("tiktok") || lower.contains("musically") || lower.contains("aweme")) return "TikTok";
        if (lower.contains("snapchat")) return "Snapchat";
        if (lower.contains("youtube")) return "YouTube";
        return "Distracting App";
    }

    private synchronized void showOverlay(String appName) {
        if (isOverlayShowing) return;

        showDistractionNotification();

        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (windowManager == null) return;

        overlayContainer = new FrameLayout(this);
        overlayContainer.setBackgroundColor(Color.parseColor("#E6121212"));

        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.gravity = Gravity.CENTER;
        cardParams.setMargins(48, 48, 48, 48);

        try {
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_distraction_alert, overlayContainer, false);
            overlayContainer.addView(dialogView, cardParams);

            TextView tvTitle = dialogView.findViewById(R.id.tvAlertTitle);
            TextView tvMessage = dialogView.findViewById(R.id.tvAlertMessage);
            TextView tvDistractedAppName = dialogView.findViewById(R.id.tvDistractedAppName);
            View btnContinueAnyway = dialogView.findViewById(R.id.btnContinueAnyway);
            View btnBackToStudy = dialogView.findViewById(R.id.btnBackToStudy);

            if (tvTitle != null) tvTitle.setText("Focus on study");
            if (tvDistractedAppName != null) tvDistractedAppName.setText(appName);
            if (tvMessage != null) tvMessage.setText("You opened " + appName + " while your focus session is active. Please return to your study session.");

            if (btnBackToStudy instanceof TextView) {
                ((TextView) btnBackToStudy).setText("Back to Study");
            }
            if (btnContinueAnyway instanceof TextView) {
                ((TextView) btnContinueAnyway).setText("Deny");
            }

            if (btnContinueAnyway != null) {
                btnContinueAnyway.setOnClickListener(v -> {
                    dismissOverlay();
                });
            }

            if (btnBackToStudy != null) {
                btnBackToStudy.setOnClickListener(v -> {
                    dismissOverlay();
                    lastDistractionAlertTime = 0; // Reset cooldown so distraction alerts block instantly on next exit

                    Intent intent = new Intent(FocusService.this, FocusSessionActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                });
            }

            int layoutType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                    WindowManager.LayoutParams.TYPE_PHONE;

            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    layoutType,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                    PixelFormat.TRANSLUCENT
            );

            windowManager.addView(overlayContainer, params);
            isOverlayShowing = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized void dismissOverlay() {
        if (!isOverlayShowing || overlayContainer == null) return;
        try {
            WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            if (windowManager != null && overlayContainer.getParent() != null) {
                windowManager.removeView(overlayContainer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            overlayContainer = null;
            isOverlayShowing = false;
            lastDistractionAlertTime = System.currentTimeMillis();
        }
    }

    private synchronized void recordDistraction(String appName) {
        distractionCount++;
        if (!clickedAppsList.contains(appName)) {
            clickedAppsList.add(appName);
        }
        if (listener != null) {
            listener.onDistractionUpdated(distractionCount, appName);
        }

        // Persist immediately to the database
        final long sessionId = currentSessionId;
        final int count = distractionCount;
        StringBuilder appsBuilder = new StringBuilder();
        for (int i = 0; i < clickedAppsList.size(); i++) {
            appsBuilder.append(clickedAppsList.get(i));
            if (i < clickedAppsList.size() - 1) {
                appsBuilder.append(", ");
            }
        }
        final String appsStr = appsBuilder.toString();

        AppDatabase database = AppDatabase.getDatabase(this);
        AppDatabase.databaseWriteExecutor.execute(() -> {
            if (sessionId != -1) {
                database.focusSessionDao().updateDistractions(sessionId, count, appsStr);
            }
        });
    }

    private void showDistractionNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) return;

        Intent notificationIntent = new Intent(this, FocusSessionActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_timer)
                .setContentTitle("Focus Session Active")
                .setContentText("Return to your study session to stay productive.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    @Override
    public void onDestroy() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        stopDistractionChecking();
        dismissOverlay();

        // Mark current session as Interrupted if still active
        final long sessionId = currentSessionId;
        if (sessionId != -1) {
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            String endTime = timeFormat.format(new Date());
            long elapsedFocusSeconds = totalDurationSeconds - totalSecondsRemaining;
            double pct = Math.max(0, 100 - (distractionCount * 8.0));
            AppDatabase database = AppDatabase.getDatabase(this);
            AppDatabase.databaseWriteExecutor.execute(() -> {
                database.focusSessionDao().completeSession(sessionId, endTime, elapsedFocusSeconds, pct, "Interrupted");
            });
        }

        staticListener = null;
        instance = null;
        super.onDestroy();
    }
}
