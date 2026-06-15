CREATE TABLE IF NOT EXISTS student_visit_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '回访记录ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    class_id BIGINT NOT NULL COMMENT '班级ID',
    visitor_id BIGINT NOT NULL COMMENT '回访人ID',
    visitor_role VARCHAR(20) NOT NULL COMMENT '回访人角色: TEACHER, ASSISTANT',
    visit_method VARCHAR(30) NOT NULL COMMENT '回访方式: PHONE, WECHAT, OFFLINE, ONLINE, OTHER',
    visit_result VARCHAR(30) NOT NULL COMMENT '回访结果: REACHED, UNREACHED, NEED_FOLLOW, RESOLVED',
    content TEXT NOT NULL COMMENT '回访内容',
    next_follow_time DATETIME NULL COMMENT '下次跟进时间',
    visit_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '回访时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记',
    INDEX idx_student_time (student_id, visit_time),
    INDEX idx_class_time (class_id, visit_time),
    INDEX idx_visitor_time (visitor_id, visit_time)
) COMMENT='学员回访记录表';
