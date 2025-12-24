package com.example.eduflow.models;

import java.util.List;

public class Video {
    private String id;
    private String title;
    private String author;
    private String thumbnailUrl;
    private String videoUrl;
    private int likes;
    private int comments;
    private List<String> tags;

    public Video(String id, String title, String author, String thumbnailUrl, String videoUrl, int likes, int comments,
            List<String> tags) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.thumbnailUrl = thumbnailUrl;
        this.videoUrl = videoUrl;
        this.likes = likes;
        this.comments = comments;
        this.tags = tags;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public int getLikes() {
        return likes;
    }

    public int getComments() {
        return comments;
    }

    public List<String> getTags() {
        return tags;
    }
}
