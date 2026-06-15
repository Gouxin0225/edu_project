-- 试卷模板表
CREATE TABLE IF NOT EXISTS `question_paper_template` (
  `id`               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '模板ID',
  `name`             VARCHAR(100) NOT NULL COMMENT '模板名称',
  `description`      VARCHAR(500) DEFAULT NULL COMMENT '模板描述',
  `course_id`        BIGINT DEFAULT NULL COMMENT '课程ID',
  `course_name`      VARCHAR(100) DEFAULT NULL COMMENT '课程名称',
  `total_score`      INT DEFAULT 100 COMMENT '总分',
  `question_count`   INT DEFAULT 0 COMMENT '题目数量',
  `creator_id`       BIGINT NOT NULL COMMENT '创建人ID',
  `creator_name`     VARCHAR(50) DEFAULT NULL COMMENT '创建人姓名',
  `created_at`       DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`       DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted`      TINYINT DEFAULT 0 COMMENT '软删除标记'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷模板表';

-- 模板题目关联表
CREATE TABLE IF NOT EXISTS `template_question_rel` (
  `id`              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `template_id`     BIGINT NOT NULL COMMENT '模板ID',
  `question_id`     BIGINT NOT NULL COMMENT '题目ID',
  `score`           INT NOT NULL COMMENT '分值',
  `sort_order`      INT NOT NULL COMMENT '排序号'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板题目关联表';