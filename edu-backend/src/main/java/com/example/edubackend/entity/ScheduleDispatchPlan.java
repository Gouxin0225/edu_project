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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("schedule_dispatch_plan")
public class ScheduleDispatchPlan implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("plan_no")
    private String planNo;
    @TableField("campus_grade_id")
    private Long campusGradeId;
    @TableField("course_id")
    private Long courseId;
    @TableField("plan_type")
    private String planType;
    @TableField("plan_status")
    private String planStatus;
    @TableField("priority")
    private String priority;
    @TableField("expected_start_date")
    private LocalDate expectedStartDate;
    @TableField("expected_end_date")
    private LocalDate expectedEndDate;
    @TableField("dispatch_reason")
    private String dispatchReason;
    @TableField("requirement_desc")
    private String requirementDesc;
    @TableField("created_by")
    private Long createdBy;
    @TableField("submitted_time")
    private LocalDateTime submittedTime;
    @TableField("approved_by")
    private Long approvedBy;
    @TableField("approved_time")
    private LocalDateTime approvedTime;
    @TableField("cancel_reason")
    private String cancelReason;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableField("is_deleted")
    @TableLogic
    private Byte isDeleted;
}
