-- =====================================================
-- VIDEO_NOTES TABLE FIX
-- =====================================================
-- Run this in your Supabase SQL Editor if you get RLS errors

-- First, check if table exists, if not create it
CREATE TABLE IF NOT EXISTS video_notes (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  video_id TEXT NOT NULL,
  timestamp_seconds INTEGER NOT NULL DEFAULT 0,
  note_content TEXT NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_video_notes_user_id ON video_notes(user_id);
CREATE INDEX IF NOT EXISTS idx_video_notes_video_id ON video_notes(video_id);
CREATE INDEX IF NOT EXISTS idx_video_notes_user_video ON video_notes(user_id, video_id);

-- Enable RLS
ALTER TABLE video_notes ENABLE ROW LEVEL SECURITY;

-- Drop existing policies if any (to avoid conflicts)
DROP POLICY IF EXISTS "Users can view own video notes" ON video_notes;
DROP POLICY IF EXISTS "Users can insert own video notes" ON video_notes;
DROP POLICY IF EXISTS "Users can update own video notes" ON video_notes;
DROP POLICY IF EXISTS "Users can delete own video notes" ON video_notes;

-- Create policies
CREATE POLICY "Users can view own video notes"
  ON video_notes FOR SELECT
  USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own video notes"
  ON video_notes FOR INSERT
  WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own video notes"
  ON video_notes FOR UPDATE
  USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own video notes"
  ON video_notes FOR DELETE
  USING (auth.uid() = user_id);

-- Grant permissions to authenticated users
GRANT ALL ON video_notes TO authenticated;
GRANT USAGE ON SCHEMA public TO authenticated;
