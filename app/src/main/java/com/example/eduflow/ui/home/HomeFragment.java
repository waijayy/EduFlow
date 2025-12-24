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
import com.example.eduflow.adapters.VideoAdapter;
import com.example.eduflow.databinding.FragmentHomeBinding;
import com.example.eduflow.models.Video;

import java.util.Arrays;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private VideoAdapter videoAdapter;

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
        // Sample videos for demo
        List<Video> videos = Arrays.asList(
                new Video(
                        "1",
                        "React Hooks Explained in 60 Seconds",
                        "@sarah_dev",
                        "",
                        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                        12500,
                        234,
                        Arrays.asList("React", "JavaScript", "WebDev")),
                new Video(
                        "2",
                        "Python List Comprehension Tutorial",
                        "@code_master",
                        "",
                        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                        8900,
                        156,
                        Arrays.asList("Python", "Programming", "Tips")),
                new Video(
                        "3",
                        "CSS Grid Layout Made Easy",
                        "@design_guru",
                        "",
                        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                        5600,
                        89,
                        Arrays.asList("CSS", "WebDesign", "Frontend")),
                new Video(
                        "4",
                        "Git Commands You Must Know",
                        "@dev_tips",
                        "",
                        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                        15200,
                        312,
                        Arrays.asList("Git", "DevTools", "Programming")),
                new Video(
                        "5",
                        "JavaScript Async/Await Explained",
                        "@js_ninja",
                        "",
                        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
                        9800,
                        201,
                        Arrays.asList("JavaScript", "Async", "WebDev")));

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (videoAdapter != null) {
            videoAdapter.releaseAllPlayers();
        }
        binding = null;
    }
}
