package com.example.eduflow.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.eduflow.QuizActivity;
import com.example.eduflow.R;
import com.example.eduflow.adapters.VideoAdapter;
import com.example.eduflow.databinding.FragmentHomeBinding;
import com.example.eduflow.models.Video;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private VideoAdapter videoAdapter;

    // Category configurations: folder name -> display info
    private static final String[][] CATEGORIES = {
            { "math", "Math", "@math_tutor" },
            { "photography", "Photography", "@photo_pro" },
            { "programming", "Programming", "@code_master" },
            { "business", "Business", "@biz_guru" },
            { "design", "Design", "@design_hub" },
            { "english", "English", "@english_ace" }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupVideoFeed();
    }

    private void setupVideoFeed() {
        // Load videos dynamically from raw folder categories
        List<Video> videos = loadVideosFromRawCategories();

        // Shuffle videos for a mixed "For You" feed
        Collections.shuffle(videos);

        videoAdapter = new VideoAdapter(videos, this::navigateToQuiz);

        binding.viewPagerVideos.setAdapter(videoAdapter);
        binding.viewPagerVideos.setOrientation(ViewPager2.ORIENTATION_VERTICAL);

        // Page change callback for video playback management
        binding.viewPagerVideos.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                videoAdapter.playVideoAt(position);
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (videoAdapter != null) {
            videoAdapter.saveAllWatchData(); // Save all watch data before pausing
            videoAdapter.pauseAllVideos();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (videoAdapter != null && binding != null) {
            videoAdapter.playVideoAt(binding.viewPagerVideos.getCurrentItem());
        }
    }

    private void navigateToQuiz(Video video) {
        if (getContext() != null) {
            Intent intent = new Intent(getContext(), QuizActivity.class);
            intent.putExtra(QuizActivity.EXTRA_VIDEO_ID, video.getId());
            intent.putExtra(QuizActivity.EXTRA_VIDEO_TITLE, video.getTitle());
            startActivity(intent);
        }
    }

    private List<Video> loadVideosFromRawCategories() {
        List<Video> videos = new ArrayList<>();
        Random random = new Random();

        if (getContext() == null)
            return videos;

        String packageName = getContext().getPackageName();

        for (String[] categoryInfo : CATEGORIES) {
            String folderName = categoryInfo[0];
            String displayName = categoryInfo[1];
            String author = categoryInfo[2];

            // Check for vid1, vid2, vid3, etc. in each category
            for (int i = 1; i <= 10; i++) {
                String resourceName = folderName + "_vid" + i;
                int resourceId = getContext().getResources().getIdentifier(resourceName, "raw", packageName);

                if (resourceId != 0) {
                    String videoUri = "android.resource://" + packageName + "/" + resourceId;
                    // Use category_number format for stable video IDs
                    String videoId = folderName + "_" + i;

                    videos.add(new Video(
                            videoId, // Stable ID: "math_1", "photography_2", etc.
                            displayName + " Tutorial #" + i,
                            author,
                            "",
                            videoUri,
                            random.nextInt(10000) + 1000, // Random likes
                            random.nextInt(500) + 50, // Random comments
                            Arrays.asList(displayName, "Learning", "EduFlow"),
                            folderName));
                }
            }
        }

        return videos;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (videoAdapter != null) {
            videoAdapter.saveAllWatchData(); // Save all watch data before destroying
            videoAdapter.releaseAllPlayers();
        }
        binding = null;
    }
}
