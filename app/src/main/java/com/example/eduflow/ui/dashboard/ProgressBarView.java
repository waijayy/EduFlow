package com.example.eduflow.ui.dashboard;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.eduflow.R;

public class ProgressBarView extends LinearLayout {

    private TextView tvDay;
    private ProgressBar progressBar;
    private TextView tvPercent;

    public ProgressBarView(Context context) {
        this(context, null);
    }

    public ProgressBarView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ProgressBarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.item_progress_bar, this, true);
        setOrientation(HORIZONTAL);

        tvDay = findViewById(R.id.tvDay);
        progressBar = findViewById(R.id.progressBar);
        tvPercent = findViewById(R.id.tvPercent);
    }

    public void setDay(String day) {
        if (tvDay != null) {
            tvDay.setText(day);
        }
    }

    public int getProgress() {
        return progressBar != null ? progressBar.getProgress() : 0;
    }

    public void setProgress(int value) {
        if (progressBar != null) {
            progressBar.setProgress(value);
        }
    }
}
