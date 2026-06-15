-- AI 问答助手第六阶段：轻量知识库 / RAG 表结构

CREATE TABLE IF NOT EXISTS `ai_knowledge_document` (
  `id`              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '知识文档ID',
  `title`           VARCHAR(200) NOT NULL COMMENT '文档标题',
  `course_category` VARCHAR(50) NOT NULL COMMENT '课程方向',
  `knowledge_point` VARCHAR(100) DEFAULT NULL COMMENT '知识点',
  `source_type`     VARCHAR(20) NOT NULL COMMENT '来源类型: TEXT, FILE',
  `source_name`     VARCHAR(255) DEFAULT NULL COMMENT '来源文件名或录入来源',
  `visibility`      VARCHAR(20) DEFAULT 'PRIVATE' COMMENT '可见范围: PRIVATE, PUBLIC',
  `access_scope`    VARCHAR(20) DEFAULT 'TEACHER_ONLY' COMMENT '引用范围: STUDENT_SAFE, TEACHER_ONLY',
  `owner_id`        BIGINT NOT NULL COMMENT '创建者ID',
  `owner_role`      VARCHAR(20) NOT NULL COMMENT '创建者角色',
  `chunk_count`     INT DEFAULT 0 COMMENT '切片数量',
  `create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`      TINYINT DEFAULT 0 COMMENT '软删除标记',
  KEY `idx_course_visibility` (`course_category`, `visibility`),
  KEY `idx_owner_time` (`owner_id`, `owner_role`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI知识库文档表';

CREATE TABLE IF NOT EXISTS `ai_knowledge_chunk` (
  `id`              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '知识切片ID',
  `document_id`     BIGINT NOT NULL COMMENT '文档ID',
  `chunk_index`     INT NOT NULL COMMENT '切片序号',
  `course_category` VARCHAR(50) NOT NULL COMMENT '课程方向',
  `knowledge_point` VARCHAR(100) DEFAULT NULL COMMENT '知识点',
  `content`         TEXT NOT NULL COMMENT '切片内容',
  `source_title`    VARCHAR(200) NOT NULL COMMENT '来源标题',
  `source_name`     VARCHAR(255) DEFAULT NULL COMMENT '来源文件名',
  `visibility`      VARCHAR(20) DEFAULT 'PRIVATE' COMMENT '可见范围: PRIVATE, PUBLIC',
  `access_scope`    VARCHAR(20) DEFAULT 'TEACHER_ONLY' COMMENT '引用范围: STUDENT_SAFE, TEACHER_ONLY',
  `owner_id`        BIGINT NOT NULL COMMENT '创建者ID',
  `owner_role`      VARCHAR(20) NOT NULL COMMENT '创建者角色',
  `create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted`      TINYINT DEFAULT 0 COMMENT '软删除标记',
  KEY `idx_doc_index` (`document_id`, `chunk_index`),
  KEY `idx_course_knowledge` (`course_category`, `knowledge_point`),
  KEY `idx_visibility_owner` (`visibility`, `owner_id`, `owner_role`),
  FULLTEXT KEY `ft_chunk_content` (`content`, `source_title`, `knowledge_point`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI知识库切片表';
