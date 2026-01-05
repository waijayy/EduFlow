-- =====================================================
-- EduFlow Database Setup Script for Supabase
-- =====================================================
-- Run this script in your Supabase SQL Editor
-- This script creates all necessary tables, indexes, 
-- functions, and policies for video tracking, hours 
-- watched, and login streak functionality.
-- =====================================================

-- =====================================================
-- 1. VIDEO_WATCHES TABLE
-- =====================================================
-- Tracks individual video watch events with upsert support
-- Uses unique constraint on (user_id, video_id) to prevent duplicates

CREATE TABLE IF NOT EXISTS video_watches (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  video_id TEXT NOT NULL,
  watch_duration_seconds INTEGER NOT NULL DEFAULT 0,
  video_duration_seconds INTEGER NOT NULL DEFAULT 0,
  watch_percentage REAL DEFAULT 0.0 CHECK (watch_percentage >= 0.0 AND watch_percentage <= 1.0),
  completed BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  -- Unique constraint to prevent duplicates (enables upsert)
  UNIQUE(user_id, video_id)
);

-- Indexes for faster queries
CREATE INDEX IF NOT EXISTS idx_video_watches_user_id ON video_watches(user_id);
CREATE INDEX IF NOT EXISTS idx_video_watches_video_id ON video_watches(video_id);
CREATE INDEX IF NOT EXISTS idx_video_watches_created_at ON video_watches(created_at);
CREATE INDEX IF NOT EXISTS idx_video_watches_completed ON video_watches(completed) WHERE completed = true;

-- Trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_video_watches_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_video_watches_updated_at
  BEFORE UPDATE ON video_watches
  FOR EACH ROW
  EXECUTE FUNCTION update_video_watches_updated_at();

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

-- Policy: Users can update their own video watches (for upsert)
CREATE POLICY "Users can update own video watches"
  ON video_watches FOR UPDATE
  USING (auth.uid() = user_id);

-- =====================================================
-- 2. USER_ACTIVITY_LOGS TABLE
-- =====================================================
-- Tracks daily user activity for login streaks and daily stats

CREATE TABLE IF NOT EXISTS user_activity_logs (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  activity_date DATE NOT NULL DEFAULT CURRENT_DATE,
  login_count INTEGER DEFAULT 0,
  videos_watched INTEGER DEFAULT 0,
  hours_watched REAL DEFAULT 0.0,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  -- Unique constraint: one record per user per day
  UNIQUE(user_id, activity_date)
);

-- Indexes for faster queries
CREATE INDEX IF NOT EXISTS idx_user_activity_logs_user_id ON user_activity_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_user_activity_logs_activity_date ON user_activity_logs(activity_date);
CREATE INDEX IF NOT EXISTS idx_user_activity_logs_user_date ON user_activity_logs(user_id, activity_date DESC);

-- Trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_user_activity_logs_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_user_activity_logs_updated_at
  BEFORE UPDATE ON user_activity_logs
  FOR EACH ROW
  EXECUTE FUNCTION update_user_activity_logs_updated_at();

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

-- =====================================================
-- 3. CALCULATE LOGIN STREAK FUNCTION
-- =====================================================
-- Calculates the current consecutive login streak for a user

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
    AND login_count > 0
  ) THEN
    streak_count := 1;
    previous_date := current_date - INTERVAL '1 day';
    
    -- Count consecutive days backwards
    WHILE EXISTS (
      SELECT 1 FROM user_activity_logs 
      WHERE user_id = p_user_id 
      AND activity_date = previous_date
      AND login_count > 0
    ) LOOP
      streak_count := streak_count + 1;
      previous_date := previous_date - INTERVAL '1 day';
    END LOOP;
  END IF;
  
  RETURN streak_count;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Grant execute permission to authenticated users
GRANT EXECUTE ON FUNCTION calculate_login_streak(UUID) TO authenticated;

-- =====================================================
-- 4. HELPER FUNCTIONS (Optional but useful)
-- =====================================================

-- Function: Get total hours watched for a user
CREATE OR REPLACE FUNCTION get_total_hours_watched(p_user_id UUID)
RETURNS REAL AS $$
BEGIN
  RETURN (
    SELECT COALESCE(SUM(watch_duration_seconds) / 3600.0, 0)
    FROM video_watches
    WHERE user_id = p_user_id
  );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

GRANT EXECUTE ON FUNCTION get_total_hours_watched(UUID) TO authenticated;

-- Function: Get total unique videos watched
CREATE OR REPLACE FUNCTION get_total_videos_watched(p_user_id UUID)
RETURNS INTEGER AS $$
BEGIN
  RETURN (
    SELECT COUNT(DISTINCT video_id)
    FROM video_watches
    WHERE user_id = p_user_id
  );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

GRANT EXECUTE ON FUNCTION get_total_videos_watched(UUID) TO authenticated;

-- =====================================================
-- 5. VIEWS FOR ANALYTICS (Optional)
-- =====================================================

-- View: Daily video watch summary
CREATE OR REPLACE VIEW daily_video_summary AS
SELECT 
  user_id,
  DATE(created_at) as watch_date,
  COUNT(DISTINCT video_id) as unique_videos_watched,
  SUM(watch_duration_seconds) / 3600.0 as total_hours_watched,
  COUNT(*) as total_watch_events,
  SUM(CASE WHEN completed THEN 1 ELSE 0 END) as completed_videos
FROM video_watches
GROUP BY user_id, DATE(created_at);

-- Grant access to authenticated users
GRANT SELECT ON daily_video_summary TO authenticated;

-- =====================================================
-- VERIFICATION QUERIES (Run these to verify setup)
-- =====================================================

-- Check if tables exist
-- SELECT table_name FROM information_schema.tables 
-- WHERE table_schema = 'public' 
-- AND table_name IN ('video_watches', 'user_activity_logs');

-- Check if functions exist
-- SELECT routine_name FROM information_schema.routines 
-- WHERE routine_schema = 'public' 
-- AND routine_name IN ('calculate_login_streak', 'get_total_hours_watched', 'get_total_videos_watched');

-- Check RLS policies
-- SELECT tablename, policyname FROM pg_policies 
-- WHERE schemaname = 'public' 
-- AND tablename IN ('video_watches', 'user_activity_logs');

-- =====================================================
-- NOTES
-- =====================================================
-- 1. The video_watches table uses UNIQUE(user_id, video_id) to enable upsert
--    When inserting, if a record exists, it will be updated instead
-- 
-- 2. The user_activity_logs table tracks daily activity
--    Use upsert (INSERT ... ON CONFLICT) to update existing records
--
-- 3. Login streak is calculated by checking consecutive days
--    starting from today and going backwards
--
-- 4. All tables have RLS enabled for security
--    Users can only access their own data
--
-- 5. The functions use SECURITY DEFINER to allow execution
--    while still respecting RLS policies
-- =====================================================

