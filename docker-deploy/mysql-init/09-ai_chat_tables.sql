-- AI 问答助手第二阶段表结构

CREATE TABLE IF NOT EXISTS `ai_chat_session` (
  `id`                BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '会话ID',
  `user_id`           BIGINT NOT NULL COMMENT '会话所属用户ID',
  `user_role`         VARCHAR(20) NOT NULL COMMENT '用户角色: TEACHER, STUDENT',
  `title`             VARCHAR(100) NOT NULL COMMENT '会话标题',
  `course_category`   VARCHAR(50) DEFAULT NULL COMMENT '课程方向',
  `knowledge_point`   VARCHAR(100) DEFAULT NULL COMMENT '知识点',
  `last_message_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '最近消息时间',
  `create_time`       DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`       DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`        TINYINT DEFAULT 0 COMMENT '软删除标记',
  KEY `idx_user_role_time` (`user_id`, `user_role`, `last_message_time`),
  KEY `idx_course_knowledge` (`course_category`, `knowledge_point`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI问答会话表';

CREATE TABLE IF NOT EXISTS `ai_chat_message` (
  `id`              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
  `session_id`      BIGINT NOT NULL COMMENT '所属会话ID',
  `user_id`         BIGINT NOT NULL COMMENT '消息所属用户ID',
  `user_role`       VARCHAR(20) NOT NULL COMMENT '用户角色: TEACHER, STUDENT',
  `message_role`    VARCHAR(20) NOT NULL COMMENT '消息角色: USER, ASSISTANT',
  `content`         TEXT NOT NULL COMMENT '消息内容',
  `course_category` VARCHAR(50) DEFAULT NULL COMMENT '课程方向',
  `knowledge_point` VARCHAR(100) DEFAULT NULL COMMENT '知识点',
  `mode`            VARCHAR(20) DEFAULT NULL COMMENT '回答模式: SIMPLE, DETAILED, LESSON_PLAN, QUESTION',
  `intent`          VARCHAR(30) DEFAULT NULL COMMENT '任务意图: LEARN, PLATFORM_QUERY, KNOWLEDGE_QA, QUESTION_GEN, QUESTION_REVIEW, GENERAL',
  `response_source` VARCHAR(30) DEFAULT NULL COMMENT '回答来源策略: AUTO, PLATFORM_ONLY, KNOWLEDGE_ONLY, GENERAL_AI',
  `source_summary` VARCHAR(1000) DEFAULT NULL COMMENT '回答来源说明',
  `model`           VARCHAR(100) DEFAULT NULL COMMENT 'AI模型名称',
  `status`          VARCHAR(20) DEFAULT 'finished' COMMENT '消息状态: generating, finished, interrupted, failed',
  `create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted`      TINYINT DEFAULT 0 COMMENT '软删除标记',
  KEY `idx_session_time` (`session_id`, `create_time`),
  KEY `idx_user_session` (`user_id`, `session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI问答消息表';

ALTER TABLE `ai_chat_message`
  ADD COLUMN IF NOT EXISTS `status` VARCHAR(20) DEFAULT 'finished' COMMENT '消息状态: generating, finished, interrupted, failed' AFTER `model`;

ALTER TABLE `ai_chat_message`
  ADD COLUMN IF NOT EXISTS `intent` VARCHAR(30) DEFAULT NULL COMMENT '任务意图: LEARN, PLATFORM_QUERY, KNOWLEDGE_QA, QUESTION_GEN, QUESTION_REVIEW, GENERAL' AFTER `mode`;

ALTER TABLE `ai_chat_message`
  ADD COLUMN IF NOT EXISTS `response_source` VARCHAR(30) DEFAULT NULL COMMENT '回答来源策略: AUTO, PLATFORM_ONLY, KNOWLEDGE_ONLY, GENERAL_AI' AFTER `intent`;

ALTER TABLE `ai_chat_message`
  ADD COLUMN IF NOT EXISTS `source_summary` VARCHAR(1000) DEFAULT NULL COMMENT '回答来源说明' AFTER `response_source`;
