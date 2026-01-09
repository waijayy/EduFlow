package com.example.eduflow.adapters;

import android.content.Context;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eduflow.R;
import com.example.eduflow.databinding.ItemVideoBinding;
import com.example.eduflow.models.Video;

import com.example.eduflow.auth.SupabaseManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private final List<Video> videos;
    private final OnQuizClickListener onQuizClick;
    private final Map<Integer, ExoPlayer> players = new HashMap<>();
    private final Map<Integer, VideoWatchTracker> watchTrackers = new HashMap<>();
    private int currentPlayingPosition = -1;

    // Minimum watch duration to count as "watched" (in seconds)
    private static final int MIN_WATCH_DURATION = 5;
    // Percentage threshold to consider video "completed" (80%)
    private static final float COMPLETION_THRESHOLD = 0.8f;

    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    public interface OnQuizClickListener {
        void onQuizClick(Video video);
    }

    public VideoAdapter(List<Video> videos, OnQuizClickListener onQuizClick) {
        this.videos = videos;
        this.onQuizClick = onQuizClick;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemVideoBinding binding = ItemVideoBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false);
        return new VideoViewHolder(binding);
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        Video video = videos.get(position);

        holder.binding.tvTitle.setText(video.getTitle());
        holder.binding.tvAuthor.setText(video.getAuthor());
        holder.binding.tvLikes.setText(formatCount(video.getLikes()));
        holder.binding.tvComments.setText(formatCount(video.getComments()));

        // Setup tags
        holder.binding.tagsContainer.removeAllViews();
        for (String tag : video.getTags()) {
            View tagView = LayoutInflater.from(holder.itemView.getContext())
                    .inflate(R.layout.item_tag, holder.binding.tagsContainer, false);
            TextView tvTag = tagView.findViewById(R.id.tvTag);
            tvTag.setText("#" + tag);
            holder.binding.tagsContainer.addView(tagView);
        }

        // Initialize ExoPlayer
        Context context = holder.itemView.getContext();
        ExoPlayer player = new ExoPlayer.Builder(context).build();
        MediaItem mediaItem = MediaItem.fromUri(video.getVideoUrl());
        player.setMediaItem(mediaItem);
        player.setRepeatMode(Player.REPEAT_MODE_ONE);
        player.prepare();

        holder.player = player;
        players.put(position, player);
        holder.binding.playerView.setPlayer(player);

        // Initialize video watch tracker
        VideoWatchTracker tracker = new VideoWatchTracker(video.getId());
        watchTrackers.put(position, tracker);
        holder.watchTracker = tracker;

        // Setup player listener for tracking
        setupVideoTracking(holder, position, video);

        // Click listeners
        holder.binding.btnLike.setOnClickListener(v -> {
            holder.binding.btnLike.setSelected(!holder.binding.btnLike.isSelected());
        });

        // Note button - open notes activity
        holder.binding.btnNote.setOnClickListener(v -> {
            android.content.Context ctx = holder.itemView.getContext();
            android.content.Intent intent = new android.content.Intent(ctx,
                    com.example.eduflow.VideoNotesActivity.class);
            intent.putExtra(com.example.eduflow.VideoNotesActivity.EXTRA_VIDEO_ID, video.getId());
            intent.putExtra(com.example.eduflow.VideoNotesActivity.EXTRA_VIDEO_TITLE, video.getTitle());
            ctx.startActivity(intent);
        });

        // Swipe gesture detector for quiz navigation
        GestureDetector gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null)
                    return false;

                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

                // Check for horizontal swipe (swipe left to access quiz)
                if (Math.abs(diffX) > Math.abs(diffY) &&
                        Math.abs(diffX) > SWIPE_THRESHOLD &&
                        Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {

                    if (diffX < 0) {
                        // Swipe left - open quiz
                        onQuizClick.onQuizClick(video);
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (player.isPlaying()) {
                    player.pause();
                    holder.binding.btnPlayPause.setVisibility(View.VISIBLE);
                } else {
                    player.play();
                    holder.binding.btnPlayPause.setVisibility(View.GONE);
                }
                return true;
            }
        });

        // Set touch listener for swipe detection
        holder.binding.playerView.setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });

        // Ensure play/pause overlay click also works if gesture detector consumes it
        holder.binding.playerView.setOnClickListener(v -> {
            // handled by gesture detector
        });

        setupSeekBar(holder);
    }

    /**
     * Setup video tracking that properly handles pausing, seeking, and swiping
     */
    private void setupVideoTracking(VideoViewHolder holder, int position, Video video) {
        ExoPlayer player = holder.player;
        VideoWatchTracker tracker = holder.watchTracker;

        // Track playback state changes
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                android.util.Log.d("VideoAdapter", String.format(
                        "Playback state changed: position=%d, state=%d", position, playbackState));

                if (playbackState == Player.STATE_READY) {
                    // Video is ready, try to get duration
                    // Duration might not be available immediately, so check periodically
                    checkAndSetVideoDuration(player, tracker, video.getId());
                } else if (playbackState == Player.STATE_ENDED) {
                    // Video ended, save final watch position
                    android.util.Log.d("VideoAdapter", String.format(
                            "Video ended: videoId=%s", video.getId()));
                    // Ensure duration is set before saving
                    checkAndSetVideoDuration(player, tracker, video.getId());
                    tracker.stopTracking(); // Stop tracking first
                    tracker.updateWatchPosition((int) (player.getCurrentPosition() / 1000));
                    saveVideoWatch(tracker, video.getId(), player);
                }
            }

            @Override
            public void onMediaItemTransition(MediaItem mediaItem, int reason) {
                // Try to get duration when media item changes
                checkAndSetVideoDuration(player, tracker, video.getId());
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                android.util.Log.d("VideoAdapter", String.format(
                        "IsPlaying changed: position=%d, isPlaying=%s", position, isPlaying));

                if (isPlaying) {
                    // Video started playing
                    tracker.startTracking();
                    android.util.Log.d("VideoAdapter", String.format(
                            "Started tracking: videoId=%s", video.getId()));
                } else {
                    // Video paused, save current position and stop tracking
                    if (player.getCurrentPosition() > 0) {
                        tracker.updateWatchPosition((int) (player.getCurrentPosition() / 1000));
                    }
                    tracker.stopTracking();
                    android.util.Log.d("VideoAdapter", String.format(
                            "Stopped tracking: videoId=%s", video.getId()));
                }
            }
        });

        // Periodic update to track watch position (every 5 seconds)
        holder.trackingUpdater = new Runnable() {
            @Override
            public void run() {
                if (holder.player != null && holder.player.isPlaying() && tracker != null) {
                    long currentPosition = holder.player.getCurrentPosition();
                    if (currentPosition > 0) {
                        tracker.updateWatchPosition((int) (currentPosition / 1000));
                    }
                }
                // Schedule next update
                if (holder.binding != null && holder.binding.getRoot() != null) {
                    holder.binding.getRoot().postDelayed(this, 5000); // Update every 5 seconds
                }
            }
        };
        holder.binding.getRoot().postDelayed(holder.trackingUpdater, 5000);

        // Also check duration periodically until it's available
        Runnable durationChecker = new Runnable() {
            @Override
            public void run() {
                if (holder.player != null && tracker != null) {
                    long duration = holder.player.getDuration();
                    if (duration > 0 && tracker.getVideoDuration() == 0) {
                        tracker.setVideoDuration((int) (duration / 1000));
                        android.util.Log.d("VideoAdapter", String.format(
                                "Duration set via checker: videoId=%s, duration=%ds",
                                video.getId(), (int) (duration / 1000)));
                    } else if (tracker.getVideoDuration() == 0) {
                        // Keep checking until duration is available
                        holder.binding.getRoot().postDelayed(this, 500);
                    }
                }
            }
        };
        holder.binding.getRoot().postDelayed(durationChecker, 500);
    }

    /**
     * Check and set video duration if available
     */
    private void checkAndSetVideoDuration(ExoPlayer player, VideoWatchTracker tracker, String videoId) {
        if (player != null && tracker != null) {
            long duration = player.getDuration();
            if (duration > 0 && duration != C.TIME_UNSET) {
                int durationSeconds = (int) (duration / 1000);
                if (tracker.getVideoDuration() == 0) {
                    tracker.setVideoDuration(durationSeconds);
                    android.util.Log.d("VideoAdapter", String.format(
                            "Video duration set: videoId=%s, duration=%ds", videoId, durationSeconds));
                }
            } else {
                android.util.Log.d("VideoAdapter", String.format(
                        "Duration not available yet: videoId=%s, duration=%d", videoId, duration));
            }
        }
    }

    /**
     * Save video watch data to database
     */
    private void saveVideoWatch(VideoWatchTracker tracker, String videoId, ExoPlayer player) {
        // Ensure tracking is stopped before calculating final duration
        tracker.stopTracking();

        // If duration is still 0, try to get it from the player
        if (tracker.getVideoDuration() == 0 && player != null) {
            long duration = player.getDuration();
            android.util.Log.d("VideoAdapter", String.format(
                    "Checking duration from player: videoId=%s, duration=%d, TIME_UNSET=%d",
                    videoId, duration, C.TIME_UNSET));
            if (duration > 0 && duration != C.TIME_UNSET) {
                tracker.setVideoDuration((int) (duration / 1000));
                android.util.Log.d("VideoAdapter", String.format(
                        "Duration retrieved from player at save time: videoId=%s, duration=%ds",
                        videoId, (int) (duration / 1000)));
            } else {
                android.util.Log.w("VideoAdapter", String.format(
                        "Player duration still not available: videoId=%s, duration=%d",
                        videoId, duration));
            }
        }

        int watchDuration = tracker.getTotalWatchDuration();
        int videoDuration = tracker.getVideoDuration();

        // Debug logging with more details
        android.util.Log.d("VideoAdapter", String.format(
                "=== SAVE VIDEO WATCH ===\n" +
                        "videoId: %s\n" +
                        "watchDuration: %d seconds\n" +
                        "videoDuration: %d seconds\n" +
                        "accumulatedWatchTime: %d\n" +
                        "maxWatchPosition: %d\n" +
                        "isTracking: %s",
                videoId, watchDuration, videoDuration,
                tracker.accumulatedWatchTime, tracker.maxWatchPosition, tracker.isTracking));

        // Only save if user watched at least minimum duration AND video duration is
        // available
        if (watchDuration >= MIN_WATCH_DURATION && videoDuration > 0) {
            float watchPercentage = Math.min(1.0f, watchDuration / (float) videoDuration);
            boolean completed = watchPercentage >= COMPLETION_THRESHOLD;

            android.util.Log.d("VideoAdapter", String.format(
                    "Calling upsertVideoWatch: watchPercentage=%.2f, completed=%s",
                    watchPercentage, completed));

            // Use upsert to avoid duplicates
            SupabaseManager.upsertVideoWatch(
                    videoId,
                    watchDuration,
                    videoDuration,
                    watchPercentage,
                    completed);
        } else {
            android.util.Log.w("VideoAdapter", String.format(
                    "SKIPPING SAVE - watchDuration=%d < MIN_WATCH_DURATION=%d OR videoDuration=%d <= 0",
                    watchDuration, MIN_WATCH_DURATION, videoDuration));
            if (videoDuration == 0) {
                android.util.Log.w("VideoAdapter", "Video duration is 0 - video metadata may not be loaded yet");
            }
        }
    }

    /**
     * Helper to get video ID for a position
     */
    private String getVideoIdForPosition(int position) {
        if (position >= 0 && position < videos.size()) {
            return videos.get(position).getId();
        }
        return "";
    }

    private void setupSeekBar(VideoViewHolder holder) {
        holder.binding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && holder.player != null) {
                    holder.player.seekTo(progress);
                    // Update tracker when user seeks manually
                    if (holder.watchTracker != null) {
                        holder.watchTracker.updateWatchPosition(progress / 1000); // Convert ms to seconds
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        holder.progressUpdater = new Runnable() {
            @Override
            public void run() {
                if (holder.player != null) {
                    if (holder.player.isPlaying()) {
                        long duration = holder.player.getDuration();
                        long position = holder.player.getCurrentPosition();
                        if (duration > 0) {
                            holder.binding.seekBar.setMax((int) duration);
                            holder.binding.seekBar.setProgress((int) position);
                        }
                    }
                }
                holder.binding.seekBar.postDelayed(this, 500);
            }
        };
        holder.binding.seekBar.post(holder.progressUpdater);
    }

    public void onViewRecycled(@NonNull VideoViewHolder holder) {
        super.onViewRecycled(holder);
        int position = holder.getAdapterPosition();

        // Save watch data before recycling
        if (position != RecyclerView.NO_POSITION && position < videos.size()) {
            VideoWatchTracker tracker = watchTrackers.get(position);
            if (tracker != null && holder.player != null) {
                checkAndSetVideoDuration(holder.player, tracker, videos.get(position).getId());
                tracker.updateWatchPosition((int) (holder.player.getCurrentPosition() / 1000));
                Video video = videos.get(position);
                saveVideoWatch(tracker, video.getId(), holder.player);
            }
        }

        // Cleanup
        if (holder.progressUpdater != null) {
            holder.binding.seekBar.removeCallbacks(holder.progressUpdater);
            holder.progressUpdater = null;
        }
        if (holder.trackingUpdater != null && holder.binding != null && holder.binding.getRoot() != null) {
            holder.binding.getRoot().removeCallbacks(holder.trackingUpdater);
            holder.trackingUpdater = null;
        }
        if (holder.player != null) {
            holder.player.release();
            holder.player = null;
        }
        if (position != RecyclerView.NO_POSITION) {
            watchTrackers.remove(position);
            players.remove(position);
        }
    }

    @Override
    public int getItemCount() {
        return videos.size();
    }

    public void playVideoAt(int position) {
        // Save watch data for previous video before switching
        if (currentPlayingPosition != -1 && currentPlayingPosition != position) {
            ExoPlayer currentPlayer = players.get(currentPlayingPosition);
            VideoWatchTracker currentTracker = watchTrackers.get(currentPlayingPosition);
            if (currentPlayer != null && currentTracker != null) {
                // Save watch position before pausing
                currentTracker.updateWatchPosition((int) (currentPlayer.getCurrentPosition() / 1000));
                if (currentPlayingPosition < videos.size()) {
                    Video currentVideo = videos.get(currentPlayingPosition);
                    saveVideoWatch(currentTracker, currentVideo.getId(), currentPlayer);
                }
            }
            if (currentPlayer != null) {
                currentPlayer.pause();
            }
        }

        // Play new video
        ExoPlayer newPlayer = players.get(position);
        if (newPlayer != null) {
            newPlayer.play();
        }
        currentPlayingPosition = position;
    }

    public void pauseAllVideos() {
        // Save watch data for all videos before pausing
        for (Map.Entry<Integer, ExoPlayer> entry : players.entrySet()) {
            int pos = entry.getKey();
            ExoPlayer player = entry.getValue();
            VideoWatchTracker tracker = watchTrackers.get(pos);

            if (player != null && tracker != null && pos < videos.size()) {
                checkAndSetVideoDuration(player, tracker, videos.get(pos).getId());
                tracker.stopTracking(); // Stop tracking first
                tracker.updateWatchPosition((int) (player.getCurrentPosition() / 1000));
                Video video = videos.get(pos);
                saveVideoWatch(tracker, video.getId(), player);
            }

            if (player != null) {
                player.pause();
            }
        }
    }

    /**
     * Save all video watch data before adapter is destroyed
     */
    public void saveAllWatchData() {
        for (Map.Entry<Integer, VideoWatchTracker> entry : watchTrackers.entrySet()) {
            int pos = entry.getKey();
            VideoWatchTracker tracker = entry.getValue();
            ExoPlayer player = players.get(pos);

            if (tracker != null && pos < videos.size()) {
                if (player != null) {
                    checkAndSetVideoDuration(player, tracker, videos.get(pos).getId());
                    tracker.stopTracking();
                    tracker.updateWatchPosition((int) (player.getCurrentPosition() / 1000));
                }
                Video video = videos.get(pos);
                saveVideoWatch(tracker, video.getId(), player);
            }
        }
    }

    public void releaseAllPlayers() {
        for (ExoPlayer player : players.values()) {
            player.release();
        }
        players.clear();
    }

    private String formatCount(int count) {
        if (count >= 1000000) {
            return String.format("%.1fM", count / 1000000.0);
        } else if (count >= 1000) {
            return String.format("%.1fK", count / 1000.0);
        } else {
            return String.valueOf(count);
        }
    }

    static class VideoViewHolder extends RecyclerView.ViewHolder {
        final ItemVideoBinding binding;
        ExoPlayer player;
        Runnable progressUpdater;
        Runnable trackingUpdater;
        VideoWatchTracker watchTracker;

        VideoViewHolder(ItemVideoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    /**
     * Helper class to track video watch progress accurately
     */
    private static class VideoWatchTracker {
        private final String videoId;
        private int videoDuration = 0; // in seconds
        int maxWatchPosition = 0; // Maximum position reached (in seconds) - package private for debugging
        private long lastPlayStartTime = 0; // Timestamp when playback started
        int accumulatedWatchTime = 0; // Total seconds watched (excluding pauses) - package private for debugging
        private boolean isTracking = false;

        public VideoWatchTracker(String videoId) {
            this.videoId = videoId;
        }

        public void setVideoDuration(int duration) {
            this.videoDuration = duration;
        }

        public int getVideoDuration() {
            return videoDuration;
        }

        public void startTracking() {
            if (!isTracking) {
                isTracking = true;
                lastPlayStartTime = System.currentTimeMillis();
            }
        }

        public void updateWatchPosition(int currentPositionSeconds) {
            if (isTracking && lastPlayStartTime > 0) {
                // Calculate time since last update
                long now = System.currentTimeMillis();
                long elapsed = (now - lastPlayStartTime) / 1000; // Convert to seconds
                accumulatedWatchTime += elapsed;
                lastPlayStartTime = now;
            }

            // Track maximum position reached (handles seeking forward)
            if (currentPositionSeconds > maxWatchPosition) {
                maxWatchPosition = currentPositionSeconds;
            }
        }

        public void stopTracking() {
            if (isTracking && lastPlayStartTime > 0) {
                // Calculate final elapsed time before stopping
                long now = System.currentTimeMillis();
                long elapsed = (now - lastPlayStartTime) / 1000;
                accumulatedWatchTime += elapsed;
                lastPlayStartTime = 0;
            }
            isTracking = false;
        }

        public int getTotalWatchDuration() {
            // Use max position reached as primary metric (handles seeking forward)
            // But ensure we don't count more than video duration
            int duration = Math.min(maxWatchPosition, videoDuration > 0 ? videoDuration : Integer.MAX_VALUE);

            // If user watched normally (no seeking), use accumulated time
            // Otherwise use max position reached
            if (accumulatedWatchTime > 0 && Math.abs(accumulatedWatchTime - maxWatchPosition) < 5) {
                // Close match means normal playback, use accumulated time
                return accumulatedWatchTime;
            }

            return duration;
        }
    }
}
