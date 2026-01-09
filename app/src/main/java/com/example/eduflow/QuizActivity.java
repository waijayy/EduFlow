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

        // Extract category from video ID (e.g., "math_1" -> "math")
        String category = extractCategoryFromVideoId(videoId);

        // Fetch quiz from database by category
        fetchQuizFromDatabase(category, videoId, videoTitle);

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

    private String extractCategoryFromVideoId(String videoId) {
        if (videoId == null || videoId.isEmpty()) {
            return "programming"; // Default fallback
        }
        // Extract category from video ID format: "category_number" (e.g., "math_1" ->
        // "math")
        int underscoreIndex = videoId.lastIndexOf('_');
        if (underscoreIndex > 0) {
            return videoId.substring(0, underscoreIndex);
        }
        return "programming"; // Default fallback
    }

    private void fetchQuizFromDatabase(String category, String videoId, String videoTitle) {
        // Show loading state (optional - could add a loading fragment)
        com.example.eduflow.auth.SupabaseManager.getQuizByCategory(category, quizData -> {
            runOnUiThread(() -> {
                if (quizData != null && !quizData.questions.isEmpty()) {
                    // Convert QuizData to Quiz model
                    currentQuiz = convertQuizDataToQuiz(quizData, videoId);
                } else {
                    // Fallback: create a default quiz if none found
                    currentQuiz = createFallbackQuiz(videoId, videoTitle, category);
                }
                // Start with intro fragment
                showIntroFragment();
            });
        });
    }

    private Quiz convertQuizDataToQuiz(com.example.eduflow.auth.SupabaseManager.QuizData quizData,
            String videoId) {
        List<Question> questions = new ArrayList<>();
        for (com.example.eduflow.auth.SupabaseManager.QuestionData qData : quizData.questions) {
            questions.add(new Question(
                    qData.questionText,
                    qData.options,
                    qData.correctIndex,
                    qData.points));
        }
        return new Quiz(quizData.id + "_" + videoId, videoId, quizData.title, questions);
    }

    private Quiz createFallbackQuiz(String videoId, String videoTitle, String category) {
        // Fallback quiz if database query fails
        List<Question> questions = Arrays.asList(
                new Question(
                        "This quiz is currently unavailable.",
                        Arrays.asList("Try again later", "OK", "Close", "Exit"),
                        1,
                        10));

        String quizTitle = category.substring(0, 1).toUpperCase() + category.substring(1) + " Quiz";
        return new Quiz("fallback_" + videoId, videoId, quizTitle, questions);
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
