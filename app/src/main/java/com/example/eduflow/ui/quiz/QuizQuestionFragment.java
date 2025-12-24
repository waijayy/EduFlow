package com.example.eduflow.ui.quiz;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.eduflow.QuizActivity;
import com.example.eduflow.R;
import com.example.eduflow.models.Question;

import java.util.ArrayList;
import java.util.List;

public class QuizQuestionFragment extends Fragment {

    private static final String ARG_QUIZ_TITLE = "quiz_title";
    private static final String ARG_QUESTION_TEXT = "question_text";
    private static final String ARG_OPTIONS = "options";
    private static final String ARG_CORRECT_INDEX = "correct_index";
    private static final String ARG_CURRENT_INDEX = "current_index";
    private static final String ARG_TOTAL_QUESTIONS = "total_questions";

    private String quizTitle;
    private String questionText;
    private ArrayList<String> options;
    private int correctIndex;
    private int currentIndex;
    private int totalQuestions;
    private boolean answered = false;

    private LinearLayout optionA, optionB, optionC, optionD;
    private ImageView ivOptionAIcon, ivOptionBIcon, ivOptionCIcon, ivOptionDIcon;
    private TextView tvOptionAText, tvOptionBText, tvOptionCText, tvOptionDText;

    public static QuizQuestionFragment newInstance(String quizTitle, Question question,
            int currentIndex, int totalQuestions) {
        QuizQuestionFragment fragment = new QuizQuestionFragment();
        Bundle args = new Bundle();
        args.putString(ARG_QUIZ_TITLE, quizTitle);
        args.putString(ARG_QUESTION_TEXT, question.getText());
        args.putStringArrayList(ARG_OPTIONS, new ArrayList<>(question.getOptions()));
        args.putInt(ARG_CORRECT_INDEX, question.getCorrectAnswerIndex());
        args.putInt(ARG_CURRENT_INDEX, currentIndex);
        args.putInt(ARG_TOTAL_QUESTIONS, totalQuestions);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            quizTitle = getArguments().getString(ARG_QUIZ_TITLE);
            questionText = getArguments().getString(ARG_QUESTION_TEXT);
            options = getArguments().getStringArrayList(ARG_OPTIONS);
            correctIndex = getArguments().getInt(ARG_CORRECT_INDEX);
            currentIndex = getArguments().getInt(ARG_CURRENT_INDEX);
            totalQuestions = getArguments().getInt(ARG_TOTAL_QUESTIONS);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quiz_question, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        ImageView btnBack = view.findViewById(R.id.btnBack);
        TextView tvQuizTitle = view.findViewById(R.id.tvQuizTitle);
        TextView tvQuestionProgress = view.findViewById(R.id.tvQuestionProgress);
        ProgressBar progressBar = view.findViewById(R.id.progressBar);
        TextView tvQuestion = view.findViewById(R.id.tvQuestion);

        optionA = view.findViewById(R.id.optionA);
        optionB = view.findViewById(R.id.optionB);
        optionC = view.findViewById(R.id.optionC);
        optionD = view.findViewById(R.id.optionD);

        tvOptionAText = view.findViewById(R.id.tvOptionAText);
        tvOptionBText = view.findViewById(R.id.tvOptionBText);
        tvOptionCText = view.findViewById(R.id.tvOptionCText);
        tvOptionDText = view.findViewById(R.id.tvOptionDText);

        ivOptionAIcon = view.findViewById(R.id.ivOptionAIcon);
        ivOptionBIcon = view.findViewById(R.id.ivOptionBIcon);
        ivOptionCIcon = view.findViewById(R.id.ivOptionCIcon);
        ivOptionDIcon = view.findViewById(R.id.ivOptionDIcon);

        // Set data
        tvQuizTitle.setText(quizTitle);
        tvQuestionProgress.setText(String.format("Question %d of %d", currentIndex + 1, totalQuestions));
        progressBar.setMax(totalQuestions);
        progressBar.setProgress(currentIndex + 1);
        tvQuestion.setText(questionText);

        // Set options
        if (options != null && options.size() >= 4) {
            tvOptionAText.setText(options.get(0));
            tvOptionBText.setText(options.get(1));
            tvOptionCText.setText(options.get(2));
            tvOptionDText.setText(options.get(3));
        }

        // Handle option D visibility
        if (options != null && options.size() < 4) {
            optionD.setVisibility(View.GONE);
        }

        // Set click listeners
        btnBack.setOnClickListener(v -> {
            if (getActivity() instanceof QuizActivity) {
                ((QuizActivity) getActivity()).goBack();
            }
        });

        View btnNext = view.findViewById(R.id.btnNext);
        btnNext.setOnClickListener(v -> {
            if (getActivity() instanceof QuizActivity) {
                ((QuizActivity) getActivity()).goToNextQuestion();
            }
        });

        optionA.setOnClickListener(v -> selectOption(0, btnNext));
        optionB.setOnClickListener(v -> selectOption(1, btnNext));
        optionC.setOnClickListener(v -> selectOption(2, btnNext));
        optionD.setOnClickListener(v -> selectOption(3, btnNext));
    }

    private void selectOption(int selectedIndex, View btnNext) {
        if (answered)
            return;
        answered = true;

        // Notify activity
        if (getActivity() instanceof QuizActivity) {
            ((QuizActivity) getActivity()).onAnswerSelected(selectedIndex);
        }

        // Show correct/incorrect states
        showAnswerFeedback(selectedIndex);

        // Show next button immediately instead of auto-advance
        btnNext.setVisibility(View.VISIBLE);
    }

    private void showAnswerFeedback(int selectedIndex) {
        LinearLayout[] optionViews = { optionA, optionB, optionC, optionD };
        ImageView[] iconViews = { ivOptionAIcon, ivOptionBIcon, ivOptionCIcon, ivOptionDIcon };

        for (int i = 0; i < optionViews.length; i++) {
            if (i == correctIndex) {
                // Show correct answer
                optionViews[i].setBackgroundResource(R.drawable.bg_quiz_option_correct);
                iconViews[i].setImageResource(R.drawable.ic_check_circle);
                iconViews[i].setVisibility(View.VISIBLE);
            } else if (i == selectedIndex) {
                // Show incorrect selection
                optionViews[i].setBackgroundResource(R.drawable.bg_quiz_option_incorrect);
                iconViews[i].setImageResource(R.drawable.ic_close_circle);
                iconViews[i].setVisibility(View.VISIBLE);
            }
        }
    }
}
