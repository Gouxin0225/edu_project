CREATE TABLE IF NOT EXISTS `outline_document` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '大纲文档ID',
  `title` VARCHAR(200) NOT NULL COMMENT '标题',
  `original_filename` VARCHAR(255) NOT NULL COMMENT '原始文件名',
  `file_path` VARCHAR(500) NOT NULL COMMENT '文件路径',
  `course_category` VARCHAR(50) NOT NULL COMMENT '课程方向',
  `outline_text` LONGTEXT NOT NULL COMMENT '解析后的大纲文本',
  `plan_json` LONGTEXT NULL COMMENT '出题计划JSON',
  `status` VARCHAR(30) NOT NULL DEFAULT 'UPLOADED' COMMENT '状态',
  `creator_id` BIGINT NOT NULL COMMENT '创建人ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '软删除标记',
  PRIMARY KEY (`id`),
  KEY `idx_outline_creator` (`creator_id`, `create_time`),
  KEY `idx_outline_course` (`course_category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI大纲出卷文档表';

CREATE TABLE IF NOT EXISTS `outline_generated_question` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '待确认题目ID',
  `document_id` BIGINT NOT NULL COMMENT '大纲文档ID',
  `course_category` VARCHAR(50) NOT NULL COMMENT '课程方向',
  `knowledge_point` VARCHAR(100) NOT NULL DEFAULT '' COMMENT '知识点',
  `type` VARCHAR(20) NOT NULL COMMENT '题型',
  `difficulty` VARCHAR(10) NOT NULL COMMENT '难度',
  `content` TEXT NOT NULL COMMENT '题干内容',
  `options_json` LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL COMMENT '客观题选项' CHECK (json_valid(`options_json`)),
  `standard_answer` TEXT NOT NULL COMMENT '标准答案',
  `analysis` TEXT NULL COMMENT '解析',
  `selected` TINYINT NOT NULL DEFAULT 1 COMMENT '是否确认入库',
  `question_id` BIGINT NULL COMMENT '确认后题库题目ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_outline_question_doc` (`document_id`),
  KEY `idx_outline_question_type` (`type`, `difficulty`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI大纲出卷待确认题目表';
