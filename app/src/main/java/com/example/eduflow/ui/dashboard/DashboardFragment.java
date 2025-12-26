package com.example.eduflow.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.eduflow.R;
import com.example.eduflow.databinding.FragmentDashboardBinding;

import java.util.ArrayList;
import java.util.List;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupStats();
        setupWeeklyProgress();
    }

    private void setupStats() {
        // Fetch stats from Supabase
        com.example.eduflow.auth.SupabaseManager.getQuizStats((quizzesDone, avgScore) -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (binding != null) {
                        binding.tvQuizzesDone.setText(String.valueOf(quizzesDone));
                        binding.tvAvgScore.setText(avgScore + "%");
                    }
                });
            }
        });

        // Mock data for others for now
        binding.tvVideosWatched.setText("47");
        binding.tvDayStreak.setText("12 days");
    }

    private void setupWeeklyProgress() {
        // Mock weekly progress data
        List<Triple> weeklyData = new ArrayList<>();
        weeklyData.add(new Triple("Mon", 66, R.id.progressMon));
        weeklyData.add(new Triple("Tue", 31, R.id.progressTue));
        weeklyData.add(new Triple("Wed", 85, R.id.progressWed));
        weeklyData.add(new Triple("Thu", 45, R.id.progressThu));
        weeklyData.add(new Triple("Fri", 72, R.id.progressFri));
        weeklyData.add(new Triple("Sat", 58, R.id.progressSat));
        weeklyData.add(new Triple("Sun", 40, R.id.progressSun));

        for (Triple data : weeklyData) {
            View progressView = binding.getRoot().findViewById(data.viewId);
            if (progressView != null) {
                TextView tvDay = progressView.findViewById(R.id.tvDay);
                if (tvDay != null)
                    tvDay.setText(data.day);

                ProgressBar progressBar = progressView.findViewById(R.id.progressBar);
                if (progressBar != null)
                    progressBar.setProgress(data.progress);

                TextView tvPercent = progressView.findViewById(R.id.tvPercent);
                if (tvPercent != null)
                    tvPercent.setText(data.progress + "%");
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // Helper class for Triple
    private static class Triple {
        String day;
        int progress;
        int viewId;

        Triple(String day, int progress, int viewId) {
            this.day = day;
            this.progress = progress;
            this.viewId = viewId;
        }
    }
}
