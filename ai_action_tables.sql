CREATE TABLE IF NOT EXISTS student_review_note (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '笔记ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    source_message_id BIGINT NULL COMMENT '来源AI消息ID',
    course_category VARCHAR(50) NOT NULL COMMENT '课程方向',
    knowledge_point VARCHAR(100) NOT NULL COMMENT '知识点',
    title VARCHAR(200) NOT NULL COMMENT '笔记标题',
    content TEXT NOT NULL COMMENT '笔记内容',
    source_type VARCHAR(50) NOT NULL DEFAULT 'AI_CHAT' COMMENT '来源类型',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT '软删除标记',
    INDEX idx_student_course (student_id, course_category),
    INDEX idx_source_message (source_message_id)
) COMMENT='学生复习笔记表';
