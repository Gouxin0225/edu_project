ALTER TABLE `student_submission`
    ADD COLUMN IF NOT EXISTS `start_time` DATETIME DEFAULT NULL COMMENT '开始答题时间' AFTER `status`;

UPDATE `student_submission`
SET `start_time` = COALESCE(`start_time`, `submit_time`, NOW())
WHERE `start_time` IS NULL;

ALTER TABLE `survey_task`
    ADD COLUMN IF NOT EXISTS `status` TINYINT DEFAULT 0 COMMENT '状态: 0-草稿, 1-已发布' AFTER `target_teacher_id`;

UPDATE `survey_task`
SET `status` = 1
WHERE `status` IS NULL OR (`status` = 0 AND `is_deleted` = 0);
