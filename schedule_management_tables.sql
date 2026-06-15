-- ==========================================================
-- 中心派课管理模块基础表
-- ==========================================================

CREATE TABLE IF NOT EXISTS `schedule_course` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '课程ID',
  `course_code` VARCHAR(50) NOT NULL COMMENT '课程编码',
  `course_name` VARCHAR(100) NOT NULL COMMENT '课程名称',
  `course_direction` VARCHAR(50) NOT NULL COMMENT '课程方向',
  `course_level` VARCHAR(20) NOT NULL COMMENT '课程层级: BASIC, INTERMEDIATE, ADVANCED',
  `course_type` VARCHAR(20) NOT NULL DEFAULT 'FIXED' COMMENT '课程类型: FIXED, CUSTOM',
  `teacher_capacity` INT DEFAULT 1 COMMENT '建议讲师承载人数',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE, DISABLED',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '软删除标记: 0-未删除,1-已删除',
  UNIQUE KEY `uk_schedule_course_code` (`course_code`),
  KEY `idx_schedule_course_level` (`course_level`, `status`),
  KEY `idx_schedule_course_direction` (`course_direction`, `status`),
  KEY `idx_schedule_course_type` (`course_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='中心派课课程基础表';

CREATE TABLE IF NOT EXISTS `schedule_course_prerequisite` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `course_id` BIGINT NOT NULL COMMENT '目标课程ID',
  `prerequisite_course_id` BIGINT NOT NULL COMMENT '前置课程ID',
  `rule_type` VARCHAR(30) NOT NULL DEFAULT 'COURSE_COMPLETED' COMMENT '规则类型',
  `required_status` VARCHAR(20) NOT NULL DEFAULT 'COMPLETED' COMMENT '要求状态',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE, DISABLED',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY `uk_schedule_course_prereq` (`course_id`, `prerequisite_course_id`),
  KEY `idx_schedule_prereq_course` (`course_id`),
  KEY `idx_schedule_prereq_required` (`prerequisite_course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='中心派课课程前置规则表';

CREATE TABLE IF NOT EXISTS `schedule_teacher_skill` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `teacher_id` BIGINT NOT NULL COMMENT '讲师用户ID',
  `course_id` BIGINT NOT NULL COMMENT '课程ID',
  `skill_level` VARCHAR(20) NOT NULL COMMENT '能力等级: ASSIST, TEACH, EXPERT',
  `certified` TINYINT DEFAULT 0 COMMENT '是否认证',
  `score` DECIMAL(5,2) DEFAULT NULL COMMENT '能力评分',
  `last_teach_time` DATETIME DEFAULT NULL COMMENT '最近授课时间',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE, DISABLED',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY `uk_schedule_teacher_course` (`teacher_id`, `course_id`),
  KEY `idx_schedule_skill_course` (`course_id`, `skill_level`, `status`),
  KEY `idx_schedule_skill_teacher` (`teacher_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='中心派课讲师能力表';

CREATE TABLE IF NOT EXISTS `schedule_teacher_profile` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `teacher_id` BIGINT NOT NULL COMMENT '讲师用户ID',
  `home_school_name` VARCHAR(100) DEFAULT NULL COMMENT '常驻校区',
  `dispatch_mode` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '派课模式: NORMAL, LOCAL_ONLY, REMOTE_ONLY, DISABLED',
  `dispatch_status` VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' COMMENT '派课状态: AVAILABLE, PAUSED, UNAVAILABLE',
  `max_parallel_plans` INT DEFAULT 1 COMMENT '最大并行计划数',
  `max_monthly_dispatch_days` INT DEFAULT NULL COMMENT '月最大派遣天数',
  `can_travel` TINYINT DEFAULT 1 COMMENT '是否可出差',
  `allow_cross_school` TINYINT DEFAULT 1 COMMENT '是否允许跨校区',
  `is_center_reserve` TINYINT DEFAULT 0 COMMENT '是否中心保底讲师',
  `priority` INT DEFAULT 100 COMMENT '优先级',
  `available_remark` VARCHAR(500) DEFAULT NULL COMMENT '可用性备注',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE, DISABLED',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY `uk_schedule_teacher_profile` (`teacher_id`),
  KEY `idx_schedule_teacher_profile_school` (`home_school_name`, `status`),
  KEY `idx_schedule_teacher_profile_mode` (`dispatch_mode`, `status`),
  KEY `idx_schedule_teacher_profile_dispatch` (`dispatch_status`, `is_center_reserve`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='中心派课讲师派课配置表';

CREATE TABLE IF NOT EXISTS `schedule_direction_reserve` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `school_name` VARCHAR(100) DEFAULT NULL COMMENT '校区名称',
  `direction_code` VARCHAR(50) NOT NULL COMMENT '方向编码',
  `course_level` VARCHAR(20) DEFAULT NULL COMMENT '课程层级',
  `min_teacher_count` INT NOT NULL DEFAULT 1 COMMENT '保底讲师数',
  `min_expert_count` INT DEFAULT 0 COMMENT '保底专家数',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE, DISABLED',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY `uk_schedule_direction_reserve` (`school_name`, `direction_code`, `course_level`),
  KEY `idx_schedule_direction_reserve_status` (`direction_code`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='中心派课方向保底配置表';

CREATE TABLE IF NOT EXISTS `schedule_campus_grade` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '校区年级ID',
  `school_name` VARCHAR(100) NOT NULL COMMENT '校区名称',
  `grade_name` VARCHAR(100) NOT NULL COMMENT '年级/期次名称',
  `class_id` BIGINT DEFAULT NULL COMMENT '关联班级ID',
  `major_direction` VARCHAR(50) DEFAULT NULL COMMENT '主方向',
  `student_count` INT DEFAULT 0 COMMENT '学生人数快照',
  `start_date` DATE DEFAULT NULL COMMENT '开班日期',
  `expected_graduate_date` DATE DEFAULT NULL COMMENT '预计毕业日期',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE, GRADUATED, SUSPENDED, CLOSED',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '软删除标记: 0-未删除,1-已删除',
  UNIQUE KEY `uk_schedule_grade_school_name` (`school_name`, `grade_name`),
  KEY `idx_schedule_grade_class` (`class_id`),
  KEY `idx_schedule_grade_school_status` (`school_name`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='中心派课校区年级表';

CREATE TABLE IF NOT EXISTS `schedule_campus_course_progress` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `campus_grade_id` BIGINT NOT NULL COMMENT '校区年级ID',
  `course_id` BIGINT NOT NULL COMMENT '课程ID',
  `expected_student_count` INT DEFAULT 0 COMMENT '预计学生人数',
  `progress_status` VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED' COMMENT '进度状态: NOT_STARTED, PLANNED, IN_PROGRESS, COMPLETED, NEED_MAKEUP, CANCELED',
  `progress_percent` DECIMAL(5,2) DEFAULT 0.00 COMMENT '进度百分比',
  `planned_start_date` DATE DEFAULT NULL COMMENT '计划开始日期',
  `planned_end_date` DATE DEFAULT NULL COMMENT '计划结束日期',
  `actual_start_date` DATE DEFAULT NULL COMMENT '实际开始日期',
  `actual_end_date` DATE DEFAULT NULL COMMENT '实际结束日期',
  `current_teacher_id` BIGINT DEFAULT NULL COMMENT '当前讲师ID',
  `last_plan_id` BIGINT DEFAULT NULL COMMENT '最近派遣计划ID',
  `completion_confirmed_by` BIGINT DEFAULT NULL COMMENT '完成确认人ID',
  `completion_confirmed_time` DATETIME DEFAULT NULL COMMENT '完成确认时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY `uk_schedule_grade_course` (`campus_grade_id`, `course_id`),
  KEY `idx_schedule_progress_status` (`progress_status`),
  KEY `idx_schedule_progress_course_status` (`course_id`, `progress_status`),
  KEY `idx_schedule_progress_teacher` (`current_teacher_id`),
  KEY `idx_schedule_progress_plan` (`last_plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='中心派课校区课程进度表';

CREATE TABLE IF NOT EXISTS `schedule_dispatch_plan` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '派遣计划ID',
  `plan_no` VARCHAR(50) NOT NULL COMMENT '派遣单号',
  `campus_grade_id` BIGINT NOT NULL COMMENT '校区年级ID',
  `course_id` BIGINT NOT NULL COMMENT '课程ID',
  `plan_type` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '计划类型',
  `plan_status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '计划状态',
  `priority` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '优先级',
  `expected_start_date` DATE DEFAULT NULL COMMENT '预计开始日期',
  `expected_end_date` DATE DEFAULT NULL COMMENT '预计结束日期',
  `dispatch_reason` VARCHAR(500) DEFAULT NULL COMMENT '派遣原因',
  `requirement_desc` VARCHAR(1000) DEFAULT NULL COMMENT '讲师要求',
  `created_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
  `submitted_time` DATETIME DEFAULT NULL COMMENT '提交时间',
  `approved_by` BIGINT DEFAULT NULL COMMENT '审批人ID',
  `approved_time` DATETIME DEFAULT NULL COMMENT '审批时间',
  `cancel_reason` VARCHAR(500) DEFAULT NULL COMMENT '取消原因',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '软删除标记: 0-未删除,1-已删除',
  UNIQUE KEY `uk_schedule_plan_no` (`plan_no`),
  KEY `idx_schedule_plan_grade_course` (`campus_grade_id`, `course_id`),
  KEY `idx_schedule_plan_status_time` (`plan_status`, `expected_start_date`),
  KEY `idx_schedule_plan_creator` (`created_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='中心派课派遣计划主表';

CREATE TABLE IF NOT EXISTS `schedule_dispatch_plan_teacher` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `plan_id` BIGINT NOT NULL COMMENT '派遣计划ID',
  `teacher_id` BIGINT NOT NULL COMMENT '讲师ID',
  `teacher_role` VARCHAR(20) NOT NULL DEFAULT 'MAIN' COMMENT '讲师角色',
  `match_score` DECIMAL(5,2) DEFAULT NULL COMMENT '匹配分',
  `match_reason` VARCHAR(500) DEFAULT NULL COMMENT '匹配原因',
  `assign_status` VARCHAR(20) NOT NULL DEFAULT 'CANDIDATE' COMMENT '指派状态',
  `confirm_time` DATETIME DEFAULT NULL COMMENT '确认时间',
  `reject_reason` VARCHAR(500) DEFAULT NULL COMMENT '拒绝原因',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY `uk_schedule_plan_teacher_role` (`plan_id`, `teacher_id`, `teacher_role`),
  KEY `idx_schedule_plan_teacher_teacher` (`teacher_id`, `assign_status`),
  KEY `idx_schedule_plan_teacher_plan` (`plan_id`, `assign_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='中心派课派遣计划讲师表';

CREATE TABLE IF NOT EXISTS `schedule_dispatch_feedback` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '反馈ID',
  `plan_id` BIGINT NOT NULL COMMENT '派遣计划ID',
  `campus_grade_id` BIGINT NOT NULL COMMENT '校区年级ID',
  `course_id` BIGINT NOT NULL COMMENT '课程ID',
  `teacher_id` BIGINT NOT NULL COMMENT '讲师ID',
  `feedback_type` VARCHAR(20) NOT NULL DEFAULT 'PROGRESS' COMMENT '反馈类型',
  `execution_status` VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS' COMMENT '执行状态',
  `progress_percent` DECIMAL(5,2) DEFAULT NULL COMMENT '反馈进度',
  `feedback_content` TEXT DEFAULT NULL COMMENT '反馈内容',
  `issue_desc` VARCHAR(1000) DEFAULT NULL COMMENT '问题说明',
  `next_action` VARCHAR(1000) DEFAULT NULL COMMENT '后续动作',
  `feedback_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '反馈时间',
  `created_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  KEY `idx_schedule_feedback_plan_time` (`plan_id`, `feedback_time`),
  KEY `idx_schedule_feedback_grade_course` (`campus_grade_id`, `course_id`),
  KEY `idx_schedule_feedback_teacher_time` (`teacher_id`, `feedback_time`),
  KEY `idx_schedule_feedback_status` (`execution_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='中心派课执行反馈表';

CREATE TABLE IF NOT EXISTS `schedule_advanced_batch` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '高级课批次ID',
  `batch_no` VARCHAR(50) NOT NULL COMMENT '批次编号',
  `batch_name` VARCHAR(100) NOT NULL COMMENT '批次名称',
  `course_id` BIGINT NOT NULL COMMENT '高级课程ID',
  `school_name` VARCHAR(100) DEFAULT NULL COMMENT '牵头校区',
  `plan_id` BIGINT DEFAULT NULL COMMENT '派遣计划ID',
  `batch_status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '批次状态',
  `expected_start_date` DATE DEFAULT NULL COMMENT '预计开始日期',
  `expected_end_date` DATE DEFAULT NULL COMMENT '预计结束日期',
  `actual_start_date` DATE DEFAULT NULL COMMENT '实际开始日期',
  `actual_end_date` DATE DEFAULT NULL COMMENT '实际结束日期',
  `capacity` INT DEFAULT NULL COMMENT '容量',
  `created_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT DEFAULT 0 COMMENT '软删除标记: 0-未删除,1-已删除',
  UNIQUE KEY `uk_schedule_advanced_batch_no` (`batch_no`),
  KEY `idx_schedule_advanced_batch_course` (`course_id`, `batch_status`),
  KEY `idx_schedule_advanced_batch_plan` (`plan_id`),
  KEY `idx_schedule_advanced_batch_school` (`school_name`, `batch_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='中心派课高级课批次表';

CREATE TABLE IF NOT EXISTS `schedule_advanced_batch_student` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `batch_id` BIGINT NOT NULL COMMENT '高级课批次ID',
  `student_id` BIGINT NOT NULL COMMENT '学生ID',
  `class_id` BIGINT DEFAULT NULL COMMENT '原班级ID',
  `campus_grade_id` BIGINT DEFAULT NULL COMMENT '来源校区年级ID',
  `join_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '入批状态',
  `eligibility_status` VARCHAR(20) NOT NULL DEFAULT 'UNCHECKED' COMMENT '前置校验状态',
  `eligibility_checked_time` DATETIME DEFAULT NULL COMMENT '校验时间',
  `ineligible_reason` VARCHAR(500) DEFAULT NULL COMMENT '不符合原因',
  `joined_time` DATETIME DEFAULT NULL COMMENT '加入时间',
  `exit_time` DATETIME DEFAULT NULL COMMENT '退出时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY `uk_schedule_batch_student` (`batch_id`, `student_id`),
  KEY `idx_schedule_batch_student_student` (`student_id`, `join_status`),
  KEY `idx_schedule_batch_student_class` (`class_id`),
  KEY `idx_schedule_batch_student_grade` (`campus_grade_id`),
  KEY `idx_schedule_batch_student_eligibility` (`eligibility_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='中心派课高级课学生名单表';

INSERT IGNORE INTO `schedule_course`
(`course_code`, `course_name`, `course_direction`, `course_level`, `course_type`, `teacher_capacity`, `sort_order`, `status`, `remark`)
VALUES
('HCIA', 'HCIA', 'NETWORK', 'BASIC', 'FIXED', 1, 10, 'ACTIVE', '基础课程'),
('PYTHON', 'Python', 'DEV', 'BASIC', 'FIXED', 1, 20, 'ACTIVE', '基础课程'),
('RHCSA', 'RHCSA', 'LINUX', 'BASIC', 'FIXED', 1, 30, 'ACTIVE', '基础课程'),
('WEB', 'Web', 'WEB', 'BASIC', 'FIXED', 1, 40, 'ACTIVE', '基础课程'),
('MYSQL', 'MySQL', 'DB', 'BASIC', 'FIXED', 1, 50, 'ACTIVE', '基础课程'),
('HCIP', 'HCIP', 'NETWORK', 'INTERMEDIATE', 'FIXED', 1, 110, 'ACTIVE', '中级课程'),
('RHCE', 'RHCE', 'LINUX', 'INTERMEDIATE', 'FIXED', 1, 120, 'ACTIVE', '中级课程'),
('PENETRATION_TEST', '渗透测试', 'SECURITY', 'ADVANCED', 'FIXED', 1, 210, 'ACTIVE', '高级课程'),
('DEFENSE_PROTECTION', '防御保护', 'SECURITY', 'ADVANCED', 'FIXED', 1, 220, 'ACTIVE', '高级课程'),
('CLOUD_COMPUTING', '云计算', 'CLOUD', 'ADVANCED', 'FIXED', 1, 230, 'ACTIVE', '高级课程');

INSERT IGNORE INTO `schedule_course_prerequisite`
(`course_id`, `prerequisite_course_id`, `rule_type`, `required_status`, `status`)
SELECT c.id, p.id, 'COURSE_COMPLETED', 'COMPLETED', 'ACTIVE'
FROM `schedule_course` c
JOIN `schedule_course` p ON p.course_code = 'HCIA'
WHERE c.course_code = 'HCIP';

INSERT IGNORE INTO `schedule_course_prerequisite`
(`course_id`, `prerequisite_course_id`, `rule_type`, `required_status`, `status`)
SELECT c.id, p.id, 'COURSE_COMPLETED', 'COMPLETED', 'ACTIVE'
FROM `schedule_course` c
JOIN `schedule_course` p ON p.course_code = 'RHCSA'
WHERE c.course_code = 'RHCE';

INSERT IGNORE INTO `schedule_course_prerequisite`
(`course_id`, `prerequisite_course_id`, `rule_type`, `required_status`, `status`)
SELECT adv.id, pre.id, 'COURSE_COMPLETED', 'COMPLETED', 'ACTIVE'
FROM `schedule_course` adv
JOIN `schedule_course` pre ON pre.course_level IN ('BASIC', 'INTERMEDIATE')
WHERE adv.course_level = 'ADVANCED';
