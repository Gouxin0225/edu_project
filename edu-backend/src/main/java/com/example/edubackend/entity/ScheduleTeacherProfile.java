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
@TableName("schedule_teacher_profile")
public class ScheduleTeacherProfile implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("teacher_id")
    private Long teacherId;
    @TableField("home_school_name")
    private String homeSchoolName;
    @TableField("dispatch_mode")
    private String dispatchMode;
    @TableField("dispatch_status")
    private String dispatchStatus;
    @TableField("max_parallel_plans")
    private Integer maxParallelPlans;
    @TableField("max_monthly_dispatch_days")
    private Integer maxMonthlyDispatchDays;
    @TableField("can_travel")
    private Byte canTravel;
    @TableField("allow_cross_school")
    private Byte allowCrossSchool;
    @TableField("is_center_reserve")
    private Byte isCenterReserve;
    @TableField("priority")
    private Integer priority;
    @TableField("available_remark")
    private String availableRemark;
    @TableField("status")
    private String status;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
