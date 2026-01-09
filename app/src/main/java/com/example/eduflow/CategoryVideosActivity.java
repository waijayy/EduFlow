package com.example.eduflow;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.eduflow.adapters.VideoAdapter;
import com.example.eduflow.models.Video;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity to display videos for a specific category.
 */
public class CategoryVideosActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY_NAME = "extra_category_name";

    private ViewPager2 viewPagerVideos;
    private VideoAdapter videoAdapter;
    private TextView tvEmptyState;
    private String categoryName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_videos);

        categoryName = getIntent().getStringExtra(EXTRA_CATEGORY_NAME);

        setupViews();
        loadCategoryVideos();
    }

    private void setupViews() {
        // Category overlay text
        TextView tvCategoryOverlay = findViewById(R.id.tvCategoryOverlay);
        tvCategoryOverlay.setText(categoryName != null ? categoryName : "Videos");

        // Back button
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // ViewPager
        viewPagerVideos = findViewById(R.id.viewPagerVideos);
        viewPagerVideos.setOrientation(ViewPager2.ORIENTATION_VERTICAL);

        // Empty state
        tvEmptyState = findViewById(R.id.tvEmptyState);
    }

    private void loadCategoryVideos() {
        List<Video> videos = loadVideosFromCategory(categoryName);

        if (videos.isEmpty()) {
            viewPagerVideos.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            viewPagerVideos.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);

            videoAdapter = new VideoAdapter(videos, videoId -> {
                // Quiz navigation placeholder
            });
            viewPagerVideos.setAdapter(videoAdapter);

            // Manage video lifecycle
            viewPagerVideos.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    super.onPageSelected(position);
                    videoAdapter.playVideoAt(position);
                }
            });

            // Auto-play first video after a short delay
            viewPagerVideos.post(() -> {
                if (videoAdapter != null) {
                    videoAdapter.playVideoAt(0);
                }
            });
        }
    }

    private List<Video> loadVideosFromCategory(String category) {
        List<Video> videos = new ArrayList<>();
        if (category == null) {
            android.util.Log.e("CategoryVideos", "Category is null");
            return videos;
        }

        String categoryPrefix = category.toLowerCase();
        android.util.Log.d("CategoryVideos",
                "Loading videos for category: " + category + " (prefix: " + categoryPrefix + ")");

        try {
            Field[] fields = R.raw.class.getFields();
            android.util.Log.d("CategoryVideos", "Found " + fields.length + " raw resources");

            for (Field field : fields) {
                String resourceName = field.getName();
                android.util.Log.d("CategoryVideos", "Checking resource: " + resourceName);

                if (resourceName.startsWith(categoryPrefix + "_")) {
                    android.util.Log.d("CategoryVideos", "Match found: " + resourceName);
                    int resourceId = field.getInt(null);
                    String videoUrl = "android.resource://" + getPackageName() + "/" + resourceId;

                    // Extract video number from resource name (e.g., "math_vid1" -> "1")
                    String[] parts = resourceName.split("_");
                    String videoNumber = parts.length > 1 ? parts[parts.length - 1].replaceAll("[^0-9]", "") : "1";

                    // Generate stable video ID: category_number (e.g., "math_1", "photography_2")
                    String videoId = categoryPrefix + "_" + videoNumber;

                    // Generate title from resource name
                    String title = generateTitle(category, resourceName);

                    // Generate tags for the video
                    List<String> tags = generateTags(category);

                    Video video = new Video(
                            videoId, // Stable ID matching HomeFragment format
                            title,
                            "EduFlow",
                            "", // thumbnailUrl
                            videoUrl,
                            (int) (Math.random() * 1000),
                            (int) (Math.random() * 100),
                            tags,
                            category);
                    videos.add(video);
                }
            }
        } catch (Exception e) {
            android.util.Log.e("CategoryVideos", "Error loading videos", e);
        }

        android.util.Log.d("CategoryVideos", "Loaded " + videos.size() + " videos for " + category);
        return videos;
    }

    private String generateTitle(String category, String resourceName) {
        // Extract number from resource name (e.g., "photography_vid1" -> 1)
        String[] parts = resourceName.split("_");
        String number = parts.length > 1 ? parts[parts.length - 1].replaceAll("[^0-9]", "") : "1";

        // Generate titles based on category
        switch (category.toLowerCase()) {
            case "photography":
                return "Photography Composition Tutorial " + number;
            case "math":
                return "Advanced Mathematics Lesson " + number;
            case "programming":
                return "Coding Fundamentals Part " + number;
            case "english":
                return "English Grammar Lesson " + number;
            case "business":
                return "Business Strategy Guide " + number;
            case "exercise":
                return "Fitness Workout Session " + number;
            default:
                return category + " Tutorial " + number;
        }
    }

    private List<String> generateTags(String category) {
        List<String> tags = new ArrayList<>();

        switch (category.toLowerCase()) {
            case "photography":
                tags.add("photography");
                tags.add("camera");
                tags.add("composition");
                break;
            case "math":
                tags.add("mathematics");
                tags.add("algebra");
                tags.add("tutorial");
                break;
            case "programming":
                tags.add("coding");
                tags.add("programming");
                tags.add("tutorial");
                break;
            case "english":
                tags.add("english");
                tags.add("grammar");
                tags.add("language");
                break;
            case "business":
                tags.add("business");
                tags.add("strategy");
                tags.add("management");
                break;
            case "exercise":
                tags.add("fitness");
                tags.add("workout");
                tags.add("health");
                break;
            default:
                tags.add(category.toLowerCase());
                tags.add("tutorial");
        }

        return tags;
    }

    private String formatTitle(String resourceName) {
        // Convert "category_vid1" to "Category Vid 1"
        return resourceName.replace("_", " ")
                .substring(0, 1).toUpperCase() +
                resourceName.replace("_", " ").substring(1);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoAdapter != null) {
            videoAdapter.pauseAllVideos();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoAdapter != null) {
            videoAdapter.releaseAllPlayers();
        }
    }
}
