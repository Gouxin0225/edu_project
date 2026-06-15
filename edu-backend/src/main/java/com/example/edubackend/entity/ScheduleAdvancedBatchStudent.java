package com.example.edubackend.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("schedule_advanced_batch_student")
public class ScheduleAdvancedBatchStudent implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("batch_id")
    private Long batchId;
    @TableField("student_id")
    private Long studentId;
    @TableField("class_id")
    private Long classId;
    @TableField("campus_grade_id")
    private Long campusGradeId;
    @TableField("join_status")
    private String joinStatus;
    @TableField("eligibility_status")
    private String eligibilityStatus;
    @TableField("eligibility_checked_time")
    private LocalDateTime eligibilityCheckedTime;
    @TableField("ineligible_reason")
    private String ineligibleReason;
    @TableField("joined_time")
    private LocalDateTime joinedTime;
    @TableField("exit_time")
    private LocalDateTime exitTime;
    @TableField("remark")
    private String remark;
    @TableField("created_by")
    private Long createdBy;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
