package com.example.eduflow.ui.quiz;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.eduflow.QuizActivity;
import com.example.eduflow.R;
import com.example.eduflow.models.Quiz;

public class QuizIntroFragment extends Fragment {

    private static final String ARG_QUIZ_TITLE = "quiz_title";
    private static final String ARG_QUESTION_COUNT = "question_count";
    private static final String ARG_TOTAL_POINTS = "total_points";

    private String quizTitle;
    private int questionCount;
    private int totalPoints;

    public static QuizIntroFragment newInstance(Quiz quiz) {
        QuizIntroFragment fragment = new QuizIntroFragment();
        Bundle args = new Bundle();
        args.putString(ARG_QUIZ_TITLE, quiz.getTitle());
        args.putInt(ARG_QUESTION_COUNT, quiz.getQuestionCount());
        args.putInt(ARG_TOTAL_POINTS, quiz.getTotalPoints());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            quizTitle = getArguments().getString(ARG_QUIZ_TITLE);
            questionCount = getArguments().getInt(ARG_QUESTION_COUNT);
            totalPoints = getArguments().getInt(ARG_TOTAL_POINTS);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quiz_intro, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvQuizTitle = view.findViewById(R.id.tvQuizTitle);
        TextView tvQuestionCount = view.findViewById(R.id.tvQuestionCount);
        TextView tvTotalPoints = view.findViewById(R.id.tvTotalPoints);
        Button btnStartQuiz = view.findViewById(R.id.btnStartQuiz);

        tvQuizTitle.setText(quizTitle);
        tvQuestionCount.setText(String.valueOf(questionCount));
        tvTotalPoints.setText(String.valueOf(totalPoints));

        btnStartQuiz.setOnClickListener(v -> {
            if (getActivity() instanceof QuizActivity) {
                ((QuizActivity) getActivity()).startQuiz();
            }
        });
    }
}
