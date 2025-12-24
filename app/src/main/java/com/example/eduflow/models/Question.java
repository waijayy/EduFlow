package com.example.eduflow.models;

import java.util.List;

public class Question {
    private String text;
    private List<String> options;
    private int correctAnswerIndex;
    private int points;

    public Question(String text, List<String> options, int correctAnswerIndex, int points) {
        this.text = text;
        this.options = options;
        this.correctAnswerIndex = correctAnswerIndex;
        this.points = points;
    }

    public String getText() {
        return text;
    }

    public List<String> getOptions() {
        return options;
    }

    public int getCorrectAnswerIndex() {
        return correctAnswerIndex;
    }

    public int getPoints() {
        return points;
    }

    public boolean isCorrect(int selectedIndex) {
        return selectedIndex == correctAnswerIndex;
    }
}
