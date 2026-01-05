# Debugging Video Tracking - No Data in video_watches Table

## Issue
No data is being added to the `video_watches` table in Supabase.

## Debugging Steps

### 1. Check Logcat Output

Run the app and watch a video for at least 5 seconds. Look for these log messages in Logcat:

**Filter by:** `VideoAdapter` or `SupabaseManager`

**Expected logs:**
```
VideoAdapter: Playback state changed: position=X, state=3 (STATE_READY)
VideoAdapter: Video ready: videoId=X, duration=XXs
VideoAdapter: IsPlaying changed: position=X, isPlaying=true
VideoAdapter: Started tracking: videoId=X
VideoAdapter: === SAVE VIDEO WATCH ===
VideoAdapter: Calling upsertVideoWatch: watchPercentage=X.XX, completed=true/false
SupabaseManager: upsertVideoWatch called: videoId=X, watchDuration=XXs, videoDuration=XXs
SupabaseManager: Successfully saved video watch: videoId=X, duration=XXs
```

**If you see errors:**
- `Missing userId or token` → Authentication issue
- `Failed to upsert video watch` → Database/API issue
- `SKIPPING SAVE` → Watch duration too short or video duration not set

### 2. Verify Database Setup

**Check if table exists:**
1. Go to Supabase Dashboard → SQL Editor
2. Run: `SELECT * FROM video_watches LIMIT 1;`
3. If error: Table doesn't exist → Run `database_setup.sql`

**Check RLS policies:**
1. Go to Supabase Dashboard → Authentication → Policies
2. Verify `video_watches` table has policies for:
   - SELECT (Users can view own video watches)
   - INSERT (Users can insert own video watches)
   - UPDATE (Users can update own video watches)

**Check unique constraint:**
1. Go to Supabase Dashboard → Table Editor → video_watches
2. Check if there's a unique constraint on `(user_id, video_id)`

### 3. Test Database Connection

**Manual test in Supabase SQL Editor:**
```sql
-- Check if you can insert (replace with your actual user_id)
INSERT INTO video_watches (user_id, video_id, watch_duration_seconds, video_duration_seconds, watch_percentage, completed)
VALUES (
  'your-user-id-here',
  'test-video-1',
  10,
  60,
  0.17,
  false
);
```

If this fails, check:
- RLS policies
- User authentication
- Table structure

### 4. Common Issues

#### Issue: "Missing userId or token"
**Solution:**
- Ensure user is logged in
- Check `SupabaseManager.isLoggedIn()` returns true
- Verify token is saved in SharedPreferences

#### Issue: "Failed to upsert video watch - Status: 401"
**Solution:**
- Authentication token expired
- User needs to log out and log back in
- Check token refresh logic

#### Issue: "Failed to upsert video watch - Status: 403"
**Solution:**
- RLS policy blocking the request
- Check if policies allow INSERT/UPDATE for authenticated users
- Verify user_id matches authenticated user

#### Issue: "Failed to upsert video watch - Status: 400"
**Solution:**
- Invalid data format
- Check JSON payload in logs
- Verify all required fields are present

#### Issue: "SKIPPING SAVE - watchDuration < MIN_WATCH_DURATION"
**Solution:**
- Video must be watched for at least 5 seconds
- Check if video duration is being set correctly
- Verify tracking is working (check logs)

#### Issue: Video duration is 0
**Solution:**
- Video might not be loading properly
- Check if `STATE_READY` is being triggered
- Verify ExoPlayer is getting video metadata

### 5. Manual Testing

**Test tracking manually:**
1. Watch a video for 10+ seconds
2. Check Logcat for "SAVE VIDEO WATCH" message
3. Verify watchDuration >= 5 seconds
4. Check if "Calling upsertVideoWatch" appears
5. Check SupabaseManager logs for API response

**Test database directly:**
1. Go to Supabase Dashboard → Table Editor
2. Try inserting a record manually
3. If successful, the issue is in the app code
4. If failed, the issue is in database setup

### 6. Verify Supabase Configuration

**Check Supabase URL and Key:**
- File: `app/src/main/java/com/example/eduflow/auth/SupabaseManager.java`
- Verify `SUPABASE_URL` is correct
- Verify `SUPABASE_KEY` is correct (anon key)

**Check API endpoint:**
- URL should be: `https://your-project.supabase.co/rest/v1/video_watches`
- Verify in logs: "Sending request to: ..."

### 7. Network Debugging

**Check network requests:**
1. Enable network logging in Android Studio
2. Use Charles Proxy or similar tool
3. Verify HTTP requests are being sent
4. Check response status codes

**Common network issues:**
- No internet connection
- Firewall blocking requests
- Supabase service down
- CORS issues (shouldn't affect mobile app)

### 8. Quick Fix Checklist

- [ ] Database table `video_watches` exists
- [ ] RLS policies are enabled and correct
- [ ] Unique constraint on `(user_id, video_id)` exists
- [ ] User is logged in (`SupabaseManager.isLoggedIn()` returns true)
- [ ] Video is watched for at least 5 seconds
- [ ] Video duration is being set (check logs)
- [ ] No errors in Logcat
- [ ] Supabase URL and key are correct

## Next Steps

If after checking all above, data still isn't being saved:

1. **Share Logcat output** - Filter by `VideoAdapter` and `SupabaseManager`
2. **Check Supabase logs** - Go to Dashboard → Logs → API Logs
3. **Test with Postman** - Try the same API call manually
4. **Verify user_id** - Check if the user_id in the app matches Supabase auth.users

## Expected Behavior

When working correctly:
1. User watches video for 5+ seconds
2. Log shows "SAVE VIDEO WATCH" with correct data
3. Log shows "Successfully saved video watch"
4. Record appears in `video_watches` table
5. Dashboard refreshes and shows updated count

