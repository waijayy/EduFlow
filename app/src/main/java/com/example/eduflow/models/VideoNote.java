package com.example.eduflow.models;

/**
 * Data model for video notes.
 * Represents a timestamped note for a specific video.
 */
public class VideoNote {
    private String id;
    private String userId;
    private String videoId;
    private int timestampSeconds;
    private String noteContent;
    private String createdAt;

    public VideoNote(String id, String videoId, int timestampSeconds, String noteContent, String createdAt) {
        this.id = id;
        this.videoId = videoId;
        this.timestampSeconds = timestampSeconds;
        this.noteContent = noteContent;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getVideoId() {
        return videoId;
    }

    public int getTimestampSeconds() {
        return timestampSeconds;
    }

    public String getNoteContent() {
        return noteContent;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    /**
     * Returns the timestamp formatted as MM:SS
     */
    public String getFormattedTimestamp() {
        int minutes = timestampSeconds / 60;
        int seconds = timestampSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}
