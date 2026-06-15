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
@TableName("schedule_advanced_batch")
public class ScheduleAdvancedBatch implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("batch_no")
    private String batchNo;
    @TableField("batch_name")
    private String batchName;
    @TableField("course_id")
    private Long courseId;
    @TableField("school_name")
    private String schoolName;
    @TableField("plan_id")
    private Long planId;
    @TableField("batch_status")
    private String batchStatus;
    @TableField("expected_start_date")
    private LocalDate expectedStartDate;
    @TableField("expected_end_date")
    private LocalDate expectedEndDate;
    @TableField("actual_start_date")
    private LocalDate actualStartDate;
    @TableField("actual_end_date")
    private LocalDate actualEndDate;
    @TableField("capacity")
    private Integer capacity;
    @TableField("created_by")
    private Long createdBy;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableField("is_deleted")
    @TableLogic
    private Byte isDeleted;
}
