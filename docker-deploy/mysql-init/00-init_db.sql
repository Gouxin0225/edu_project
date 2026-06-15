-- ==========================================================
-- 内部课程考试、作业及调查平台 数据库设计 V3.1
-- ==========================================================

-- 1. 班级表
CREATE TABLE `sys_class` (
  `id`          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '班级ID',
  `class_name`  VARCHAR(100) NOT NULL COMMENT '班级名称(如: Java全栈2401期)',
  `status`      TINYINT DEFAULT 1 COMMENT '状态: 1-进行中, 0-已结课',
  `school_name` VARCHAR(100) DEFAULT NULL COMMENT '所属学校',
  `allow_student_apply` TINYINT DEFAULT 1 COMMENT '是否允许学生申请加入: 1-允许,0-不允许',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`  TINYINT DEFAULT 0 COMMENT '软删除标记: 0-未删除, 1-已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级信息表';

-- 2. 用户表
CREATE TABLE `sys_user` (
  `id`          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
  `username`    VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名/学号(登录用)',
  `password`    VARCHAR(100) NOT NULL COMMENT 'BCrypt密码哈希',
  `real_name`   VARCHAR(50)  NOT NULL COMMENT '真实姓名',
  `role`        VARCHAR(20)  NOT NULL COMMENT '角色: ADMIN, TEACHER, ASSISTANT, STUDENT',
  `phone`       VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `school_name` VARCHAR(100) DEFAULT NULL COMMENT '所属学校',
  `create_source` VARCHAR(30) DEFAULT NULL COMMENT '账号来源',
  `class_id`    BIGINT DEFAULT NULL COMMENT '所属班级ID(仅学生使用)',
  `created_by`  BIGINT DEFAULT NULL COMMENT '创建人ID',
  `must_change_password` TINYINT DEFAULT 0 COMMENT '是否必须修改密码: 1-必须修改, 0-不需要',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted`  TINYINT DEFAULT 0 COMMENT '软删除标记',
  UNIQUE KEY `uk_sys_user_phone` (`phone`),
  KEY `idx_sys_user_phone_role` (`phone`, `role`),
  KEY `idx_class_id`   (`class_id`),
  KEY `idx_created_by` (`created_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 3. 讲师-班级多对多关联表
CREATE TABLE `teacher_class_rel` (
  `id`          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `teacher_id`  BIGINT NOT NULL COMMENT '讲师用户ID',
  `class_id`    BIGINT NOT NULL COMMENT '班级ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '关联创建时间',
  UNIQUE KEY `uk_teacher_class` (`teacher_id`, `class_id`),
  KEY `idx_teacher_id` (`teacher_id`),
  KEY `idx_class_id`   (`class_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='讲师-班级多对多关联表';

-- 3.1 学生-班级多对多关联表
CREATE TABLE `class_student_rel` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `class_id` BIGINT NOT NULL COMMENT '班级ID',
  `student_id` BIGINT NOT NULL COMMENT '学生用户ID',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '关系状态: ACTIVE, LEFT, GRADUATED',
  `join_source` VARCHAR(30) DEFAULT NULL COMMENT '加入来源',
  `created_by` BIGINT DEFAULT NULL COMMENT '创建人/审核人ID',
  `join_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `leave_time` DATETIME DEFAULT NULL COMMENT '离开时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY `uk_class_student` (`class_id`, `student_id`),
  KEY `idx_student_status` (`student_id`, `status`),
  KEY `idx_class_status` (`class_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生班级关系表';

-- 3.2 学生入班申请表
CREATE TABLE `class_join_application` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `class_id` BIGINT NOT NULL COMMENT '班级ID',
  `student_id` BIGINT NOT NULL COMMENT '学生用户ID',
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '申请状态: PENDING, APPROVED, REJECTED, CANCELED',
  `apply_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `auditor_id` BIGINT DEFAULT NULL COMMENT '审核人ID',
  `reject_reason` VARCHAR(500) DEFAULT NULL COMMENT '拒绝原因',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY `idx_student_class_status` (`student_id`, `class_id`, `status`),
  KEY `idx_class_status_time` (`class_id`, `status`, `apply_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生入班申请表';

-- 4. 智能题库表
CREATE TABLE `question_bank` (
  `id`              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '题目ID',
  `course_category` VARCHAR(50)  NOT NULL COMMENT '课程方向',
  `knowledge_point` VARCHAR(100) NOT NULL COMMENT '知识点',
  `type`            VARCHAR(20)  NOT NULL COMMENT '题型: SINGLE, MULTIPLE, JUDGE, SHORT, CODE',
  `difficulty`      VARCHAR(10)  NOT NULL COMMENT '难度: EASY, MEDIUM, HARD',
  `content`         TEXT NOT NULL COMMENT '题干内容',
  `options_json`    JSON DEFAULT NULL COMMENT '客观题选项',
  `standard_answer` TEXT NOT NULL COMMENT '标准答案',
  `analysis`        TEXT DEFAULT NULL COMMENT '详细解析',
  `creator_id`      BIGINT NOT NULL COMMENT '出题人ID',
  `is_ai_generated` TINYINT DEFAULT 0 COMMENT '是否由AI生成',
  `is_public`       TINYINT DEFAULT 1 COMMENT '是否公开: 0-私有, 1-公开',
  `create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted`      TINYINT DEFAULT 0 COMMENT '软删除标记',
  KEY `idx_course_knowledge` (`course_category`, `knowledge_point`),
  KEY `idx_type_difficulty`  (`type`, `difficulty`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能题库表';

-- 5. 评估任务主表
CREATE TABLE `assessment_task` (
  `id`               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '任务ID',
  `title`            VARCHAR(200) NOT NULL COMMENT '任务标题',
  `type`             VARCHAR(20)  NOT NULL COMMENT '任务类型: EXAM, HOMEWORK',
  `start_time`       DATETIME DEFAULT NULL COMMENT '开始时间',
  `end_time`         DATETIME NOT NULL COMMENT '截止时间',
  `total_score`      INT DEFAULT 100 COMMENT '试卷总分',
  `creator_id`       BIGINT NOT NULL COMMENT '发布讲师ID',
  `target_class_ids` JSON NOT NULL COMMENT '目标班级ID集合',
  `setting_json`     JSON DEFAULT NULL COMMENT '高级设置',
  `create_time`      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted`       TINYINT DEFAULT 0 COMMENT '软删除标记'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评估任务主表';

-- 6. 试卷-题目关联表
CREATE TABLE `task_question_rel` (
  `id`          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `task_id`     BIGINT NOT NULL COMMENT '任务ID',
  `question_id` BIGINT NOT NULL COMMENT '题目ID',
  `score`       INT NOT NULL COMMENT '单题满分分值',
  `sort_order`  INT NOT NULL COMMENT '排序号',
  KEY `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务题目关联表';

-- 7. 学生提交记录主表
CREATE TABLE `student_submission` (
  `id`                   BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '提交记录ID',
  `task_id`              BIGINT NOT NULL COMMENT '任务ID',
  `student_id`           BIGINT NOT NULL COMMENT '学生ID',
  `status`               VARCHAR(20) NOT NULL COMMENT '状态: UN, SUBMITTED, GRADED, RETURNED',
  `start_time`           DATETIME DEFAULT NULL COMMENT '开始答题时间',
  `total_score_gained`   DECIMAL(5,1) DEFAULT NULL COMMENT '最终总得分',
  `teacher_comment`      TEXT DEFAULT NULL COMMENT '讲师总评语',
  `assistant_comment`    TEXT DEFAULT NULL COMMENT '班主任批注',
  `submit_time`          DATETIME DEFAULT NULL COMMENT '实际交卷时间',
  `version`              INT DEFAULT 1 COMMENT '提交版本号',
  `parent_submission_id` BIGINT DEFAULT NULL COMMENT '上一版本提交记录ID',
  `switch_screen_count`  INT DEFAULT 0 COMMENT '考试切屏次数',
  KEY `idx_task_student` (`task_id`, `student_id`),
  KEY `idx_parent`       (`parent_submission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生提交记录主表';

-- 8. 作业提交内容表
CREATE TABLE `homework_submission_content` (
  `id`            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '内容ID',
  `submission_id` BIGINT NOT NULL COMMENT '关联 student_submission.id',
  `content`       TEXT DEFAULT NULL COMMENT '作业文字内容',
  `git_link`      VARCHAR(500) DEFAULT NULL COMMENT 'Git仓库链接',
  `file_url`      VARCHAR(255) DEFAULT NULL COMMENT '上传附件文件名',
  `create_time`   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  KEY `idx_submission_id` (`submission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业提交内容表';

-- 9. 学生答题明细表
CREATE TABLE `student_answer_detail` (
  `id`             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '明细ID',
  `submission_id`  BIGINT NOT NULL COMMENT '提交主表ID',
  `question_id`    BIGINT NOT NULL COMMENT '题目ID',
  `student_answer` TEXT DEFAULT NULL COMMENT '学生提交的答案',
  `is_correct`     TINYINT DEFAULT NULL COMMENT '是否正确: 1-正确, 0-错误, 2-部分正确',
  `score_gained`   DECIMAL(5,1) DEFAULT 0 COMMENT '该题实际得分',
  KEY `idx_submission_id`    (`submission_id`),
  KEY `idx_student_question` (`submission_id`, `question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生答题明细表';

-- 10. 学生智能错题本
CREATE TABLE `student_mistake_book` (
  `id`              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '错题本记录ID',
  `student_id`      BIGINT NOT NULL COMMENT '学生ID',
  `question_id`     BIGINT NOT NULL COMMENT '错题ID',
  `wrong_count`     INT DEFAULT 1 COMMENT '累计答错次数',
  `last_wrong_time` DATETIME COMMENT '最近一次答错时间',
  `is_mastered`     TINYINT DEFAULT 0 COMMENT '是否已攻克: 1-已掌握, 0-待攻克',
  UNIQUE KEY `uk_student_question` (`student_id`, `question_id`),
  KEY `idx_student_id`   (`student_id`),
  KEY `idx_is_mastered`  (`is_mastered`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生智能错题本';

-- 11. 问卷任务表
CREATE TABLE `survey_task` (
  `id`                    BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '问卷ID',
  `title`                 VARCHAR(200) NOT NULL COMMENT '问卷标题',
  `end_time`              DATETIME NOT NULL COMMENT '截止填写时间',
  `creator_id`            BIGINT NOT NULL COMMENT '发布人ID',
  `target_class_ids`      JSON NOT NULL COMMENT '目标班级ID集合',
  `is_anonymous_required` TINYINT DEFAULT 1 COMMENT '是否强制匿名',
  `target_teacher_id`     BIGINT DEFAULT NULL COMMENT '被评价讲师ID',
  `status`                TINYINT DEFAULT 0 COMMENT '状态: 0-草稿, 1-已发布',
  `create_time`           DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted`            TINYINT DEFAULT 0 COMMENT '软删除标记',
  KEY `idx_target_teacher` (`target_teacher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问卷调查任务主表';

-- 12. 问卷题目配置表
CREATE TABLE `survey_question` (
  `id`           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '问卷题目ID',
  `survey_id`    BIGINT NOT NULL COMMENT '所属问卷ID',
  `type`         VARCHAR(20)  NOT NULL COMMENT '题型: STAR, NPS, SCALE, TEXT',
  `title`        VARCHAR(255) NOT NULL COMMENT '题干',
  `options_json` JSON DEFAULT NULL COMMENT '选项配置',
  `is_required`  TINYINT DEFAULT 1 COMMENT '是否必填',
  `sort_order`   INT NOT NULL COMMENT '题目排序',
  KEY `idx_survey_id` (`survey_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问卷动态表单题目表';

-- 13. 问卷反馈记录主表
CREATE TABLE `survey_record` (
  `id`          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '反馈记录ID',
  `survey_id`   BIGINT NOT NULL COMMENT '问卷ID',
  `student_id`  BIGINT DEFAULT NULL COMMENT '学生ID',
  `client_ip`   VARCHAR(50) DEFAULT NULL COMMENT '提交者IP',
  `submit_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  KEY `idx_survey_id` (`survey_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问卷意见反馈记录表';

-- 14. 问卷反馈明细表
CREATE TABLE `survey_answer_detail` (
  `id`                 BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '明细ID',
  `record_id`          BIGINT NOT NULL COMMENT '反馈记录ID',
  `survey_question_id` BIGINT NOT NULL COMMENT '问卷题目ID',
  `answer_value`       TEXT NOT NULL COMMENT '具体打分或意见',
  KEY `idx_record_id`   (`record_id`),
  KEY `idx_question_id` (`survey_question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='问卷反馈明细表';

-- 15. AI问答会话表
CREATE TABLE `ai_chat_session` (
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

-- 16. AI问答消息表
CREATE TABLE `ai_chat_message` (
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
  `create_time`     DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted`      TINYINT DEFAULT 0 COMMENT '软删除标记',
  KEY `idx_session_time` (`session_id`, `create_time`),
  KEY `idx_user_session` (`user_id`, `session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI问答消息表';
