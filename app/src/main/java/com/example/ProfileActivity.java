package com.example;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.database.User;
import com.example.databinding.ActivityProfileBinding;
import com.example.viewmodel.FocusViewModel;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private SessionManager sessionManager;
    private FocusViewModel viewModel;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        sessionManager = new SessionManager(this);
        viewModel = new ViewModelProvider(this).get(FocusViewModel.class);

        // Populate basic stats
        binding.tvFocusStreakValue.setText(sessionManager.getFocusStreak() + " Days");
        binding.etProfileEmail.setText(sessionManager.getLoggedInEmail());

        // Load details from database
        String email = sessionManager.getLoggedInEmail();
        if (email != null) {
            viewModel.getUserByEmail(email, new FocusViewModel.DatabaseCallback<User>() {
                @Override
                public void onSuccess(User result) {
                    currentUser = result;
                    runOnUiThread(() -> binding.etProfileName.setText(result.getFullName()));
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> binding.etProfileName.setText(sessionManager.getLoggedInName()));
                }
            });
        }

        // Save profile
        binding.btnSaveProfile.setOnClickListener(v -> saveProfileDetails());

        // Quick settings shortcut
        binding.btnSettingsLink.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        // Focus history shortcut
        binding.btnHistoryLink.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, HistoryActivity.class);
            startActivity(intent);
        });

        // Logout
        binding.btnLogout.setOnClickListener(v -> {
            sessionManager.logoutUser();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void saveProfileDetails() {
        String newName = binding.etProfileName.getText().toString().trim();

        if (TextUtils.isEmpty(newName)) {
            binding.nameInputLayout.setError("Name is required");
            return;
        } else {
            binding.nameInputLayout.setError(null);
        }

        if (currentUser != null) {
            currentUser.setFullName(newName);
            viewModel.updateUser(currentUser, new FocusViewModel.DatabaseCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    runOnUiThread(() -> {
                        sessionManager.setLoggedInName(newName);
                        Toast.makeText(ProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> Toast.makeText(ProfileActivity.this, message, Toast.LENGTH_LONG).show());
                }
            });
        } else {
            sessionManager.setLoggedInName(newName);
            Toast.makeText(this, "Profile cached successfully!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
}
