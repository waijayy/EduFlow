package com.example.eduflow.auth;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

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

    private static SharedPreferences prefs;
    private static final OkHttpClient client = new OkHttpClient();
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void initialize(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
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
                        saveSession(email, "");
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
                        saveSession(email, name);
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

    private static void saveSession(String email, String name) {
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
}
