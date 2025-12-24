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
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.recyclerview.widget.RecyclerView;

import com.example.eduflow.R;
import com.example.eduflow.databinding.ItemVideoBinding;
import com.example.eduflow.models.Video;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private final List<Video> videos;
    private final OnQuizClickListener onQuizClick;
    private final Map<Integer, ExoPlayer> players = new HashMap<>();
    private int currentPlayingPosition = -1;

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

        // Click listeners
        holder.binding.btnLike.setOnClickListener(v -> {
            holder.binding.btnLike.setSelected(!holder.binding.btnLike.isSelected());
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

    private void setupSeekBar(VideoViewHolder holder) {
        holder.binding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && holder.player != null) {
                    holder.player.seekTo(progress);
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
        if (holder.progressUpdater != null) {
            holder.binding.seekBar.removeCallbacks(holder.progressUpdater);
            holder.progressUpdater = null;
        }
        if (holder.player != null) {
            holder.player.release();
            holder.player = null;
        }
    }

    @Override
    public int getItemCount() {
        return videos.size();
    }

    public void playVideoAt(int position) {
        // Pause current video
        if (currentPlayingPosition != -1 && currentPlayingPosition != position) {
            ExoPlayer currentPlayer = players.get(currentPlayingPosition);
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
        for (ExoPlayer player : players.values()) {
            player.pause();
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

        VideoViewHolder(ItemVideoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
