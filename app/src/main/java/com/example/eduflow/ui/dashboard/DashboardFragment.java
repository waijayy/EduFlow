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

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when fragment becomes visible
        fetchData();
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
                    // Convert hours to minutes (totalHours is in hours, multiply by 60)
                    int totalMinutes = (int) (stats.totalHours * 60);
                    binding.tvTotalHours.setText(String.format("%dm", totalMinutes));
                    binding.tvAvgPerDay.setText(String.valueOf(stats.totalVideosWatched));
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
        SupabaseManager.getUserAchievements(achievements -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (binding == null)
                        return;

                    // Convert flat list to grouped list
                    List<BadgeAdapter.BadgeGroup> groups = new ArrayList<>();

                    // Group 1: Quiz Achievements
                    List<BadgeAdapter.Badge> quizBadges = new ArrayList<>();
                    for (SupabaseManager.Achievement ach : achievements) {
                        if (ach.type.equals("first_quiz") || ach.type.equals("perfect_score")) {
                            quizBadges.add(new BadgeAdapter.Badge(ach.icon, ach.title, ach.isUnlocked));
                        }
                    }
                    if (quizBadges.size() == 2) {
                        groups.add(new BadgeAdapter.BadgeGroup("🎓 Quiz Achievements", quizBadges));
                    }

                    // Group 2: Video Achievements
                    List<BadgeAdapter.Badge> videoBadges = new ArrayList<>();
                    for (SupabaseManager.Achievement ach : achievements) {
                        if (ach.type.equals("first_video") || ach.type.equals("ten_videos")) {
                            videoBadges.add(new BadgeAdapter.Badge(ach.icon, ach.title, ach.isUnlocked));
                        }
                    }
                    if (videoBadges.size() == 2) {
                        groups.add(new BadgeAdapter.BadgeGroup("🎬 Video Achievements", videoBadges));
                    }

                    // Group 3: Streak Achievements
                    List<BadgeAdapter.Badge> streakBadges = new ArrayList<>();
                    for (SupabaseManager.Achievement ach : achievements) {
                        if (ach.type.equals("seven_day_streak") || ach.type.equals("thirty_day_streak")) {
                            streakBadges.add(new BadgeAdapter.Badge(ach.icon, ach.title, ach.isUnlocked));
                        }
                    }
                    if (streakBadges.size() == 2) {
                        groups.add(new BadgeAdapter.BadgeGroup("🔥 Streak Achievements", streakBadges));
                    }

                    BadgeAdapter adapter = new BadgeAdapter(groups);
                    binding.rvBadges.setLayoutManager(new LinearLayoutManager(getContext()));
                    binding.rvBadges.setAdapter(adapter);
                });
            }
        });
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
