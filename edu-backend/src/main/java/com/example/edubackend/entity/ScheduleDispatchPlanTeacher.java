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
@TableName("schedule_dispatch_plan_teacher")
public class ScheduleDispatchPlanTeacher implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("plan_id")
    private Long planId;
    @TableField("teacher_id")
    private Long teacherId;
    @TableField("teacher_role")
    private String teacherRole;
    @TableField("match_score")
    private BigDecimal matchScore;
    @TableField("match_reason")
    private String matchReason;
    @TableField("assign_status")
    private String assignStatus;
    @TableField("confirm_time")
    private LocalDateTime confirmTime;
    @TableField("reject_reason")
    private String rejectReason;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
