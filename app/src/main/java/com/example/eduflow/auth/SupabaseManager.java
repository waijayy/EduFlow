package com.example.eduflow.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        public int totalVideosWatched = 0;
    }

    public static class Achievement {
        public String type;
        public String icon;
        public String title;
        public boolean isUnlocked;

        public Achievement(String type, String icon, String title, boolean isUnlocked) {
            this.type = type;
            this.icon = icon;
            this.title = title;
            this.isUnlocked = isUnlocked;
        }
    }

    public interface AchievementsCallback {
        void onAchievementsLoaded(List<Achievement> achievements);
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

                    // Fetch total videos watched and hours watched
                    try {
                        Request videoRequest = new Request.Builder()
                                .url(SUPABASE_URL
                                        + "/rest/v1/video_watches?select=video_id,watch_duration_seconds&user_id=eq."
                                        + userId)
                                .addHeader("apikey", SUPABASE_KEY)
                                .addHeader("Authorization", "Bearer " + token)
                                .get()
                                .build();

                        try (Response videoResponse = client.newCall(videoRequest).execute()) {
                            if (videoResponse.isSuccessful() && videoResponse.body() != null) {
                                String videoBody = videoResponse.body().string();
                                JSONArray videoWatches = new JSONArray(videoBody);
                                // Count unique videos watched and sum watch duration
                                Set<String> uniqueVideos = new HashSet<>();
                                long totalWatchSeconds = 0;

                                for (int i = 0; i < videoWatches.length(); i++) {
                                    JSONObject watch = videoWatches.getJSONObject(i);
                                    String videoId = watch.optString("video_id", "");
                                    int watchDuration = watch.optInt("watch_duration_seconds", 0);

                                    if (!videoId.isEmpty()) {
                                        uniqueVideos.add(videoId);
                                    }
                                    totalWatchSeconds += watchDuration;
                                }

                                stats.totalVideosWatched = uniqueVideos.size();
                                stats.totalHours = totalWatchSeconds / 3600.0f;
                            }
                        } catch (Exception e) {
                            // Table might not exist yet, keep default values
                            e.printStackTrace();
                        }
                    } catch (Exception e) {
                        // Table might not exist yet, keep default values
                        e.printStackTrace();
                    }

                    // Fetch login streak synchronously (inline)
                    try {
                        JSONObject streakJson = new JSONObject();
                        streakJson.put("p_user_id", userId);

                        Request streakRequest = new Request.Builder()
                                .url(SUPABASE_URL + "/rest/v1/rpc/calculate_login_streak")
                                .addHeader("apikey", SUPABASE_KEY)
                                .addHeader("Authorization", "Bearer " + token)
                                .addHeader("Content-Type", "application/json")
                                .post(RequestBody.create(streakJson.toString(), MediaType.parse("application/json")))
                                .build();

                        try (Response streakResponse = client.newCall(streakRequest).execute()) {
                            if (streakResponse.isSuccessful() && streakResponse.body() != null) {
                                String streakBody = streakResponse.body().string().trim().replace("\"", "");
                                stats.streakDays = Integer.parseInt(streakBody);
                            }
                        } catch (Exception e) {
                            // Function might not exist yet, keep default
                            e.printStackTrace();
                        }
                    } catch (Exception e) {
                        // Function might not exist yet, keep default
                        e.printStackTrace();
                    }

                    callback.onStatsLoaded(stats);
                }
            } catch (Exception e) {
                e.printStackTrace();
                callback.onStatsLoaded(new DashboardStats());
            }
        });
    }

    public static void getUserAchievements(AchievementsCallback callback) {
        executor.execute(() -> {
            try {
                String userId = getUserId();
                String token = getAccessToken();

                if (userId.isEmpty() || token.isEmpty()) {
                    callback.onAchievementsLoaded(new ArrayList<>());
                    return;
                }

                JSONObject json = new JSONObject();
                json.put("p_user_id", userId);

                Request request = new Request.Builder()
                        .url(SUPABASE_URL + "/rest/v1/rpc/get_user_achievements")
                        .addHeader("apikey", SUPABASE_KEY)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Content-Type", "application/json")
                        .post(RequestBody.create(json.toString(), MediaType.parse("application/json")))
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    List<Achievement> achievements = new ArrayList<>();
                    if (response.isSuccessful() && response.body() != null) {
                        JSONArray results = new JSONArray(response.body().string());

                        for (int i = 0; i < results.length(); i++) {
                            JSONObject obj = results.getJSONObject(i);
                            String type = obj.getString("achievement_type");
                            boolean unlocked = obj.getBoolean("is_unlocked");

                            // Map type to icon and title
                            String icon = "";
                            String title = "";
                            switch (type) {
                                case "first_quiz":
                                    icon = "🧠";
                                    title = "First Quiz Completed";
                                    break;
                                case "perfect_score":
                                    icon = "💯";
                                    title = "Perfect Score";
                                    break;
                                case "first_video":
                                    icon = "📺";
                                    title = "First Video Watch";
                                    break;
                                case "ten_videos":
                                    icon = "🏅";
                                    title = "10 Videos Watched";
                                    break;
                                case "seven_day_streak":
                                    icon = "🔥";
                                    title = "7-Day Streak";
                                    break;
                                case "thirty_day_streak":
                                    icon = "⚡";
                                    title = "30-Day Streak";
                                    break;
                            }

                            achievements.add(new Achievement(type, icon, title, unlocked));
                        }
                    }
                    callback.onAchievementsLoaded(achievements);
                }
            } catch (Exception e) {
                e.printStackTrace();
                callback.onAchievementsLoaded(new ArrayList<>());
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

    /**
     * Upsert video watch record (insert or update if exists)
     * Uses PostgreSQL ON CONFLICT to avoid duplicates
     */
    public static void upsertVideoWatch(String videoId, int watchDurationSeconds,
            int videoDurationSeconds, float watchPercentage,
            boolean completed) {
        executor.execute(() -> {
            try {
                String userId = getUserId();
                String token = getAccessToken();

                Log.d("SupabaseManager", String.format(
                        "upsertVideoWatch called: videoId=%s, watchDuration=%ds, videoDuration=%ds, userId=%s",
                        videoId, watchDurationSeconds, videoDurationSeconds, userId));

                if (userId.isEmpty() || token.isEmpty()) {
                    Log.e("SupabaseManager",
                            "Missing userId or token - userId: " + userId + ", token empty: " + token.isEmpty());
                    return;
                }

                JSONObject json = new JSONObject();
                json.put("user_id", userId);
                json.put("video_id", videoId);
                json.put("watch_duration_seconds", watchDurationSeconds);
                json.put("video_duration_seconds", videoDurationSeconds);
                json.put("watch_percentage", watchPercentage);
                json.put("completed", completed);

                Log.d("SupabaseManager", "JSON payload: " + json.toString());

                // Use POST with upsert header - Supabase will handle the conflict resolution
                // The unique constraint on (user_id, video_id) will trigger the merge
                Request request = new Request.Builder()
                        .url(SUPABASE_URL + "/rest/v1/video_watches")
                        .addHeader("apikey", SUPABASE_KEY)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "resolution=merge-duplicates")
                        .post(RequestBody.create(json.toString(), MediaType.parse("application/json")))
                        .build();

                Log.d("SupabaseManager", "Sending request to: " + SUPABASE_URL + "/rest/v1/video_watches");

                try (Response response = client.newCall(request).execute()) {
                    int statusCode = response.code();
                    String responseBody = response.body() != null ? response.body().string() : "No response body";

                    Log.d("SupabaseManager", String.format(
                            "Response status: %d, body: %s", statusCode, responseBody));

                    if (!response.isSuccessful()) {
                        Log.e("SupabaseManager", String.format(
                                "Failed to upsert video watch - Status: %d, Error: %s",
                                statusCode, responseBody));
                        System.err.println("Failed to upsert video watch: " + responseBody);
                    } else {
                        Log.d("SupabaseManager", String.format(
                                "Successfully saved video watch: videoId=%s, duration=%ds",
                                videoId, watchDurationSeconds));
                        // Update daily activity log with hours watched
                        float hoursWatched = watchDurationSeconds / 3600.0f;
                        updateDailyActivity(hoursWatched, 1); // 1 video watched
                    }
                } catch (Exception e) {
                    Log.e("SupabaseManager", "Exception during upsert", e);
                    e.printStackTrace();
                }
            } catch (Exception e) {
                Log.e("SupabaseManager", "Exception in upsertVideoWatch", e);
                e.printStackTrace();
            }
        });
    }

    /**
     * Update daily activity log (PostgreSQL upsert syntax)
     * Increments login_count, hours_watched, and videos_watched
     */
    private static void updateDailyActivity(float hoursWatched, int videosWatched) {
        executor.execute(() -> {
            try {
                String userId = getUserId();
                String token = getAccessToken();

                if (userId.isEmpty() || token.isEmpty()) {
                    return;
                }

                String today = java.time.LocalDate.now().toString();

                // First, try to get existing record
                Request getRequest = new Request.Builder()
                        .url(SUPABASE_URL + "/rest/v1/user_activity_logs?user_id=eq." + userId
                                + "&activity_date=eq." + today + "&select=login_count,hours_watched,videos_watched")
                        .addHeader("apikey", SUPABASE_KEY)
                        .addHeader("Authorization", "Bearer " + token)
                        .get()
                        .build();

                try (Response getResponse = client.newCall(getRequest).execute()) {
                    JSONObject json = new JSONObject();
                    json.put("user_id", userId);
                    json.put("activity_date", today);

                    if (getResponse.isSuccessful() && getResponse.body() != null) {
                        String body = getResponse.body().string();
                        JSONArray existing = new JSONArray(body);

                        if (existing.length() > 0) {
                            // Update existing record - increment values
                            JSONObject existingRecord = existing.getJSONObject(0);
                            int currentLoginCount = existingRecord.optInt("login_count", 0);
                            float currentHours = (float) existingRecord.optDouble("hours_watched", 0.0);
                            int currentVideos = existingRecord.optInt("videos_watched", 0);

                            json.put("login_count", currentLoginCount); // Keep existing, login tracked separately
                            json.put("hours_watched", currentHours + hoursWatched);
                            json.put("videos_watched", currentVideos + videosWatched);

                            // Use PATCH to update
                            Request patchRequest = new Request.Builder()
                                    .url(SUPABASE_URL + "/rest/v1/user_activity_logs?user_id=eq." + userId
                                            + "&activity_date=eq." + today)
                                    .addHeader("apikey", SUPABASE_KEY)
                                    .addHeader("Authorization", "Bearer " + token)
                                    .addHeader("Content-Type", "application/json")
                                    .addHeader("Prefer", "return=minimal")
                                    .patch(RequestBody.create(json.toString(), MediaType.parse("application/json")))
                                    .build();

                            client.newCall(patchRequest).execute();
                        } else {
                            // Insert new record
                            json.put("login_count", 0); // Login tracked separately
                            json.put("hours_watched", hoursWatched);
                            json.put("videos_watched", videosWatched);

                            Request insertRequest = new Request.Builder()
                                    .url(SUPABASE_URL + "/rest/v1/user_activity_logs")
                                    .addHeader("apikey", SUPABASE_KEY)
                                    .addHeader("Authorization", "Bearer " + token)
                                    .addHeader("Content-Type", "application/json")
                                    .addHeader("Prefer", "return=minimal")
                                    .post(RequestBody.create(json.toString(), MediaType.parse("application/json")))
                                    .build();

                            client.newCall(insertRequest).execute();
                        }
                    } else {
                        // If get fails, try insert
                        json.put("login_count", 0);
                        json.put("hours_watched", hoursWatched);
                        json.put("videos_watched", videosWatched);

                        Request insertRequest = new Request.Builder()
                                .url(SUPABASE_URL + "/rest/v1/user_activity_logs")
                                .addHeader("apikey", SUPABASE_KEY)
                                .addHeader("Authorization", "Bearer " + token)
                                .addHeader("Content-Type", "application/json")
                                .addHeader("Prefer", "resolution=merge-duplicates")
                                .post(RequestBody.create(json.toString(), MediaType.parse("application/json")))
                                .build();

                        client.newCall(insertRequest).execute();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Track user login for streak calculation
     * Call this when MainActivity starts or when user logs in
     */
    public static void trackLogin() {
        executor.execute(() -> {
            try {
                String userId = getUserId();
                String token = getAccessToken();

                if (userId.isEmpty() || token.isEmpty()) {
                    return;
                }

                String today = java.time.LocalDate.now().toString();

                // Check if record exists for today
                Request getRequest = new Request.Builder()
                        .url(SUPABASE_URL + "/rest/v1/user_activity_logs?user_id=eq." + userId
                                + "&activity_date=eq." + today + "&select=login_count")
                        .addHeader("apikey", SUPABASE_KEY)
                        .addHeader("Authorization", "Bearer " + token)
                        .get()
                        .build();

                try (Response getResponse = client.newCall(getRequest).execute()) {
                    JSONObject json = new JSONObject();
                    json.put("user_id", userId);
                    json.put("activity_date", today);

                    if (getResponse.isSuccessful() && getResponse.body() != null) {
                        String body = getResponse.body().string();
                        JSONArray existing = new JSONArray(body);

                        if (existing.length() > 0) {
                            // Update existing record - increment login_count
                            JSONObject existingRecord = existing.getJSONObject(0);
                            int currentLoginCount = existingRecord.optInt("login_count", 0);
                            json.put("login_count", currentLoginCount + 1);

                            // Get other fields to preserve them
                            Request getFullRequest = new Request.Builder()
                                    .url(SUPABASE_URL + "/rest/v1/user_activity_logs?user_id=eq." + userId
                                            + "&activity_date=eq." + today)
                                    .addHeader("apikey", SUPABASE_KEY)
                                    .addHeader("Authorization", "Bearer " + token)
                                    .get()
                                    .build();

                            try (Response fullResponse = client.newCall(getFullRequest).execute()) {
                                if (fullResponse.isSuccessful() && fullResponse.body() != null) {
                                    JSONArray fullRecords = new JSONArray(fullResponse.body().string());
                                    if (fullRecords.length() > 0) {
                                        JSONObject fullRecord = fullRecords.getJSONObject(0);
                                        json.put("hours_watched", fullRecord.optDouble("hours_watched", 0.0));
                                        json.put("videos_watched", fullRecord.optInt("videos_watched", 0));
                                    }
                                }
                            }

                            Request patchRequest = new Request.Builder()
                                    .url(SUPABASE_URL + "/rest/v1/user_activity_logs?user_id=eq." + userId
                                            + "&activity_date=eq." + today)
                                    .addHeader("apikey", SUPABASE_KEY)
                                    .addHeader("Authorization", "Bearer " + token)
                                    .addHeader("Content-Type", "application/json")
                                    .addHeader("Prefer", "return=minimal")
                                    .patch(RequestBody.create(json.toString(), MediaType.parse("application/json")))
                                    .build();

                            client.newCall(patchRequest).execute();
                        } else {
                            // Insert new record
                            json.put("login_count", 1);
                            json.put("hours_watched", 0.0);
                            json.put("videos_watched", 0);

                            Request insertRequest = new Request.Builder()
                                    .url(SUPABASE_URL + "/rest/v1/user_activity_logs")
                                    .addHeader("apikey", SUPABASE_KEY)
                                    .addHeader("Authorization", "Bearer " + token)
                                    .addHeader("Content-Type", "application/json")
                                    .addHeader("Prefer", "resolution=merge-duplicates")
                                    .post(RequestBody.create(json.toString(), MediaType.parse("application/json")))
                                    .build();

                            client.newCall(insertRequest).execute();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public interface StreakCallback {
        void onStreakLoaded(int streakDays);
    }

    /**
     * Get login streak for current user using PostgreSQL function
     */
    public static void getLoginStreak(StreakCallback callback) {
        executor.execute(() -> {
            try {
                String userId = getUserId();
                String token = getAccessToken();

                if (userId.isEmpty() || token.isEmpty()) {
                    callback.onStreakLoaded(0);
                    return;
                }

                // Call the PostgreSQL function
                JSONObject json = new JSONObject();
                json.put("p_user_id", userId);

                Request request = new Request.Builder()
                        .url(SUPABASE_URL + "/rest/v1/rpc/calculate_login_streak")
                        .addHeader("apikey", SUPABASE_KEY)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Content-Type", "application/json")
                        .post(RequestBody.create(json.toString(), MediaType.parse("application/json")))
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String body = response.body().string().trim();
                        // Remove quotes if present
                        body = body.replace("\"", "");
                        int streak = Integer.parseInt(body);
                        callback.onStreakLoaded(streak);
                    } else {
                        callback.onStreakLoaded(0);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                callback.onStreakLoaded(0);
            }
        });
    }

    // =====================================================
    // VIDEO NOTES METHODS
    // =====================================================

    public interface VideoNotesCallback {
        void onNotesLoaded(java.util.List<com.example.eduflow.models.VideoNote> notes);
    }

    public interface SaveNoteCallback {
        void onComplete(boolean success);
    }

    /**
     * Save a new video note to Supabase
     */
    public static void saveVideoNote(String videoId, int timestampSeconds, String noteContent,
            SaveNoteCallback callback) {
        executor.execute(() -> {
            try {
                String userId = getUserId();
                String token = getAccessToken();

                if (userId.isEmpty() || token.isEmpty()) {
                    Log.e("SupabaseManager", "saveVideoNote: Missing userId or token");
                    callback.onComplete(false);
                    return;
                }

                JSONObject json = new JSONObject();
                json.put("user_id", userId);
                json.put("video_id", videoId);
                json.put("timestamp_seconds", timestampSeconds);
                json.put("note_content", noteContent);

                Request request = new Request.Builder()
                        .url(SUPABASE_URL + "/rest/v1/video_notes")
                        .addHeader("apikey", SUPABASE_KEY)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "return=minimal")
                        .post(RequestBody.create(json.toString(), MediaType.parse("application/json")))
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        Log.d("SupabaseManager", "Video note saved successfully");
                        callback.onComplete(true);
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "No error body";
                        Log.e("SupabaseManager", "Failed to save video note: " + errorBody);
                        callback.onComplete(false);
                    }
                }
            } catch (Exception e) {
                Log.e("SupabaseManager", "Exception saving video note", e);
                callback.onComplete(false);
            }
        });
    }

    /**
     * Get all notes for a specific video
     */
    public static void getVideoNotes(String videoId, VideoNotesCallback callback) {
        executor.execute(() -> {
            try {
                String userId = getUserId();
                String token = getAccessToken();

                if (userId.isEmpty() || token.isEmpty()) {
                    callback.onNotesLoaded(new java.util.ArrayList<>());
                    return;
                }

                // Fetch notes for this video, ordered by timestamp
                Request request = new Request.Builder()
                        .url(SUPABASE_URL
                                + "/rest/v1/video_notes?select=id,video_id,timestamp_seconds,note_content,created_at&user_id=eq."
                                + userId + "&video_id=eq." + videoId + "&order=timestamp_seconds.asc")
                        .addHeader("apikey", SUPABASE_KEY)
                        .addHeader("Authorization", "Bearer " + token)
                        .get()
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    java.util.List<com.example.eduflow.models.VideoNote> notes = new java.util.ArrayList<>();
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        JSONArray results = new JSONArray(responseBody);

                        for (int i = 0; i < results.length(); i++) {
                            JSONObject obj = results.getJSONObject(i);
                            String id = obj.optString("id", "");
                            String vId = obj.optString("video_id", "");
                            int timestamp = obj.optInt("timestamp_seconds", 0);
                            String content = obj.optString("note_content", "");
                            String createdAt = obj.optString("created_at", "");

                            notes.add(new com.example.eduflow.models.VideoNote(id, vId, timestamp, content, createdAt));
                        }
                    }
                    callback.onNotesLoaded(notes);
                }
            } catch (Exception e) {
                Log.e("SupabaseManager", "Exception fetching video notes", e);
                callback.onNotesLoaded(new java.util.ArrayList<>());
            }
        });
    }

    /**
     * Get all notes for the current user, grouped by video
     */
    public interface AllNotesCallback {
        void onNotesLoaded(java.util.List<com.example.eduflow.models.VideoNoteSummary> notes);
    }

    public static void getAllVideoNotes(AllNotesCallback callback) {
        executor.execute(() -> {
            try {
                String userId = getUserId();
                String token = getAccessToken();

                Log.d("SupabaseManager",
                        "getAllVideoNotes called - userId: " + userId + ", token empty: " + token.isEmpty());

                if (userId.isEmpty() || token.isEmpty()) {
                    Log.e("SupabaseManager", "getAllVideoNotes: Missing userId or token");
                    callback.onNotesLoaded(new java.util.ArrayList<>());
                    return;
                }

                String url = SUPABASE_URL
                        + "/rest/v1/video_notes?select=id,video_id,timestamp_seconds,note_content,created_at&user_id=eq."
                        + userId + "&order=created_at.desc";
                Log.d("SupabaseManager", "getAllVideoNotes URL: " + url);

                // Fetch all notes for this user, ordered by created_at desc
                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("apikey", SUPABASE_KEY)
                        .addHeader("Authorization", "Bearer " + token)
                        .get()
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    int statusCode = response.code();
                    String responseBody = response.body() != null ? response.body().string() : "";

                    Log.d("SupabaseManager",
                            "getAllVideoNotes response - status: " + statusCode + ", body: " + responseBody);

                    java.util.List<com.example.eduflow.models.VideoNoteSummary> summaries = new java.util.ArrayList<>();
                    if (response.isSuccessful()) {
                        JSONArray results = new JSONArray(responseBody);
                        Log.d("SupabaseManager", "getAllVideoNotes found " + results.length() + " notes");

                        // Group notes by video_id
                        java.util.Map<String, java.util.List<JSONObject>> groupedNotes = new java.util.LinkedHashMap<>();

                        for (int i = 0; i < results.length(); i++) {
                            JSONObject obj = results.getJSONObject(i);
                            String videoId = obj.optString("video_id", "");
                            if (!videoId.isEmpty()) {
                                if (!groupedNotes.containsKey(videoId)) {
                                    groupedNotes.put(videoId, new java.util.ArrayList<>());
                                }
                                groupedNotes.get(videoId).add(obj);
                            }
                        }

                        Log.d("SupabaseManager", "getAllVideoNotes grouped into " + groupedNotes.size() + " videos");

                        // Create summaries for each video
                        for (java.util.Map.Entry<String, java.util.List<JSONObject>> entry : groupedNotes.entrySet()) {
                            String videoId = entry.getKey();
                            java.util.List<JSONObject> videoNotes = entry.getValue();

                            if (!videoNotes.isEmpty()) {
                                // Get the first (most recent) note for preview
                                JSONObject firstNote = videoNotes.get(0);
                                String notePreview = firstNote.optString("note_content", "");
                                String createdAt = firstNote.optString("created_at", "");

                                // Generate a title based on video ID (in real app, you'd fetch video metadata)
                                String videoTitle = "Video " + videoId;

                                summaries.add(new com.example.eduflow.models.VideoNoteSummary(
                                        videoId,
                                        videoTitle,
                                        notePreview,
                                        videoNotes.size(),
                                        createdAt));
                            }
                        }
                    } else {
                        Log.e("SupabaseManager", "getAllVideoNotes failed: " + responseBody);
                    }

                    Log.d("SupabaseManager", "getAllVideoNotes returning " + summaries.size() + " summaries");
                    callback.onNotesLoaded(summaries);
                }
            } catch (Exception e) {
                Log.e("SupabaseManager", "Exception fetching all video notes", e);
                callback.onNotesLoaded(new java.util.ArrayList<>());
            }
        });
    }
}
