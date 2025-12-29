package com.example.eduflow.auth;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SupabaseManager {

    // Supabase credentials
    private static final String SUPABASE_URL = "https://aozoqfxvhhfidtmmhqru.supabase.co";
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFvem9xZnh2aGhmaWR0bW1ocXJ1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjU1MDE3MjcsImV4cCI6MjA4MTA3NzcyN30.MbDACbaieM10zt-Ki5Lv8I_736iLF6sZiwEEU1-aQ04";

    private static final String PREFS_NAME = "eduflow_prefs";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_MEMBER_SINCE = "member_since";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_USER_ID = "user_id";

    private static SharedPreferences prefs;
    private static final OkHttpClient client = new OkHttpClient();
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void initialize(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Check if logged in but missing token (legacy session)
        if (isLoggedIn() && getAccessToken().isEmpty()) {
            signOut();
        }
    }

    public static CompletableFuture<Boolean> signIn(String email, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("email", email);
                json.put("password", password);

                Request request = new Request.Builder()
                        .url(SUPABASE_URL + "/auth/v1/token?grant_type=password")
                        .addHeader("apikey", SUPABASE_KEY)
                        .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                        .addHeader("Content-Type", "application/json")
                        .post(RequestBody.create(json.toString(), MediaType.parse("application/json")))
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        String accessToken = jsonResponse.getString("access_token");
                        String userId = jsonResponse.getJSONObject("user").getString("id");

                        saveSession(email, "", accessToken, userId);
                        return true;
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "No error body";
                        throw new Exception("Sign in failed: " + errorBody);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }, executor);
    }

    public static CompletableFuture<Boolean> signUp(String name, String email, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JSONObject json = new JSONObject();
                json.put("email", email);
                json.put("password", password);

                // Add metadata for user name
                JSONObject data = new JSONObject();
                data.put("name", name);
                json.put("data", data);

                Request request = new Request.Builder()
                        .url(SUPABASE_URL + "/auth/v1/signup")
                        .addHeader("apikey", SUPABASE_KEY)
                        .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                        .addHeader("Content-Type", "application/json")
                        .post(RequestBody.create(json.toString(), MediaType.parse("application/json")))
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        String accessToken = jsonResponse.getString("access_token");
                        String userId = jsonResponse.getJSONObject("user").getString("id");

                        saveSession(email, name, accessToken, userId);
                        return true;
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "No error body";
                        throw new Exception("Sign up failed: " + errorBody);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }, executor);
    }

    private static void saveSession(String email, String name, String accessToken, String userId) {
        if (prefs != null) {
            SharedPreferences.Editor editor = prefs.edit();
            String userName = name;
            if (userName.isEmpty() && email.contains("@")) {
                userName = email.substring(0, email.indexOf("@"));
            }
            editor.putString(KEY_USER_NAME, userName);
            editor.putString(KEY_USER_EMAIL, email);
            editor.putBoolean(KEY_IS_LOGGED_IN, true);
            editor.putString(KEY_MEMBER_SINCE, "December 2024");
            editor.putString(KEY_ACCESS_TOKEN, accessToken);
            editor.putString(KEY_USER_ID, userId);
            editor.apply();
        }
    }

    public static void signOut() {
        if (prefs != null) {
            prefs.edit().clear().apply();
        }
    }

    public static boolean isLoggedIn() {
        return prefs != null && prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public static String getUserEmail() {
        return prefs != null ? prefs.getString(KEY_USER_EMAIL, "") : "";
    }

    public static String getUserName() {
        return prefs != null ? prefs.getString(KEY_USER_NAME, "User") : "User";
    }

    public static String getMemberSince() {
        return prefs != null ? prefs.getString(KEY_MEMBER_SINCE, "December 2024") : "December 2024";
    }

    public static String getUserId() {
        return prefs != null ? prefs.getString(KEY_USER_ID, "") : "";
    }

    public static String getAccessToken() {
        return prefs != null ? prefs.getString(KEY_ACCESS_TOKEN, "") : "";
    }

    public static void saveQuizResult(String quizId, int score, int totalQuestions) {
        executor.execute(() -> {
            try {
                String userId = getUserId();
                String token = getAccessToken();

                if (userId.isEmpty() || token.isEmpty()) {
                    System.err.println("saveQuizResult: Missing userId or token");
                    return;
                }

                JSONObject json = new JSONObject();
                json.put("user_id", userId);
                json.put("quiz_id", quizId);
                json.put("score", score);
                json.put("total_questions", totalQuestions);

                Request request = new Request.Builder()
                        .url(SUPABASE_URL + "/rest/v1/quiz_results")
                        .addHeader("apikey", SUPABASE_KEY)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "return=minimal")
                        .post(RequestBody.create(json.toString(), MediaType.parse("application/json")))
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        // Log error or handle it
                        System.err.println("Failed to save quiz result: " + response.body().string());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public interface DashboardStatsCallback {
        void onStatsLoaded(DashboardStats stats);
    }

    public static class DashboardStats {
        public int quizzesDone;
        public int avgScore;
        public int bestScore;
        public int lowestScore;
        public List<Integer> recentScores = new ArrayList<>();
        // Mocked/Future
        public float totalHours = 42.5f;
        public float avgHoursPerDay = 2.3f;
        public int streakDays = 12;
    }

    public static void getDashboardStats(DashboardStatsCallback callback) {
        executor.execute(() -> {
            try {
                String userId = getUserId();
                String token = getAccessToken();

                if (userId.isEmpty() || token.isEmpty()) {
                    callback.onStatsLoaded(new DashboardStats());
                    return;
                }

                // Fetch all quiz results, ordered by date (newest first)
                Request request = new Request.Builder()
                        .url(SUPABASE_URL + "/rest/v1/quiz_results?select=score,total_questions,created_at&user_id=eq."
                                + userId + "&order=created_at.desc")
                        .addHeader("apikey", SUPABASE_KEY)
                        .addHeader("Authorization", "Bearer " + token)
                        .get()
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    DashboardStats stats = new DashboardStats();
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        JSONArray results = new JSONArray(responseBody);

                        stats.quizzesDone = results.length();
                        long totalPercentageSum = 0;
                        int min = 101;
                        int max = -1;

                        // Recent 7 for trend (results are descending, so take first 7 and reverse for
                        // chronological)
                        List<Integer> recent = new ArrayList<>();

                        for (int i = 0; i < stats.quizzesDone; i++) {
                            JSONObject result = results.getJSONObject(i);
                            int score = result.optInt("score", 0);
                            int total = result.optInt("total_questions", 1);
                            if (total == 0)
                                total = 1; // avoid divide by zero

                            int percent = (int) ((score / (float) total) * 100);

                            totalPercentageSum += percent;
                            if (percent < min)
                                min = percent;
                            if (percent > max)
                                max = percent;

                            if (i < 7) {
                                recent.add(percent);
                            }
                        }

                        if (stats.quizzesDone > 0) {
                            stats.avgScore = (int) (totalPercentageSum / stats.quizzesDone);
                            stats.lowestScore = min;
                            stats.bestScore = max;
                        } else {
                            stats.lowestScore = 0;
                            stats.bestScore = 0;
                        }

                        // Reverse recent to be chronological (oldest -> newest) for the graph
                        java.util.Collections.reverse(recent);
                        stats.recentScores = recent;
                    }
                    callback.onStatsLoaded(stats);
                }
            } catch (Exception e) {
                e.printStackTrace();
                callback.onStatsLoaded(new DashboardStats());
            }
        });
    }

    public interface QuizListCallback {
        void onQuizzesLoaded(List<QuizResult> quizzes);
    }

    public static class QuizResult {
        public String quizName; // We might not have this in quiz_results... will default/mock if missing
        public int scorePercent;
        public String date;

        public QuizResult(String name, int score, String date) {
            this.quizName = name;
            this.scorePercent = score;
            this.date = date;
        }
    }

    public static void getQuizHistory(QuizListCallback callback) {
        executor.execute(() -> {
            try {
                String userId = getUserId();
                String token = getAccessToken();

                if (userId.isEmpty() || token.isEmpty()) {
                    callback.onQuizzesLoaded(new ArrayList<>());
                    return;
                }

                // Fetch expanded info if possible, but for now we just get results
                // We'd ideally need a join or separate fetch for Quiz Details to get Name.
                // Assuming "quiz_id" can be mapped or we just show "Quiz #ID" for now.
                Request request = new Request.Builder()
                        .url(SUPABASE_URL
                                + "/rest/v1/quiz_results?select=quiz_id,score,total_questions,created_at&user_id=eq."
                                + userId + "&order=created_at.desc")
                        .addHeader("apikey", SUPABASE_KEY)
                        .addHeader("Authorization", "Bearer " + token)
                        .get()
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    List<QuizResult> list = new ArrayList<>();
                    if (response.isSuccessful() && response.body() != null) {
                        JSONArray results = new JSONArray(response.body().string());
                        for (int i = 0; i < results.length(); i++) {
                            JSONObject obj = results.getJSONObject(i);
                            String qId = obj.optString("quiz_id", "Unknown");
                            int s = obj.optInt("score", 0);
                            int t = obj.optInt("total_questions", 1);
                            String d = obj.optString("created_at", "").split("T")[0];

                            // Map common IDs to names if hardcoded or just formatted
                            String name = "Quiz " + qId;
                            if (qId.equals("1"))
                                name = "React Hooks Basics";
                            else if (qId.equals("2"))
                                name = "JavaScript ES6+";
                            else if (qId.equals("3"))
                                name = "CSS Grid Layout";

                            int pct = (t > 0) ? (int) ((s / (float) t) * 100) : 0;
                            list.add(new QuizResult(name, pct, d));
                        }
                    }
                    callback.onQuizzesLoaded(list);
                }
            } catch (Exception e) {
                e.printStackTrace();
                callback.onQuizzesLoaded(new ArrayList<>());
            }
        });
    }

    // Deprecated but kept for compatibility until switch
    public interface StatsCallback {
        void onStatsLoaded(int quizzesDone, int avgScore);
    }

    public static void getQuizStats(StatsCallback callback) {
        getDashboardStats(stats -> callback.onStatsLoaded(stats.quizzesDone, stats.avgScore));
    }
}
