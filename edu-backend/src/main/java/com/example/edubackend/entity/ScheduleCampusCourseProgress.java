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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@TableName("schedule_campus_course_progress")
public class ScheduleCampusCourseProgress implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("campus_grade_id")
    private Long campusGradeId;
    @TableField("course_id")
    private Long courseId;
    @TableField("expected_student_count")
    private Integer expectedStudentCount;
    @TableField("progress_status")
    private String progressStatus;
    @TableField("progress_percent")
    private BigDecimal progressPercent;
    @TableField("planned_start_date")
    private LocalDate plannedStartDate;
    @TableField("planned_end_date")
    private LocalDate plannedEndDate;
    @TableField("actual_start_date")
    private LocalDate actualStartDate;
    @TableField("actual_end_date")
    private LocalDate actualEndDate;
    @TableField("current_teacher_id")
    private Long currentTeacherId;
    @TableField("last_plan_id")
    private Long lastPlanId;
    @TableField("completion_confirmed_by")
    private Long completionConfirmedBy;
    @TableField("completion_confirmed_time")
    private LocalDateTime completionConfirmedTime;
    @TableField("remark")
    private String remark;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
