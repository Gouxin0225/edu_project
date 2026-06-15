package com.example.edubackend.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("schedule_dispatch_feedback")
public class ScheduleDispatchFeedback implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("plan_id")
    private Long planId;
    @TableField("campus_grade_id")
    private Long campusGradeId;
    @TableField("course_id")
    private Long courseId;
    @TableField("teacher_id")
    private Long teacherId;
    @TableField("feedback_type")
    private String feedbackType;
    @TableField("execution_status")
    private String executionStatus;
    @TableField("progress_percent")
    private BigDecimal progressPercent;
    @TableField("feedback_content")
    private String feedbackContent;
    @TableField("issue_desc")
    private String issueDesc;
    @TableField("next_action")
    private String nextAction;
    @TableField("feedback_time")
    private LocalDateTime feedbackTime;
    @TableField("created_by")
    private Long createdBy;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
