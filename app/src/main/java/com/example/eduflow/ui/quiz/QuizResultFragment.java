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

public class QuizResultFragment extends Fragment {

    private static final String ARG_CORRECT_COUNT = "correct_count";
    private static final String ARG_TOTAL_COUNT = "total_count";

    private int correctCount;
    private int totalCount;

    public static QuizResultFragment newInstance(int correctCount, int totalCount) {
        QuizResultFragment fragment = new QuizResultFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_CORRECT_COUNT, correctCount);
        args.putInt(ARG_TOTAL_COUNT, totalCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            correctCount = getArguments().getInt(ARG_CORRECT_COUNT);
            totalCount = getArguments().getInt(ARG_TOTAL_COUNT);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quiz_result, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvResultTitle = view.findViewById(R.id.tvResultTitle);
        TextView tvScorePercent = view.findViewById(R.id.tvScorePercent);
        TextView tvScoreDetail = view.findViewById(R.id.tvScoreDetail);
        TextView tvCorrectCount = view.findViewById(R.id.tvCorrectCount);
        TextView tvWrongCount = view.findViewById(R.id.tvWrongCount);
        TextView tvTotalCount = view.findViewById(R.id.tvTotalCount);
        Button btnFinish = view.findViewById(R.id.btnFinish);

        // Calculate percentage
        int percentage = totalCount > 0 ? (correctCount * 100) / totalCount : 0;
        int wrongCount = totalCount - correctCount;

        // Set encouraging title based on score
        if (percentage >= 80) {
            tvResultTitle.setText("Excellent!");
        } else if (percentage >= 60) {
            tvResultTitle.setText("Good Job!");
        } else if (percentage >= 40) {
            tvResultTitle.setText("Keep Learning!");
        } else {
            tvResultTitle.setText("Keep Trying!");
        }

        tvScorePercent.setText(percentage + "%");
        tvScoreDetail.setText(correctCount + " out of " + totalCount + " correct");
        tvCorrectCount.setText(String.valueOf(correctCount));
        tvWrongCount.setText(String.valueOf(wrongCount));
        tvTotalCount.setText(String.valueOf(totalCount));

        btnFinish.setOnClickListener(v -> {
            if (getActivity() instanceof QuizActivity) {
                ((QuizActivity) getActivity()).finishQuiz();
            }
        });
    }
}
