# Video Tracking Implementation Suggestions

This document provides suggestions for implementing comprehensive video watch tracking, hours watched tracking, and login streak tracking in the EduFlow app.

## Overview

To properly track user engagement, you'll need to:
1. Track when users watch videos
2. Track how long users watch videos
3. Track daily login streaks

## 1. Database Schema (Supabase)

### Table: `video_watches`

Create a table in Supabase to track video watch events:

```sql
CREATE TABLE video_watches (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  video_id TEXT NOT NULL,
  watch_duration_seconds INTEGER NOT NULL DEFAULT 0,
  video_duration_seconds INTEGER NOT NULL DEFAULT 0,
  watch_percentage REAL DEFAULT 0.0, -- Percentage of video watched (0.0 to 1.0)
  completed BOOLEAN DEFAULT FALSE, -- True if user watched >= 80% of video
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Index for faster queries
CREATE INDEX idx_video_watches_user_id ON video_watches(user_id);
CREATE INDEX idx_video_watches_video_id ON video_watches(video_id);
CREATE INDEX idx_video_watches_created_at ON video_watches(created_at);

-- Enable Row Level Security (RLS)
ALTER TABLE video_watches ENABLE ROW LEVEL SECURITY;

-- Policy: Users can only see their own watch history
CREATE POLICY "Users can view own video watches"
  ON video_watches FOR SELECT
  USING (auth.uid() = user_id);

-- Policy: Users can insert their own video watches
CREATE POLICY "Users can insert own video watches"
  ON video_watches FOR INSERT
  WITH CHECK (auth.uid() = user_id);

-- Policy: Users can update their own video watches
CREATE POLICY "Users can update own video watches"
  ON video_watches FOR UPDATE
  USING (auth.uid() = user_id);
```

### Table: `user_activity_logs`

Create a table to track daily login activity for streak calculation:

```sql
CREATE TABLE user_activity_logs (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  activity_date DATE NOT NULL DEFAULT CURRENT_DATE,
  login_count INTEGER DEFAULT 1,
  videos_watched INTEGER DEFAULT 0,
  hours_watched REAL DEFAULT 0.0,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  UNIQUE(user_id, activity_date)
);

-- Index for faster queries
CREATE INDEX idx_user_activity_logs_user_id ON user_activity_logs(user_id);
CREATE INDEX idx_user_activity_logs_activity_date ON user_activity_logs(activity_date);

-- Enable Row Level Security (RLS)
ALTER TABLE user_activity_logs ENABLE ROW LEVEL SECURITY;

-- Policy: Users can only see their own activity
CREATE POLICY "Users can view own activity"
  ON user_activity_logs FOR SELECT
  USING (auth.uid() = user_id);

-- Policy: Users can insert their own activity
CREATE POLICY "Users can insert own activity"
  ON user_activity_logs FOR INSERT
  WITH CHECK (auth.uid() = user_id);

-- Policy: Users can update their own activity
CREATE POLICY "Users can update own activity"
  ON user_activity_logs FOR UPDATE
  USING (auth.uid() = user_id);
```

### Function: Calculate Login Streak

Create a PostgreSQL function to calculate the current login streak:

```sql
CREATE OR REPLACE FUNCTION calculate_login_streak(p_user_id UUID)
RETURNS INTEGER AS $$
DECLARE
  streak_count INTEGER := 0;
  current_date DATE := CURRENT_DATE;
  previous_date DATE;
BEGIN
  -- Check if user logged in today
  IF EXISTS (
    SELECT 1 FROM user_activity_logs 
    WHERE user_id = p_user_id 
    AND activity_date = current_date
  ) THEN
    streak_count := 1;
    previous_date := current_date - INTERVAL '1 day';
    
    -- Count consecutive days backwards
    WHILE EXISTS (
      SELECT 1 FROM user_activity_logs 
      WHERE user_id = p_user_id 
      AND activity_date = previous_date
    ) LOOP
      streak_count := streak_count + 1;
      previous_date := previous_date - INTERVAL '1 day';
    END LOOP;
  END IF;
  
  RETURN streak_count;
END;
$$ LANGUAGE plpgsql;
```

## 2. Android Implementation

### A. Track Video Watch Events

Add methods to `SupabaseManager.java` to track video watches:

