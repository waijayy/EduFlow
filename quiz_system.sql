-- =====================================================
-- 8. QUIZ SYSTEM TABLES
-- =====================================================
-- Tables for storing category-based quizzes

-- Quizzes table
CREATE TABLE IF NOT EXISTS quizzes (
  id TEXT PRIMARY KEY,
  category TEXT NOT NULL,
  title TEXT NOT NULL,
  description TEXT,
  question_count INTEGER DEFAULT 0,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Quiz questions table
CREATE TABLE IF NOT EXISTS quiz_questions (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  quiz_id TEXT NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
  question_text TEXT NOT NULL,
  options JSONB NOT NULL,  -- Array of 4 options
  correct_index INTEGER NOT NULL,
  points INTEGER DEFAULT 10,
  order_index INTEGER NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Indexes for faster queries
CREATE INDEX IF NOT EXISTS idx_quizzes_category ON quizzes(category);
CREATE INDEX IF NOT EXISTS idx_quiz_questions_quiz_id ON quiz_questions(quiz_id);
CREATE INDEX IF NOT EXISTS idx_quiz_questions_order ON quiz_questions(quiz_id, order_index);

-- Enable Row Level Security (RLS)
ALTER TABLE quizzes ENABLE ROW LEVEL SECURITY;
ALTER TABLE quiz_questions ENABLE ROW LEVEL SECURITY;

-- Policy: Anyone can read quizzes (they're public content)
CREATE POLICY "Anyone can view quizzes"
  ON quizzes FOR SELECT
  USING (true);

-- Policy: Anyone can read quiz questions (they're public content)
CREATE POLICY "Anyone can view quiz questions"
  ON quiz_questions FOR SELECT
  USING (true);

-- =====================================================
-- 9. SAMPLE QUIZ DATA
-- =====================================================
-- Insert sample quizzes for each category

-- Math Quiz
INSERT INTO quizzes (id, category, title, description, question_count)
VALUES ('math_quiz', 'math', 'Mathematics Fundamentals Quiz', 'Test your knowledge of basic algebra and mathematics', 3)
ON CONFLICT (id) DO NOTHING;

INSERT INTO quiz_questions (quiz_id, question_text, options, correct_index, points, order_index)
VALUES 
  ('math_quiz', 'What is the value of x in the equation: 2x + 5 = 15?', 
   '["3", "5", "10", "7"]', 1, 10, 1),
  ('math_quiz', 'Which of the following is a prime number?', 
   '["12", "15", "17", "21"]', 2, 10, 2),
  ('math_quiz', 'What is the area of a circle with radius 5? (Use π ≈ 3.14)', 
   '["78.5", "31.4", "15.7", "50"]', 0, 10, 3)
ON CONFLICT DO NOTHING;

-- Photography Quiz
INSERT INTO quizzes (id, category, title, description, question_count)
VALUES ('photography_quiz', 'photography', 'Photography Basics Quiz', 'Test your understanding of camera settings and composition', 3)
ON CONFLICT (id) DO NOTHING;

INSERT INTO quiz_questions (quiz_id, question_text, options, correct_index, points, order_index)
VALUES 
  ('photography_quiz', 'What does ISO control in a camera?', 
   '["Exposure time", "Light sensitivity", "Aperture size", "Focus distance"]', 1, 10, 1),
  ('photography_quiz', 'Which aperture setting creates the shallowest depth of field?', 
   '["f/16", "f/8", "f/4", "f/1.8"]', 3, 10, 2),
  ('photography_quiz', 'What is the rule of thirds used for?', 
   '["Calculating exposure", "Composition", "Focus stacking", "White balance"]', 1, 10, 3)
ON CONFLICT DO NOTHING;

-- Programming Quiz
INSERT INTO quizzes (id, category, title, description, question_count)
VALUES ('programming_quiz', 'programming', 'Programming Fundamentals Quiz', 'Test your knowledge of programming concepts', 3)
ON CONFLICT (id) DO NOTHING;

INSERT INTO quiz_questions (quiz_id, question_text, options, correct_index, points, order_index)
VALUES 
  ('programming_quiz', 'What is a variable in programming?', 
   '["A fixed value", "A container for storing data", "A type of loop", "A function"]', 1, 10, 1),
  ('programming_quiz', 'Which data structure uses LIFO (Last In First Out)?', 
   '["Queue", "Array", "Stack", "HashMap"]', 2, 10, 2),
  ('programming_quiz', 'What does "OOP" stand for?', 
   '["Online Operating Platform", "Object-Oriented Programming", "Optimized Output Process", "Open-Source Protocol"]', 1, 10, 3)
ON CONFLICT DO NOTHING;

-- Business Quiz
INSERT INTO quizzes (id, category, title, description, question_count)
VALUES ('business_quiz', 'business', 'Business Strategy Quiz', 'Test your understanding of business concepts and strategy', 3)
ON CONFLICT (id) DO NOTHING;

INSERT INTO quiz_questions (quiz_id, question_text, options, correct_index, points, order_index)
VALUES 
  ('business_quiz', 'What does ROI stand for?', 
   '["Return on Investment", "Rate of Interest", "Revenue Over Income", "Risk of Inflation"]', 0, 10, 1),
  ('business_quiz', 'What is a SWOT analysis used for?', 
   '["Financial reporting", "Strategic planning", "Employee evaluation", "Product pricing"]', 1, 10, 2),
  ('business_quiz', 'Which term describes a company''s unique selling point?', 
   '["Market share", "Competitive advantage", "Revenue stream", "Cost structure"]', 1, 10, 3)
ON CONFLICT DO NOTHING;

-- Design Quiz
INSERT INTO quizzes (id, category, title, description, question_count)
VALUES ('design_quiz', 'design', 'Design Principles Quiz', 'Test your knowledge of UI/UX design principles', 3)
ON CONFLICT (id) DO NOTHING;

INSERT INTO quiz_questions (quiz_id, question_text, options, correct_index, points, order_index)
VALUES 
  ('design_quiz', 'What does UI stand for?', 
   '["Universal Interface", "User Interface", "Unified Integration", "Update Indicator"]', 1, 10, 1),
  ('design_quiz', 'Which color scheme uses colors opposite on the color wheel?', 
   '["Analogous", "Monochromatic", "Complementary", "Triadic"]', 2, 10, 2),
  ('design_quiz', 'What is whitespace in design?', 
   '["Empty space between elements", "Background color", "Text color", "Border style"]', 0, 10, 3)
ON CONFLICT DO NOTHING;

-- English Quiz
INSERT INTO quizzes (id, category, title, description, question_count)
VALUES ('english_quiz', 'english', 'English Grammar Quiz', 'Test your knowledge of English grammar and language rules', 3)
ON CONFLICT (id) DO NOTHING;

INSERT INTO quiz_questions (quiz_id, question_text, options, correct_index, points, order_index)
VALUES 
  ('english_quiz', 'Which sentence is grammatically correct?', 
   '["She don''t like apples", "She doesn''t likes apples", "She doesn''t like apples", "She don''t likes apples"]', 2, 10, 1),
  ('english_quiz', 'What is a noun?', 
   '["An action word", "A describing word", "A person, place, or thing", "A connecting word"]', 2, 10, 2),
  ('english_quiz', 'Which is the past tense of "run"?', 
   '["Runned", "Running", "Ran", "Runs"]', 2, 10, 3)
ON CONFLICT DO NOTHING;
