package com.example.eduflow.models;

public class Category {
    private String id;
    private String name;
    private int iconResId;
    private int videoCount;

    public Category(String id, String name, int iconResId, int videoCount) {
        this.id = id;
        this.name = name;
        this.iconResId = iconResId;
        this.videoCount = videoCount;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getIconResId() {
        return iconResId;
    }

    public int getVideoCount() {
        return videoCount;
    }
}
