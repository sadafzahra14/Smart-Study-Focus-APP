package com.example;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.adapter.FocusSessionAdapter;
import com.example.databinding.ActivityHistoryBinding;
import com.example.viewmodel.FocusViewModel;

public class HistoryActivity extends AppCompatActivity {

    private ActivityHistoryBinding binding;
    private FocusViewModel viewModel;
    private SessionManager sessionManager;
    private FocusSessionAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        sessionManager = new SessionManager(this);
        viewModel = new ViewModelProvider(this).get(FocusViewModel.class);

        // Configure RecyclerView
        binding.recyclerViewHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FocusSessionAdapter();
        binding.recyclerViewHistory.setAdapter(adapter);

        // Fetch logs
        String email = sessionManager.getLoggedInEmail();
        if (email != null) {
            viewModel.getSessionsForUser(email).observe(this, sessions -> {
                if (sessions == null || sessions.isEmpty()) {
                    binding.recyclerViewHistory.setVisibility(View.GONE);
                    binding.layoutEmptyHistory.setVisibility(View.VISIBLE);
                } else {
                    binding.layoutEmptyHistory.setVisibility(View.GONE);
                    binding.recyclerViewHistory.setVisibility(View.VISIBLE);
                    adapter.setSessions(sessions);
                }
            });
        }
    }
}