```java
/**
 * Track when a user watches a video
 * @param videoId The ID of the video being watched
 * @param watchDurationSeconds How long the user watched (in seconds)
 * @param videoDurationSeconds Total duration of the video (in seconds)
 */
public static void trackVideoWatch(String videoId, int watchDurationSeconds, int videoDurationSeconds) {
    executor.execute(() -> {
        try {
            String userId = getUserId();
            String token = getAccessToken();

            if (userId.isEmpty() || token.isEmpty()) {
                System.err.println("trackVideoWatch: Missing userId or token");
                return;
            }

            float watchPercentage = videoDurationSeconds > 0 
                ? (watchDurationSeconds / (float) videoDurationSeconds) 
                : 0.0f;
            boolean completed = watchPercentage >= 0.8f; // Consider watched if >= 80%

            JSONObject json = new JSONObject();
            json.put("user_id", userId);
            json.put("video_id", videoId);
            json.put("watch_duration_seconds", watchDurationSeconds);
            json.put("video_duration_seconds", videoDurationSeconds);
            json.put("watch_percentage", watchPercentage);
            json.put("completed", completed);

            Request request = new Request.Builder()
                    .url(SUPABASE_URL + "/rest/v1/video_watches")
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer " + token)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .post(RequestBody.create(json.toString(), MediaType.parse("application/json")))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.err.println("Failed to track video watch: " + response.body().string());
                } else {
                    // Update daily activity log
                    updateDailyActivity(watchDurationSeconds / 3600.0f); // Convert to hours
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    });
}

/**
 * Update daily activity log for streak tracking
 */
private static void updateDailyActivity(float hoursWatched) {
    try {
        String userId = getUserId();
        String token = getAccessToken();

        if (userId.isEmpty() || token.isEmpty()) {
            return;
        }

        // Try to update existing record for today
        JSONObject updateJson = new JSONObject();
        updateJson.put("login_count", new JSONObject().put("$inc", 1));
        updateJson.put("hours_watched", new JSONObject().put("$inc", hoursWatched));
        updateJson.put("videos_watched", new JSONObject().put("$inc", 1));

        Request updateRequest = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/user_activity_logs?user_id=eq." + userId 
                    + "&activity_date=eq." + java.time.LocalDate.now())
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .patch(RequestBody.create(updateJson.toString(), MediaType.parse("application/json")))
                .build();

        try (Response response = client.newCall(updateRequest).execute()) {
            if (!response.isSuccessful()) {
                // If update fails, insert new record
                JSONObject insertJson = new JSONObject();
                insertJson.put("user_id", userId);
                insertJson.put("activity_date", java.time.LocalDate.now().toString());
                insertJson.put("login_count", 1);
                insertJson.put("hours_watched", hoursWatched);
                insertJson.put("videos_watched", 1);

                Request insertRequest = new Request.Builder()
                        .url(SUPABASE_URL + "/rest/v1/user_activity_logs")
                        .addHeader("apikey", SUPABASE_KEY)
                        .addHeader("Authorization", "Bearer " + token)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Prefer", "return=minimal")
                        .post(RequestBody.create(insertJson.toString(), MediaType.parse("application/json")))
                        .build();

                client.newCall(insertRequest).execute();
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```

### B. Track Video Playback in VideoAdapter

Modify `VideoAdapter.java` to track video watch time:

```java
// Add these fields to VideoAdapter class
private Map<Integer, Long> videoStartTimes = new HashMap<>();
private Map<Integer, Integer> videoDurations = new HashMap<>();

// In onBindViewHolder, track when video starts playing
holder.player.addListener(new Player.Listener() {
    @Override
    public void onPlaybackStateChanged(int playbackState) {
        if (playbackState == Player.STATE_READY) {
            videoDurations.put(position, (int) (player.getDuration() / 1000)); // Convert to seconds
        }
        if (playbackState == Player.STATE_PLAYING) {
            videoStartTimes.put(position, System.currentTimeMillis());
        }
        if (playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE) {
            // Track watch time when video ends or is paused
            trackWatchTime(position);
        }
    }
});

// Add method to track watch time
private void trackWatchTime(int position) {
    Long startTime = videoStartTimes.get(position);
    if (startTime != null) {
        long watchDurationMs = System.currentTimeMillis() - startTime;
        int watchDurationSeconds = (int) (watchDurationMs / 1000);
        Integer videoDuration = videoDurations.get(position);
        
        if (videoDuration != null && watchDurationSeconds > 0) {
            Video video = videos.get(position);
            SupabaseManager.trackVideoWatch(video.getId(), watchDurationSeconds, videoDuration);
        }
        
        videoStartTimes.remove(position);
    }
}

// Also track when view is recycled
@Override
public void onViewRecycled(@NonNull VideoViewHolder holder) {
    super.onViewRecycled(holder);
    int position = holder.getAdapterPosition();
    if (position != RecyclerView.NO_POSITION) {
        trackWatchTime(position);
    }
    // ... existing cleanup code ...
}
```

