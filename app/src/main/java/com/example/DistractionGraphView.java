package com.example;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

public class DistractionGraphView extends View {

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // App counts
    private int whatsappCount = 0;
    private int instagramCount = 0;
    private int facebookCount = 0;
    private int tiktokCount = 0;
    private int youtubeCount = 0;

    // Animation progress
    private float animationProgress = 0f;

    // App labels
    private final String[] apps = {"WhatsApp", "Instagram", "Facebook", "TikTok", "YouTube"};
    
    // Colors for the apps
    private final int[] appColors = {
            Color.parseColor("#4CAF50"), // Green
            Color.parseColor("#EC407A"), // Pink
            Color.parseColor("#1877F2"), // Blue
            Color.parseColor("#9C27B0"), // Purple
            Color.parseColor("#E53935")  // Red
    };

    public DistractionGraphView(Context context) {
        super(context);
        init();
    }

    public DistractionGraphView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DistractionGraphView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Configure text paint
        textPaint.setColor(Color.parseColor("#555555"));
        textPaint.setTextSize(spToPx(11));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        // Configure line paint
        linePaint.setColor(Color.parseColor("#E0E0E0"));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dpToPx(1));
        linePaint.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0));
    }

    public void setAppCounts(int whatsapp, int instagram, int facebook, int tiktok, int youtube) {
        this.whatsappCount = whatsapp;
        this.instagramCount = instagram;
        this.facebookCount = facebook;
        this.tiktokCount = tiktok;
        this.youtubeCount = youtube;
        
        startAnimation();
    }

    private void startAnimation() {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1200);
        animator.setInterpolator(new DecelerateInterpolator(1.5f));
        animator.addUpdateListener(animation -> {
            animationProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        int paddingLeft = getPaddingLeft() + dpToPx(20);
        int paddingRight = getPaddingRight() + dpToPx(20);
        int paddingTop = getPaddingTop() + dpToPx(30);
        int paddingBottom = getPaddingBottom() + dpToPx(30);

        int graphWidth = width - paddingLeft - paddingRight;
        int graphHeight = height - paddingTop - paddingBottom;

        int[] counts = {whatsappCount, instagramCount, facebookCount, tiktokCount, youtubeCount};

        // Determine max value for scaling
        int maxVal = 4; // default minimum scale roof
        for (int count : counts) {
            if (count > maxVal) {
                maxVal = count;
            }
        }

        // Draw horizontal grid lines and markers
        linePaint.setAlpha(80);
        for (int i = 0; i <= 4; i++) {
            float ratio = i / 4.0f;
            float y = paddingTop + graphHeight * (1f - ratio);
            canvas.drawLine(paddingLeft, y, paddingLeft + graphWidth, y, linePaint);
            
            // Draw axis markers on left
            Paint markerPaint = new Paint(textPaint);
            markerPaint.setColor(Color.parseColor("#888888"));
            markerPaint.setTextSize(spToPx(10));
            markerPaint.setTextAlign(Paint.Align.RIGHT);
            int valueLabel = Math.round(maxVal * ratio);
            canvas.drawText(String.valueOf(valueLabel), paddingLeft - dpToPx(6), y + dpToPx(3), markerPaint);
        }

        // Draw the 5 bars
        int numBars = counts.length;
        float barWidth = graphWidth / (numBars * 1.8f);
        float spacing = (graphWidth - (barWidth * numBars)) / (numBars - 1 + 2); // left/right offset padding included

        float startX = paddingLeft + spacing;

        for (int i = 0; i < numBars; i++) {
            float x = startX + i * (barWidth + spacing);
            float countVal = counts[i];

            // Animate scale height
            float animatedValue = countVal * animationProgress;
            float ratio = (maxVal > 0) ? (animatedValue / maxVal) : 0f;
            float barHeight = graphHeight * ratio;

            float yTop = paddingTop + graphHeight - barHeight;
            float yBottom = paddingTop + graphHeight;

            // Draw shadow or glow behind bar
            RectF barRect = new RectF(x, yTop, x + barWidth, yBottom);
            barPaint.setColor(appColors[i]);
            
            // Draw rounded bar (top-rounded only or fully rounded with minimum height)
            if (barHeight > dpToPx(4)) {
                canvas.drawRoundRect(barRect, dpToPx(8), dpToPx(8), barPaint);
                // Cover the bottom rounding to keep bottom flat if we want, or keep beautiful capsule
                RectF bottomFlatCover = new RectF(x, yBottom - dpToPx(8), x + barWidth, yBottom);
                canvas.drawRect(bottomFlatCover, barPaint);
            } else if (barHeight > 0) {
                canvas.drawRect(barRect, barPaint);
            }

            // Draw value count text on top of the bar
            String countText = String.valueOf(counts[i]);
            textPaint.setColor(Color.parseColor("#333333"));
            textPaint.setTextSize(spToPx(12));
            canvas.drawText(countText, x + barWidth / 2f, yTop - dpToPx(6), textPaint);

            // Draw app label at bottom
            textPaint.setColor(Color.parseColor("#666666"));
            textPaint.setTextSize(spToPx(10));
            // Truncate label if too long
            String displayLabel = apps[i];
            if (displayLabel.length() > 9) {
                displayLabel = displayLabel.substring(0, 7) + "..";
            }
            canvas.drawText(displayLabel, x + barWidth / 2f, yBottom + dpToPx(16), textPaint);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private int spToPx(int sp) {
        return (int) (sp * getResources().getDisplayMetrics().scaledDensity);
    }
}
