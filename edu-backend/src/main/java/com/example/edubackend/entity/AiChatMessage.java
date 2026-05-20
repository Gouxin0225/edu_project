package com.example.edubackend.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("ai_chat_message")
public class AiChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("session_id")
    private Long sessionId;

    @TableField("user_id")
    private Long userId;

    @TableField("user_role")
    private String userRole;

    @TableField("message_role")
    private String messageRole;

    @TableField("content")
    private String content;

    @TableField("course_category")
    private String courseCategory;

    @TableField("knowledge_point")
    private String knowledgePoint;

    @TableField("mode")
    private String mode;

    @TableField("intent")
    private String intent;

    @TableField("response_source")
    private String responseSource;

    @TableField("source_summary")
    private String sourceSummary;

    @TableField("model")
    private String model;

    @TableField("status")
    private String status;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField("is_deleted")
    @TableLogic
    private Byte isDeleted;
}
