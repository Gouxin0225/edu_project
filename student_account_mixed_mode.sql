ALTER TABLE `sys_user`
    ADD COLUMN IF NOT EXISTS `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    ADD COLUMN IF NOT EXISTS `school_name` VARCHAR(100) DEFAULT NULL COMMENT '所属学校',
    ADD COLUMN IF NOT EXISTS `create_source` VARCHAR(30) DEFAULT NULL COMMENT '账号来源';

ALTER TABLE `sys_class`
    ADD COLUMN IF NOT EXISTS `school_name` VARCHAR(100) DEFAULT NULL COMMENT '所属学校',
    ADD COLUMN IF NOT EXISTS `allow_student_apply` TINYINT DEFAULT 1 COMMENT '是否允许学生申请加入: 1-允许,0-不允许';

CREATE UNIQUE INDEX IF NOT EXISTS `uk_sys_user_phone` ON `sys_user` (`phone`);
CREATE INDEX IF NOT EXISTS `idx_sys_user_phone_role` ON `sys_user` (`phone`, `role`);
CREATE INDEX IF NOT EXISTS `idx_sys_class_school_apply` ON `sys_class` (`school_name`, `allow_student_apply`, `status`);

CREATE TABLE IF NOT EXISTS `class_student_rel` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `class_id` BIGINT NOT NULL COMMENT '班级ID',
    `student_id` BIGINT NOT NULL COMMENT '学生用户ID',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '关系状态: ACTIVE, LEFT, GRADUATED',
    `join_source` VARCHAR(30) DEFAULT NULL COMMENT '加入来源: SELF_APPLY, ADMIN_IMPORT, ASSISTANT_IMPORT, ADMIN_ADD, ASSISTANT_ADD, LEGACY',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人/审核人ID',
    `join_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `leave_time` DATETIME DEFAULT NULL COMMENT '离开时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_class_student` (`class_id`, `student_id`),
    KEY `idx_student_status` (`student_id`, `status`),
    KEY `idx_class_status` (`class_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生班级关系表';

CREATE TABLE IF NOT EXISTS `class_join_application` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `class_id` BIGINT NOT NULL COMMENT '班级ID',
    `student_id` BIGINT NOT NULL COMMENT '学生用户ID',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '申请状态: PENDING, APPROVED, REJECTED, CANCELED',
    `apply_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
    `auditor_id` BIGINT DEFAULT NULL COMMENT '审核人ID',
    `reject_reason` VARCHAR(500) DEFAULT NULL COMMENT '拒绝原因',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_student_class_status` (`student_id`, `class_id`, `status`),
    KEY `idx_class_status_time` (`class_id`, `status`, `apply_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生入班申请表';

INSERT IGNORE INTO `class_student_rel` (`class_id`, `student_id`, `status`, `join_source`, `created_by`, `join_time`)
SELECT `class_id`, `id`, 'ACTIVE', 'LEGACY', `created_by`, COALESCE(`create_time`, NOW())
FROM `sys_user`
WHERE `role` = 'STUDENT' AND `class_id` IS NOT NULL AND COALESCE(`is_deleted`, 0) = 0;