### C. Track Login Streak

Add method to track login and calculate streak:

```java
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

            // Upsert today's activity log
            JSONObject json = new JSONObject();
            json.put("user_id", userId);
            json.put("activity_date", java.time.LocalDate.now().toString());
            json.put("login_count", 1);

            Request request = new Request.Builder()
                    .url(SUPABASE_URL + "/rest/v1/user_activity_logs")
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer " + token)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "resolution=merge-duplicates")
                    .post(RequestBody.create(json.toString(), MediaType.parse("application/json")))
                    .build();

            client.newCall(request).execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    });
}

/**
 * Get login streak for current user
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
            Request request = new Request.Builder()
                    .url(SUPABASE_URL + "/rest/v1/rpc/calculate_login_streak?p_user_id=" + userId)
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer " + token)
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    int streak = Integer.parseInt(body.trim());
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

public interface StreakCallback {
    void onStreakLoaded(int streakDays);
}
```

### D. Update DashboardStats to Fetch Real Data

Update `getDashboardStats` to fetch real data:

```java
// In getDashboardStats method, add:

// Fetch total hours watched
try {
    Request hoursRequest = new Request.Builder()
            .url(SUPABASE_URL + "/rest/v1/video_watches?select=watch_duration_seconds&user_id=eq." + userId)
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer " + token)
            .get()
            .build();
    
    try (Response hoursResponse = client.newCall(hoursRequest).execute()) {
        if (hoursResponse.isSuccessful() && hoursResponse.body() != null) {
            String hoursBody = hoursResponse.body().string();
            JSONArray watches = new JSONArray(hoursBody);
            long totalSeconds = 0;
            for (int i = 0; i < watches.length(); i++) {
                JSONObject watch = watches.getJSONObject(i);
                totalSeconds += watch.optInt("watch_duration_seconds", 0);
            }
            stats.totalHours = totalSeconds / 3600.0f;
        }
    }
} catch (Exception e) {
    // Keep default value
}

// Fetch login streak
getLoginStreak(streak -> {
    stats.streakDays = streak;
    // Note: This is async, so you might need to handle callback differently
});
```

### E. Call trackLogin() in MainActivity

Add login tracking when the app starts:

```java
// In MainActivity.onCreate(), after checking authentication:
if (SupabaseManager.isLoggedIn()) {
    SupabaseManager.trackLogin(); // Track daily login
    // ... rest of initialization
}
```

## 3. Implementation Steps

1. **Create Database Tables**: Run the SQL scripts in Supabase SQL Editor
2. **Add Tracking Methods**: Add the tracking methods to `SupabaseManager.java`
3. **Integrate Video Tracking**: Modify `VideoAdapter.java` to track video playback
4. **Update Dashboard**: Update `getDashboardStats` to fetch real data
5. **Track Logins**: Add `trackLogin()` call in `MainActivity.onCreate()`
6. **Test**: Test the tracking by watching videos and checking the dashboard

## 4. Additional Considerations

### Privacy & Performance
- Batch video watch events if user watches many videos quickly
- Consider tracking only completed watches (>= 80% watched)
- Add local caching to reduce API calls

### Edge Cases
- Handle offline scenarios (queue events to sync later)
- Handle timezone differences for streak calculation
- Prevent duplicate tracking if user watches same video multiple times

### Analytics Queries

You can create additional views/functions for analytics:

```sql
-- View: Daily video watch summary
CREATE VIEW daily_video_summary AS
SELECT 
  user_id,
  DATE(created_at) as watch_date,
  COUNT(DISTINCT video_id) as unique_videos_watched,
  SUM(watch_duration_seconds) / 3600.0 as total_hours_watched,
  COUNT(*) as total_watch_events
FROM video_watches
GROUP BY user_id, DATE(created_at);

-- Function: Get total hours watched
CREATE OR REPLACE FUNCTION get_total_hours_watched(p_user_id UUID)
RETURNS REAL AS $$
BEGIN
  RETURN (
    SELECT COALESCE(SUM(watch_duration_seconds) / 3600.0, 0)
    FROM video_watches
    WHERE user_id = p_user_id
  );
END;
$$ LANGUAGE plpgsql;
```

## 5. Testing

Test the implementation by:
1. Watching videos and verifying records in `video_watches` table
2. Checking that login streak increments correctly
3. Verifying dashboard shows correct totals
4. Testing edge cases (multiple watches, partial watches, etc.)

