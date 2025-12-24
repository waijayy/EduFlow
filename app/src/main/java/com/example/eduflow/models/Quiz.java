package com.example.eduflow.models;

import java.util.List;

public class Quiz {
    private String id;
    private String videoId;
    private String title;
    private List<Question> questions;

    public Quiz(String id, String videoId, String title, List<Question> questions) {
        this.id = id;
        this.videoId = videoId;
        this.title = title;
        this.questions = questions;
    }

    public String getId() {
        return id;
    }

    public String getVideoId() {
        return videoId;
    }

    public String getTitle() {
        return title;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public int getQuestionCount() {
        return questions != null ? questions.size() : 0;
    }

    public int getTotalPoints() {
        if (questions == null) return 0;
        int total = 0;
        for (Question q : questions) {
            total += q.getPoints();
        }
        return total;
    }
}
