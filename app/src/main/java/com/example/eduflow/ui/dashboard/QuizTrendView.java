package com.example.eduflow.ui.dashboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuizTrendView extends View {

    private Paint linePaint;
    private Paint fillPaint;
    private Path path;
    private List<Integer> scores;
    private int lineColor = Color.parseColor("#00D9FF"); // Neon Cyan

    public QuizTrendView(Context context) {
        super(context);
        init();
    }

    public QuizTrendView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public QuizTrendView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private Paint textPaint;

    private void init() {
        linePaint = new Paint();
        linePaint.setColor(lineColor);
        linePaint.setStrokeWidth(8f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setAntiAlias(true);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        fillPaint = new Paint();
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(32f);
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);

        path = new Path();
        scores = new ArrayList<>();
    }

    public void setScores(List<Integer> newScores) {
        this.scores = newScores;
        invalidate(); // Redraw
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (scores == null || scores.isEmpty())
            return;

        int width = getWidth();
        int height = getHeight();
        int padding = 40; // Side/Top padding
        int bottomPadding = 80; // Extra space for text

        int drawWidth = width - (padding * 2);
        int drawHeight = height - padding - bottomPadding;

        // Find min/max to normalize
        int min = 0;
        int max = 100;

        float xStep = drawWidth / (float) (Math.max(scores.size() - 1, 1));

        path.reset();

        // Construct path
        for (int i = 0; i < scores.size(); i++) {
            float x = padding + (i * xStep);

            // Invert Y because canvas 0 is top
            float normalizedScore = (float) scores.get(i) / max;
            float y = height - bottomPadding - (normalizedScore * drawHeight);

            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }

            // Draw points
            canvas.drawCircle(x, y, 8f, linePaint);

            // Draw axis label (1-based index)
            canvas.drawText(String.valueOf(i + 1), x, height - 20, textPaint);
        }

        // Draw properties
        canvas.drawPath(path, linePaint);

        // Fill below line with gradient
        // Close the path to the bottom
        Path fillPath = new Path(path);
        fillPath.lineTo(padding + ((scores.size() - 1) * xStep), height - bottomPadding);
        fillPath.lineTo(padding, height - bottomPadding);
        fillPath.close();

        fillPaint.setShader(new LinearGradient(0, 0, 0, height,
                Color.parseColor("#4D00D9FF"), // Transparent Cyan
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP));

        canvas.drawPath(fillPath, fillPaint);
    }
}
