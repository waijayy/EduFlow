package com.example.eduflow.models;

/**
 * Data model for video note summary (grouped by video).
 * Used in the My Notes page to show notes grouped by video.
 */
public class VideoNoteSummary {
    private String videoId;
    private String videoTitle;
    private String notePreview;
    private int noteCount;
    private String lastUpdated;

    public VideoNoteSummary(String videoId, String videoTitle, String notePreview, int noteCount, String lastUpdated) {
        this.videoId = videoId;
        this.videoTitle = videoTitle;
        this.notePreview = notePreview;
        this.noteCount = noteCount;
        this.lastUpdated = lastUpdated;
    }

    public String getVideoId() {
        return videoId;
    }

    public String getVideoTitle() {
        return videoTitle;
    }

    public String getNotePreview() {
        return notePreview;
    }

    public int getNoteCount() {
        return noteCount;
    }

    public String getLastUpdated() {
        return lastUpdated;
    }

    /**
     * Format the date for display (e.g., "Dec 15")
     */
    public String getFormattedDate() {
        if (lastUpdated == null || lastUpdated.isEmpty()) {
            return "";
        }
        try {
            // Parse ISO date and format as "MMM dd"
            String[] parts = lastUpdated.split("T")[0].split("-");
            if (parts.length >= 3) {
                int month = Integer.parseInt(parts[1]);
                int day = Integer.parseInt(parts[2]);
                String[] months = { "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
                return months[month - 1] + " " + day;
            }
        } catch (Exception e) {
            // Return raw date if parsing fails
        }
        return lastUpdated;
    }
}
