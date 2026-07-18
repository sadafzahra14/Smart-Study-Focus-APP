package com.example;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "SmartStudyPrefs";
    private static final String KEY_LOGGED_IN_EMAIL = "loggedInEmail";
    private static final String KEY_LOGGED_IN_NAME = "loggedInName";
    private static final String KEY_DARK_MODE = "darkMode";
    private static final String KEY_NOTIFICATIONS = "notifications";
    private static final String KEY_REMINDER_TIME = "reminderTime";
    private static final String KEY_FOCUS_STREAK = "focusStreak";

    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void createLoginSession(String email, String name) {
        editor.putString(KEY_LOGGED_IN_EMAIL, email);
        editor.putString(KEY_LOGGED_IN_NAME, name);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return getLoggedInEmail() != null;
    }

    public String getLoggedInEmail() {
        return pref.getString(KEY_LOGGED_IN_EMAIL, null);
    }

    public String getLoggedInName() {
        return pref.getString(KEY_LOGGED_IN_NAME, "Focused Learner");
    }

    public void setLoggedInName(String name) {
        editor.putString(KEY_LOGGED_IN_NAME, name);
        editor.apply();
    }

    public void logoutUser() {
        editor.remove(KEY_LOGGED_IN_EMAIL);
        editor.remove(KEY_LOGGED_IN_NAME);
        editor.apply();
    }

    public boolean isDarkMode() {
        return pref.getBoolean(KEY_DARK_MODE, false);
    }

    public void setDarkMode(boolean isDark) {
        editor.putBoolean(KEY_DARK_MODE, isDark);
        editor.apply();
    }

    public boolean isNotificationsEnabled() {
        return pref.getBoolean(KEY_NOTIFICATIONS, true);
    }

    public void setNotificationsEnabled(boolean isEnabled) {
        editor.putBoolean(KEY_NOTIFICATIONS, isEnabled);
        editor.apply();
    }

    public String getReminderTime() {
        return pref.getString(KEY_REMINDER_TIME, "09:00 AM");
    }

    public void setReminderTime(String time) {
        editor.putString(KEY_REMINDER_TIME, time);
        editor.apply();
    }

    public int getFocusStreak() {
        return pref.getInt(KEY_FOCUS_STREAK, 5); // default streak of 5 days for onboarding
    }

    public void incrementFocusStreak() {
        editor.putInt(KEY_FOCUS_STREAK, getFocusStreak() + 1);
        editor.apply();
    }
}
