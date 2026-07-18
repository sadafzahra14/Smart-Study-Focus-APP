package com.example;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.databinding.ActivitySearchBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SearchActivity extends AppCompatActivity {

    private ActivitySearchBinding binding;

    // List of allowed study-related keyword categories/sub-terms
    private static final Set<String> ALLOWED_KEYWORDS = new HashSet<>(Arrays.asList(
        "programming", "java", "android", "cyber", "security", "artificial", "intelligence", "ai",
        "machine", "learning", "operating", "systems", "os", "database", "sql", "networking", "pdf",
        "notes", "assignment", "assignments", "research", "paper", "papers", "tutorial", "tutorials",
        "university", "subject", "subjects", "study", "exam", "exams", "course", "courses", "lecture",
        "lectures", "book", "books", "code", "coding", "science", "math", "mathematics", "physics",
        "chemistry", "biology", "history", "literature", "engineering", "algorithm", "algorithms",
        "data", "structure", "structures", "html", "css", "javascript", "python", "c++", "swift",
        "kotlin", "web", "cloud", "computation", "calculus", "algebra", "lesson", "lessons",
        "homework", "syllabus", "exam notes", "developer", "development",
        "how", "what", "why", "define", "example", "quiz", "class", "school", "college", "career",
        "learn", "explanation", "definition", "question", "answer", "solved", "problem", "solution",
        "git", "github", "stack", "overflow", "education", "academy", "info", "information", "topic",
        "concept", "theory", "guide", "cheat", "sheet", "summary", "notes", "quizlet"
    ));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // --- Fade In Entrance Animation ---
        Animation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(600);
        fadeIn.setInterpolator(new DecelerateInterpolator());
        binding.getRoot().startAnimation(fadeIn);

        // --- Back Button Behavior ---
        binding.btnSearchBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // --- Search "GO" Button Behavior ---
        binding.btnSearchGo.setOnClickListener(v -> triggerSearch());

        // --- Keyboard Search Action Listener ---
        binding.editSearchQuery.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                triggerSearch();
                return true;
            }
            return false;
        });

        // --- Initialize / Display Recent Searches ---
        refreshRecentSearchesView();

        // --- Modern AndroidX Back Press Callback ---
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });
    }

    private void triggerSearch() {
        String query = binding.editSearchQuery.getText().toString().trim();

        if (query.isEmpty()) {
            Toast.makeText(this, "Please enter a study topic to search.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isStudyRelated(query)) {
            // Save search to SharedPreferences
            saveRecentSearch(query);

            // Refresh recent list view
            refreshRecentSearchesView();

            // Clear input text to keep search flow fresh
            binding.editSearchQuery.setText("");

            // Create Google Search URL inside app webview
            String encodedQuery = Uri.encode(query);
            String url = "https://www.google.com/search?q=" + encodedQuery;

            Intent intent = new Intent(SearchActivity.this, StudyWebActivity.class);
            intent.putExtra("query", query);
            intent.putExtra("url", url);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } else {
            // Show restrict warning
            Toast.makeText(this, "Search is restricted to study-related educational topics only.", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Checks if the query is related to educational study topics based on keywords.
     */
    private boolean isStudyRelated(String query) {
        String lowercaseQuery = query.toLowerCase();

        // Direct containment check first
        for (String keyword : ALLOWED_KEYWORDS) {
            if (lowercaseQuery.contains(keyword)) {
                return true;
            }
        }

        // Tokenized check for individual words (e.g. "java", "os")
        String[] words = lowercaseQuery.split("[\\s\\p{Punct}]+");
        for (String word : words) {
            if (ALLOWED_KEYWORDS.contains(word)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Loads recent searches from SharedPreferences.
     */
    private List<String> getRecentSearches() {
        SharedPreferences prefs = getSharedPreferences("study_search_prefs", MODE_PRIVATE);
        String raw = prefs.getString("recent_searches", "");
        List<String> list = new ArrayList<>();
        if (!raw.isEmpty()) {
            String[] items = raw.split("\\|\\|");
            for (String item : items) {
                String trimmed = item.trim();
                if (!trimmed.isEmpty()) {
                    list.add(trimmed);
                }
            }
        }
        return list;
    }

    /**
     * Saves a query to recent searches, moving it to top and capping list at 10 items.
     */
    private void saveRecentSearch(String query) {
        if (query == null || query.trim().isEmpty()) return;
        String cleanQuery = query.trim();

        List<String> list = getRecentSearches();
        list.remove(cleanQuery); // Remove duplicate to push to top
        list.add(0, cleanQuery);  // Add as newest at index 0

        if (list.size() > 10) {
            list = list.subList(0, 10);
        }

        // Save delimited string
        SharedPreferences prefs = getSharedPreferences("study_search_prefs", MODE_PRIVATE);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            if (i < list.size() - 1) {
                sb.append("||");
            }
        }
        prefs.edit().putString("recent_searches", sb.toString()).apply();
    }

    /**
     * Updates UI dynamically based on the stored recent search list.
     */
    private void refreshRecentSearchesView() {
        List<String> list = getRecentSearches();

        if (list.isEmpty()) {
            binding.layoutEmptyState.setVisibility(View.VISIBLE);
            binding.txtRecentHeader.setVisibility(View.GONE);
            binding.layoutRecentItems.setVisibility(View.GONE);
        } else {
            binding.layoutEmptyState.setVisibility(View.GONE);
            binding.txtRecentHeader.setVisibility(View.VISIBLE);
            binding.layoutRecentItems.setVisibility(View.VISIBLE);

            // Populate the recent searches linear layout dynamically
            binding.layoutRecentItems.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(this);

            for (String term : list) {
                View itemView = inflater.inflate(R.layout.item_recent_search, binding.layoutRecentItems, false);
                TextView txtName = itemView.findViewById(R.id.txtRecentItemName);
                txtName.setText(term);

                // Re-search when recent item clicked
                itemView.setOnClickListener(v -> {
                    binding.editSearchQuery.setText(term);
                    binding.editSearchQuery.setSelection(term.length());
                    triggerSearch();
                });

                binding.layoutRecentItems.addView(itemView);
            }
        }
    }
}
