package com.example;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import com.example.databinding.ActivityStudyWebBinding;

public class StudyWebActivity extends AppCompatActivity {

    private ActivityStudyWebBinding binding;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStudyWebBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get query and url from intent
        String query = getIntent().getStringExtra("query");
        String url = getIntent().getStringExtra("url");

        if (query == null) query = "";
        if (url == null || url.isEmpty()) {
            url = "https://www.google.com";
        }

        binding.txtWebSubtitle.setText(query);

        // Set up Back Button
        binding.btnWebBack.setOnClickListener(v -> handleBackNavigation());

        // Configure WebView with robust settings for modern web search (e.g., Google)
        binding.webViewStudy.getSettings().setJavaScriptEnabled(true);
        binding.webViewStudy.getSettings().setDomStorageEnabled(true);
        binding.webViewStudy.getSettings().setDatabaseEnabled(true);
        binding.webViewStudy.getSettings().setSupportZoom(true);
        binding.webViewStudy.getSettings().setBuiltInZoomControls(true);
        binding.webViewStudy.getSettings().setDisplayZoomControls(false);
        binding.webViewStudy.getSettings().setLoadsImagesAutomatically(true);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            binding.webViewStudy.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        // Set modern Chrome user agent to prevent Google search from blocking or serving a degraded blank layout
        String chromeUserAgent = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Mobile Safari/537.36";
        binding.webViewStudy.getSettings().setUserAgentString(chromeUserAgent);

        // Force open links inside WebView itself naturally
        binding.webViewStudy.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // Return false so that the WebView itself handles the loading of the URL naturally,
                // preventing infinite redirect loops or empty blanks caused by manually overriding.
                return false;
            }

            @Deprecated
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                binding.progressWeb.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                binding.progressWeb.setVisibility(View.GONE);
            }
        });

        // Track page loading progress
        binding.webViewStudy.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                binding.progressWeb.setProgress(newProgress);
                if (newProgress == 100) {
                    binding.progressWeb.setVisibility(View.GONE);
                } else {
                    binding.progressWeb.setVisibility(View.VISIBLE);
                }
            }
        });

        // Load study URL
        binding.webViewStudy.loadUrl(url);

        // --- Modern AndroidX Back Press Callback ---
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackNavigation();
            }
        });
    }

    private void handleBackNavigation() {
        if (binding.webViewStudy.canGoBack()) {
            binding.webViewStudy.goBack();
        } else {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }

    @Override
    protected void onDestroy() {
        if (binding != null && binding.webViewStudy != null) {
            binding.webViewStudy.stopLoading();
            binding.webViewStudy.destroy();
        }
        super.onDestroy();
    }
}
