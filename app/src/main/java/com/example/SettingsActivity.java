package com.example;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.databinding.ActivitySettingsBinding;

import java.util.Calendar;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        sessionManager = new SessionManager(this);

        // Load values
        binding.switchNotifications.setChecked(sessionManager.isNotificationsEnabled());
        binding.switchDarkMode.setChecked(sessionManager.isDarkMode());
        binding.btnSetReminderTime.setText(sessionManager.getReminderTime());

        // Listeners
        binding.switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sessionManager.setNotificationsEnabled(isChecked);
            Toast.makeText(this, isChecked ? "Notifications enabled" : "Notifications disabled", Toast.LENGTH_SHORT).show();
        });

        binding.switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sessionManager.setDarkMode(isChecked);
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
            Toast.makeText(this, "Dark mode updated", Toast.LENGTH_SHORT).show();
        });

        binding.btnSetReminderTime.setOnClickListener(v -> openTimePickerDialog());
    }

    private void openTimePickerDialog() {
        // Parse current preference time (default 09:00 AM)
        String currentPref = sessionManager.getReminderTime();
        int defaultHour = 9;
        int defaultMinute = 0;

        try {
            String[] parts = currentPref.split(":");
            defaultHour = Integer.parseInt(parts[0].trim());
            String[] minAndAmPm = parts[1].split(" ");
            defaultMinute = Integer.parseInt(minAndAmPm[0].trim());
            if ("PM".equalsIgnoreCase(minAndAmPm[1].trim()) && defaultHour < 12) {
                defaultHour += 12;
            } else if ("AM".equalsIgnoreCase(minAndAmPm[1].trim()) && defaultHour == 12) {
                defaultHour = 0;
            }
        } catch (Exception ignored) {}

        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    String amPm = "AM";
                    int hourDisplay = hourOfDay;
                    if (hourOfDay >= 12) {
                        amPm = "PM";
                        if (hourOfDay > 12) {
                            hourDisplay = hourOfDay - 12;
                        }
                    } else if (hourOfDay == 0) {
                        hourDisplay = 12;
                    }

                    String formattedTime = String.format(Locale.getDefault(), "%02d:%02d %s", hourDisplay, minute, amPm);
                    sessionManager.setReminderTime(formattedTime);
                    binding.btnSetReminderTime.setText(formattedTime);
                    Toast.makeText(this, "Daily study reminder set to " + formattedTime, Toast.LENGTH_SHORT).show();
                }, defaultHour, defaultMinute, false);

        timePickerDialog.show();
    }
}
