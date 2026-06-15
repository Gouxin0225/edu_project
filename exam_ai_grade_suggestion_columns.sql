ALTER TABLE student_answer_detail
    ADD COLUMN IF NOT EXISTS ai_suggest_score DECIMAL(5,1) NULL COMMENT 'AI建议得分' AFTER score_gained,
    ADD COLUMN IF NOT EXISTS ai_suggest_detail TEXT NULL COMMENT 'AI建议详情JSON' AFTER ai_suggest_score;
