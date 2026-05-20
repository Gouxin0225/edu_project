-- AI知识库优化：区分学生可引用内容，并预留MySQL全文检索索引。

ALTER TABLE `ai_knowledge_document`
  ADD COLUMN IF NOT EXISTS `access_scope` VARCHAR(20) DEFAULT 'TEACHER_ONLY'
  COMMENT '引用范围: STUDENT_SAFE, TEACHER_ONLY' AFTER `visibility`;

ALTER TABLE `ai_knowledge_chunk`
  ADD COLUMN IF NOT EXISTS `access_scope` VARCHAR(20) DEFAULT 'TEACHER_ONLY'
  COMMENT '引用范围: STUDENT_SAFE, TEACHER_ONLY' AFTER `visibility`;

UPDATE `ai_knowledge_document`
SET `access_scope` = 'TEACHER_ONLY'
WHERE `access_scope` IS NULL OR `access_scope` = '';

UPDATE `ai_knowledge_chunk`
SET `access_scope` = 'TEACHER_ONLY'
WHERE `access_scope` IS NULL OR `access_scope` = '';

ALTER TABLE `ai_knowledge_chunk`
  ADD FULLTEXT INDEX `ft_chunk_content` (`content`, `source_title`, `knowledge_point`);
