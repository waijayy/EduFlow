package com.example.eduflow;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.eduflow.models.Question;
import com.example.eduflow.models.Quiz;
import com.example.eduflow.ui.quiz.QuizIntroFragment;
import com.example.eduflow.ui.quiz.QuizQuestionFragment;
import com.example.eduflow.ui.quiz.QuizResultFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class QuizActivity extends AppCompatActivity {

    public static final String EXTRA_VIDEO_ID = "extra_video_id";
    public static final String EXTRA_VIDEO_TITLE = "extra_video_title";

    private Quiz currentQuiz;
    private int currentQuestionIndex = 0;
    private int correctAnswers = 0;
    private List<Integer> userAnswers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_quiz);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.quizContainer), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Get video info from intent
        String videoId = getIntent().getStringExtra(EXTRA_VIDEO_ID);
        String videoTitle = getIntent().getStringExtra(EXTRA_VIDEO_TITLE);

        // Create sample quiz based on video
        currentQuiz = createSampleQuiz(videoId, videoTitle);

        // Start with intro fragment
        showIntroFragment();

        setupGestureDetector();
    }

    private void setupGestureDetector() {
        android.view.GestureDetector gestureDetector = new android.view.GestureDetector(this,
                new android.view.GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onFling(android.view.MotionEvent e1, android.view.MotionEvent e2, float velocityX,
                            float velocityY) {
                        if (e1 == null)
                            return false;

                        float diffX = e2.getX() - e1.getX();
                        float diffY = e2.getY() - e1.getY();

                        // Swipe right (finger moves left to right) to exit
                        if (Math.abs(diffX) > Math.abs(diffY) &&
                                Math.abs(diffX) > 100 &&
                                Math.abs(velocityX) > 100) {

                            if (diffX > 0) {
                                finishQuiz();
                                return true;
                            }
                        }
                        return false;
                    }
                });

        findViewById(R.id.quizContainer).setOnTouchListener((v, event) -> {
            gestureDetector.onTouchEvent(event);
            return true;
        });

        // Also override dispatchTouchEvent to catch swipes over child views
        this.gestureDetector = gestureDetector;
    }

    private android.view.GestureDetector gestureDetector;

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        if (gestureDetector != null && gestureDetector.onTouchEvent(ev)) {
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    private Quiz createSampleQuiz(String videoId, String videoTitle) {
        // Create sample questions based on the video topic
        List<Question> questions = Arrays.asList(
                new Question(
                        "What does the useState hook return?",
                        Arrays.asList("A single value", "An array with two elements", "An object", "A function"),
                        1, // Correct: "An array with two elements"
                        10),
                new Question(
                        "When does useEffect run by default?",
                        Arrays.asList("Only on mount", "On every render", "Only on unmount", "Never"),
                        1, // Correct: "On every render"
                        10),
                new Question(
                        "What is the purpose of the dependency array in useEffect?",
                        Arrays.asList("To define state variables", "To control when the effect runs",
                                "To import dependencies", "To export values"),
                        1, // Correct: "To control when the effect runs"
                        10));

        String quizTitle = videoTitle != null ? videoTitle.replace("Explained", "Quiz") : "React Hooks Quiz";
        quizTitle = quizTitle.replace("Tutorial", "Quiz");
        if (!quizTitle.contains("Quiz")) {
            quizTitle = quizTitle + " Quiz";
        }

        return new Quiz("quiz_" + videoId, videoId, quizTitle, questions);
    }

    private void showIntroFragment() {
        QuizIntroFragment fragment = QuizIntroFragment.newInstance(currentQuiz);
        loadFragment(fragment);
    }

    public void startQuiz() {
        currentQuestionIndex = 0;
        correctAnswers = 0;
        userAnswers.clear();
        showQuestionFragment();
    }

    private void showQuestionFragment() {
        if (currentQuestionIndex < currentQuiz.getQuestionCount()) {
            Question question = currentQuiz.getQuestions().get(currentQuestionIndex);
            QuizQuestionFragment fragment = QuizQuestionFragment.newInstance(
                    currentQuiz.getTitle(),
                    question,
                    currentQuestionIndex,
                    currentQuiz.getQuestionCount());
            loadFragment(fragment);
        } else {
            showResultFragment();
        }
    }

    public void onAnswerSelected(int selectedIndex) {
        Question currentQuestion = currentQuiz.getQuestions().get(currentQuestionIndex);
        userAnswers.add(selectedIndex);

        if (currentQuestion.isCorrect(selectedIndex)) {
            correctAnswers++;
        }
    }

    public void goToNextQuestion() {
        currentQuestionIndex++;
        showQuestionFragment();
    }

    private void showResultFragment() {
        // Save result to Supabase
        com.example.eduflow.auth.SupabaseManager.saveQuizResult(
                currentQuiz.getId(),
                correctAnswers,
                currentQuiz.getQuestionCount());

        QuizResultFragment fragment = QuizResultFragment.newInstance(
                correctAnswers,
                currentQuiz.getQuestionCount());
        loadFragment(fragment);
    }

    public void finishQuiz() {
        finish();
    }

    public void goBack() {
        if (currentQuestionIndex > 0) {
            // Go to previous question
            currentQuestionIndex--;
            userAnswers.remove(userAnswers.size() - 1);
            showQuestionFragment();
        } else {
            // Go back to intro or finish
            finish();
        }
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out);
        transaction.replace(R.id.quizContainer, fragment);
        transaction.commit();
    }

    public Quiz getCurrentQuiz() {
        return currentQuiz;
    }

    public int getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }

    public int getCorrectAnswers() {
        return correctAnswers;
    }
}
