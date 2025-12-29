package com.example.eduflow.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eduflow.R;
import com.example.eduflow.auth.SupabaseManager;
import com.example.eduflow.databinding.FragmentDashboardBinding;

import java.util.ArrayList;
import java.util.List;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private boolean isHistoryExpanded = false;

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

        setupClickListeners();
        fetchData();
        setupQuizHistory();
    }

    private void setupClickListeners() {
        binding.headerQuizHistory.setOnClickListener(v -> toggleHistory());
    }

    private void toggleHistory() {
        isHistoryExpanded = !isHistoryExpanded;
        binding.rvQuizHistory.setVisibility(isHistoryExpanded ? View.VISIBLE : View.GONE);
        binding.ivExpand.setImageResource(isHistoryExpanded ? R.drawable.ic_arrow_up : R.drawable.ic_arrow_down);
    }

    private void fetchData() {
        SupabaseManager.getDashboardStats(stats -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (binding == null)
                        return;

                    // Time Stats
                    binding.tvTotalHours.setText(String.format("%.1fh", stats.totalHours));
                    binding.tvAvgPerDay.setText(String.format("%.1fh", stats.avgHoursPerDay));
                    binding.tvStreak.setText(String.valueOf(stats.streakDays));

                    // Quiz Stats
                    binding.tvBestScore.setText(stats.bestScore + "%");
                    binding.tvAvgScore.setText(stats.avgScore + "%");
                    binding.tvLowestScore.setText(stats.lowestScore + "%");

                    // Trend Graph
                    binding.trendGraph.setScores(stats.recentScores);

                    // Badges
                    setupBadges(stats);
                });
            }
        });
    }

    private void setupBadges(SupabaseManager.DashboardStats stats) {
        List<BadgeAdapter.Badge> badges = new ArrayList<>();

        // Logic for badges
        badges.add(new BadgeAdapter.Badge("🏅", "10 Videos Watched", stats.totalHours > 2.0)); // Mock logic
        badges.add(new BadgeAdapter.Badge("🧠", "First Quiz Completed", stats.quizzesDone >= 1));
        badges.add(new BadgeAdapter.Badge("🔥", "7-Day Streak", stats.streakDays >= 7));
        badges.add(new BadgeAdapter.Badge("💯", "Perfect Score", stats.bestScore == 100));
        badges.add(new BadgeAdapter.Badge("🎯", "50 Videos Watched", false)); // Locked mock
        badges.add(new BadgeAdapter.Badge("⚡", "30-Day Streak", stats.streakDays >= 30));

        BadgeAdapter adapter = new BadgeAdapter(badges);
        binding.rvBadges.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.rvBadges.setAdapter(adapter);
    }

    private void setupQuizHistory() {
        SupabaseManager.getQuizHistory(quizzes -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (binding == null)
                        return;

                    QuizHistoryAdapter adapter = new QuizHistoryAdapter(quizzes);
                    binding.rvQuizHistory.setLayoutManager(new LinearLayoutManager(getContext()));
                    binding.rvQuizHistory.setAdapter(adapter);
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
